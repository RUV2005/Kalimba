// 文件名: SoundResources.kt
package com.danmo.kalimba

import com.danmo.kalimba.R

/**
 * 共享的音频资源配置
 * 避免在多个Manager中重复定义
 */
object SoundResources {
    val SOUND_LIST = listOf(
        // d行（下排）
        "d4d" to R.raw.d4_down,
        "d6d" to R.raw.d6_down,
        "d1m" to R.raw.d1_middle,
        "d3m" to R.raw.d3_middle,
        "d5m" to R.raw.d5_middle,
        "d7m" to R.raw.d7_middle,
        "d2h" to R.raw.d2_high,
        "d4h" to R.raw.d4_high,
        "d6h" to R.raw.d6_high,
        "d1hh" to R.raw.d1_high_high,
        "d3hh" to R.raw.d3_high_high,
        "d5hh" to R.raw.d5_high_high,
        // u行（上排）
        "u5d" to R.raw.u5_down,
        "u7d" to R.raw.u7_down,
        "u2m" to R.raw.u2_middle,
        "u4m" to R.raw.u4_middle,
        "u6m" to R.raw.u6_middle,
        "u1h" to R.raw.u1_high,
        "u3h" to R.raw.u3_high,
        "u5h" to R.raw.u5_high,
        "u7h" to R.raw.u7_high,
        "u2hh" to R.raw.u2_high_high,
        "u4hh" to R.raw.u4_high_high,
        "u6hh" to R.raw.u6_high_high,
    )
}