// 文件：app/src/main/java/com/danmo/kalimba/MainActivity.kt
package com.danmo.kalimba

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.danmo.kalimba.main.KalimbaScreen
import com.danmo.kalimba.nmn.NmnScreen
import com.danmo.kalimba.practice.PracticeScreen
import com.danmo.kalimba.practice.PracticeViewModel
import com.danmo.kalimba.practice.SmartPracticeScreen
import com.danmo.kalimba.settings.SettingsScreen
import com.danmo.kalimba.sheetlist.SheetListScreen
import com.danmo.kalimba.ui.auth.LoginScreen
import com.danmo.kalimba.ui.theme.KalimbaAppTheme
import com.danmo.kalimba.data.local.KalimbaDatabase

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "麦克风权限已授予", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                this,
                "未授予麦克风权限，智能跟练功能将不可用",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestMicrophonePermission()

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

                        // ✅ 练习模式
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
                                },
                                onNavigateToSmartPractice = { segmentIndex, bpm ->
                                    // ✅ 跳转到智能跟练
                                    navController.navigate("smart_practice/$sheetId/$segmentIndex/$bpm")
                                }
                            )
                        }

                        // ✅ 智能跟练模式 - 简化路由
                        composable(
                            route = "smart_practice/{sheetId}/{segmentIndex}/{bpm}",
                            arguments = listOf(
                                navArgument("sheetId") { type = NavType.LongType },
                                navArgument("segmentIndex") { type = NavType.IntType },
                                navArgument("bpm") { type = NavType.IntType }
                            )
                        ) { backStackEntry ->
                            val sheetId = backStackEntry.arguments?.getLong("sheetId") ?: -1L
                            val segmentIndex = backStackEntry.arguments?.getInt("segmentIndex") ?: 0
                            val bpm = backStackEntry.arguments?.getInt("bpm") ?: 80

                            // 加载段落数据
                            SmartPracticeRoute(
                                sheetId = sheetId,
                                segmentIndex = segmentIndex,
                                bpm = bpm,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // 设置页面
                        composable("settings") {
                            SettingsScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onNavigateToLogin = {
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
                                baseUrl = "https://guangji.online/api/"
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkAndRequestMicrophonePermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                // 权限已授予
            }
            shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) -> {
                Toast.makeText(
                    this,
                    "智能跟练功能需要麦克风权限来识别您的演奏",
                    Toast.LENGTH_LONG
                ).show()
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    fun hasMicrophonePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
}

/**
 * ✅ 智能跟练路由封装
 * 负责加载数据并显示 SmartPracticeScreen
 */
@Composable
private fun SmartPracticeRoute(
    sheetId: Long,
    segmentIndex: Int,
    bpm: Int,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { KalimbaDatabase.getDatabase(context) }
    val viewModel: PracticeViewModel = viewModel(
        factory = PracticeViewModel.Factory(sheetId, database)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 获取指定段落
    val segment = uiState.segments.getOrNull(segmentIndex)

    if (uiState.isLoading) {
        // 加载中
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            androidx.compose.material3.CircularProgressIndicator()
        }
    } else if (segment == null) {
        // 段落不存在，自动返回
        LaunchedEffect(Unit) {
            onNavigateBack()
        }
    } else {
        // 显示智能跟练
        SmartPracticeScreen(
            segment = segment,
            bpm = bpm,
            onNavigateBack = onNavigateBack
        )
    }
}