// 文件名: SheetMusicDao.kt
package com.danmo.kalimba.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 简谱数据访问对象
 */
@Dao
interface SheetMusicDao {

    // ==================== 基本 CRUD ====================

    @Query("SELECT * FROM sheet_music ORDER BY updatedAt DESC")
    fun getAllSheets(): Flow<List<SheetMusicEntity>>

    @Query("SELECT * FROM sheet_music WHERE id = :id")
    suspend fun getSheetById(id: Long): SheetMusicEntity?

    @Query("SELECT * FROM sheet_music WHERE cloudId = :cloudId")
    suspend fun getSheetByCloudId(cloudId: String): SheetMusicEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSheet(sheet: SheetMusicEntity): Long

    @Update
    suspend fun updateSheet(sheet: SheetMusicEntity)

    @Delete
    suspend fun deleteSheet(sheet: SheetMusicEntity)

    @Query("DELETE FROM sheet_music WHERE id = :id")
    suspend fun deleteSheetById(id: Long)

    // ==================== 搜索与过滤 ====================

    @Query("SELECT * FROM sheet_music WHERE name LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchSheets(query: String): Flow<List<SheetMusicEntity>>

    @Query("SELECT * FROM sheet_music WHERE tags LIKE '%' || :tag || '%' ORDER BY updatedAt DESC")
    fun getSheetsByTag(tag: String): Flow<List<SheetMusicEntity>>

    @Query("SELECT * FROM sheet_music WHERE difficulty = :difficulty ORDER BY updatedAt DESC")
    fun getSheetsByDifficulty(difficulty: Int): Flow<List<SheetMusicEntity>>

    // ==================== 云同步相关 ====================

    @Query("SELECT * FROM sheet_music WHERE needsSync = 1")
    suspend fun getSheetsNeedingSync(): List<SheetMusicEntity>

    @Query("SELECT * FROM sheet_music WHERE isUploaded = 0")
    suspend fun getUnsyncedSheets(): List<SheetMusicEntity>

    @Query("UPDATE sheet_music SET isUploaded = :uploaded, cloudId = :cloudId, lastSyncedAt = :timestamp WHERE id = :id")
    suspend fun markAsSynced(id: Long, uploaded: Boolean, cloudId: String, timestamp: Long)
    

    @Query("UPDATE sheet_music SET needsSync = :needsSync WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, needsSync: Boolean)



    // ==================== 统计 ====================

    @Query("SELECT COUNT(*) FROM sheet_music")
    suspend fun getSheetCount(): Int

    @Query("SELECT SUM(totalNotes) FROM sheet_music")
    suspend fun getTotalNotesCount(): Int?

    @Query("SELECT COUNT(*) FROM sheet_music WHERE isUploaded = 1")
    suspend fun getUploadedSheetCount(): Int
}