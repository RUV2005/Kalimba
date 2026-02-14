// 文件名: SheetMusicEntity.kt
package com.danmo.kalimba.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 简谱实体 - 存储在本地数据库
 */
@Entity(tableName = "sheet_music")
@TypeConverters(Converters::class)
data class SheetMusicEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 基本信息
    val name: String,                      // 简谱名称
    val description: String? = null,       // 简谱描述
    val createdAt: Long,                   // 创建时间戳
    val updatedAt: Long,                   // 更新时间戳

    // 简谱内容 (JSON)
    val segments: String,                  // List<SegmentData> 的 JSON

    // 元数据
    val totalNotes: Int,                   // 总音符数
    val totalSegments: Int,                // 总段落数
    val bpm: Int = 80,                     // 速度

    // 云同步相关
    val cloudId: String? = null,           // 云端 ID
    val isUploaded: Boolean = false,       // 是否已上传到云端
    val lastSyncedAt: Long? = null,        // 最后同步时间
    val needsSync: Boolean = false,        // 是否需要同步

    // 分享相关
    val isPublic: Boolean = false,         // 是否公开分享
    val shareCode: String? = null,         // 分享码
    val downloadCount: Int = 0,            // 下载次数

    // 标签
    val tags: String? = null,              // 标签 (逗号分隔)
    val difficulty: Int = 1                // 难度 (1-5)
)

/**
 * 段落数据 (用于序列化)
 */
data class SegmentData(
    val id: Int,
    val name: String,
    val notes: List<NoteData>
)

/**
 * 音符数据 (用于序列化)
 */
data class NoteData(
    val keyId: String,
    val pitch: Int,
    val octave: String,      // DOWN, MIDDLE, HIGH, HIGH_HIGH
    val isRest: Boolean = false,
    val durationMs: Long = 1000
)

/**
 * Room 类型转换器
 */
class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromSegmentList(value: String?): List<SegmentData> {
        if (value == null) return emptyList()
        return try {
            val listType = object : TypeToken<List<SegmentData>>() {}.type
            gson.fromJson(value, listType)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun toSegmentList(list: List<SegmentData>): String {
        return gson.toJson(list)
    }
}