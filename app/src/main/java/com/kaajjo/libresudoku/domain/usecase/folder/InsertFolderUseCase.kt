package com.kaajjo.libresudoku.domain.usecase.folder

import com.kaajjo.libresudoku.data.database.model.Folder
import com.kaajjo.libresudoku.domain.repository.FolderRepository
import javax.inject.Inject

class InsertFolderUseCase @Inject constructor(
    private val folderRepository: FolderRepository
) {
    suspend operator fun invoke(folder: Folder) = folderRepository.insert(folder)
}