// 文件名: NmnAudioManager.kt (简化版)
package com.danmo.kalimba.nmn

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.danmo.kalimba.AudioConstants
import com.danmo.kalimba.SoundResources
import java.util.concurrent.ConcurrentHashMap

/**
 * 简谱音频管理器（简化版）
 * ⚠️ 移除了 AccessibilityEvent 播报
 * 语音播报由 AccessibilityHelper 统一管理
 */
class NmnAudioManager(context: Context) {

    private val soundPool: SoundPool
    private val soundMap = ConcurrentHashMap<String, Int>()
    private val loadedSounds = ConcurrentHashMap<Int, Boolean>()
    private var isSoundReady = false

    private val previewHandler = Handler(Looper.getMainLooper())
    private var isPreviewPlaying = false

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(audioAttributes)
            .build()

        loadSounds(context)
    }

    private fun loadSounds(context: Context) {
        val soundList = SoundResources.SOUND_LIST

        var loadedCount = 0
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loadedSounds[sampleId] = true
                loadedCount++
                if (loadedCount >= soundList.size) {
                    isSoundReady = true
                    Log.d(TAG, "All sounds loaded successfully")
                }
            } else {
                Log.e(TAG, "Failed to load sound, sampleId: $sampleId, status: $status")
            }
        }

        soundList.forEach { (id, resId) ->
            try {
                val soundId = soundPool.load(context, resId, 1)
                soundMap[id] = soundId
            } catch (e: Exception) {
                Log.e(TAG, "Error loading sound for id: $id", e)
            }
        }
    }

    fun playSoundWithCallback(keyId: String, onComplete: () -> Unit) {
        try {
            val soundId = soundMap[keyId]
            if (soundId != null && loadedSounds[soundId] == true) {
                soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f)
                Handler(Looper.getMainLooper()).postDelayed({
                    onComplete()
                }, AudioConstants.SOUND_DURATION)
            } else {
                Log.w(TAG, "Sound not ready for keyId: $keyId")
                onComplete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing sound for keyId: $keyId", e)
            onComplete()
        }
    }

    fun previewSegment(
        notes: List<NmnNote>,
        onNote: (Int) -> Unit,
        onComplete: () -> Unit,
        bpm: Int = 80
    ) {
        if (isPreviewPlaying) {
            stopPreview()
        }
        isPreviewPlaying = true

        val beatIntervalMs = 60000 / bpm

        try {
            notes.forEachIndexed { index, note ->
                previewHandler.postDelayed({
                    if (!isPreviewPlaying) return@postDelayed

                    if (!note.isRest) {
                        val soundId = soundMap[note.keyId]
                        if (soundId != null && loadedSounds[soundId] == true) {
                            soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f)
                        }
                    }
                    onNote(index)

                    if (index == notes.size - 1) {
                        previewHandler.postDelayed({
                            isPreviewPlaying = false
                            onComplete()
                        }, beatIntervalMs.toLong())
                    }
                }, (index * beatIntervalMs).toLong())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in preview segment", e)
            isPreviewPlaying = false
            onComplete()
        }
    }

    fun stopPreview() {
        isPreviewPlaying = false
        previewHandler.removeCallbacksAndMessages(null)
    }

    fun isReady() = isSoundReady

    fun release() {
        try {
            stopPreview()
            soundPool.release()
            Log.d(TAG, "NmnAudioManager released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing NmnAudioManager", e)
        }
    }

    companion object {
        private const val TAG = "NmnAudioManager"
    }
}