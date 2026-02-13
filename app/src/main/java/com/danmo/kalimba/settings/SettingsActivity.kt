// 文件名: SettingsActivity.kt
package com.danmo.kalimba.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import com.danmo.kalimba.accessibility.AccessibilityHelper
import com.danmo.kalimba.ui.theme.KalimbaAppTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // 使用 MaterialTheme 替代 KalimbaTheme
            KalimbaAppTheme {
                Surface {
                    SettingsScreen(
                        accessibilityHelper = AccessibilityHelper(this),
                        onNavigateBack = { finish() }
                    )
                }
            }
        }
    }
}