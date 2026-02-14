// 文件名: NmnModels.kt
package com.danmo.kalimba.nmn

import com.danmo.kalimba.data.local.NoteData
import com.danmo.kalimba.data.local.SegmentData

enum class Octave {
    DOWN, MIDDLE, HIGH, HIGH_HIGH
}

data class NmnSegment(
    val id: Int,
    val name: String,
    val notes: List<NmnNote>
)

data class NmnNote(
    val keyId: String,
    val pitch: Int,
    val octave: Octave,
    val isRest: Boolean = false,
    val durationMs: Long = 1000 // 停顿时间，默认1秒
) {
    companion object {
        // 创建休止符（停顿）
        fun createRest(durationMs: Long = 1000): NmnNote {
            return NmnNote(
                keyId = "rest",
                pitch = 0,
                octave = Octave.MIDDLE,
                isRest = true,
                durationMs = durationMs
            )
        }
    }

    // 判断是否为休止符
    fun isRestNote(): Boolean = isRest || keyId == "rest"


    // 建议在 NmnNote / NmnSegment 的定义处添加这些转换扩展
    fun SegmentData.toDomainModel(): NmnSegment {
        return NmnSegment(
            id = this.id,
            name = this.name,
            notes = this.notes.map { it.toDomainModel() }
        )
    }

    fun NoteData.toDomainModel(): NmnNote {
        return if (this.isRest) {
            NmnNote.createRest(this.durationMs)
        } else {
            NmnNote(
                keyId = this.keyId,
                pitch = this.pitch,
                octave = Octave.valueOf(this.octave) // 确保这里的 Octave 枚举一致
            )
        }
    }
}