package com.github.charleslzq.sample

import android.app.Application
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger


private const val LOG_DIRECTORY = "/logs"

class SampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        System.setProperty("rx2.buffer-size", "20")

        Logger.addLogAdapter(AndroidLogAdapter())

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Logger.e(throwable, "Exception occur at ${thread.name}")
        }
    }
}