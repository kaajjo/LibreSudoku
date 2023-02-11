package com.kaajjo.libresudoku.domain.usecase.folder

import com.kaajjo.libresudoku.domain.repository.FolderRepository
import javax.inject.Inject

class GetLastSavedGamesAnyFolderUseCase @Inject constructor(
    private val folderRepository: FolderRepository
) {
    operator fun invoke(gamesCount: Int) = folderRepository.getLastSavedGamesAnyFolder(gamesCount)
}