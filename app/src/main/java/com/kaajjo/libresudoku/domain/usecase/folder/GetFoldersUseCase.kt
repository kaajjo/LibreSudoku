package com.kaajjo.libresudoku.domain.usecase.folder

import com.kaajjo.libresudoku.data.database.model.Folder
import com.kaajjo.libresudoku.domain.repository.FolderRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFoldersUseCase @Inject constructor(
    private val folderRepository: FolderRepository
) {
    operator fun invoke(): Flow<List<Folder>> = folderRepository.getAll()
}