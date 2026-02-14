// 文件名: data/local/AuthDataStore.kt
package com.danmo.kalimba.data.local

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.authDataStore by preferencesDataStore(name = "auth_prefs")

/**
 * 认证数据存储 - 使用 DataStore 保存登录信息
 */
class AuthDataStore(private val context: Context) {

    private val gson = Gson()

    private object Keys {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val USER_INFO = stringPreferencesKey("user_info")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    }

    /**
     * 是否已登录
     */
    val isLoggedIn: Flow<Boolean> = context.authDataStore.data.map {
        it[Keys.IS_LOGGED_IN] ?: false
    }

    /**
     * 访问令牌
     */
    val accessToken: Flow<String?> = context.authDataStore.data.map {
        it[Keys.ACCESS_TOKEN]
    }

    /**
     * 刷新令牌
     */
    val refreshToken: Flow<String?> = context.authDataStore.data.map {
        it[Keys.REFRESH_TOKEN]
    }

    /**
     * 用户信息
     */
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
     * 保存认证信息
     */
    suspend fun saveAuth(token: String, refreshToken: String, user: UserInfo) {
        context.authDataStore.edit { prefs ->
            prefs[Keys.ACCESS_TOKEN] = token
            prefs[Keys.REFRESH_TOKEN] = refreshToken
            prefs[Keys.USER_INFO] = gson.toJson(user)
            prefs[Keys.IS_LOGGED_IN] = true
        }
    }

    /**
     * 清除认证信息（退出登录）
     */
    suspend fun clearAuth() {
        context.authDataStore.edit { prefs ->
            prefs.remove(Keys.ACCESS_TOKEN)
            prefs.remove(Keys.REFRESH_TOKEN)
            prefs.remove(Keys.USER_INFO)
            prefs[Keys.IS_LOGGED_IN] = false
        }
    }

    /**
     * 获取访问令牌（同步）
     */
    suspend fun getAccessToken(): String? {
        return accessToken.first()
    }

    /**
     * 获取刷新令牌（同步）
     */
    suspend fun getRefreshToken(): String? {
        return refreshToken.first()
    }

    /**
     * 获取用户信息（同步）
     */
    suspend fun getUserInfo(): UserInfo? {
        return userInfo.first()
    }

    /**
     * 检查是否已登录（同步）
     */
    suspend fun checkIsLoggedIn(): Boolean {
        return isLoggedIn.first()
    }
}

/**
 * 用户信息数据类
 */
data class UserInfo(
    val id: String,
    val username: String,
    val nickname: String? = null,
    val avatar: String? = null,
    val email: String? = null
)