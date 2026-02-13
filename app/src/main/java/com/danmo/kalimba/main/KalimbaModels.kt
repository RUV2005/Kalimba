package com.danmo.kalimba.main

enum class Octave {
    DOWN, MIDDLE, HIGH, HIGH_HIGH
}

data class KalimbaKey(
    val id: String,           // 唯一标识，如 "d1m"
    val row: String,          // "d" 或 "u"
    val index: Int,           // 0-11，从左到右
    val pitch: Int,           // 1-7
    val octave: Octave,
    val resId: Int,           // R.raw.xxx
    val displayName: String   // 显示名称
) {
    fun getPositionDescription(): String {
        val rowText = if (row == "d") "下排" else "上排"
        val octaveText = when(octave) {
            Octave.DOWN -> "低音"
            Octave.MIDDLE -> "中音"
            Octave.HIGH -> "高音"
            Octave.HIGH_HIGH -> "倍高音"
        }
        return "${rowText}第${index + 1}键，$octaveText$pitch"
    }
}