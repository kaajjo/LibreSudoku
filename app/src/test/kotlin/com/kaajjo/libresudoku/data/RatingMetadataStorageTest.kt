package com.kaajjo.libresudoku.data

import com.kaajjo.libresudoku.core.generator.rating.LogicTier
import com.kaajjo.libresudoku.core.generator.rating.Technique
import com.kaajjo.libresudoku.core.qqwing.GameDifficulty
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.core.qqwing.models.RatingMetadata
import com.kaajjo.libresudoku.data.backup.BackupData
import com.kaajjo.libresudoku.data.database.converters.RatingMetadataConverter
import com.kaajjo.libresudoku.data.database.model.SudokuBoard
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

class RatingMetadataStorageTest {
    private val converter = RatingMetadataConverter()
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    @Test
    fun unratedDatabaseRowsRemainNull() {
        assertNull(converter.fromMetadata(null))
        assertNull(converter.toMetadata(null))
    }

    @Test
    fun everyTierAndTechniqueRoundTripsUsingNames() {
        for (tier in LogicTier.entries) {
            val metadata = RatingMetadata(
                version = "libresudoku-logic-v3.0.0:chain12:als3:policy-v3",
                tier = tier,
                techniqueCounts = Technique.entries.associateWith { it.name.length },
                logicallySolved = false,
                effortScore = 420,
                scorePolicy = "libresudoku-effort-v1"
            )
            val encoded = requireNotNull(converter.fromMetadata(metadata))
            assertEquals(metadata, converter.toMetadata(encoded))
            val stored = json.parseToJsonElement(encoded).jsonObject
            assertEquals(tier.name, stored.getValue("tier").jsonPrimitive.content)
            assertEquals(
                Technique.entries.map { it.name }.toSet(),
                stored.getValue("techniqueCounts").jsonObject.keys
            )
        }
    }

    @Test
    fun stableFixtureKeepsOriginalVersionAndRecognizesExtendedTechniques() {
        val stored = """
            {
              "version": "libresudoku-logic-v2.0.0:chain12:als3:policy-v1",
              "tier": "CHAINS",
              "techniqueCounts": {"SINGLE": 27, "X_CHAIN": 2, "ALS_XZ": 1},
              "logicallySolved": true,
              "futureDiagnostic": "ignored"
            }
        """.trimIndent()
        val restored = converter.toMetadata(stored)
        assertEquals(
            RatingMetadata(
                "libresudoku-logic-v2.0.0:chain12:als3:policy-v1",
                LogicTier.CHAINS,
                mapOf(Technique.SINGLE to 27, Technique.X_CHAIN to 2, Technique.ALS_XZ to 1)
            ),
            restored
        )
        assertNull(restored?.effortScore)
        assertNull(restored?.scorePolicy)
        assertEquals(restored, converter.toMetadata(converter.fromMetadata(restored)))
    }

    @Test
    fun explicitNullScoresAndPolicyRemainAbsent() {
        val stored = """
            {
              "version":"historical-model", "tier":"SINGLES", "techniqueCounts":{"SINGLE":40},
              "logicallySolved":true, "effortScore":null, "scorePolicy":null
            }
        """.trimIndent()
        val restored = requireNotNull(converter.toMetadata(stored))
        assertNull(restored.effortScore)
        assertNull(restored.scorePolicy)
        val encoded = json.parseToJsonElement(requireNotNull(converter.fromMetadata(restored))).jsonObject
        assertTrue(encoded["effortScore"] == null || encoded["effortScore"] == JsonNull)
        assertTrue(encoded["scorePolicy"] == null || encoded["scorePolicy"] == JsonNull)
    }

    @Test
    fun zeroScoresAndUnknownPolicyIdsArePreservedWithoutRecalculation() {
        val stored = """
            {
              "version":"future-model", "tier":"SINGLES", "techniqueCounts":{"SINGLE":40},
              "logicallySolved":true, "effortScore":0, "scorePolicy":"future-policy",
              "futureDiagnostic":{"extraSteps":12}
            }
        """.trimIndent()
        val restored = requireNotNull(converter.toMetadata(stored))
        assertEquals(0, restored.effortScore)
        assertEquals("future-policy", restored.scorePolicy)
        assertEquals(restored, converter.toMetadata(converter.fromMetadata(restored)))
    }

    @Test
    fun scoreFieldsAreNeverInferredFromEachOther() {
        val base = """
            "version":"v-next","tier":"SINGLES","techniqueCounts":{},"logicallySolved":true
        """.trimIndent()
        val withScore = requireNotNull(converter.toMetadata("{$base,\"effortScore\":25}"))
        assertEquals(25, withScore.effortScore)
        assertNull(withScore.scorePolicy)
        val withPolicy = requireNotNull(converter.toMetadata("{$base,\"scorePolicy\":\"historical-policy\"}"))
        assertNull(withPolicy.effortScore)
        assertEquals("historical-policy", withPolicy.scorePolicy)
    }

    @Test
    fun negativeScoresAreRejectedAtTheModelAndStorageBoundaries() {
        assertThrows(IllegalArgumentException::class.java) {
            RatingMetadata("v3", LogicTier.SINGLES, emptyMap(), effortScore = -1)
        }
        assertThrows(SerializationException::class.java) {
            converter.toMetadata(
                """{"version":"v3","tier":"SINGLES","techniqueCounts":{},"logicallySolved":true,"effortScore":-1}"""
            )
        }
    }

    @Test
    fun missingVersionAndUnknownOrOrdinalIdsAreNotReinterpreted() {
        val invalid = listOf(
            """{"tier":"SINGLES","techniqueCounts":{},"logicallySolved":true}""",
            """{"version":"v-next","tier":"FUTURE_TIER","techniqueCounts":{},"logicallySolved":true}""",
            """{"version":"v-next","tier":"SINGLES","techniqueCounts":{"FUTURE_TECHNIQUE":1},"logicallySolved":true}""",
            """{"version":"v2","tier":"1","techniqueCounts":{"0":1},"logicallySolved":true}"""
        )
        invalid.forEach { stored ->
            assertThrows(SerializationException::class.java) { converter.toMetadata(stored) }
        }
    }

    @Test
    fun oldBackupLoadsWithoutAttributingANewRating() {
        val oldBackup = """
            {
              "appVersionName":"2.0.2", "appVersionCode":22, "backupSchemeVersion":1,
              "createdAt":1700000000,
              "boards":[{
                "uid":42, "initialBoard":"100000", "solvedBoard":"123456",
                "difficulty":"Hard", "type":"Default6x6", "folderId":null, "killerCages":null
              }],
              "savedGames":[]
            }
        """.trimIndent()
        val restored = json.decodeFromString<BackupData>(oldBackup)
        assertNull(restored.boards.single().ratingMetadata)
        assertEquals(GameDifficulty.Hard, restored.boards.single().difficulty)
        assertNull(json.decodeFromString<BackupData>(json.encodeToString(restored)).boards.single().ratingMetadata)
    }

    @Test
    fun oldRatedBackupDoesNotAcquireAScoreWhenRestoredAndExported() {
        val oldBackup = """
            {
              "appVersionName":"2.0.2", "appVersionCode":22, "backupSchemeVersion":1,
              "createdAt":1700000000,
              "boards":[{
                "uid":42, "initialBoard":"100000", "solvedBoard":"123456",
                "difficulty":"Hard", "type":"Default6x6",
                "ratingMetadata":{
                  "version":"libresudoku-logic-v2.0.0:chain12:als3:policy-v1", "tier":"ADVANCED",
                  "techniqueCounts":{"SINGLE":30,"X_WING":1}, "logicallySolved":true
                }
              }],
              "savedGames":[]
            }
        """.trimIndent()
        val restored = json.decodeFromString<BackupData>(oldBackup)
        val exported = json.decodeFromString<BackupData>(json.encodeToString(restored))
        val metadata = requireNotNull(exported.boards.single().ratingMetadata)
        assertEquals("libresudoku-logic-v2.0.0:chain12:als3:policy-v1", metadata.version)
        assertNull(metadata.effortScore)
        assertNull(metadata.scorePolicy)
        assertEquals(restored.boards, exported.boards)
    }

    @Test
    fun backupRetainsMetadataAndUnratedBoardsTogether() {
        val metadata = RatingMetadata(
            "libresudoku-logic-v3.0.0:chain12:als3:policy-v3",
            LogicTier.ADVANCED,
            mapOf(Technique.SINGLE to 30, Technique.X_WING to 1),
            effortScore = 290,
            scorePolicy = "libresudoku-effort-v1"
        )
        val board = SudokuBoard(42, "100000", "123456", GameDifficulty.Hard, GameType.Default6x6)
        val original = BackupData(
            appVersionName = "test",
            appVersionCode = 22,
            createdAt = ZonedDateTime.ofInstant(Instant.ofEpochSecond(1700000000), ZoneOffset.UTC),
            boards = listOf(board, board.copy(uid = 43, ratingMetadata = metadata)),
            savedGames = emptyList()
        )
        val restored = json.decodeFromString<BackupData>(json.encodeToString(original))
        assertEquals(original.boards, restored.boards)
        assertEquals(original.createdAt.toInstant(), restored.createdAt.toInstant())
        assertEquals(metadata, restored.boards.last().copy(folderId = 9).ratingMetadata)
    }
}
