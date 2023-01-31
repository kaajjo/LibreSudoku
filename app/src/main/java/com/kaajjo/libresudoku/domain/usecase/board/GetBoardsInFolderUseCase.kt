package com.kaajjo.libresudoku.domain.usecase.board

import com.kaajjo.libresudoku.domain.repository.BoardRepository
import javax.inject.Inject

class GetBoardsInFolderUseCase @Inject constructor(
    private val boardRepository: BoardRepository
) {
    operator fun invoke(uid: Long) = boardRepository.getBoardsInFolder(uid)
}