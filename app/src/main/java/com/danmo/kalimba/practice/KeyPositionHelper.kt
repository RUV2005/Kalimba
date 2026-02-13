package com.danmo.kalimba.practice

import androidx.compose.ui.graphics.Color

object KeyPositionHelper {

    private val KEY_INFO = mapOf(
        // d行（下排）
        "d4d" to Triple("下排第1键", "低音4", Color(0xFF90CAF9)),
        "d6d" to Triple("下排第2键", "低音6", Color(0xFF90CAF9)),
        "d1m" to Triple("下排第3键", "中音1", Color(0xFF64B5F6)),
        "d3m" to Triple("下排第4键", "中音3", Color(0xFF64B5F6)),
        "d5m" to Triple("下排第5键", "中音5", Color(0xFF64B5F6)),
        "d7m" to Triple("下排第6键", "中音7", Color(0xFF64B5F6)),
        "d2h" to Triple("下排第7键", "高音2", Color(0xFF42A5F5)),
        "d4h" to Triple("下排第8键", "高音4", Color(0xFF42A5F5)),
        "d6h" to Triple("下排第9键", "高音6", Color(0xFF2196F3)),
        "d1hh" to Triple("下排第10键", "倍高音1", Color(0xFF2196F3)),
        "d3hh" to Triple("下排第11键", "倍高音3", Color(0xFF2196F3)),
        "d5hh" to Triple("下排第12键", "倍高音5", Color(0xFF2196F3)),
        // u行（上排）
        "u5d" to Triple("上排第1键", "低音5", Color(0xFFA5D6A7)),
        "u7d" to Triple("上排第2键", "低音7", Color(0xFFA5D6A7)),
        "u2m" to Triple("上排第3键", "中音2", Color(0xFF81C784)),
        "u4m" to Triple("上排第4键", "中音4", Color(0xFF81C784)),
        "u6m" to Triple("上排第5键", "中音6", Color(0xFF81C784)),
        "u1h" to Triple("上排第6键", "高音1", Color(0xFF66BB6A)),
        "u3h" to Triple("上排第7键", "高音3", Color(0xFF66BB6A)),
        "u5h" to Triple("上排第8键", "高音5", Color(0xFF66BB6A)),
        "u7h" to Triple("上排第9键", "高音7", Color(0xFF66BB6A)),
        "u2hh" to Triple("上排第10键", "倍高音2", Color(0xFF4CAF50)),
        "u4hh" to Triple("上排第11键", "倍高音4", Color(0xFF4CAF50)),
        "u6hh" to Triple("上排第12键", "倍高音6", Color(0xFF4CAF50)),
    )

    fun getPositionDescription(keyId: String): String {
        val info = KEY_INFO[keyId] ?: return "未知键位"
        return "${info.first}，${info.second}"
    }

    fun getDisplayName(keyId: String): String {
        return when(keyId) {
            "d4d" -> "4̣"; "d6d" -> "6̣"; "d1m" -> "1"; "d3m" -> "3"
            "d5m" -> "5"; "d7m" -> "7"; "d2h" -> "2́"; "d4h" -> "4́"
            "d6h" -> "6́"; "d1hh" -> "1́́"; "d3hh" -> "3́́"; "d5hh" -> "5́́"
            "u5d" -> "5̣"; "u7d" -> "7̣"; "u2m" -> "2"; "u4m" -> "4"
            "u6m" -> "6"; "u1h" -> "1́"; "u3h" -> "3́"; "u5h" -> "5́"
            "u7h" -> "7́"; "u2hh" -> "2́́"; "u4hh" -> "4́́"; "u6hh" -> "6́́"
            else -> "?"
        }
    }

}