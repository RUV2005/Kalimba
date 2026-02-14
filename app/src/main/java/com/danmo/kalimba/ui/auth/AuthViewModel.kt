// 文件名: ui/auth/AuthViewModel.kt
package com.danmo.kalimba.ui.auth

import android.content.Context
import android.media.MediaPlayer
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.danmo.kalimba.accessibility.AccessibilityHelper
import com.danmo.kalimba.data.local.AuthDataStore
import com.danmo.kalimba.data.local.UserInfo
import com.danmo.kalimba.data.remote.AuthApiService
import com.danmo.kalimba.data.remote.CaptchaType
import com.danmo.kalimba.data.remote.LoginRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * 认证 UI 状态
 */
data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val username: String = "",
    val password: String = "",
    val captchaInput: String = "",
    val captchaType: CaptchaType = CaptchaType.IMAGE,
    val captchaToken: String = "",
    val captchaImageSource: Any? = null,
    val captchaAudioUrl: String? = null,
    val errorMessage: String? = null,
    val userInfo: UserInfo? = null
)

class AuthViewModel(
    private val apiService: AuthApiService,
    private val authDataStore: AuthDataStore,
    private val accessibilityHelper: AccessibilityHelper,
    private val context: Context // 用于获取 cacheDir
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null

    companion object {
        private const val TAG = "AuthViewModel"
    }

    init {
        checkLoginStatus()
    }

    // --- 音频播放逻辑开始 ---

    /**
     * 将 Base64 音频转存为临时文件并播放
     */
    fun playVoiceCaptcha(base64Audio: String?) {
        if (base64Audio.isNullOrEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. 去掉前缀
                val cleanBase64 = if (base64Audio.contains(",")) {
                    base64Audio.split(",")[1]
                } else {
                    base64Audio
                }

                // 2. 解码
                val audioBytes = Base64.decode(cleanBase64, Base64.DEFAULT)

                // 3. 写入临时文件
                val tempFile = File.createTempFile("captcha_voice", ".mp3", context.cacheDir)
                tempFile.deleteOnExit()

                FileOutputStream(tempFile).use { fos ->
                    fos.write(audioBytes)
                }

                // 4. 切回主线程播放
                withContext(Dispatchers.Main) {
                    startPlaying(tempFile.absolutePath)
                }
            } catch (e: Exception) {
                Log.e(TAG, "音频转换失败: ${e.message}")
            }
        }
    }

    private fun startPlaying(filePath: String) {
        stopPlaying()
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(filePath)
                prepareAsync()
                setOnPreparedListener { it.start() }
                setOnCompletionListener { stopPlaying() }
                setOnErrorListener { _, _, _ ->
                    stopPlaying()
                    false
                }
            } catch (e: IOException) {
                Log.e(TAG, "播放失败: ${e.message}")
            }
        }
    }

    fun stopPlaying() {
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null
    }

    // --- 音频播放逻辑结束 ---

    private fun checkLoginStatus() {
        viewModelScope.launch {
            // ✅ 使用 withContext(Dispatchers.IO) 确保磁盘读取不阻塞主线程
            val isLoggedIn = withContext(Dispatchers.IO) {
                authDataStore.checkIsLoggedIn()
            }
            if (isLoggedIn) {
                val user = withContext(Dispatchers.IO) {
                    authDataStore.getUserInfo()
                }
                // 切回主线程更新 UI
                _uiState.value = _uiState.value.copy(isLoggedIn = true, userInfo = user)
            }
        }
    }

    fun loadCaptcha() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                // 1. 在 IO 线程获取数据并完成耗时的解码
                val result = withContext(Dispatchers.IO) {
                    val apiResult = if (_uiState.value.captchaType == CaptchaType.IMAGE) {
                        apiService.getImageCaptcha()
                    } else {
                        apiService.getAudioCaptcha()
                    }

                    // 预处理数据：在 IO 线程解码图像，避免主线程计算
                    apiResult.map { response ->
                        var bitmap: android.graphics.Bitmap? = null
                        if (response.success && response.captchaData != null && _uiState.value.captchaType == CaptchaType.IMAGE) {
                            val base64Data = response.captchaData.substringAfter(",")
                            val imageBytes = Base64.decode(base64Data, Base64.DEFAULT)
                            bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        }
                        Pair(response, bitmap)
                    }
                }

                // 2. 回到主线程更新 UI
                result.onSuccess { (response, bitmap) ->
                    if (response.success && response.captchaData != null) {
                        if (_uiState.value.captchaType == CaptchaType.IMAGE) {
                            _uiState.value = _uiState.value.copy(
                                captchaToken = response.captchaToken,
                                captchaImageSource = bitmap,
                                isLoading = false
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(
                                captchaToken = response.captchaToken,
                                captchaAudioUrl = response.captchaData,
                                isLoading = false
                            )
                            playVoiceCaptcha(response.captchaData)
                            accessibilityHelper.speak("语音验证码已加载并开始播放")
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = response.error ?: "获取失败",
                            isLoading = false
                        )
                    }
                }.onFailure { e ->
                    Log.e(TAG, "验证码加载失败", e)
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "网络错误：${e.message}",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "验证码加载异常", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "加载异常",
                    isLoading = false
                )
            }
        }
    }

    fun switchCaptchaType() {
        val newType = if (_uiState.value.captchaType == CaptchaType.IMAGE) CaptchaType.AUDIO else CaptchaType.IMAGE
        stopPlaying() // 切换时停止播放
        _uiState.value = _uiState.value.copy(captchaType = newType, captchaInput = "", captchaImageSource = null)
        loadCaptcha()
        accessibilityHelper.speak(if (newType == CaptchaType.AUDIO) "已切换到语音" else "已切换到图形")
    }

    /**
     * 更新用户名
     */
    fun onUsernameChange(value: String) {
        _uiState.value = _uiState.value.copy(username = value)
    }

    /**
     * 更新密码
     */
    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value)
    }

    /**
     * 更新验证码输入
     */
    fun onCaptchaChange(value: String) {
        _uiState.value = _uiState.value.copy(captchaInput = value)
    }

    /**
     * 登录
     */
    /**
     * 登录
     */
    fun login() {
        val state = _uiState.value

        // 验证输入
        if (state.username.isBlank()) {
            _uiState.value = state.copy(errorMessage = "请输入用户名")
            return
        }

        if (state.password.isBlank()) {
            _uiState.value = state.copy(errorMessage = "请输入密码")
            return
        }

        if (state.captchaInput.isBlank()) {
            _uiState.value = state.copy(errorMessage = "请输入验证码")
            return
        }

        if (state.captchaToken.isBlank()) {
            _uiState.value = state.copy(errorMessage = "请先获取验证码")
            loadCaptcha()
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(
                isLoading = true,
                errorMessage = null
            )

            try {
                val request = LoginRequest(
                    username = state.username,
                    password = state.password,
                    captcha = state.captchaInput,
                    captchaToken = state.captchaToken,
                    captchaType = state.captchaType.toString()  // ✅ 转为字符串
                )

                Log.d(TAG, "发送登录请求: username=${request.username}, captchaType=${request.captchaType}")

                // ✅ 核心修复：使用 withContext(Dispatchers.IO) 确保同步网络请求不在 UI 线程执行
                val result = withContext(Dispatchers.IO) {
                    apiService.login(request)
                }

                result.onSuccess { response ->
                    Log.d(TAG, "登录响应: success=${response.success}")

                    // ✅ 兼容服务器返回的字段名
                    val accessToken = response.accessToken ?: response.token

                    if (response.success && accessToken != null && response.user != null) {
                        // ✅ 转换服务器的 UserInfo 为本地 UserInfo
                        val localUser = UserInfo(
                            id = response.user.id.toString(),  // ✅ Int 转 String
                            username = response.user.username,
                            nickname = response.user.nickname,
                            email = response.user.email,
                            avatar = response.user.avatar
                        )

                        // ✅ 保存认证信息（涉及磁盘 IO，切到 IO 线程处理）
                        withContext(Dispatchers.IO) {
                            authDataStore.saveAuth(
                                token = accessToken,
                                refreshToken = response.refreshToken ?: "",
                                user = localUser
                            )
                        }

                        // 回到主线程更新 UI 状态
                        _uiState.value = _uiState.value.copy(
                            isLoggedIn = true,
                            isLoading = false,
                            userInfo = localUser,
                            errorMessage = null
                        )

                        val welcomeMsg = "登录成功，欢迎 ${localUser.nickname ?: localUser.username}"
                        accessibilityHelper.speak(welcomeMsg)
                        Log.d(TAG, welcomeMsg)
                    } else {
                        val errorMsg = response.error ?: "登录失败"
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = errorMsg
                        )

                        // 登录失败，刷新验证码
                        loadCaptcha()
                        Log.w(TAG, "登录失败: $errorMsg")
                    }
                }.onFailure { e ->
                    Log.e(TAG, "登录请求失败", e)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "登录失败：${e.message}"
                    )
                    // 失败后也刷新验证码
                    loadCaptcha()
                }
            } catch (e: Exception) {
                Log.e(TAG, "登录异常", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "登录出错：${e.message}"
                )
            }
        }
    }

    /**
     * 退出登录
     */
    fun logout() {
        viewModelScope.launch {
            // ✅ 显式在 IO 线程清除数据
            withContext(Dispatchers.IO) {
                authDataStore.clearAuth()
            }
            // 重置 UI 状态
            _uiState.value = AuthUiState()
            accessibilityHelper.speak("已退出登录")
            Log.d(TAG, "已退出登录")
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPlaying()
        // ✅ 在 IO 线程执行关闭逻辑
        viewModelScope.launch(Dispatchers.IO) {
            try {
                apiService.close()
            } catch (e: Exception) {
                Log.e(TAG, "关闭 ApiService 异常: ${e.message}")
            }
        }
    }

    /**
     * ViewModel 工厂 - ✅ 已修复参数传递
     */
    class Factory(
        private val context: Context,
        private val baseUrl: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val apiService = AuthApiService(baseUrl)
            val authDataStore = AuthDataStore(context)
            val accessibilityHelper = AccessibilityHelper(context)
            // ✅ 传入 context 参数
            return AuthViewModel(apiService, authDataStore, accessibilityHelper, context) as T
        }
    }
}