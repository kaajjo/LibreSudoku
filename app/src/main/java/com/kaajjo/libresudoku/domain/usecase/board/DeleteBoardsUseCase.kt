package com.kaajjo.libresudoku.domain.usecase.board

import com.kaajjo.libresudoku.data.database.model.SudokuBoard
import com.kaajjo.libresudoku.domain.repository.BoardRepository
import javax.inject.Inject

class DeleteBoardsUseCase @Inject constructor(
    private val boardRepository: BoardRepository
) {
    suspend operator fun invoke(boards: List<SudokuBoard>) = boardRepository.delete(boards)
}