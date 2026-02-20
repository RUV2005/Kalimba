// 文件：app/src/main/java/com/danmo/kalimba/practice/SmartPracticeScreen.kt
package com.danmo.kalimba.practice

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.danmo.kalimba.R
import com.danmo.kalimba.accessibility.AccessibilityHelper
import com.danmo.kalimba.accessibility.VibrationType
import com.danmo.kalimba.metronome.MetronomeManager
import com.danmo.kalimba.pitch.NativePitchDetector
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "SmartPractice"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartPracticeScreen(
    segment: PracticeSegment,
    bpm: Int,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accessibilityHelper = remember { AccessibilityHelper(context) }
    val metronome = remember { MetronomeManager(context, accessibilityHelper) }
    val pitchDetector = remember { NativePitchDetector(context) }

    var isPracticing by remember { mutableStateOf(false) }
    var currentNoteIndex by remember { mutableIntStateOf(0) }
    var correctCount by remember { mutableIntStateOf(0) }
    var wrongCount by remember { mutableIntStateOf(0) }
    var lastFeedback by remember { mutableStateOf<String?>(null) }
    var showResults by remember { mutableStateOf(false) }

    // ✅ 新增：防止重复判断的标记
    var lastProcessedKeyId by remember { mutableStateOf<String?>(null) }
    var isProcessingFeedback by remember { mutableStateOf(false) }

    // 权限状态
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var showPermissionDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (!isGranted) {
            showPermissionDialog = true
        }
    }

    val detectedPitch by pitchDetector.detectedPitch.collectAsState()
    val detectedKeyId by pitchDetector.detectedKeyId.collectAsState()

    val currentNote = segment.notes.getOrNull(currentNoteIndex)

    // ✅ 核心修复：音高检测逻辑重构
    LaunchedEffect(detectedKeyId, isPracticing, currentNoteIndex) {
        if (!isPracticing || currentNote == null || isProcessingFeedback) return@LaunchedEffect

        val detected = detectedKeyId ?: return@LaunchedEffect

        // 跳过休止符
        if (currentNote.isRest) {
            delay(500)
            if (currentNoteIndex < segment.notes.size - 1) {
                currentNoteIndex++
            }
            return@LaunchedEffect
        }

        // 实时显示检测结果
        Log.d(TAG, "🎵 检测到: $detected, 目标: ${currentNote.keyId}, 索引: $currentNoteIndex")

        // ✅ 防止同一音符重复判断
        if (detected == lastProcessedKeyId) {
            return@LaunchedEffect
        }

        // ✅ 判断是否正确
        if (detected == currentNote.keyId) {
            // 🎉 正确
            isProcessingFeedback = true
            lastProcessedKeyId = detected
            correctCount++
            lastFeedback = "✅ 正确"

            accessibilityHelper.provideFeedback(
                text = "正确",
                vibrationType = VibrationType.DOUBLE_CLICK
            )

            Log.d(TAG, "✅ 正确！进度: ${currentNoteIndex + 1}/${segment.notes.size}")

            // 等待反馈显示
            delay(800)
            lastFeedback = null

            // 进入下一个音符
            if (currentNoteIndex < segment.notes.size - 1) {
                currentNoteIndex++
                lastProcessedKeyId = null
                isProcessingFeedback = false

                val nextNote = segment.notes[currentNoteIndex]
                if (!nextNote.isRest) {
                    accessibilityHelper.speak("下一音，${KeyPositionHelper.getPositionDescription(nextNote.keyId)}")
                }
            } else {
                // 🎊 全部完成
                isPracticing = false
                showResults = true
                pitchDetector.stopListening()
                metronome.stop()
                accessibilityHelper.speak("练习完成！正确${correctCount}个，错误${wrongCount}个")
            }
        } else {
            // ❌ 错误（只在首次错误时计数）
            if (lastProcessedKeyId != detected) {
                wrongCount++
                lastProcessedKeyId = detected
                lastFeedback = "❌ 错误"

                accessibilityHelper.provideFeedback(
                    text = "弹错了，目标是${KeyPositionHelper.getPositionDescription(currentNote.keyId)}",
                    vibrationType = VibrationType.STRONG
                )

                Log.d(TAG, "❌ 错误！检测到 $detected，但目标是 ${currentNote.keyId}")

                scope.launch {
                    delay(1500)
                    lastFeedback = null
                    lastProcessedKeyId = null // 重置，允许重新尝试
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            pitchDetector.stopListening()
            metronome.release()
            accessibilityHelper.release()
        }
    }

    // 权限提示对话框
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("需要麦克风权限") },
            text = {
                Text("智能跟练功能需要使用麦克风来识别您的演奏。\n\n请在系统设置中手动授予麦克风权限。")
            },
            confirmButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("我知道了")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("智能跟练 - ${segment.name}") },
                navigationIcon = {
                    IconButton(onClick = {
                        pitchDetector.stopListening()
                        metronome.stop()
                        onNavigateBack()
                    }) {
                        Icon(painterResource(R.drawable.ic_arrow_back), "返回")
                    }
                }
            )
        }
    ) { padding ->
        if (showResults) {
            ResultScreen(
                correctCount = correctCount,
                wrongCount = wrongCount,
                totalNotes = segment.notes.filter { !it.isRest }.size, // 只计算非休止符
                onRestart = {
                    showResults = false
                    currentNoteIndex = 0
                    correctCount = 0
                    wrongCount = 0
                    lastProcessedKeyId = null
                },
                onExit = onNavigateBack
            )
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 权限警告
                if (!hasPermission) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF3E0)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_info),
                                contentDescription = null,
                                tint = Color(0xFFFF9800)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "需要麦克风权限",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100)
                                )
                                Text(
                                    "智能跟练需要使用麦克风识别演奏",
                                    fontSize = 12.sp,
                                    color = Color(0xFFE65100)
                                )
                            }
                            TextButton(
                                onClick = {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            ) {
                                Text("授权")
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // 进度条
                LinearProgressIndicator(
                    progress = { (currentNoteIndex + 1).toFloat() / segment.notes.size },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(16.dp))

                // ✅ 新增：实时显示检测到的音高
                if (isPracticing && detectedKeyId != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE8F5E9)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_volume_up),
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    "麦克风检测中",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    "检测到: ${KeyPositionHelper.getPositionDescription(detectedKeyId!!)} (${detectedPitch?.toInt() ?: 0}Hz)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // 当前目标音符
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            lastFeedback?.contains("✅") == true -> Color(0xFFE8F5E9)
                            lastFeedback?.contains("❌") == true -> Color(0xFFFFEBEE)
                            isPracticing -> Color(0xFFE3F2FD)
                            else -> Color(0xFFF5F5F5)
                        }
                    )
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (currentNote != null && !currentNote.isRest) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = KeyPositionHelper.getDisplayName(currentNote.keyId),
                                    fontSize = 72.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF1976D2)
                                )
                                Text(
                                    text = KeyPositionHelper.getPositionDescription(currentNote.keyId),
                                    fontSize = 18.sp,
                                    color = Color.Gray
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = "第 ${currentNoteIndex + 1}/${segment.notes.size}",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            }
                        } else if (currentNote?.isRest == true) {
                            Text(
                                text = "休止 (0)",
                                fontSize = 48.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                // 反馈信息
                lastFeedback?.let {
                    Text(
                        text = it,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (it.contains("✅")) Color(0xFF4CAF50) else Color(0xFFE53935)
                    )
                }

                Spacer(Modifier.height(16.dp))

                // 统计信息
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatCard("正确", correctCount, Color(0xFF4CAF50))
                    StatCard("错误", wrongCount, Color(0xFFE53935))
                }

                Spacer(Modifier.weight(1f))

                // 开始/停止按钮
                Button(
                    onClick = {
                        if (!hasPermission) {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            return@Button
                        }

                        if (isPracticing) {
                            isPracticing = false
                            pitchDetector.stopListening()
                            metronome.stop()
                            accessibilityHelper.speak("已停止")
                        } else {
                            isPracticing = true
                            currentNoteIndex = 0
                            correctCount = 0
                            wrongCount = 0
                            lastFeedback = null
                            lastProcessedKeyId = null
                            isProcessingFeedback = false

                            metronome.start(bpm)

                            pitchDetector.startListening { keyId, freq ->
                                Log.d(TAG, "🎤 实时回调: $keyId at ${freq.toInt()} Hz")
                            }

                            val firstNote = segment.notes.firstOrNull { !it.isRest }
                            if (firstNote != null) {
                                accessibilityHelper.speak("开始练习，第一音，${KeyPositionHelper.getPositionDescription(firstNote.keyId)}")
                            } else {
                                accessibilityHelper.speak("开始练习")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPracticing) Color(0xFFE53935) else Color(0xFF4CAF50)
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!hasPermission) {
                            Icon(
                                painter = painterResource(R.drawable.ic_volume_off),
                                contentDescription = null
                            )
                        }
                        Text(
                            if (!hasPermission) "授予权限后开始"
                            else if (isPracticing) "停止练习"
                            else "开始跟练",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun StatCard(label: String, count: Int, color: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 14.sp, color = Color.Gray)
            Text(count.toString(), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun ResultScreen(
    correctCount: Int,
    wrongCount: Int,
    totalNotes: Int,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    val accuracy = if (totalNotes > 0) (correctCount.toFloat() / totalNotes * 100).toInt() else 0

    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🎉 练习完成", fontSize = 32.sp, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(32.dp))

        Text("准确率", fontSize = 16.sp, color = Color.Gray)
        Text("$accuracy%", fontSize = 64.sp, fontWeight = FontWeight.Black, color = Color(0xFF4CAF50))

        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("✅ 正确", color = Color.Gray)
                Text(correctCount.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("❌ 错误", color = Color.Gray)
                Text(wrongCount.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📊 总计", color = Color.Gray)
                Text(totalNotes.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(48.dp))

        Button(onClick = onRestart, Modifier.fillMaxWidth().height(56.dp)) {
            Text("再练一次", fontSize = 16.sp)
        }

        Spacer(Modifier.height(16.dp))

        OutlinedButton(onClick = onExit, Modifier.fillMaxWidth().height(56.dp)) {
            Text("返回", fontSize = 16.sp)
        }
    }
}