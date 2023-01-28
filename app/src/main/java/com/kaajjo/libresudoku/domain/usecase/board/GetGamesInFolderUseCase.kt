package com.kaajjo.libresudoku.domain.usecase.board

import com.kaajjo.libresudoku.domain.repository.BoardRepository
import javax.inject.Inject

class GetGamesInFolderUseCase @Inject constructor(
    private val boardRepository: BoardRepository
) {
    operator fun invoke(folderUid: Long) = boardRepository.getAllInFolderList(folderUid)
}