package com.saeed.ideasmo.core.drive

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The Drive's pure rules — no Android, no file IO that the JVM cannot do — so they are unit-tested on
 * the JVM like the structure engine. [DriveStore] delegates to these.
 *
 * What lives here is exactly what is easy to get wrong: what a file IS by its name, what a safe name
 * looks like, how a collision is avoided, and how a size and a date are written for a human.
 */
object DrivePaths {

    /** The kind of a file, from its extension. The kind decides how the Drive opens it. */
    fun kindOf(name: String): DriveKind {
        val ext = name.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif" -> DriveKind.Image
            "txt", "md", "csv", "json", "log", "xml", "kt", "py", "js", "html", "css", "yaml", "yml" -> DriveKind.Text
            "mp3", "wav", "ogg", "m4a", "flac", "aac", "opus" -> DriveKind.Audio
            "mp4", "mkv", "webm", "mov", "avi", "3gp" -> DriveKind.Video
            "pdf" -> DriveKind.Pdf
            "doc", "docx", "odt", "rtf", "xls", "xlsx", "ppt", "pptx" -> DriveKind.Document
            "zip", "rar", "7z", "tar", "gz" -> DriveKind.Archive
            else -> DriveKind.Other
        }
    }

    /** A name that is safe on every file system in the app's own storage. */
    fun sanitize(name: String): String = name
        .replace('/', '_')
        .replace('\\', '_')
        .replace('\u0000', '_')
        .trim()
        .trimStart('.')
        .take(96)

    /** A name that does not collide with anything already in [dir] ("photo.jpg" → "photo (2).jpg"). */
    fun uniqueName(dir: File, desired: String): String {
        var candidate = desired.ifEmpty { "file" }
        if (!File(dir, candidate).exists()) return candidate
        val stem = candidate.substringBeforeLast('.', candidate)
        val ext = candidate.substringAfterLast('.', "")
        var n = 2
        while (true) {
            val next = if (ext.isEmpty()) "$stem ($n)" else "$stem ($n).$ext"
            if (!File(dir, next).exists()) return next
            n++
        }
    }

    /** "900 B" · "12 KB" · "4.2 MB" */
    fun formatSize(bytes: Long): String = when {
        bytes < 1024 -> bytes.toString() + " B"
        bytes < 1024 * 1024 -> (bytes / 1024).toString() + " KB"
        bytes < 1024L * 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
        else -> String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }

    /** "17 Oct 10:31" */
    fun formatDate(millis: Long): String {
        if (millis <= 0L) return "—"
        return SimpleDateFormat("d MMM HH:mm", Locale.getDefault()).format(Date(millis))
    }

    /** A Drive path from a folder and a name. */
    fun join(parent: String, name: String): String =
        if (parent.isEmpty()) name else parent.trimEnd('/') + "/" + name

    /** One step up: "a/b/c" → "a/b", "a" → "". */
    fun parentOf(relative: String): String {
        if (relative.isEmpty()) return ""
        val trimmed = relative.trimEnd('/')
        val cut = trimmed.lastIndexOf('/')
        return if (cut < 0) "" else trimmed.substring(0, cut)
    }
}