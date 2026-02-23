// 文件位置: data/local/AuthDataStore.kt
package com.danmo.kalimba.data.local

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.authDataStore by preferencesDataStore(name = "auth_prefs")

class AuthDataStore(private val context: Context) {

    private val gson = Gson()

    private object Keys {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val USER_INFO = stringPreferencesKey("user_info")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val TOKEN_EXPIRES_AT = longPreferencesKey("token_expires_at")
    }

    companion object {
        private const val TAG = "AuthDataStore"
    }

    val isLoggedIn: Flow<Boolean> = context.authDataStore.data.map {
        it[Keys.IS_LOGGED_IN] ?: false
    }

    val accessToken: Flow<String?> = context.authDataStore.data.map {
        it[Keys.ACCESS_TOKEN]
    }

    val refreshToken: Flow<String?> = context.authDataStore.data.map {
        it[Keys.REFRESH_TOKEN]
    }

    val userInfo: Flow<UserInfo?> = context.authDataStore.data.map { prefs ->
        prefs[Keys.USER_INFO]?.let {
            try {
                gson.fromJson(it, UserInfo::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * ✅ 保存认证信息（包含过期时间）
     */
    suspend fun saveAuth(token: String, refreshToken: String, user: UserInfo, expiresIn: Int = 7200) {
        context.authDataStore.edit { prefs ->
            prefs[Keys.ACCESS_TOKEN] = token
            prefs[Keys.REFRESH_TOKEN] = refreshToken
            prefs[Keys.USER_INFO] = gson.toJson(user)
            prefs[Keys.IS_LOGGED_IN] = true

            // ✅ 计算过期时间（提前5分钟刷新）
            val expiresAt = System.currentTimeMillis() + ((expiresIn - 300) * 1000L)
            prefs[Keys.TOKEN_EXPIRES_AT] = expiresAt

            Log.d(TAG, "✅ Token已保存，将在 ${expiresIn}秒 后过期")
        }
    }

    /**
     * ✅ 检查Token是否即将过期（需要刷新）
     */
    suspend fun isTokenExpiring(): Boolean {
        val expiresAt = context.authDataStore.data.first()[Keys.TOKEN_EXPIRES_AT] ?: 0L
        val now = System.currentTimeMillis()

        val isExpiring = now >= expiresAt

        if (isExpiring) {
            Log.d(TAG, "⏰ Token即将过期 (过期时间: $expiresAt, 当前时间: $now)")
        }

        return isExpiring
    }

    /**
     * ✅ 更新AccessToken（刷新后调用）
     */
    suspend fun updateAccessToken(newToken: String, expiresIn: Int = 7200) {
        context.authDataStore.edit { prefs ->
            prefs[Keys.ACCESS_TOKEN] = newToken

            val expiresAt = System.currentTimeMillis() + ((expiresIn - 300) * 1000L)
            prefs[Keys.TOKEN_EXPIRES_AT] = expiresAt

            Log.d(TAG, "✅ AccessToken已更新")
        }
    }

    suspend fun clearAuth() {
        context.authDataStore.edit { prefs ->
            prefs.remove(Keys.ACCESS_TOKEN)
            prefs.remove(Keys.REFRESH_TOKEN)
            prefs.remove(Keys.USER_INFO)
            prefs.remove(Keys.TOKEN_EXPIRES_AT)
            prefs[Keys.IS_LOGGED_IN] = false
        }
        Log.d(TAG, "🗑️ 认证信息已清除")
    }

    suspend fun getAccessToken(): String? {
        return accessToken.first()
    }

    suspend fun getRefreshToken(): String? {
        return refreshToken.first()
    }

    suspend fun getUserInfo(): UserInfo? {
        return userInfo.first()
    }

    suspend fun checkIsLoggedIn(): Boolean {
        return isLoggedIn.first()
    }
}

data class UserInfo(
    val id: String,
    val username: String,
    val nickname: String? = null,
    val avatar: String? = null,
    val email: String? = null
)