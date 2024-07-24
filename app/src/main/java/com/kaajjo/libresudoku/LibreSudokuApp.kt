package com.kaajjo.libresudoku

import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class LibreSudokuApp : Application(), Configuration.Provider {
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
    }

    @Inject
    lateinit var hiltWorkerFactory: HiltWorkerFactory
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(hiltWorkerFactory)
            .build()

}