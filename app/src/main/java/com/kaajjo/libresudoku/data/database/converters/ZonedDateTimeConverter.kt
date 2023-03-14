package com.kaajjo.libresudoku.data.database.converters

import androidx.room.TypeConverter
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Converts date represented by Instant to Long seconds and reverse
 */
class ZonedDateTimeConverter {
    /**
     * Converts date in seconds to ZoneDateTime with system default time zone
     * @param value date in seconds
     * @return ZonedDateTime of seconds
     */
    @TypeConverter
    fun toZonedDateTime(value: Long?): ZonedDateTime? {
        return if (value != null) {
            ZonedDateTime.ofInstant(
                Instant.ofEpochSecond(value),
                ZoneId.systemDefault()
            )
        } else {
            null
        }

    }

    /**
     * Converts ZonedDateTime to seconds
     * @param zonedDateTime date
     */
    @TypeConverter
    fun fromZonedDateTime(zonedDateTime: ZonedDateTime?): Long? {
        return zonedDateTime?.toInstant()?.epochSecond
    }
}