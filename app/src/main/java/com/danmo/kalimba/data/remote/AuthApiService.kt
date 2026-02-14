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

/**
 * 认证 API 服务 - 完全适配微服务架构
 *
 * 架构说明：
 * - API Gateway: http://your-ip:3000/api/
 * - Captcha Service: POST /api/captcha/image, POST /api/captcha/audio
 * - User Service: POST /api/auth/login
 */
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

    /**
     * 获取图形验证码
     * ⚠️ 服务端使用 POST，不是 GET
     */
    suspend fun getImageCaptcha(): Result<CaptchaResponse> {
        Log.d(TAG, "请求图形验证码: ${baseUrl}captcha/image")
        return executePost("${baseUrl}captcha/image", emptyMap<String, String>())
    }

    /**
     * 获取语音验证码
     * ⚠️ 服务端使用 POST，不是 GET
     */
    suspend fun getAudioCaptcha(): Result<CaptchaResponse> {
        Log.d(TAG, "请求语音验证码: ${baseUrl}captcha/audio")
        return executePost("${baseUrl}captcha/audio", emptyMap<String, String>())
    }

    // ==================== 认证 API ====================

    /**
     * 登录
     */
    suspend fun login(request: LoginRequest): Result<LoginResponse> {
        Log.d(TAG, "登录请求: username=${request.username}")
        return executePost("${baseUrl}auth/login", request)
    }

    /**
     * 注册
     */
    suspend fun register(request: RegisterRequest): Result<RegisterResponse> {
        return executePost("${baseUrl}auth/register", request)
    }

    /**
     * 刷新 Token
     */
    suspend fun refreshToken(refreshToken: String): Result<RefreshTokenResponse> {
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
    val captchaType: String = "IMAGE"  // ✅ 必须有这个参数
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

/**
 * 验证码响应
 */
data class CaptchaResponse(
    val success: Boolean,
    val captchaToken: String,
    val captchaData: String?,        // JPG Data URI
    val codeForTTS: String? = null,
    val code: String? = null,
    val error: String? = null,
    val expiresIn: Int? = null
)

/**
 * 登录响应
 */
data class LoginResponse(
    val success: Boolean,
    @SerializedName("accessToken") val accessToken: String? = null,  // ✅ 服务器字段名
    val token: String? = null,                                       // ✅ 兼容旧版
    val refreshToken: String? = null,
    val expiresIn: Int? = null,
    val user: ServerUserInfo? = null,  // ✅ 服务器用户结构
    val error: String? = null
) {
    /**
     * 服务器返回的用户信息结构
     */
    data class ServerUserInfo(
        val id: Int,              // ✅ 服务器返回 Int
        val username: String,
        val nickname: String?,
        val email: String?,
        val avatar: String?,      // ✅ 服务器字段名
        val createdAt: String?    // ✅ 服务器字段名（驼峰）
    )
}

/**
 * 服务端用户信息格式
 * ⚠️ 服务端返回的 user.id 是 Int，不是 String
 */
data class ServerUserInfo(
    val id: Int,                    // ⚠️ 服务端返回 Int
    val username: String,
    val nickname: String?,
    val email: String?,
    val avatar: String?,            // avatar_url -> avatar
    val createdAt: String?          // created_at -> createdAt (Gson 自动驼峰转换)
)

/**
 * 注册响应
 */
data class RegisterResponse(
    val success: Boolean,
    val message: String?,
    val user: ServerUserInfo? = null,
    val error: String? = null
)

data class RefreshTokenResponse(
    val success: Boolean,
    @SerializedName("accessToken") val accessToken: String? = null,
    val token: String? = null,
    val error: String? = null
)

/**
 * 验证码类型枚举
 */
enum class CaptchaType {
    IMAGE,
    AUDIO;

    override fun toString(): String {
        return name
    }
}