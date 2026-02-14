// 文件名: data/SheetMusicRepository.kt
package com.danmo.kalimba.data

import com.danmo.kalimba.data.local.*
import com.danmo.kalimba.data.remote.*
import kotlinx.coroutines.flow.Flow

class SheetMusicRepository(
    private val localDataSource: SheetMusicDao,
    private val remoteDataSource: KalimbaApiService,
    private val authDataStore: AuthDataStore
) {

    fun getAllLocalSheets(): Flow<List<SheetMusicEntity>> =
        localDataSource.getAllSheets()

    suspend fun saveSheet(sheet: SheetMusicEntity): Long =
        localDataSource.insertSheet(sheet)

    suspend fun deleteSheet(sheet: SheetMusicEntity) =
        localDataSource.deleteSheet(sheet)

    // ✅ 上传简谱到云端
    suspend fun uploadSheet(sheet: SheetMusicEntity): Result<UploadResponse> {
        return try {
            val token = authDataStore.getAccessToken()
            if (token == null) {
                return Result.failure(Exception("未登录"))
            }

            val request = sheet.toUploadRequest()
            val response = remoteDataSource.uploadSheet("Bearer $token", request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                // 更新本地记录
                localDataSource.markAsSynced(
                    sheet.id,
                    true,
                    body.cloudId,
                    System.currentTimeMillis()
                )

                // 更新完整记录
                localDataSource.updateSheet(
                    sheet.copy(
                        isUploaded = true,
                        cloudId = body.cloudId,
                        shareCode = body.shareCode,
                        lastSyncedAt = System.currentTimeMillis()
                    )
                )

                Result.success(body)
            } else {
                Result.failure(Exception("上传失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ✅ 通过分享码下载简谱
    suspend fun downloadByShareCode(shareCode: String): Result<SheetMusicDto> {
        return try {
            val token = authDataStore.getAccessToken()
            if (token == null) {
                return Result.failure(Exception("未登录"))
            }

            val response = remoteDataSource.downloadByShareCode(
                authorization = "Bearer $token",
                shareCode = shareCode
            )

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                val entity = body.data.toEntity()
                val newId: Long = localDataSource.insertSheet(entity)

                Result.success(body.data)
            } else {
                Result.failure(Exception("下载失败: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }



    // ✅ 获取公开简谱列表
    suspend fun getPublicSheets(page: Int = 1, search: String? = null): Result<List<SheetMusicDto>> {
        return try {
            val response = remoteDataSource.getPublicSheets(
                authorization = null, // 公开列表不需要认证
                page = page,
                limit = 20
            )

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.data)
            } else {
                Result.failure(Exception("获取失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ✅ 获取我的云端简谱
    suspend fun getMyCloudSheets(): Result<List<SheetMusicDto>> {
        return try {
            val token = authDataStore.getAccessToken()
                ?: return Result.failure(Exception("未登录"))

            val response = remoteDataSource.getMySheets("Bearer $token")

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.data)
            } else {
                Result.failure(Exception("获取失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ✅ 删除云端简谱
    suspend fun deleteCloudSheet(cloudId: String): Result<Boolean> {
        return try {
            val token = authDataStore.getAccessToken()
                ?: return Result.failure(Exception("未登录"))

            val response = remoteDataSource.deleteSheet("Bearer $token", cloudId)

            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(true)
            } else {
                Result.failure(Exception("删除失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// ==================== 扩展函数 ====================

fun SheetMusicEntity.toUploadRequest() = UploadSheetRequest(
    name = name,
    description = description,
    author = null, // 后端会从 token 中获取
    segments = Converters().fromSegmentList(segments).map { it.toDto() },
    bpm = bpm,
    tags = tags?.split(",")?.filter { it.isNotBlank() }, // ✅ 处理空标签
    difficulty = difficulty,
    isPublic = isPublic
)

fun SegmentData.toDto() = SegmentDto(
    id = id.toString(), // ✅ Int -> String
    name = name,
    notes = notes.map { it.toDto() }
)

fun NoteData.toDto() = NoteDto(
    keyId = keyId,
    pitch = pitch,
    octave = octave, // ✅ 都是 String，直接传递
    isRest = isRest,
    durationMs = durationMs
)

// ✅ 关键修复：toEntity() - 正确处理所有字段
fun SheetMusicDto.toEntity() = SheetMusicEntity(
    name = name,
    description = description,
    createdAt = parseDateToTimestamp(createdAt),
    updatedAt = parseDateToTimestamp(updatedAt),
    segments = Converters().toSegmentList(
        segments.map { it.toEntity() }
    ),
    totalNotes = totalNotes,
    totalSegments = totalSegments,
    bpm = bpm,
    cloudId = _id,
    isUploaded = true,
    lastSyncedAt = System.currentTimeMillis(),
    isPublic = isPublic,
    shareCode = shareCode,
    downloadCount = downloadCount,
    tags = tags.joinToString(","), // ✅ List<String> -> String
    difficulty = difficulty
)

fun SegmentDto.toEntity() = SegmentData(
    id = id.toIntOrNull() ?: 0, // ✅ String -> Int
    name = name,
    notes = notes.map { it.toEntity() }
)

fun NoteDto.toEntity() = NoteData(
    keyId = keyId,
    pitch = pitch,
    octave = octave, // ✅ 都是 String
    isRest = isRest,
    durationMs = durationMs
)

// 辅助函数：将 ISO 日期字符串转为时间戳
private fun parseDateToTimestamp(dateString: String): Long {
    return try {
        // 尝试解析带毫秒的格式
        val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
        format.timeZone = java.util.TimeZone.getTimeZone("UTC")
        format.parse(dateString)?.time ?: System.currentTimeMillis()
    } catch (e: Exception) {
        try {
            // 尝试不带毫秒的格式
            val format2 = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault())
            format2.timeZone = java.util.TimeZone.getTimeZone("UTC")
            format2.parse(dateString)?.time ?: System.currentTimeMillis()
        } catch (e2: Exception) {
            System.currentTimeMillis()
        }
    }
}