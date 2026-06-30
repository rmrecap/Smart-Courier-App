package com.smartcourier.app

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.smartcourier.core.ui.theme.SmartCourierTheme
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SmartCourierApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}

@Composable
fun SmartCourierAppContent() {
    SmartCourierTheme {
        AppNavGraph()
    }
}
