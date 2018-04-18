package com.github.charleslzq.sample

import android.app.Application
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.Message
import com.orhanobut.logger.*
import java.io.File
import java.io.FileWriter
import java.io.IOException


private const val LOG_DIRECTORY = "/logs"

class SampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        System.setProperty("rx2.buffer-size", "20")

        Logger.addLogAdapter(DiskLogAdapter(PrettyFormatStrategy.newBuilder()
                .logStrategy(DiskLogStrategy(WriteHandler(
                        Looper.getMainLooper(),
                        Environment.getExternalStorageDirectory().absolutePath + LOG_DIRECTORY,
                        30000
                )))
                .build()
        ))
        Logger.addLogAdapter(AndroidLogAdapter())

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Logger.e(throwable, "Exception occur at ${thread.name}")
        }
    }

    internal class WriteHandler(looper: Looper, folder: String, private val maxFileSize: Int) : Handler(checkNotNull(looper)) {

        private val folder: String = checkNotNull(folder)

        override fun handleMessage(msg: Message) {
            val content = msg.obj as String

            var fileWriter: FileWriter? = null
            val logFile = getLogFile(folder, "logs")

            try {
                fileWriter = FileWriter(logFile, true)

                writeLog(fileWriter, content)

                fileWriter.flush()
                fileWriter.close()
            } catch (e: IOException) {
                if (fileWriter != null) {
                    try {
                        fileWriter.flush()
                        fileWriter.close()
                    } catch (e1: IOException) { /* fail silently */
                    }

                }
            }

        }

        /**
         * This is always called on a single background thread.
         * Implementing classes must ONLY write to the fileWriter and nothing more.
         * The abstract class takes care of everything else including close the stream and catching IOException
         *
         * @param fileWriter an instance of FileWriter already initialised to the correct file
         */
        @Throws(IOException::class)
        private fun writeLog(fileWriter: FileWriter, content: String) {
            checkNotNull(fileWriter)
            checkNotNull(content)

            fileWriter.append(content)
        }

        private fun getLogFile(folderName: String, fileName: String): File {
            checkNotNull(folderName)
            checkNotNull(fileName)

            val folder = File(folderName)
            if (!folder.exists()) {
                //TODO: What if folder is not created, what happens then?
                folder.mkdirs()
            }

            var newFileCount = 0
            var newFile: File
            var existingFile: File? = null

            newFile = File(folder, String.format("%s_%s.csv", fileName, newFileCount))
            while (newFile.exists()) {
                existingFile = newFile
                newFileCount++
                newFile = File(folder, String.format("%s_%s.csv", fileName, newFileCount))
            }

            return if (existingFile != null) {
                if (existingFile.length() >= maxFileSize) {
                    newFile
                } else existingFile
            } else newFile

        }
    }
}