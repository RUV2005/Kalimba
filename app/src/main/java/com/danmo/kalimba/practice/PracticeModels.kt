package com.danmo.kalimba.practice

enum class Octave {
    DOWN, MIDDLE, HIGH, HIGH_HIGH
}

data class PracticeSegment(
    val id: Int,
    val name: String,
    val notes: List<PracticeNote>
)

data class PracticeNote(
    val keyId: String,
    val pitch: Int,
    val octave: Octave,
    val isRest: Boolean = false
)