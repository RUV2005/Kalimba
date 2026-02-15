package com.danmo.kalimba.settings

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import coil.compose.AsyncImage
import com.danmo.kalimba.R
import com.danmo.kalimba.accessibility.AccessibilityHelper
import com.danmo.kalimba.accessibility.VibrationType
import com.danmo.kalimba.data.local.AuthDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "app_settings")

data class AppSettings(
    val speechEnabled: Boolean = false,
    val vibrationEnabled: Boolean = true,
    val speechRate: Float = 1.3f,
    val autoPlayAnnounceInterval: Int = 5,
    val hapticFeedbackStrength: Int = 2,
    val masterVolume: Float = 0.8f,
    val keyPressVolume: Float = 1.0f,
    val autoPlayBpm: Int = 80,
    val soundSet: String = "default",
    val showKeyLabels: Boolean = true,
    val showPitchNames: Boolean = true,
    val highContrastMode: Boolean = false,
    val largeTextMode: Boolean = false,
    val autoSave: Boolean = true,
    val defaultBpm: Int = 80,
    val showGridLines: Boolean = true,
    val inAppSpeechEnabled: Boolean = false,
    // ✅ 节拍器设置
    val metronomeEnabled: Boolean = false,
    val defaultMetronomeBpm: Int = 80,
    val metronomeBeatsPerMeasure: Int = 4,
    val metronomeVolume: Float = 0.8f,           // ✅ 新增：节拍器音量
    val metronomeVibrationEnabled: Boolean = true, // ✅ 新增：节拍器振动
    val metronomeAccentFirstBeat: Boolean = true   // ✅ 新增：强调首拍
)

class SettingsManager(private val context: Context) {
    private object PreferencesKeys {
        val SPEECH_ENABLED = booleanPreferencesKey("speech_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val SPEECH_RATE = floatPreferencesKey("speech_rate")
        val AUTO_PLAY_ANNOUNCE_INTERVAL = intPreferencesKey("auto_play_announce_interval")
        val HAPTIC_FEEDBACK_STRENGTH = intPreferencesKey("haptic_feedback_strength")
        val MASTER_VOLUME = floatPreferencesKey("master_volume")
        val KEY_PRESS_VOLUME = floatPreferencesKey("key_press_volume")
        val AUTO_PLAY_BPM = intPreferencesKey("auto_play_bpm")
        val SOUND_SET = stringPreferencesKey("sound_set")
        val SHOW_KEY_LABELS = booleanPreferencesKey("show_key_labels")
        val SHOW_PITCH_NAMES = booleanPreferencesKey("show_pitch_names")
        val HIGH_CONTRAST_MODE = booleanPreferencesKey("high_contrast_mode")
        val LARGE_TEXT_MODE = booleanPreferencesKey("large_text_mode")
        val AUTO_SAVE = booleanPreferencesKey("auto_save")
        val DEFAULT_BPM = intPreferencesKey("default_bpm")
        val SHOW_GRID_LINES = booleanPreferencesKey("show_grid_lines")
        val IN_APP_SPEECH_ENABLED = booleanPreferencesKey("in_app_speech_enabled")
        // ✅ 节拍器相关键
        val METRONOME_ENABLED = booleanPreferencesKey("metronome_enabled")
        val DEFAULT_METRONOME_BPM = intPreferencesKey("default_metronome_bpm")
        val METRONOME_BEATS_PER_MEASURE = intPreferencesKey("metronome_beats_per_measure")
        val METRONOME_VOLUME = floatPreferencesKey("metronome_volume")
        val METRONOME_VIBRATION_ENABLED = booleanPreferencesKey("metronome_vibration_enabled")
        val METRONOME_ACCENT_FIRST_BEAT = booleanPreferencesKey("metronome_accent_first_beat")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            speechEnabled = prefs[PreferencesKeys.SPEECH_ENABLED] ?: false,
            vibrationEnabled = prefs[PreferencesKeys.VIBRATION_ENABLED] ?: true,
            speechRate = prefs[PreferencesKeys.SPEECH_RATE] ?: 1.3f,
            autoPlayAnnounceInterval = prefs[PreferencesKeys.AUTO_PLAY_ANNOUNCE_INTERVAL] ?: 5,
            hapticFeedbackStrength = prefs[PreferencesKeys.HAPTIC_FEEDBACK_STRENGTH] ?: 2,
            masterVolume = prefs[PreferencesKeys.MASTER_VOLUME] ?: 0.8f,
            keyPressVolume = prefs[PreferencesKeys.KEY_PRESS_VOLUME] ?: 1.0f,
            autoPlayBpm = prefs[PreferencesKeys.AUTO_PLAY_BPM] ?: 80,
            soundSet = prefs[PreferencesKeys.SOUND_SET] ?: "default",
            showKeyLabels = prefs[PreferencesKeys.SHOW_KEY_LABELS] ?: true,
            showPitchNames = prefs[PreferencesKeys.SHOW_PITCH_NAMES] ?: true,
            highContrastMode = prefs[PreferencesKeys.HIGH_CONTRAST_MODE] ?: false,
            largeTextMode = prefs[PreferencesKeys.LARGE_TEXT_MODE] ?: false,
            autoSave = prefs[PreferencesKeys.AUTO_SAVE] ?: true,
            defaultBpm = prefs[PreferencesKeys.DEFAULT_BPM] ?: 80,
            showGridLines = prefs[PreferencesKeys.SHOW_GRID_LINES] ?: true,
            inAppSpeechEnabled = prefs[PreferencesKeys.IN_APP_SPEECH_ENABLED] ?: true,
            // ✅ 节拍器设置读取
            metronomeEnabled = prefs[PreferencesKeys.METRONOME_ENABLED] ?: false,
            defaultMetronomeBpm = prefs[PreferencesKeys.DEFAULT_METRONOME_BPM] ?: 80,
            metronomeBeatsPerMeasure = prefs[PreferencesKeys.METRONOME_BEATS_PER_MEASURE] ?: 4,
            metronomeVolume = prefs[PreferencesKeys.METRONOME_VOLUME] ?: 0.8f,
            metronomeVibrationEnabled = prefs[PreferencesKeys.METRONOME_VIBRATION_ENABLED] ?: true,
            metronomeAccentFirstBeat = prefs[PreferencesKeys.METRONOME_ACCENT_FIRST_BEAT] ?: true
        )
    }

    // ✅ 现有方法保持不变
    suspend fun updateSpeechEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.SPEECH_ENABLED] = enabled }
    }
    suspend fun updateVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.VIBRATION_ENABLED] = enabled }
    }
    suspend fun updateSpeechRate(rate: Float) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.SPEECH_RATE] = rate }
    }
    suspend fun updateHapticFeedbackStrength(strength: Int) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.HAPTIC_FEEDBACK_STRENGTH] = strength }
    }
    suspend fun updateAutoPlayAnnounceInterval(interval: Int) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.AUTO_PLAY_ANNOUNCE_INTERVAL] = interval }
    }
    suspend fun updateMasterVolume(volume: Float) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.MASTER_VOLUME] = volume }
    }
    suspend fun updateKeyPressVolume(volume: Float) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.KEY_PRESS_VOLUME] = volume }
    }
    suspend fun updateAutoPlayBpm(bpm: Int) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.AUTO_PLAY_BPM] = bpm }
    }
    suspend fun updateSoundSet(soundSet: String) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.SOUND_SET] = soundSet }
    }
    suspend fun updateShowKeyLabels(show: Boolean) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.SHOW_KEY_LABELS] = show }
    }
    suspend fun updateShowPitchNames(show: Boolean) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.SHOW_PITCH_NAMES] = show }
    }
    suspend fun updateHighContrastMode(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.HIGH_CONTRAST_MODE] = enabled }
    }
    suspend fun updateLargeTextMode(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.LARGE_TEXT_MODE] = enabled }
    }
    suspend fun updateAutoSave(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.AUTO_SAVE] = enabled }
    }
    suspend fun updateDefaultBpm(bpm: Int) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.DEFAULT_BPM] = bpm }
    }
    suspend fun updateShowGridLines(show: Boolean) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.SHOW_GRID_LINES] = show }
    }
    suspend fun updateInAppSpeechEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.IN_APP_SPEECH_ENABLED] = enabled
        }
    }

    // ✅ 新增：节拍器相关更新方法
    suspend fun updateMetronomeEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.METRONOME_ENABLED] = enabled
        }
    }

    suspend fun updateDefaultMetronomeBpm(bpm: Int) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.DEFAULT_METRONOME_BPM] = bpm
        }
    }

    suspend fun updateMetronomeBeatsPerMeasure(beats: Int) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.METRONOME_BEATS_PER_MEASURE] = beats
        }
    }

    suspend fun updateMetronomeVolume(volume: Float) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.METRONOME_VOLUME] = volume
        }
    }

    suspend fun updateMetronomeVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.METRONOME_VIBRATION_ENABLED] = enabled
        }
    }

    suspend fun updateMetronomeAccentFirstBeat(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.METRONOME_ACCENT_FIRST_BEAT] = enabled
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit = {}
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val authDataStore = remember { AuthDataStore(context) }
    val settings by settingsManager.settingsFlow.collectAsState(initial = AppSettings())
    val scope = rememberCoroutineScope()
    val accessibilityHelper = remember { AccessibilityHelper(context) }

    var isLoggedIn by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf<String?>(null) }
    var avatar by remember { mutableStateOf<String?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoggedIn = authDataStore.checkIsLoggedIn()
        if (isLoggedIn) {
            val user = authDataStore.getUserInfo()
            username = user?.username ?: ""
            nickname = user?.nickname
            avatar = user?.avatar
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_back),
                            contentDescription = "返回"
                        )
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            UserInfoCard(
                isLoggedIn = isLoggedIn,
                username = username,
                nickname = nickname,
                avatar = avatar,
                onLoginClick = {
                    accessibilityHelper.vibrate(VibrationType.CLICK)
                    onNavigateToLogin()
                },
                onLogoutClick = {
                    accessibilityHelper.vibrate(VibrationType.CLICK)
                    showLogoutDialog = true
                }
            )

            HorizontalDivider()

            // 无障碍设置
            SettingsSection(
                title = "无障碍",
                iconResId = R.drawable.ic_accessibility
            ) {
                SwitchSetting(
                    title = "语音播报",
                    subtitle = "播报琴键位置和操作反馈",
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
                            scope.launch { settingsManager.updateSpeechRate(rate) }
                        },
                        valueLabel = { "%.1f倍".format(it) }
                    )
                }

                SwitchSetting(
                    title = "振动反馈",
                    subtitle = "操作时提供触觉提示",
                    checked = settings.vibrationEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            settingsManager.updateVibrationEnabled(enabled)
                            if (enabled) accessibilityHelper.vibrate(VibrationType.MEDIUM)
                        }
                    }
                )

                if (settings.vibrationEnabled) {
                    RadioGroupSetting(
                        title = "振动强度",
                        options = listOf(1 to "轻微", 2 to "中等", 3 to "强烈"),
                        selectedValue = settings.hapticFeedbackStrength,
                        onValueChange = { strength ->
                            scope.launch {
                                settingsManager.updateHapticFeedbackStrength(strength)
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

            HorizontalDivider()

            // 音频设置
            SettingsSection(
                title = "音频",
                iconResId = R.drawable.ic_volume_up
            ) {
                SliderSetting(
                    title = "主音量",
                    value = settings.masterVolume,
                    valueRange = 0f..1f,
                    onValueChange = { volume ->
                        scope.launch { settingsManager.updateMasterVolume(volume) }
                    },
                    valueLabel = { "${(it * 100).toInt()}%" }
                )

                SliderSetting(
                    title = "按键音量",
                    value = settings.keyPressVolume,
                    valueRange = 0f..1f,
                    onValueChange = { volume ->
                        scope.launch { settingsManager.updateKeyPressVolume(volume) }
                    },
                    valueLabel = { "${(it * 100).toInt()}%" }
                )

                SliderSetting(
                    title = "自动播放速度",
                    value = settings.autoPlayBpm.toFloat(),
                    valueRange = 40f..120f,
                    steps = 15,
                    onValueChange = { bpm ->
                        scope.launch { settingsManager.updateAutoPlayBpm(bpm.toInt()) }
                    },
                    valueLabel = { "${it.toInt()} BPM" }
                )
            }

            HorizontalDivider()

            // ✅ 新增：节拍器设置
            SettingsSection(
                title = "节拍器",
                iconResId = R.drawable.ic_play  // 使用播放图标，或添加专门的节拍器图标
            ) {
                SwitchSetting(
                    title = "默认启用节拍器",
                    subtitle = "进入练习模式时自动开启",
                    checked = settings.metronomeEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            settingsManager.updateMetronomeEnabled(enabled)
                            accessibilityHelper.speak(
                                if (enabled) "节拍器将默认启用" else "节拍器将默认关闭"
                            )
                        }
                    }
                )

                SliderSetting(
                    title = "默认速度",
                    subtitle = "练习模式的初始节拍速度",
                    value = settings.defaultMetronomeBpm.toFloat(),
                    valueRange = 40f..200f,
                    steps = 31,  // 40, 45, 50... 200 (每5一档)
                    onValueChange = { bpm ->
                        scope.launch {
                            settingsManager.updateDefaultMetronomeBpm(bpm.toInt())
                        }
                    },
                    valueLabel = { "${it.toInt()} BPM" }
                )

                RadioGroupSetting(
                    title = "拍号",
                    options = listOf(
                        2 to "2/4（2拍）",
                        3 to "3/4（3拍）",
                        4 to "4/4（4拍）",
                        6 to "6/8（6拍）"
                    ),
                    selectedValue = settings.metronomeBeatsPerMeasure,
                    onValueChange = { beats ->
                        scope.launch {
                            settingsManager.updateMetronomeBeatsPerMeasure(beats)
                            accessibilityHelper.speak("拍号已设为${beats}拍")
                        }
                    }
                )

                SliderSetting(
                    title = "节拍器音量",
                    value = settings.metronomeVolume,
                    valueRange = 0f..1f,
                    onValueChange = { volume ->
                        scope.launch {
                            settingsManager.updateMetronomeVolume(volume)
                        }
                    },
                    valueLabel = { "${(it * 100).toInt()}%" }
                )

                SwitchSetting(
                    title = "节拍振动",
                    subtitle = "节拍时同步振动反馈",
                    checked = settings.metronomeVibrationEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            settingsManager.updateMetronomeVibrationEnabled(enabled)
                            if (enabled) {
                                accessibilityHelper.vibrate(VibrationType.LIGHT)
                            }
                        }
                    }
                )

                SwitchSetting(
                    title = "强调首拍",
                    subtitle = "每小节第一拍使用不同音效",
                    checked = settings.metronomeAccentFirstBeat,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            settingsManager.updateMetronomeAccentFirstBeat(enabled)
                            accessibilityHelper.speak(
                                if (enabled) "将强调首拍" else "首拍不强调"
                            )
                        }
                    }
                )
            }

            HorizontalDivider()

            // 显示设置
            SettingsSection(
                title = "显示",
                iconResId = R.drawable.ic_visibility
            ) {
                SwitchSetting(
                    title = "显示按键标签",
                    subtitle = "在琴键上显示数字标识",
                    checked = settings.showKeyLabels,
                    onCheckedChange = { show ->
                        scope.launch { settingsManager.updateShowKeyLabels(show) }
                    }
                )

                SwitchSetting(
                    title = "显示音高名称",
                    subtitle = "在琴键下方显示音高",
                    checked = settings.showPitchNames,
                    onCheckedChange = { show ->
                        scope.launch { settingsManager.updateShowPitchNames(show) }
                    }
                )

                SwitchSetting(
                    title = "高对比度模式",
                    subtitle = "增强视觉对比度",
                    checked = settings.highContrastMode,
                    onCheckedChange = { enabled ->
                        scope.launch { settingsManager.updateHighContrastMode(enabled) }
                    }
                )

                SwitchSetting(
                    title = "大字体模式",
                    subtitle = "增大界面文字",
                    checked = settings.largeTextMode,
                    onCheckedChange = { enabled ->
                        scope.launch { settingsManager.updateLargeTextMode(enabled) }
                    }
                )
            }

            HorizontalDivider()

            // 简谱编辑设置
            SettingsSection(
                title = "简谱编辑",
                iconResId = R.drawable.ic_edit
            ) {
                SwitchSetting(
                    title = "自动保存",
                    subtitle = "退出时自动保存简谱",
                    checked = settings.autoSave,
                    onCheckedChange = { enabled ->
                        scope.launch { settingsManager.updateAutoSave(enabled) }
                    }
                )

                SliderSetting(
                    title = "默认速度",
                    value = settings.defaultBpm.toFloat(),
                    valueRange = 40f..120f,
                    steps = 15,
                    onValueChange = { bpm ->
                        scope.launch { settingsManager.updateDefaultBpm(bpm.toInt()) }
                    },
                    valueLabel = { "${it.toInt()} BPM" }
                )

                SwitchSetting(
                    title = "显示网格线",
                    subtitle = "编辑时显示辅助网格",
                    checked = settings.showGridLines,
                    onCheckedChange = { show ->
                        scope.launch { settingsManager.updateShowGridLines(show) }
                    }
                )
            }

            HorizontalDivider()

            // 关于
            SettingsSection(
                title = "关于",
                iconResId = R.drawable.ic_info
            ) {
                ListItem(
                    headlineContent = { Text("版本") },
                    supportingContent = { Text("1.0.0") }
                )
                ListItem(
                    headlineContent = { Text("开发者") },
                    supportingContent = { Text("Danmo") }
                )
            }

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
                        "• 如果您正在使用 TalkBack，建议关闭应用内语音播报\n" +
                                "• 振动反馈可帮助您确认操作是否成功\n" +
                                "• 节拍器可帮助您保持稳定的演奏节奏\n" +
                                "• 设置会自动保存",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("确认退出登录") },
            text = { Text("退出登录后，您将无法上传简谱到云端。本地简谱不会受影响。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            authDataStore.clearAuth()
                            isLoggedIn = false
                            username = ""
                            nickname = null
                            avatar = null
                            showLogoutDialog = false
                            accessibilityHelper.provideFeedback(
                                text = "已退出登录",
                                vibrationType = VibrationType.MEDIUM
                            )
                        }
                    }
                ) {
                    Text("退出登录", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

// ✅ 以下辅助 Composable 保持不变
@Composable
private fun UserInfoCard(
    isLoggedIn: Boolean,
    username: String,
    nickname: String?,
    avatar: String?,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        if (isLoggedIn) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (avatar != null) {
                        AsyncImage(
                            model = avatar,
                            contentDescription = "用户头像",
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "默认头像",
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column {
                        Text(
                            text = nickname ?: username,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (nickname != null) {
                            Text(
                                text = "@$username",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4CAF50))
                            )
                            Text(
                                text = "已登录",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                IconButton(onClick = onLogoutClick) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "退出登录",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "未登录",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                )
                Text(
                    text = "未登录",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Button(
                    onClick = onLoginClick,
                    modifier = Modifier.fillMaxWidth(0.6f)
                ) {
                    Text("登录 / 注册")
                }
                Text(
                    text = "登录后可将简谱同步到云端",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    iconResId: Int,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() }
            )
        }
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
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    )
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