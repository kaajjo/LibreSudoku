package com.kaajjo.libresudoku.di

import android.app.Application
import android.content.Context
import com.kaajjo.libresudoku.data.database.AppDatabase
import com.kaajjo.libresudoku.data.database.dao.BoardDao
import com.kaajjo.libresudoku.data.database.dao.RecordDao
import com.kaajjo.libresudoku.data.database.dao.SavedGameDao
import com.kaajjo.libresudoku.data.database.repository.BoardRepository
import com.kaajjo.libresudoku.data.database.repository.RecordRepository
import com.kaajjo.libresudoku.data.database.repository.SavedGameRepository
import com.kaajjo.libresudoku.data.datastore.AppSettingsManager
import com.kaajjo.libresudoku.data.datastore.ThemeSettingsManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    // records
    @Singleton
    @Provides
    fun provideRecordRepository(recordDao: RecordDao): RecordRepository =
        RecordRepository(recordDao)

    @Singleton
    @Provides
    fun provideRecordDao(appDatabase: AppDatabase): RecordDao = appDatabase.recordDao()


    // boards
    @Singleton
    @Provides
    fun provideBoardRepository(boardDao: BoardDao): BoardRepository = BoardRepository(boardDao)

    @Singleton
    @Provides
    fun provideBoardDao(appDatabase: AppDatabase): BoardDao = appDatabase.boardDao()


    // saved games
    @Singleton
    @Provides
    fun provideSavedGameRepository(savedGameDao: SavedGameDao): SavedGameRepository =
        SavedGameRepository(savedGameDao)

    @Singleton
    @Provides
    fun provideSavedGameDao(appDatabase: AppDatabase): SavedGameDao = appDatabase.savedGameDao()


    // settings datastore
    @Provides
    @Singleton
    fun provideAppSettingsManager(@ApplicationContext context: Context) =
        AppSettingsManager(context)

    // appTheme datastore
    @Provides
    @Singleton
    fun provideThemeSettingsManager(@ApplicationContext context: Context) =
        ThemeSettingsManager(context)

    @Singleton
    @Provides
    fun provideAppDatabase(app: Application): AppDatabase = AppDatabase.getInstance(context = app)
}