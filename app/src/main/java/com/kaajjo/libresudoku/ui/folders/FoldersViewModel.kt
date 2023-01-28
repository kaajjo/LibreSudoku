package com.kaajjo.libresudoku.ui.folders

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaajjo.libresudoku.core.utils.SdmParser
import com.kaajjo.libresudoku.data.database.model.Folder
import com.kaajjo.libresudoku.domain.usecase.board.GetGamesInFolderUseCase
import com.kaajjo.libresudoku.domain.usecase.folder.DeleteFolderUseCase
import com.kaajjo.libresudoku.domain.usecase.folder.GetFoldersUseCase
import com.kaajjo.libresudoku.domain.usecase.folder.InsertFolderUseCase
import com.kaajjo.libresudoku.domain.usecase.folder.UpdateFolderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import javax.inject.Inject

@HiltViewModel
class FoldersViewModel @Inject constructor(
    getFoldersUseCase: GetFoldersUseCase,
    private val insertFolderUseCase: InsertFolderUseCase,
    private val updateFolderUseCase: UpdateFolderUseCase,
    private val deleteFolderUseCase: DeleteFolderUseCase,
    private val getGamesInFolderUseCase: GetGamesInFolderUseCase
) : ViewModel() {
    val folders = getFoldersUseCase()

    @OptIn(ExperimentalMaterialApi::class)
    var drawerState: ModalBottomSheetState = ModalBottomSheetState(
        ModalBottomSheetValue.Hidden
    )

    var selectedFolder: Folder? by mutableStateOf(null)

    private val _sudokuListToImport = MutableStateFlow(emptyList<String>())
    val sudokuListToImport = _sudokuListToImport.asStateFlow()

    fun createFolder(name: String) {
        viewModelScope.launch {
            insertFolderUseCase(
                Folder(
                    uid = 0,
                    name = name.trim(),
                    createdAt = ZonedDateTime.now()
                )
            )
        }
    }

    fun renameFolder(newName: String) {
        selectedFolder?.let {
            viewModelScope.launch {
                updateFolderUseCase(
                    it.copy(name = newName.trim())
                )
            }
        }
    }

    fun deleteFolder() {
        selectedFolder?.let {
            viewModelScope.launch {
                deleteFolderUseCase(it)
            }
        }
    }

    fun generateFolderExportData(): ByteArray {
        val gamesInFolder = getGamesInFolderUseCase(selectedFolder!!.uid)

        val sdmParser = SdmParser()
        return sdmParser.boardsToSdm(gamesInFolder).toByteArray()
    }
}