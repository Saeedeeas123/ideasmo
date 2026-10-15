package com.saeed.ideasmo.core.log

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale

/**
 * Durable per-run log. Standing hard rule: every app carries a live log plus an
 * in-app viewer. One timestamped file per run, flushed per line, last 10 retained.
 * Primary location is the app external files dir (always writable, no permission);
 * a mirror copy goes to Download/Ideasmo/logs when that path is writable.
 */
object AppLog {

    enum class Level(val short: String, val priority: Int) {
        D("D", Log.DEBUG),
        I("I", Log.INFO),
        W("W", Log.WARN),
        E("E", Log.ERROR)
    }

    private const val LOG_TAG = "Ideasmo"
    private const val MAX_SESSIONS = 10
    private const val MEMORY_TAIL = 500

    private val stampFmt = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
    private val timeFmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    private val lock = Any()
    private val tail = ArrayDeque<String>()

    private var primaryWriter: BufferedWriter? = null
    private var mirrorWriter: BufferedWriter? = null

    @Volatile
    private var started = false

    @Volatile
    var sessionFileName: String = ""
        private set

    /** Non-null when a log file could not be opened or written. Surfaced in the viewer UI. */
    @Volatile
    var openError: String? = null
        private set

    val memoryTail: List<String>
        get() = synchronized(lock) { tail.toList() }

    fun init(context: Context) {
        synchronized(lock) {
            if (started) return
            started = true
            sessionFileName = "ideasmo-" + stampFmt.format(Date()) + ".log"
        }
        primaryWriter = openWriter(logDir(context), "primary")
        mirrorWriter = openWriter(mirrorDir(), "mirror")
        installCrashHandler(context)
        writeHeader(context)
    }

    fun logDir(context: Context): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, "logs")

    fun mirrorDir(): File =
        File(Environment.getExternalStorageDirectory(), "Download/Ideasmo/logs")

    fun logFile(context: Context): File = File(logDir(context), sessionFileName)

    fun mirrorFile(): File = File(mirrorDir(), sessionFileName)

    private fun openWriter(dir: File, label: String): BufferedWriter? {
        return try {
            if (!dir.exists() && !dir.mkdirs()) {
                noteFailure("cannot create " + label + " dir: " + dir.absolutePath)
                return null
            }
            val writer = BufferedWriter(FileWriter(File(dir, sessionFileName), true))
            prune(dir)
            writer
        } catch (t: Throwable) {
            noteFailure("cannot open " + label + " file: " + t)
            null
        }
    }

    private fun prune(dir: File) {
        try {
            val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".log") }
                ?.sortedByDescending { it.lastModified() }
                ?: return
            files.drop(MAX_SESSIONS).forEach { it.delete() }
        } catch (t: Throwable) {
            noteFailure("prune failed: " + t)
        }
    }

    private fun noteFailure(message: String) {
        val existing = openError
        openError = if (existing == null) message else existing + " | " + message
        Log.e(LOG_TAG, message)
    }

    fun log(level: Level, tag: String, message: String) {
        val line = timeFmt.format(Date()) + " " + level.short + " [" + tag + "] " + message
        synchronized(lock) {
            tail.addLast(line)
            while (tail.size > MEMORY_TAIL) tail.removeFirst()
            try {
                primaryWriter?.let { it.write(line); it.newLine(); it.flush() }
                mirrorWriter?.let { it.write(line); it.newLine(); it.flush() }
            } catch (t: Throwable) {
                noteFailure("write failed: " + t)
            }
        }
        Log.println(level.priority, LOG_TAG, tag + ": " + message)
    }

    fun d(tag: String, message: String) = log(Level.D, tag, message)
    fun i(tag: String, message: String) = log(Level.I, tag, message)
    fun w(tag: String, message: String) = log(Level.W, tag, message)
    fun e(tag: String, message: String) = log(Level.E, tag, message)

    fun e(tag: String, message: String, throwable: Throwable) =
        log(Level.E, tag, message + "\n" + throwable.stackTraceToString())

    fun memoryLine(context: Context): String {
        val rssKb = try {
            val statm = File("/proc/self/statm").readText().trim().split(" ")
            statm[0].toLong() * 4096L / 1024L
        } catch (t: Throwable) {
            -1L
        }
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        var availKb = -1L
        if (am != null) {
            val info = ActivityManager.MemoryInfo()
            am.getMemoryInfo(info)
            availKb = info.availMem / 1024L
        }
        return "rss=" + rssKb + "KB avail=" + availKb + "KB"
    }

    private fun installCrashHandler(context: Context) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                log(Level.E, "CRASH", "uncaught on thread '" + thread.name + "'")
                log(Level.E, "CRASH", throwable.stackTraceToString())
                log(Level.E, "CRASH", "memory at crash: " + memoryLine(context))
                flush()
            } catch (ignored: Throwable) {
                Log.e(LOG_TAG, "crash handler failed", ignored)
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    @Suppress("DEPRECATION")
    private fun writeHeader(context: Context) {
        var versionName = "?"
        var versionCode = -1L
        try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            versionName = info.versionName ?: "?"
            // longVersionCode is API 28+; minSdk is 26, so the deprecated field is still needed below it.
            versionCode = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
        } catch (t: Throwable) {
            noteFailure("package info failed: " + t)
        }
        i("App", "==== session start ====")
        i("App", "package=" + context.packageName + " version=" + versionName + " (" + versionCode + ")")
        i("App", "device=" + Build.MANUFACTURER + " " + Build.MODEL + " (" + Build.DEVICE + ")")
        i("App", "os=Android " + Build.VERSION.RELEASE + " sdk=" + Build.VERSION.SDK_INT)
        i("App", "abis=" + Build.SUPPORTED_ABIS.joinToString(","))
        i("App", "log=" + logFile(context).absolutePath)
        i("App", "mirror=" + mirrorFile().absolutePath)
        i("Mem", memoryLine(context))
        val err = openError
        if (err != null) w("App", "LOG OPEN WARNING: " + err)
    }

    fun sessions(context: Context): List<File> =
        logDir(context).listFiles { f -> f.isFile && f.name.endsWith(".log") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

    fun readTail(file: File?, maxLines: Int): List<String> {
        if (file == null || !file.exists()) return emptyList()
        return try {
            val all = file.readLines()
            if (all.size <= maxLines) all else all.subList(all.size - maxLines, all.size)
        } catch (t: Throwable) {
            noteFailure("read failed for " + file.name + ": " + t)
            emptyList()
        }
    }

    fun flush() {
        synchronized(lock) {
            try {
                primaryWriter?.flush()
            } catch (ignored: Throwable) {
            }
            try {
                mirrorWriter?.flush()
            } catch (ignored: Throwable) {
            }
        }
    }

    fun close() {
        synchronized(lock) {
            try {
                primaryWriter?.flush()
                primaryWriter?.close()
            } catch (ignored: Throwable) {
            }
            try {
                mirrorWriter?.flush()
                mirrorWriter?.close()
            } catch (ignored: Throwable) {
            }
            primaryWriter = null
            mirrorWriter = null
        }
    }
}