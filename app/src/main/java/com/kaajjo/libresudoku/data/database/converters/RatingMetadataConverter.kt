package com.kaajjo.libresudoku.data.database.converters

import androidx.room.TypeConverter
import com.kaajjo.libresudoku.core.qqwing.models.RatingMetadata
import com.kaajjo.libresudoku.data.backup.serializer.RatingMetadataSerializer
import kotlinx.serialization.json.Json

class RatingMetadataConverter {
    @TypeConverter
    fun fromMetadata(metadata: RatingMetadata?): String? = metadata?.let {
        json.encodeToString(RatingMetadataSerializer, it)
    }

    @TypeConverter
    fun toMetadata(value: String?): RatingMetadata? = value?.let {
        json.decodeFromString(RatingMetadataSerializer, it)
    }

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}
