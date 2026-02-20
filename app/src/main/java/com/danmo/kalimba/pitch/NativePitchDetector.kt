// 文件位置: app/src/main/java/com/danmo/kalimba/pitch/NativePitchDetector.kt
package com.danmo.kalimba.pitch

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

class NativePitchDetector(private val context: Context) {

    private val _detectedPitch = MutableStateFlow<Float?>(null)
    val detectedPitch: StateFlow<Float?> = _detectedPitch.asStateFlow()

    private val _detectedKeyId = MutableStateFlow<String?>(null)
    val detectedKeyId: StateFlow<String?> = _detectedKeyId.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var isListening = false
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val sampleRate = 44100
    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    ) * 8  // ✅ 进一步增大缓冲区，提高低频精度

    private val volumeThreshold = 2000f

    private var lastDetectedKey: String? = null
    private var lastDetectedCount = 0
    private val stabilityThreshold = 3

    private var callback: ((String, Float) -> Unit)? = null

    companion object {
        private const val TAG = "NativePitchDetector"

        private val NOTE_FREQUENCIES = mapOf(
            // 下排（d行）
            "d4d" to 329.63f,
            "d6d" to 440.00f,
            "d1m" to 523.25f,   // ⭐ 目标：中音1
            "d3m" to 659.25f,
            "d5m" to 783.99f,
            "d7m" to 987.77f,
            "d2h" to 1174.66f,
            "d4h" to 1318.51f,
            "d6h" to 1567.98f,  // 1700Hz 检测到的实际是这个
            "d1hh" to 2093.00f,
            "d3hh" to 2637.02f,
            "d5hh" to 3135.96f,

            // 上排（u行）
            "u5d" to 391.99f,
            "u7d" to 493.88f,
            "u2m" to 587.33f,
            "u4m" to 698.46f,
            "u6m" to 880.00f,
            "u1h" to 1046.50f,
            "u3h" to 1318.51f,
            "u5h" to 1567.98f,
            "u7h" to 1975.53f,
            "u2hh" to 2349.32f,
            "u4hh" to 2793.83f,
            "u6hh" to 3520.00f
        )

        private const val FREQUENCY_TOLERANCE = 0.10f  // ✅ 放宽到 10%
    }

    fun startListening(onDetected: (String, Float) -> Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "没有录音权限")
            return
        }

        callback = onDetected
        isListening = true

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            audioRecord?.startRecording()
            Log.d(TAG, "✅ 音高检测已启动（基频优先模式）")

            scope.launch {
                processAudio()
            }
        } catch (e: Exception) {
            Log.e(TAG, "启动录音失败", e)
        }
    }

    private suspend fun processAudio() {
        val buffer = ShortArray(bufferSize)

        while (isListening) {
            val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
            if (readSize <= 0) continue

            val volume = calculateVolume(buffer, readSize)

            if (volume < volumeThreshold) {
                withContext(Dispatchers.Main) {
                    _detectedPitch.value = null
                    _detectedKeyId.value = null
                }
                lastDetectedKey = null
                lastDetectedCount = 0
                delay(50)
                continue
            }

            // ✅ 关键改进：多候选频率检测
            val candidates = detectMultipleCandidates(buffer, readSize)

            if (candidates.isEmpty()) {
                delay(50)
                continue
            }

            // ✅ 核心：基频优先选择策略
            val fundamentalFreq = selectFundamentalFrequency(candidates)

            if (fundamentalFreq == null) {
                delay(50)
                continue
            }

            val keyId = findClosestNote(fundamentalFreq)

            if (keyId != null) {
                if (keyId == lastDetectedKey) {
                    lastDetectedCount++
                    if (lastDetectedCount >= stabilityThreshold) {
                        withContext(Dispatchers.Main) {
                            _detectedPitch.value = fundamentalFreq
                            _detectedKeyId.value = keyId
                            callback?.invoke(keyId, fundamentalFreq)
                        }
                        Log.d(TAG, "✅ 最终识别: $keyId at ${fundamentalFreq.toInt()}Hz")
                    }
                } else {
                    lastDetectedKey = keyId
                    lastDetectedCount = 1
                }
            }

            delay(50)
        }
    }

    private fun calculateVolume(buffer: ShortArray, size: Int): Float {
        var sum = 0.0
        for (i in 0 until size) {
            sum += buffer[i] * buffer[i]
        }
        return sqrt(sum / size).toFloat()
    }

    /**
     * ✅ 核心改进：检测多个候选频率（包括基频和泛音）
     */
    private fun detectMultipleCandidates(buffer: ShortArray, size: Int): List<Pair<Float, Float>> {
        val floatBuffer = FloatArray(size) { buffer[it].toFloat() / 32768f }

        // ✅ 扩大搜索范围，同时检测低频和高频
        val minPeriod = (sampleRate / 4000).toInt()
        val maxPeriod = (sampleRate / 250).toInt()

        val diffFunction = FloatArray(maxPeriod + 1)
        for (tau in minPeriod..maxPeriod) {
            var sum = 0f
            for (i in 0 until min(size - tau, 4096)) {  // 限制计算量
                val delta = floatBuffer[i] - floatBuffer[i + tau]
                sum += delta * delta
            }
            diffFunction[tau] = sum
        }

        val cmndf = FloatArray(maxPeriod + 1)
        cmndf[0] = 1f
        var runningSum = 0f

        for (tau in 1..maxPeriod) {
            runningSum += diffFunction[tau]
            cmndf[tau] = if (runningSum == 0f) 1f else diffFunction[tau] * tau / runningSum
        }

        // ✅ 找到所有局部最小值（候选频率）
        val candidates = mutableListOf<Pair<Float, Float>>()

        for (tau in minPeriod until maxPeriod) {
            // 检查是否为局部最小值
            if (tau > minPeriod && tau < maxPeriod - 1) {
                if (cmndf[tau] < cmndf[tau - 1] && cmndf[tau] < cmndf[tau + 1]) {
                    if (cmndf[tau] < 0.3f) {  // 置信度阈值
                        val frequency = sampleRate.toFloat() / tau
                        candidates.add(Pair(frequency, cmndf[tau]))

                        Log.d(TAG, "📍 候选: ${frequency.toInt()}Hz (CMNDF=${cmndf[tau]})")
                    }
                }
            }
        }

        return candidates.sortedBy { it.second }  // 按置信度排序
    }

    /**
     * ✅ 核心策略：基频优先选择
     *
     * 规则：
     * 1. 优先选择 < 1000Hz 的候选（可能是基频）
     * 2. 如果没有低频候选，检查高频候选是否为倍频程
     * 3. 如果是倍频程，计算并返回基频
     */
    private fun selectFundamentalFrequency(candidates: List<Pair<Float, Float>>): Float? {
        if (candidates.isEmpty()) return null

        // ✅ 策略1：优先选择低频候选（< 1000Hz）
        val lowFreqCandidates = candidates.filter { it.first < 1000f }
        if (lowFreqCandidates.isNotEmpty()) {
            val selected = lowFreqCandidates.first().first
            Log.d(TAG, "🎯 选择低频基频: ${selected.toInt()}Hz")
            return selected
        }

        // ✅ 策略2：如果只有高频，尝试倍频程还原
        val highestConfidence = candidates.first()
        val detectedFreq = highestConfidence.first

        Log.d(TAG, "⚠️ 只检测到高频: ${detectedFreq.toInt()}Hz，尝试倍频程还原...")

        // 尝试除以 2, 3, 4, 5, 6 来找基频
        for (divisor in listOf(2f, 3f, 4f, 5f, 6f)) {
            val possibleFundamental = detectedFreq / divisor

            // 检查这个可能的基频是否匹配已知音符
            for ((key, targetFreq) in NOTE_FREQUENCIES) {
                val tolerance = targetFreq * FREQUENCY_TOLERANCE
                if (abs(possibleFundamental - targetFreq) <= tolerance) {
                    Log.d(TAG, "✅ 倍频程修正: ${detectedFreq.toInt()}Hz ÷ $divisor = ${possibleFundamental.toInt()}Hz ($key)")
                    return targetFreq  // 返回标准频率
                }
            }
        }

        // ✅ 策略3：如果都不匹配，直接返回检测到的频率（可能是噪音）
        Log.d(TAG, "❌ 无法确定基频，忽略 ${detectedFreq.toInt()}Hz")
        return null
    }

    private fun findClosestNote(frequency: Float): String? {
        var closestKey: String? = null
        var minDifference = Float.MAX_VALUE

        for ((key, targetFreq) in NOTE_FREQUENCIES) {
            val tolerance = targetFreq * FREQUENCY_TOLERANCE
            val difference = abs(frequency - targetFreq)

            if (difference <= tolerance && difference < minDifference) {
                minDifference = difference
                closestKey = key
            }
        }

        return closestKey
    }

    fun stopListening() {
        isListening = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        _detectedPitch.value = null
        _detectedKeyId.value = null
        lastDetectedKey = null
        lastDetectedCount = 0

        Log.d(TAG, "音高检测已停止")
    }

    fun release() {
        stopListening()
        scope.cancel()
    }
}