package com.danmo.kalimba.practice

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
 * 简化后的音频管理器，仅负责 SoundPool 音频播放
 */
class PracticeAudioManager(context: Context) {

    private val soundPool: SoundPool
    private val soundMap = ConcurrentHashMap<String, Int>()
    private val loadedSounds = ConcurrentHashMap<Int, Boolean>()
    private var isSoundReady = false

    // 用于预览的Handler
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

    /**
     * 播放声音，完成后通过回调通知
     */
    fun playSoundWithCallback(keyId: String, onComplete: () -> Unit) {
        try {
            val soundId = soundMap[keyId]
            if (soundId != null && loadedSounds[soundId] == true) {
                soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f)
                // 估算声音时长后回调
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

    /**
     * 预览整段（不包含播报逻辑，纯音频播放）
     */
    fun previewSegment(
        notes: List<PracticeNote>,
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
            Log.d(TAG, "PracticeAudioManager released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing PracticeAudioManager", e)
        }
    }

    companion object {
        private const val TAG = "PracticeAudioManager"
    }
}