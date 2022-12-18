package com.kaajjo.libresudoku.data.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kaajjo.libresudoku.data.database.converters.DurationConverter
import com.kaajjo.libresudoku.data.database.converters.GameDifficultyConverter
import com.kaajjo.libresudoku.data.database.converters.GameTypeConverter
import com.kaajjo.libresudoku.data.database.converters.ZonedDateTimeConverter
import com.kaajjo.libresudoku.data.database.dao.BoardDao
import com.kaajjo.libresudoku.data.database.dao.RecordDao
import com.kaajjo.libresudoku.data.database.dao.SavedGameDao
import com.kaajjo.libresudoku.data.database.model.SudokuBoard
import com.kaajjo.libresudoku.data.database.model.Record
import com.kaajjo.libresudoku.data.database.model.SavedGame

@Database(
    entities = [Record::class, SudokuBoard::class, SavedGame::class],
    version = 2,
    autoMigrations = [ AutoMigration(from = 1, to = 2)]
)
@TypeConverters(
    DurationConverter::class,
    ZonedDateTimeConverter::class,
    GameDifficultyConverter::class,
    GameTypeConverter::class
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordDao(): RecordDao
    abstract fun boardDao(): BoardDao
    abstract fun savedGameDao(): SavedGameDao

    companion object {
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context) : AppDatabase {
            if(INSTANCE == null) {
                INSTANCE = Room.databaseBuilder(
                    context,
                    AppDatabase::class.java,
                    "main_database"
                ).build()
            }

            return INSTANCE as AppDatabase
        }
    }
}