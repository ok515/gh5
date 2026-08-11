package com.pecmi.studio.util

import android.content.Context
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashLogger {

    private const val LOG_FILE_NAME = "crash_logs.txt"

    fun init(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            logError(context, "UNCAUGHT_CRASH", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    fun logError(context: Context, tag: String, throwable: Throwable) {
        try {
            val file = File(context.filesDir, LOG_FILE_NAME)
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))

            val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val logMessage = "[$timeStamp][$tag]\n${sw}\n-----------------------------------\n"

            FileWriter(file, true).use { writer ->
                writer.append(logMessage)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getLogs(context: Context): String {
        val file = File(context.filesDir, LOG_FILE_NAME)
        return if (file.exists()) {
            file.readText()
        } else {
            "لا توجد سجلات أخطاء مسجلة."
        }
    }

    fun clearLogs(context: Context) {
        val file = File(context.filesDir, LOG_FILE_NAME)
        if (file.exists()) {
            file.delete()
        }
    }
}
