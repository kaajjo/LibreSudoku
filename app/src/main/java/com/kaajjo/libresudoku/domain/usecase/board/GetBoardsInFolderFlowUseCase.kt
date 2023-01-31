package com.kaajjo.libresudoku.domain.usecase.board

import com.kaajjo.libresudoku.domain.repository.BoardRepository
import javax.inject.Inject

class GetBoardsInFolderFlowUseCase @Inject constructor(
    private val boardRepository: BoardRepository
) {
    operator fun invoke(uid: Long) = boardRepository.getBoardsInFolderFlow(uid)
}