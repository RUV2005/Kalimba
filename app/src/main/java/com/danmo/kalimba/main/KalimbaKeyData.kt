// 文件名: KalimbaKeyData.kt
package com.danmo.kalimba.main

import com.danmo.kalimba.R

object KalimbaKeyData {

    val dRowKeys = listOf(
        KalimbaKey("d4d", "d", 0, 4, Octave.DOWN, R.raw.d4_down, "4̣"),
        KalimbaKey("d6d", "d", 1, 6, Octave.DOWN, R.raw.d6_down, "6̣"),
        KalimbaKey("d1m", "d", 2, 1, Octave.MIDDLE, R.raw.d1_middle, "1"),
        KalimbaKey("d3m", "d", 3, 3, Octave.MIDDLE, R.raw.d3_middle, "3"),
        KalimbaKey("d5m", "d", 4, 5, Octave.MIDDLE, R.raw.d5_middle, "5"),
        KalimbaKey("d7m", "d", 5, 7, Octave.MIDDLE, R.raw.d7_middle, "7"),
        KalimbaKey("d2h", "d", 6, 2, Octave.HIGH, R.raw.d2_high, "2́"),
        KalimbaKey("d4h", "d", 7, 4, Octave.HIGH, R.raw.d4_high, "4́"),
        // 修正：d6h 是高音6，不是倍高音
        KalimbaKey("d6h", "d", 8, 6, Octave.HIGH, R.raw.d6_high, "6́"),
        KalimbaKey("d1hh", "d", 9, 1, Octave.HIGH_HIGH, R.raw.d1_high_high, "1́́"),
        KalimbaKey("d3hh", "d", 10, 3, Octave.HIGH_HIGH, R.raw.d3_high_high, "3́́"),
        KalimbaKey("d5hh", "d", 11, 5, Octave.HIGH_HIGH, R.raw.d5_high_high, "5́́"),
    )

    val uRowKeys = listOf(
        KalimbaKey("u5d", "u", 0, 5, Octave.DOWN, R.raw.u5_down, "5̣"),
        KalimbaKey("u7d", "u", 1, 7, Octave.DOWN, R.raw.u7_down, "7̣"),
        KalimbaKey("u2m", "u", 2, 2, Octave.MIDDLE, R.raw.u2_middle, "2"),
        KalimbaKey("u4m", "u", 3, 4, Octave.MIDDLE, R.raw.u4_middle, "4"),
        KalimbaKey("u6m", "u", 4, 6, Octave.MIDDLE, R.raw.u6_middle, "6"),
        KalimbaKey("u1h", "u", 5, 1, Octave.HIGH, R.raw.u1_high, "1́"),
        KalimbaKey("u3h", "u", 6, 3, Octave.HIGH, R.raw.u3_high, "3́"),
        KalimbaKey("u5h", "u", 7, 5, Octave.HIGH, R.raw.u5_high, "5́"),
        KalimbaKey("u7h", "u", 8, 7, Octave.HIGH, R.raw.u7_high, "7́"),
        KalimbaKey("u2hh", "u", 9, 2, Octave.HIGH_HIGH, R.raw.u2_high_high, "2́́"),
        KalimbaKey("u4hh", "u", 10, 4, Octave.HIGH_HIGH, R.raw.u4_high_high, "4́́"),
        KalimbaKey("u6hh", "u", 11, 6, Octave.HIGH_HIGH, R.raw.u6_high_high, "6́́"),
    )

    fun getAllKeys() = dRowKeys + uRowKeys
}