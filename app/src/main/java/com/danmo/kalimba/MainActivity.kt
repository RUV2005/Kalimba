package com.danmo.kalimba

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.danmo.kalimba.main.KalimbaScreen
import com.danmo.kalimba.nmn.NmnScreen
import com.danmo.kalimba.practice.PracticeScreen
import com.danmo.kalimba.settings.SettingsScreen
import com.danmo.kalimba.sheetlist.SheetListScreen
import com.danmo.kalimba.ui.auth.LoginScreen
import com.danmo.kalimba.ui.theme.KalimbaAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KalimbaAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "free_explore"
                    ) {
                        // 自由探索模式（主页面）
                        composable("free_explore") {
                            KalimbaScreen(
                                onNavigateToEditor = {
                                    navController.navigate("sheet_list")
                                },
                                onNavigateToSettings = {
                                    navController.navigate("settings")
                                }
                            )
                        }

                        // 简谱库列表
                        composable("sheet_list") {
                            SheetListScreen(
                                onNavigateToEdit = { sheetId ->
                                    val targetId = sheetId ?: -1L
                                    navController.navigate("nmn_editor/$targetId")
                                },
                                onNavigateToPractice = { sheetId ->
                                    navController.navigate("practice/$sheetId")
                                },
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onNavigateToLogin = {
                                    navController.navigate("login")
                                }
                            )
                        }

                        // 简谱编辑器（新建/编辑）
                        composable("nmn_editor/{sheetId}") { backStackEntry ->
                            val sheetId = backStackEntry.arguments
                                ?.getString("sheetId")
                                ?.toLongOrNull()
                                ?: -1L
                            NmnScreen(
                                sheetId = sheetId,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // 练习模式
                        composable("practice/{sheetId}") { backStackEntry ->
                            val sheetId = backStackEntry.arguments
                                ?.getString("sheetId")
                                ?.toLongOrNull()

                            if (sheetId == null) {
                                navController.popBackStack()
                                return@composable
                            }

                            PracticeScreen(
                                sheetId = sheetId,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // ✅ 设置页面 - 添加登录跳转
                        composable("settings") {
                            SettingsScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onNavigateToLogin = {  // ✅ 新增
                                    navController.navigate("login")
                                }
                            )
                        }

                        // 登录页面
                        composable("login") {
                            LoginScreen(
                                onLoginSuccess = {
                                    navController.popBackStack()
                                },
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                baseUrl = "https://guangji.online/captcha/api/"  // ✅ 生产环境
                            )
                        }
                    }
                }
            }
        }
    }
}