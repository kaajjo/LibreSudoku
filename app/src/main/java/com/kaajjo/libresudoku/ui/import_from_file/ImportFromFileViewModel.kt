package com.kaajjo.libresudoku.ui.import_from_file

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaajjo.libresudoku.core.parser.GsudokuParser
import com.kaajjo.libresudoku.core.parser.OpenSudokuParser
import com.kaajjo.libresudoku.core.parser.SdmParser
import com.kaajjo.libresudoku.core.qqwing.GameDifficulty
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.data.database.model.Folder
import com.kaajjo.libresudoku.data.database.model.SudokuBoard
import com.kaajjo.libresudoku.domain.repository.BoardRepository
import com.kaajjo.libresudoku.domain.usecase.folder.InsertFolderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.ZonedDateTime
import javax.inject.Inject

@HiltViewModel
class ImportFromFileViewModel @Inject constructor(
    private val insertFolderUseCase: InsertFolderUseCase,
    private val boardRepository: BoardRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val fileUri: Uri? by mutableStateOf(savedStateHandle.get<String>("uri")?.toUri())

    // uid of the folder where to add the imported sudoku.
    // If uid = -1, a new folder will be created (ask the user for the folder name)
    val folderUid by mutableLongStateOf(savedStateHandle.get<Long>("folder_uid") ?: -1L)

    var isLoading by mutableStateOf(true)
    var isSaved by mutableStateOf(false)
    var isSaving by mutableStateOf(false)

    private val _sudokuListToImport = MutableStateFlow(emptyList<String>())
    val sudokuListToImport = _sudokuListToImport.asStateFlow()

    var difficultyForImport by mutableStateOf(GameDifficulty.Easy)

    private val _importingError = MutableStateFlow(false)
    val importError = _importingError.asStateFlow()

    fun readData(inputStream: InputStreamReader) {
        viewModelScope.launch(Dispatchers.Default) {
            var toImport = listOf<String>()
            try {
                BufferedReader(inputStream).use { bufferRead ->
                    val contentText = bufferRead.readText()

                    val parser = when {
                        contentText.contains("<opensudoku") -> OpenSudokuParser()

                        contentText.contains("<onegravitysudoku") -> GsudokuParser()

                        else -> SdmParser()
                    }
                    val result = parser.toBoards(contentText)
                    toImport = result.second
                    _importingError.emit(!result.first)
                    isLoading = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _importingError.emit(true)
            } finally {
                withContext(Dispatchers.IO) {
                    inputStream.close()
                }
                if (!_importingError.value) {
                    _sudokuListToImport.emit(toImport.toList())
                }
            }
        }
    }

    fun saveImported(folderName: String? = null) {
        viewModelScope.launch {
            isSaving = true
            var folderUidToImport = folderUid

            if (folderUidToImport == -1L) {
                folderUidToImport = insertFolderUseCase(
                    Folder(
                        uid = 0,
                        name = folderName ?: sudokuListToImport.value.size.toString(),
                        createdAt = ZonedDateTime.now()
                    )
                )
            }

            val sudokuBoardsToImport = mutableListOf<SudokuBoard>()
            sudokuListToImport.value.forEach {
                sudokuBoardsToImport.add(
                    SudokuBoard(
                        uid = 0,
                        initialBoard = it,
                        solvedBoard = "",
                        difficulty = difficultyForImport,
                        type = GameType.Default9x9,
                        folderId = folderUidToImport
                    )
                )
            }
            boardRepository.insert(sudokuBoardsToImport)
            _sudokuListToImport.emit(emptyList())
            isSaving = false
            isSaved = true
        }
    }

    fun setDifficulty(difficulty: GameDifficulty) {
        difficultyForImport = difficulty
    }
}