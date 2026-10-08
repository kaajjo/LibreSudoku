package com.kaajjo.libresudoku.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kaajjo.libresudoku.core.generator.rating.LogicTier
import com.kaajjo.libresudoku.core.generator.rating.Technique
import com.kaajjo.libresudoku.core.qqwing.GameDifficulty
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.core.qqwing.models.RatingMetadata
import com.kaajjo.libresudoku.data.database.AppDatabase
import com.kaajjo.libresudoku.data.database.model.SudokuBoard
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RatingMetadataMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrateExistingBoardAndStoreNewRating() = runBlocking {
        helper.createDatabase(DATABASE_NAME, 6).apply {
            execSQL(
                "INSERT INTO board (uid, initial_board, solved_board, difficulty, type) " +
                    "VALUES (42, '100000', '123456', 4, 0)"
            )
            close()
        }
        helper.runMigrationsAndValidate(DATABASE_NAME, 7, true, AppDatabase.MIGRATION_6_7).close()

        val database = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
            DATABASE_NAME
        ).addMigrations(AppDatabase.MIGRATION_6_7).build()
        try {
            val old = database.boardDao().get(42)
            assertNull(old.ratingMetadata)
            assertEquals("100000", old.initialBoard)
            assertEquals("123456", old.solvedBoard)
            assertEquals(GameDifficulty.Hard, old.difficulty)

            val metadata = RatingMetadata(
                "libresudoku-logic-v3.0.0:chain12:als3:policy-v3",
                LogicTier.CHAINS,
                mapOf(Technique.SINGLE to 30, Technique.X_CHAIN to 2, Technique.ALS_XZ to 1),
                effortScore = 970,
                scorePolicy = "libresudoku-effort-v1"
            )
            val board = SudokuBoard(
                uid = 0,
                initialBoard = "200000",
                solvedBoard = "234561",
                difficulty = GameDifficulty.Challenge,
                type = GameType.Default6x6,
                ratingMetadata = metadata
            )
            val uid = database.boardDao().insert(board)
            assertEquals(board.copy(uid = uid), database.boardDao().get(uid))
            database.openHelper.readableDatabase.query(
                "SELECT rating_metadata FROM board WHERE uid = ?", arrayOf(uid)
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                val stored = cursor.getString(0)
                assertTrue(stored.contains("\"tier\":\"CHAINS\""))
                assertTrue(stored.contains("\"ALS_XZ\":1"))
                assertTrue(stored.contains("\"effortScore\":970"))
                assertTrue(stored.contains("\"scorePolicy\":\"libresudoku-effort-v1\""))
            }
            assertNull(database.boardDao().get(42).ratingMetadata)

            // Rating v2 and v3 coexist in the same JSON column without another schema migration.
            database.openHelper.writableDatabase.execSQL(
                "UPDATE board SET rating_metadata = ? WHERE uid = 42",
                arrayOf(
                    """{"version":"v2","tier":"SINGLES","techniqueCounts":{"SINGLE":40},"logicallySolved":true}"""
                )
            )
            val historicalMetadata = requireNotNull(database.boardDao().get(42).ratingMetadata)
            assertEquals("v2", historicalMetadata.version)
            assertNull(historicalMetadata.effortScore)
            assertNull(historicalMetadata.scorePolicy)
        } finally {
            database.close()
        }
    }

    private companion object {
        const val DATABASE_NAME = "rating-metadata-migration-test"
    }
}
