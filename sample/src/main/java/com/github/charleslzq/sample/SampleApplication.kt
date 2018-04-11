package com.github.charleslzq.sample

import android.app.Application
import android.os.Environment
import org.joda.time.LocalDateTime
import java.io.File

class SampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            val filePath = Environment.getExternalStorageDirectory().absolutePath + "/$FILE_NAME-${LocalDateTime.now()}.txt"
            File(filePath).writeText("Exception ${exception.message} happened at Thread ${thread.name}, stacktrace: ${exception.stackTrace}")
        }
    }

    companion object {
        const val FILE_NAME = "crash"
    }
}