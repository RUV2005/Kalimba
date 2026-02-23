// 文件名: data/remote/AuthApiService.kt
package com.danmo.kalimba.data.remote

import android.util.Log
import com.danmo.kalimba.data.local.UserInfo
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit

class AuthApiService(private val baseUrl: String) {

    private val client: OkHttpClient
    private val gson = Gson()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    companion object {
        private const val TAG = "AuthApiService"
    }

    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // ==================== 验证码 API ====================

    suspend fun getImageCaptcha(): Result<CaptchaResponse> {
        Log.d(TAG, "请求图形验证码: ${baseUrl}captcha/image")
        return executePost("${baseUrl}captcha/image", emptyMap<String, String>())
    }

    suspend fun getAudioCaptcha(): Result<CaptchaResponse> {
        Log.d(TAG, "请求语音验证码: ${baseUrl}captcha/audio")
        return executePost("${baseUrl}captcha/audio", emptyMap<String, String>())
    }

    // ==================== 认证 API ====================

    suspend fun login(request: LoginRequest): Result<LoginResponse> {
        Log.d(TAG, "登录请求: username=${request.username}")
        return executePost("${baseUrl}auth/login", request)
    }

    suspend fun register(request: RegisterRequest): Result<RegisterResponse> {
        return executePost("${baseUrl}auth/register", request)
    }

    /**
     * ✅ 刷新 Token（返回新的 RefreshToken）
     */
    suspend fun refreshToken(refreshToken: String): Result<RefreshTokenResponse> {
        Log.d(TAG, "刷新Token请求")
        return executePost(
            "${baseUrl}auth/refresh",
            RefreshTokenRequest(refreshToken)
        )
    }

    // ==================== HTTP 工具方法 ====================

    private inline fun <reified T> executePost(url: String, body: Any): Result<T> {
        return try {
            val json = gson.toJson(body)
            Log.d(TAG, "POST $url, Body: $json")

            val requestBody = json.toRequestBody(JSON)

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            parseResponse(response)
        } catch (e: IOException) {
            Log.e(TAG, "IO错误", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "请求错误", e)
            Result.failure(e)
        }
    }

    private inline fun <reified T> parseResponse(response: Response): Result<T> {
        return try {
            val bodyString = response.body?.string()
            Log.d(TAG, "响应码: ${response.code}, Body: $bodyString")

            if (response.isSuccessful && bodyString != null) {
                val result = gson.fromJson(bodyString, T::class.java)
                Result.success(result)
            } else {
                Result.failure(
                    IOException("HTTP ${response.code}: ${response.message}")
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析响应失败", e)
            Result.failure(e)
        } finally {
            response.close()
        }
    }

    fun close() {
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }
}

// ==================== 请求数据类 ====================

data class LoginRequest(
    val username: String,
    val password: String,
    val captcha: String,
    val captchaToken: String,
    val captchaType: String = "IMAGE"
)

data class RegisterRequest(
    val username: String,
    val password: String,
    val nickname: String?,
    val email: String?,
    val captcha: String,
    val captchaToken: String
)

data class RefreshTokenRequest(
    val refreshToken: String
)

// ==================== 响应数据类 ====================

data class CaptchaResponse(
    val success: Boolean,
    val captchaToken: String,
    val captchaData: String?,
    val codeForTTS: String? = null,
    val code: String? = null,
    val error: String? = null,
    val expiresIn: Int? = null
)

data class LoginResponse(
    val success: Boolean,
    @SerializedName("accessToken") val accessToken: String? = null,
    val token: String? = null,
    val refreshToken: String? = null,
    val expiresIn: Int? = null,
    val user: ServerUserInfo? = null,
    val error: String? = null
)

data class ServerUserInfo(
    val id: Int,
    val username: String,
    val nickname: String?,
    val email: String?,
    val avatar: String?,
    val createdAt: String?
)

data class RegisterResponse(
    val success: Boolean,
    val message: String?,
    val user: ServerUserInfo? = null,
    val error: String? = null
)

/**
 * ✅ 刷新Token响应（必须包含 refreshToken 字段）
 */
data class RefreshTokenResponse(
    val success: Boolean,
    @SerializedName("accessToken") val accessToken: String? = null,
    val token: String? = null,
    val refreshToken: String? = null,  // ✅ 关键：新的 RefreshToken
    val expiresIn: Int? = null,
    val error: String? = null
)

enum class CaptchaType {
    IMAGE,
    AUDIO;

    override fun toString(): String {
        return name
    }
}