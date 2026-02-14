package com.danmo.kalimba.ui.auth

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.danmo.kalimba.R
import com.danmo.kalimba.accessibility.AccessibilityHelper
import com.danmo.kalimba.accessibility.VibrationType
import com.danmo.kalimba.data.remote.CaptchaType
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    baseUrl: String = "http://192.168.31.99:3000/api/" // 已根据日志自动匹配 IP
) {
    val context = LocalContext.current
    val viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory(context, baseUrl))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val accessibilityHelper = remember { AccessibilityHelper(context) }

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) onLoginSuccess()
    }

    LaunchedEffect(Unit) {
        viewModel.loadCaptcha()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("登录") },
                navigationIcon = {
                    IconButton(onClick = {
                        accessibilityHelper.vibrate(VibrationType.CLICK)
                        onNavigateBack()
                    }) {
                        Icon(painterResource(R.drawable.ic_arrow_back), "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            Text("欢迎使用卡林巴琴", style = MaterialTheme.typography.headlineMedium)

            OutlinedTextField(
                value = uiState.username,
                onValueChange = viewModel::onUsernameChange,
                label = { Text("用户名") },
                modifier = Modifier.fillMaxWidth()
            )

            var passwordVisible by remember { mutableStateOf(false) }
            OutlinedTextField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                label = { Text("密码") },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(painterResource(if (passwordVisible) R.drawable.ic_visibility else R.drawable.ic_visibility_off), null)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            CaptchaSection(
                uiState = uiState,
                onCaptchaChange = viewModel::onCaptchaChange,
                onSwitchType = viewModel::switchCaptchaType,
                onRefresh = viewModel::loadCaptcha,
                accessibilityHelper = accessibilityHelper,
                context = context
            )

            if (uiState.errorMessage != null) {
                Text(uiState.errorMessage!!, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { viewModel.login() },
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (uiState.isLoading) CircularProgressIndicator(Modifier.size(24.dp)) else Text("登录")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CaptchaSection(
    uiState: AuthUiState,
    onCaptchaChange: (String) -> Unit,
    onSwitchType: () -> Unit,
    onRefresh: () -> Unit,
    accessibilityHelper: AccessibilityHelper,
    context: Context
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(if (uiState.captchaType == CaptchaType.IMAGE) "图形验证码" else "语音验证码")
            TextButton(onClick = onSwitchType) {
                Text(if (uiState.captchaType == CaptchaType.IMAGE) "切换语音" else "切换图形")
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp)
                    .clickable(enabled = !uiState.isLoading) { onRefresh() }
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(Modifier.size(32.dp))
                    } else {
                        // 根据类型显示不同内容
                        if (uiState.captchaType == CaptchaType.IMAGE) {
                            if (uiState.captchaImageSource != null) {
                                AsyncImage(
                                    model = uiState.captchaImageSource,
                                    contentDescription = "验证码",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                    error = painterResource(R.drawable.ic_error)
                                )
                            } else {
                                Text("点击刷新图片", style = MaterialTheme.typography.bodySmall)
                            }
                        } else {
                            // 语音验证码显示播放器
                            AudioCaptchaPlayer(uiState.captchaAudioUrl, accessibilityHelper, context)
                        }
                    }
                }
            }
            IconButton(onClick = onRefresh, enabled = !uiState.isLoading) {
                Icon(painterResource(R.drawable.ic_refresh), "刷新")
            }
        }

        OutlinedTextField(
            value = uiState.captchaInput,
            onValueChange = onCaptchaChange,
            label = { Text("请输入验证码") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AudioCaptchaPlayer(audioUrl: String?, accessibilityHelper: AccessibilityHelper, context: Context) {
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // 释放资源
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .clickable {
                if (audioUrl == null || isPlaying) return@clickable

                try {
                    accessibilityHelper.vibrate(VibrationType.CLICK)
                    mediaPlayer?.release()
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(audioUrl)
                        prepareAsync()
                        setOnPreparedListener {
                            start()
                            isPlaying = true
                        }
                        setOnCompletionListener {
                            isPlaying = false
                            release()
                        }
                        setOnErrorListener { _, _, _ ->
                            isPlaying = false
                            false
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AudioPlayer", "播放失败", e)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (audioUrl == null) {
            Text("暂无语音", style = MaterialTheme.typography.bodySmall)
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painterResource(if (isPlaying) R.drawable.ic_volume_up else R.drawable.ic_play),
                    contentDescription = null,
                    Modifier.size(40.dp),
                    tint = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(if (isPlaying) "播放中..." else "点击播放", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}