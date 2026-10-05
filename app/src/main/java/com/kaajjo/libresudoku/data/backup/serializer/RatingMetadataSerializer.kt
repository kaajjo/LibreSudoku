package com.kaajjo.libresudoku.data.backup.serializer

import com.kaajjo.libresudoku.core.generator.rating.LogicTier
import com.kaajjo.libresudoku.core.generator.rating.Technique
import com.kaajjo.libresudoku.core.qqwing.models.RatingMetadata
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** Shared by Room and backups. Enum names are persisted IDs; never replace them with ordinals. */
object RatingMetadataSerializer : KSerializer<RatingMetadata> {
    @Serializable
    private data class StoredMetadata(
        val version: String,
        val tier: String,
        val techniqueCounts: Map<String, Int>,
        val logicallySolved: Boolean,
        val effortScore: Int? = null,
        val scorePolicy: String? = null
    )

    override val descriptor: SerialDescriptor = StoredMetadata.serializer().descriptor

    /**
     * Writes stable technique and tier names shared by Room and backups.
     *
     * @param encoder Destination encoder for the stored metadata structure.
     * @param value Assessment metadata to persist without recalculating its rating.
     */
    override fun serialize(encoder: Encoder, value: RatingMetadata) {
        encoder.encodeSerializableValue(
            StoredMetadata.serializer(),
            StoredMetadata(
                version = value.version,
                tier = value.tier.name,
                techniqueCounts = value.techniqueCounts.mapKeys { it.key.name },
                logicallySolved = value.logicallySolved,
                effortScore = value.effortScore,
                scorePolicy = value.scorePolicy
            )
        )
    }

    /**
     * Restores metadata and validates its identifiers and score.
     *
     * @param decoder Decoder containing the stored metadata structure.
     * @throws SerializationException if identifiers or model values are invalid.
     */
    override fun deserialize(decoder: Decoder): RatingMetadata {
        val stored = decoder.decodeSerializableValue(StoredMetadata.serializer())
        try {
            return RatingMetadata(
                version = stored.version,
                tier = LogicTier.valueOf(stored.tier),
                techniqueCounts = stored.techniqueCounts.mapKeys { Technique.valueOf(it.key) },
                logicallySolved = stored.logicallySolved,
                effortScore = stored.effortScore,
                scorePolicy = stored.scorePolicy
            )
        } catch (exception: IllegalArgumentException) {
            throw SerializationException("Invalid rating metadata", exception)
        }
    }
}
