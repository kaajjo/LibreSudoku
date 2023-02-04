package com.kaajjo.libresudoku.domain.usecase.board

import com.kaajjo.libresudoku.domain.repository.BoardRepository
import javax.inject.Inject

class GetBoardsInFolderWithSavedUseCase @Inject constructor(
    private val boardRepository: BoardRepository
){
    operator fun invoke(folderUid: Long) = boardRepository.getInFolderWithSaved(folderUid)
}