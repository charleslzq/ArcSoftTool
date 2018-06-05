package com.github.charleslzq.sample

import android.app.Application
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import com.squareup.leakcanary.LeakCanary


private const val LOG_DIRECTORY = "/logs"

class SampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        System.setProperty("rx2.buffer-size", "20")

        Logger.addLogAdapter(AndroidLogAdapter())

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Logger.e(throwable, "Exception occur at ${thread.name}")
        }

        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        LeakCanary.install(this);
    }
}