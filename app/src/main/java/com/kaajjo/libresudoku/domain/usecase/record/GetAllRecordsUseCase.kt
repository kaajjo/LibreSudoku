package com.kaajjo.libresudoku.domain.usecase.record

import com.kaajjo.libresudoku.core.qqwing.GameDifficulty
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.domain.repository.RecordRepository
import javax.inject.Inject

class GetAllRecordsUseCase @Inject constructor(
    private val recordRepository: RecordRepository
) {
    operator fun invoke(difficulty: GameDifficulty, type: GameType) = recordRepository.getAll(difficulty, type)
}