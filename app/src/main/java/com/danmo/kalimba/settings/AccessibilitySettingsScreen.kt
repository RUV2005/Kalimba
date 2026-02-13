// 文件名: AccessibilitySettingsScreen.kt
package com.danmo.kalimba.settings

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.danmo.kalimba.accessibility.AccessibilityHelper
import com.danmo.kalimba.accessibility.VibrationType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// DataStore 扩展
private val Context.dataStore by preferencesDataStore(name = "accessibility_settings")

/**
 * 无障碍设置数据类
 */
data class AccessibilitySettings(
    val speechEnabled: Boolean = false,
    val vibrationEnabled: Boolean = true,
    val speechRate: Float = 1.3f,
    val autoPlayAnnounceInterval: Int = 5, // 每N个音符播报一次
    val playProgressFeedback: Boolean = true,
    val hapticFeedbackStrength: Int = 2, // 1=轻, 2=中, 3=强
    val announceKeyPosition: Boolean = true,
    val announceKeyPitch: Boolean = true,
    val announceOctave: Boolean = true
)

/**
 * 设置管理器
 */
class AccessibilitySettingsManager(private val context: Context) {

    private object PreferencesKeys {
        val SPEECH_ENABLED = booleanPreferencesKey("speech_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val SPEECH_RATE = floatPreferencesKey("speech_rate")
        val AUTO_PLAY_ANNOUNCE_INTERVAL = intPreferencesKey("auto_play_announce_interval")
        val PLAY_PROGRESS_FEEDBACK = booleanPreferencesKey("play_progress_feedback")
        val HAPTIC_FEEDBACK_STRENGTH = intPreferencesKey("haptic_feedback_strength")
        val ANNOUNCE_KEY_POSITION = booleanPreferencesKey("announce_key_position")
        val ANNOUNCE_KEY_PITCH = booleanPreferencesKey("announce_key_pitch")
        val ANNOUNCE_OCTAVE = booleanPreferencesKey("announce_octave")
    }

    val settingsFlow: Flow<AccessibilitySettings> = context.dataStore.data.map { prefs ->
        AccessibilitySettings(
            speechEnabled = prefs[PreferencesKeys.SPEECH_ENABLED] ?: false,
            vibrationEnabled = prefs[PreferencesKeys.VIBRATION_ENABLED] ?: true,
            speechRate = prefs[PreferencesKeys.SPEECH_RATE] ?: 1.3f,
            autoPlayAnnounceInterval = prefs[PreferencesKeys.AUTO_PLAY_ANNOUNCE_INTERVAL] ?: 5,
            playProgressFeedback = prefs[PreferencesKeys.PLAY_PROGRESS_FEEDBACK] ?: true,
            hapticFeedbackStrength = prefs[PreferencesKeys.HAPTIC_FEEDBACK_STRENGTH] ?: 2,
            announceKeyPosition = prefs[PreferencesKeys.ANNOUNCE_KEY_POSITION] ?: true,
            announceKeyPitch = prefs[PreferencesKeys.ANNOUNCE_KEY_PITCH] ?: true,
            announceOctave = prefs[PreferencesKeys.ANNOUNCE_OCTAVE] ?: true
        )
    }

    suspend fun updateSpeechEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SPEECH_ENABLED] = enabled
        }
    }

    suspend fun updateVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.VIBRATION_ENABLED] = enabled
        }
    }

    suspend fun updateSpeechRate(rate: Float) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SPEECH_RATE] = rate
        }
    }

    suspend fun updateAutoPlayAnnounceInterval(interval: Int) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.AUTO_PLAY_ANNOUNCE_INTERVAL] = interval
        }
    }

    suspend fun updateHapticFeedbackStrength(strength: Int) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.HAPTIC_FEEDBACK_STRENGTH] = strength
        }
    }
}

/**
 * 无障碍设置界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessibilitySettingsScreen(
    accessibilityHelper: AccessibilityHelper,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember { AccessibilitySettingsManager(context) }
    val settings by settingsManager.settingsFlow.collectAsState(initial = AccessibilitySettings())
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("无障碍设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 语音播报设置
            SettingsSection(title = "语音播报") {
                SwitchSetting(
                    title = "启用语音播报",
                    subtitle = "播报琴键位置和音高信息",
                    checked = settings.speechEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            settingsManager.updateSpeechEnabled(enabled)
                            accessibilityHelper.isSpeechEnabled = enabled
                            accessibilityHelper.provideFeedback(
                                text = if (enabled) "语音播报已启用" else "语音播报已禁用",
                                vibrationType = VibrationType.MEDIUM
                            )
                        }
                    }
                )

                if (settings.speechEnabled) {
                    SliderSetting(
                        title = "语速",
                        value = settings.speechRate,
                        valueRange = 0.5f..2.0f,
                        steps = 14,
                        onValueChange = { rate ->
                            scope.launch {
                                settingsManager.updateSpeechRate(rate)
                            }
                        },
                        valueLabel = { "%.1f倍".format(it) }
                    )
                }
            }

            // 触觉反馈设置
            SettingsSection(title = "触觉反馈") {
                SwitchSetting(
                    title = "启用振动反馈",
                    subtitle = "操作时提供振动提示",
                    checked = settings.vibrationEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            settingsManager.updateVibrationEnabled(enabled)
                            if (enabled) {
                                accessibilityHelper.vibrate(VibrationType.MEDIUM)
                            }
                        }
                    }
                )

                if (settings.vibrationEnabled) {
                    RadioGroupSetting(
                        title = "振动强度",
                        options = listOf(
                            1 to "轻微",
                            2 to "中等",
                            3 to "强烈"
                        ),
                        selectedValue = settings.hapticFeedbackStrength,
                        onValueChange = { strength ->
                            scope.launch {
                                settingsManager.updateHapticFeedbackStrength(strength)
                                // 测试振动
                                val type = when (strength) {
                                    1 -> VibrationType.LIGHT
                                    2 -> VibrationType.MEDIUM
                                    else -> VibrationType.STRONG
                                }
                                accessibilityHelper.vibrate(type)
                            }
                        }
                    )
                }
            }

            // 播放反馈设置
            SettingsSection(title = "播放反馈") {
                SliderSetting(
                    title = "自动播放播报间隔",
                    subtitle = "每播放N个音符后播报一次进度",
                    value = settings.autoPlayAnnounceInterval.toFloat(),
                    valueRange = 1f..10f,
                    steps = 8,
                    onValueChange = { interval ->
                        scope.launch {
                            settingsManager.updateAutoPlayAnnounceInterval(interval.toInt())
                        }
                    },
                    valueLabel = { "${it.toInt()}个音符" }
                )
            }

            // 提示信息
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "提示",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        "• 如果您正在使用TalkBack，建议关闭应用内语音播报，避免重复播报\n" +
                                "• 振动反馈可帮助您确认操作是否成功\n" +
                                "• 可根据个人喜好调整语速和振动强度",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(bottom = 8.dp)
                .semantics { heading() }
        )
        content()
    }
}

@Composable
private fun SwitchSetting(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SliderSetting(
    title: String,
    subtitle: String? = null,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChange: (Float) -> Unit,
    valueLabel: (Float) -> String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = valueLabel(value),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps
        )
    }
}

@Composable
private fun RadioGroupSetting(
    title: String,
    options: List<Pair<Int, String>>,
    selectedValue: Int,
    onValueChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        options.forEach { (value, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = value == selectedValue,
                    onClick = { onValueChange(value) }
                )
                Text(
                    text = label,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}