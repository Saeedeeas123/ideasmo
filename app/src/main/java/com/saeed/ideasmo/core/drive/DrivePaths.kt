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

    /**
     * The Drive is read **file-first** (Saeed's order, v0.15.0): the top of the screen is a set of
     * shelves, one per kind of thing, and choosing a shelf shows every file of that kind wherever it
     * sits — "to import a picture you go to Images and import it there".
     *
     * A shelf is what the user thinks in ("my pictures"), a folder is only where a file happens to
     * sit (an attachment of a node, or an imported file). Several kinds can share a shelf (a PDF and
     * a Word file are both Documents), which is why this is not simply [DriveKind].
     */
    enum class DriveShelf(val key: String, val folder: String, val label: String) {
        Image("image", "Images", "Images"),
        Text("text", "Texts", "Texts"),
        Document("document", "Documents", "Documents"),
        Audio("audio", "Audio", "Audio"),
        Video("video", "Video", "Video"),
        Archive("archive", "Archives", "Archives"),
        Other("other", "Other", "Other"),
        ;

        companion object {
            /** The shelves in the order the Drive shows them. */
            val order: List<DriveShelf> = listOf(Image, Text, Document, Audio, Video, Archive, Other)

            fun fromKey(key: String?): DriveShelf? = order.firstOrNull { it.key == key }
        }
    }

    /** Which shelf a kind of file belongs on. */
    fun shelfOf(kind: DriveKind): DriveShelf = when (kind) {
        DriveKind.Image -> DriveShelf.Image
        DriveKind.Text -> DriveShelf.Text
        DriveKind.Pdf, DriveKind.Document -> DriveShelf.Document
        DriveKind.Audio -> DriveShelf.Audio
        DriveKind.Video -> DriveShelf.Video
        DriveKind.Archive -> DriveShelf.Archive
        DriveKind.Folder, DriveKind.Other -> DriveShelf.Other
    }

    /** Which shelf a file belongs on, decided by its name alone. */
    fun shelfOfName(name: String): DriveShelf = shelfOf(kindOf(name))

    /** The Drive folder a file of this kind is filed into when it is imported file-first. */
    fun folderFor(kind: DriveKind): String = shelfOf(kind).folder

    /**
     * Every FILE under [root] that belongs on [shelf], deepest folders included.
     *
     * This is what makes the Drive file-first rather than folder-first: a picture attached to a node
     * (filed under `Attachments/<node>/`) and a picture imported straight to the Drive (`Images/`)
     * are both pictures, and both show up under Images. [skipNames] is the folder the Drive keeps for
     * deleted files, so the trash never leaks into a shelf.
     */
    fun filesOnShelf(root: File, shelf: DriveShelf, skipNames: Set<String> = emptySet()): List<File> {
        if (!root.isDirectory) return emptyList()
        val found = ArrayList<File>()
        root.walkTopDown().forEach { file ->
            if (!file.isFile) return@forEach
            if (file.name in skipNames) return@forEach
            if (file.parentFile?.name in skipNames) return@forEach
            if (shelfOfName(file.name) == shelf) found.add(file)
        }
        return found.sortedBy { it.name.lowercase() }
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