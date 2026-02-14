// 文件位置: data/remote/KalimbaApiService.kt
package com.danmo.kalimba.data.remote

import retrofit2.Response
import retrofit2.http.*

// ==================== API 接口定义 ====================

interface KalimbaApiService {
    @POST("api/sheets/upload")
    suspend fun uploadSheet(
        @Header("Authorization") authorization: String,
        @Body request: UploadSheetRequest
    ): Response<UploadResponse>

    @GET("api/sheets/download/share/{shareCode}")
    suspend fun downloadByShareCode(
        @Header("Authorization") authorization: String,
        @Path("shareCode") shareCode: String
    ): Response<DownloadResponse>

    @GET("api/sheets/public")
    suspend fun getPublicSheets(
        @Header("Authorization") authorization: String?,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 30
    ): Response<SheetListResponse>

    @GET("api/sheets/my")
    suspend fun getMySheets(
        @Header("Authorization") authorization: String
    ): Response<SheetListResponse>

    @DELETE("api/sheets/{cloudId}")
    suspend fun deleteSheet(
        @Header("Authorization") authorization: String,
        @Path("cloudId") cloudId: String
    ): Response<DeleteResponse>
}


// 请求数据类
data class UploadSheetRequest(
    val name: String,
    val description: String?,
    val author: String?,
    val segments: List<SegmentDto>,
    val bpm: Int,
    val tags: List<String>?,
    val difficulty: Int,
    val isPublic: Boolean
)

data class SegmentDto(
    val id: String,
    val name: String,
    val notes: List<NoteDto>
)

data class NoteDto(
    val keyId: String,
    val pitch: Int,
    val octave: String,
    val isRest: Boolean,
    val durationMs: Long
)

// 响应数据类
data class UploadResponse(
    val success: Boolean,
    val cloudId: String,
    val shareCode: String?,
    val message: String?
)

data class DownloadResponse(
    val success: Boolean,
    val data: SheetMusicDto,
    val message: String?
)

data class SheetListResponse(
    val success: Boolean,
    val data: List<SheetMusicDto>,
    val total: Int,
    val page: Int,
    val message: String?
)

data class DeleteResponse(
    val success: Boolean,
    val message: String?
)

data class SheetMusicDto(
    val _id: String,
    val name: String,
    val description: String?,
    val author: String?,
    val segments: List<SegmentDto>,
    val totalNotes: Int,
    val totalSegments: Int,
    val bpm: Int,
    val tags: List<String>,
    val difficulty: Int,
    val isPublic: Boolean,
    val shareCode: String,
    val downloadCount: Int,
    val createdAt: String,
    val updatedAt: String
)