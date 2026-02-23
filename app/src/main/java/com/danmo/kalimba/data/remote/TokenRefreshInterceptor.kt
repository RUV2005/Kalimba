// 文件位置: data/remote/TokenRefreshInterceptor.kt
package com.danmo.kalimba.data.remote

import android.util.Log
import com.danmo.kalimba.data.local.AuthDataStore
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class TokenRefreshInterceptor(
    private val authDataStore: AuthDataStore,
    private val authApiService: AuthApiService
) : Interceptor {

    companion object {
        private const val TAG = "TokenRefreshInterceptor"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // ✅ 检查是否需要刷新Token
        val needsRefresh = runBlocking {
            authDataStore.isTokenExpiring()
        }

        if (needsRefresh) {
            Log.d(TAG, "⏰ Token即将过期，开始自动刷新...")

            try {
                val refreshToken = runBlocking {
                    authDataStore.getRefreshToken()
                }

                if (refreshToken != null) {
                    Log.d(TAG, "🔄 调用刷新接口...")

                    val result = runBlocking {
                        authApiService.refreshToken(refreshToken)
                    }

                    result.onSuccess { response ->
                        if (response.success && response.accessToken != null) {
                            Log.d(TAG, "✅ Token刷新成功")

                            runBlocking {
                                if (response.refreshToken != null) {
                                    val userInfo = authDataStore.getUserInfo()
                                    if (userInfo != null) {
                                        authDataStore.saveAuth(
                                            token = response.accessToken,
                                            refreshToken = response.refreshToken,
                                            user = userInfo,
                                            expiresIn = response.expiresIn ?: 7200
                                        )
                                        Log.d(TAG, "💾 AccessToken 和 RefreshToken 均已更新")
                                    }
                                } else {
                                    authDataStore.updateAccessToken(
                                        response.accessToken,
                                        response.expiresIn ?: 7200
                                    )
                                    Log.d(TAG, "💾 AccessToken 已更新")
                                }
                            }

                            val newRequest = originalRequest.newBuilder()
                                .header("Authorization", "Bearer ${response.accessToken}")
                                .build()

                            Log.d(TAG, "🔁 使用新Token重新发送请求")
                            return chain.proceed(newRequest)
                        } else {
                            Log.e(TAG, "❌ Token刷新失败: ${response.error}")
                        }
                    }.onFailure { error ->
                        Log.e(TAG, "❌ Token刷新异常: ${error.message}")

                        // ✅ 检查是否是401错误（RefreshToken过期）
                        if (error.message?.contains("401") == true) {
                            Log.w(TAG, "⚠️ RefreshToken已过期，需要重新登录")
                        }
                    }
                } else {
                    Log.e(TAG, "❌ RefreshToken不存在")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 刷新Token时出错", e)
            }

            // ✅ 刷新失败，清除认证信息
            Log.w(TAG, "⚠️ Token刷新失败，清除本地认证信息")
            runBlocking {
                authDataStore.clearAuth()
            }

            // ✅ 返回自定义错误，让上层处理
            throw TokenExpiredException("登录已过期，请重新登录")
        }

        // Token未过期，直接发送请求
        return chain.proceed(originalRequest)
    }
}

/**
 * ✅ 自定义异常：Token过期
 */
class TokenExpiredException(message: String) : IOException(message)