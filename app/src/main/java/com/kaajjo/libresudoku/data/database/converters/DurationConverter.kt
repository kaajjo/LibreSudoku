package com.kaajjo.libresudoku.data.database.converters

import androidx.room.TypeConverter
import java.time.Duration

/**
 * Converts Duration
 */
class DurationConverter {
    /**
     * Converts seconds to Duration
     * @param value Duration in seconds
     * @return duration of seconds
     */
    @TypeConverter
    fun toDuration(value: Long): Duration {
        return Duration.ofSeconds(value)
    }

    /**
     * Converts Duration to seconds
     * @param duration Duration
     * @return duration represented in seconds
     */
    @TypeConverter
    fun fromDuration(duration: Duration): Long {
        return duration.seconds
    }
}