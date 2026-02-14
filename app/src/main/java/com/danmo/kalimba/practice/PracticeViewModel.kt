// PracticeViewModel.kt
package com.danmo.kalimba.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.danmo.kalimba.data.local.KalimbaDatabase
import com.danmo.kalimba.data.local.NoteData
import com.danmo.kalimba.data.local.SegmentData
import com.danmo.kalimba.data.local.SheetMusicEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PracticeUiState(
    val isLoading: Boolean = true,
    val sheetName: String = "",
    val segments: List<PracticeSegment> = emptyList(),
    val bpm: Int = 80,
    val error: String? = null
)

class PracticeViewModel(
    private val sheetId: Long,
    database: KalimbaDatabase
) : ViewModel() {

    private val dao = database.sheetMusicDao()

    private val _uiState = MutableStateFlow(PracticeUiState())
    val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()

    init {
        loadSheet()
    }

    private fun loadSheet() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val entity = dao.getSheetById(sheetId)
                if (entity == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "找不到该简谱"
                    )
                    return@launch
                }

                val segments = parseSegments(entity)

                _uiState.value = PracticeUiState(
                    isLoading = false,
                    sheetName = entity.name,
                    segments = segments,
                    bpm = entity.bpm,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "加载失败: ${e.message}"
                )
            }
        }
    }

    private fun parseSegments(entity: SheetMusicEntity): List<PracticeSegment> {
        // 解析 JSON 中的段落数据
        val converter = com.danmo.kalimba.data.local.Converters()
        val segmentDataList = converter.fromSegmentList(entity.segments)

        return segmentDataList.mapIndexed { index, segmentData ->
            PracticeSegment(
                id = segmentData.id,
                name = segmentData.name,
                notes = segmentData.notes.map { noteData ->
                    PracticeNote(
                        keyId = noteData.keyId,
                        pitch = noteData.pitch,
                        octave = parseOctave(noteData.octave),
                        isRest = noteData.isRest
                    )
                }
            )
        }
    }

    private fun parseOctave(octaveStr: String): Octave {
        return when (octaveStr.uppercase()) {
            "DOWN" -> Octave.DOWN
            "HIGH" -> Octave.HIGH
            "HIGH_HIGH" -> Octave.HIGH_HIGH
            else -> Octave.MIDDLE
        }
    }

    class Factory(
        private val sheetId: Long,
        private val database: KalimbaDatabase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PracticeViewModel(sheetId, database) as T
        }
    }
}