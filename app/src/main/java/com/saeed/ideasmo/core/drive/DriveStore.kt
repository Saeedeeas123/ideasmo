package com.saeed.ideasmo.core.drive

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.saeed.ideasmo.core.log.AppLog
import java.io.File

/** What a Drive entry is, by its extension. Drive is a viewer first: the kind decides how. */
enum class DriveKind { Folder, Image, Text, Audio, Video, Pdf, Document, Archive, Other }

/** One row in the Drive: a folder or a file, always addressed by its path **inside** the Drive. */
data class DriveEntry(
    val name: String,
    val relative: String,
    val isFolder: Boolean,
    val sizeBytes: Long,
    val modifiedAt: Long,
    val kind: DriveKind,
) {
    /** "12 KB · 17 Oct 10:31" — the second line of a row. */
    val detail: String
        get() = if (isFolder) "Folder" else DrivePaths.formatSize(sizeBytes) + " · " + DrivePaths.formatDate(modifiedAt)
}

/**
 * **The Drive: one private place for every file the app touches.**
 *
 * Saeed's order (v0.13.0): the Drive tile was a placeholder, and the files that a node carries had
 * nowhere to live. From now on there is exactly ONE store — `filesDir/drive` — and everything goes
 * through it:
 *
 * - a file picked for a node is **copied into the Drive** (under `Attachments/<node>/`) and the node
 *   keeps that path, so the node and the Drive are two views of the same bytes;
 * - a file that is already in the Drive can be attached to a node without copying it again;
 * - the Drive has its own light file manager (see `ui/drive/DriveScreen.kt`), reachable from Home and
 *   from inside a node's attachments.
 *
 * Design rules: paths are always **relative to the Drive root** (so the whole store can be moved or
 * backed up as one folder), names are sanitised, a name never overwrites another (a counter is added),
 * and a delete moves the entry to `.trash` instead of destroying it.
 */
object DriveStore {

    /** Folder that node attachments are filed under, one subfolder per node. */
    const val ATTACHMENTS = "Attachments"

    private const val TRASH = ".trash"

    private var root: File? = null

    fun init(context: Context) {
        val dir = File(context.filesDir, "drive")
        if (!dir.exists() && !dir.mkdirs()) {
            AppLog.e("Drive", "cannot create the Drive at " + dir.absolutePath)
        }
        root = dir
        AppLog.i("Drive", "Drive ready at " + dir.absolutePath + " (" + usedBytes() + " bytes used)")
    }

    private fun root(): File? = root

    /** True when the Drive is usable (init ran and the folder exists). */
    fun ready(): Boolean = root?.isDirectory == true

    fun rootAbsolute(): String = root?.absolutePath.orEmpty()

    /** Everything in [relative] — folders first, then files, each sorted by name. */
    fun list(relative: String = ""): List<DriveEntry> {
        val dir = resolve(relative) ?: return emptyList()
        val children = dir.listFiles() ?: return emptyList()
        return children
            .filter { it.name != TRASH }
            .map { file ->
                DriveEntry(
                    name = file.name,
                    relative = join(relative, file.name),
                    isFolder = file.isDirectory,
                    sizeBytes = if (file.isDirectory) 0L else file.length(),
                    modifiedAt = file.lastModified(),
                    kind = if (file.isDirectory) DriveKind.Folder else kindOf(file.name),
                )
            }
            .sortedWith(compareByDescending<DriveEntry> { it.isFolder }.thenBy { it.name.lowercase() })
    }

    /** The absolute path of a Drive entry, or null when it is outside the Drive or missing. */
    fun absolute(relative: String): String? {
        val file = resolve(relative) ?: return null
        return if (file.exists()) file.absolutePath else null
    }

    /** An absolute path turned back into a Drive-relative one, or null when it is not in the Drive. */
    fun relativeOf(absolutePath: String): String? {
        val base = root ?: return null
        val file = File(absolutePath).absoluteFile
        val basePath = base.absolutePath
        if (!file.path.startsWith(basePath)) return null
        return file.path.removePrefix(basePath).trimStart('/')
    }

    /** True when the path is inside the Drive (whether or not the file still exists). */
    fun contains(absolutePath: String): Boolean = relativeOf(absolutePath) != null

    fun createFolder(parentRelative: String, name: String): String? {
        val clean = sanitize(name)
        if (clean.isEmpty()) return null
        val parent = resolve(parentRelative) ?: return null
        val target = File(parent, uniqueName(parent, clean))
        val created = target.mkdirs()
        if (created) AppLog.i("Drive", "folder created: " + join(parentRelative, target.name))
        return if (created) join(parentRelative, target.name) else null
    }

    /**
     * Copies [uri] into the Drive under [intoRelative] and returns its absolute path.
     *
     * This is the one door from the outside world into the app: images, documents, anything. The
     * display name is kept when the picker offers one, so the Drive reads like the user's own files.
     */
    fun importUri(context: Context, uri: Uri, intoRelative: String = "", displayName: String? = null): String? {
        val dir = resolve(intoRelative) ?: return null
        if (!dir.isDirectory && !dir.mkdirs()) {
            AppLog.w("Drive", "cannot create the target folder: " + dir.absolutePath)
            return null
        }
        val offered = displayName ?: queryName(context, uri) ?: "file"
        val name = uniqueName(dir, sanitize(offered))
        val target = File(dir, name)
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            AppLog.i("Drive", "imported " + target.name + " (" + target.length() + " bytes) into /" + intoRelative)
            target.absolutePath
        } catch (t: Throwable) {
            AppLog.e("Drive", "import failed for " + offered, t)
            target.delete()
            null
        }
    }

    /** Writes text straight into the Drive (used when a note is exported as a file). */
    fun writeText(intoRelative: String, name: String, text: String): String? {
        val dir = resolve(intoRelative) ?: return null
        if (!dir.exists()) dir.mkdirs()
        val target = File(dir, uniqueName(dir, sanitize(name)))
        return try {
            target.writeText(text)
            AppLog.i("Drive", "text written: " + target.absolutePath)
            target.absolutePath
        } catch (t: Throwable) {
            AppLog.e("Drive", "text write failed", t)
            null
        }
    }

    /** Deletes by moving into `.trash`, so a mistake is recoverable (Saeed: nothing destroyed). */
    fun delete(relative: String): Boolean {
        val file = resolve(relative) ?: return false
        if (!file.exists()) return false
        val base = root ?: return false
        val trash = File(base, TRASH)
        if (!trash.exists()) trash.mkdirs()
        val target = File(trash, System.currentTimeMillis().toString() + "-" + file.name)
        val moved = file.renameTo(target)
        AppLog.i("Drive", (if (moved) "moved to trash: " else "DELETE FAILED: ") + relative)
        return moved
    }

    fun rename(relative: String, newName: String): Boolean {
        val file = resolve(relative) ?: return false
        val parent = file.parentFile ?: return false
        val clean = sanitize(newName)
        if (clean.isEmpty() || clean == file.name) return false
        val target = File(parent, uniqueName(parent, clean))
        val moved = file.renameTo(target)
        if (moved) AppLog.i("Drive", "renamed " + relative + " -> " + target.name)
        return moved
    }

    fun isFolder(relative: String): Boolean = resolve(relative)?.isDirectory == true

    /** How the Drive orders its rows. Saeed asked for basic operations, and sorting is one of them. */
    enum class Sort { Name, Size, Newest }

    /** Everything in [relative], ordered by [sort] (folders always first). */
    fun list(relative: String, sort: Sort): List<DriveEntry> {
        val entries = list(relative)
        val comparator: Comparator<DriveEntry> = when (sort) {
            Sort.Name -> compareBy { it.name.lowercase() }
            Sort.Size -> compareByDescending { it.sizeBytes }
            Sort.Newest -> compareByDescending { it.modifiedAt }
        }
        return entries.sortedWith(compareByDescending<DriveEntry> { it.isFolder }.then(comparator))
    }

    /**
     * Every file under [relative] whose name contains [query], ignoring case — the Drive's search.
     * Folders are searched too, so a file three levels down is still found from the root.
     */
    fun search(query: String, relative: String = ""): List<DriveEntry> {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) return emptyList()
        val dir = resolve(relative) ?: return emptyList()
        val found = ArrayList<DriveEntry>()
        dir.walkTopDown().forEach { file ->
            if (file == dir) return@forEach
            if (file.name == TRASH) return@forEach
            if (!file.name.lowercase().contains(needle)) return@forEach
            val rel = relativeOf(file.absolutePath) ?: return@forEach
            found.add(
                DriveEntry(
                    name = file.name,
                    relative = rel,
                    isFolder = file.isDirectory,
                    sizeBytes = if (file.isDirectory) 0L else file.length(),
                    modifiedAt = file.lastModified(),
                    kind = if (file.isDirectory) DriveKind.Folder else kindOf(file.name),
                ),
            )
        }
        return found.sortedWith(compareByDescending<DriveEntry> { it.isFolder }.thenBy { it.name.lowercase() })
    }

    /** Moves an entry into [targetFolder] (both Drive-relative). Refused when it would land on itself. */
    fun move(relative: String, targetFolder: String): Boolean {
        val source = resolve(relative) ?: return false
        val target = resolve(targetFolder) ?: return false
        if (!source.exists() || !target.isDirectory) return false
        if (targetFolder == DrivePaths.parentOf(relative)) return false
        if (targetFolder.startsWith(relative)) {
            AppLog.w("Drive", "refused to move " + relative + " inside itself")
            return false
        }
        val destination = File(target, uniqueName(target, source.name))
        val moved = source.renameTo(destination)
        AppLog.i("Drive", (if (moved) "moved " else "MOVE FAILED ") + relative + " -> " + targetFolder)
        return moved
    }

    /** Copies a file next to itself with a counter ("photo (2).jpg"). Folders are not copied. */
    fun copy(relative: String): String? {
        val source = resolve(relative) ?: return null
        if (!source.isFile) return null
        val parent = source.parentFile ?: return null
        val target = File(parent, uniqueName(parent, source.name))
        return try {
            source.copyTo(target, overwrite = false)
            AppLog.i("Drive", "copied " + relative + " -> " + target.name)
            relativeOf(target.absolutePath)
        } catch (t: Throwable) {
            AppLog.e("Drive", "copy failed for " + relative, t)
            null
        }
    }

    /** Every folder in the Drive, as relative paths — what the Move dialog offers. */
    fun folders(): List<String> {
        val base = root ?: return emptyList()
        val result = ArrayList<String>()
        result.add("")
        base.walkTopDown().forEach { file ->
            if (file != base && file.isDirectory && file.name != TRASH) {
                relativeOf(file.absolutePath)?.let { result.add(it) }
            }
        }
        return result.sorted()
    }

    /** Bytes used by the whole Drive. */
    fun usedBytes(): Long = root?.walkTopDown()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L

    /** The Drive folder a node's attachments belong in, created on demand. */
    fun nodeFolder(nodeName: String): String {
        val clean = sanitize(nodeName).ifEmpty { "node" }
        val folder = ATTACHMENTS + "/" + clean
        resolve(folder)?.let { if (!it.exists()) it.mkdirs() }
        return folder
    }

    /**
     * Moves the pre-Drive attachment folders (`filesDir/pics`, `filesDir/files`) into the Drive and
     * returns a map of old absolute path → new absolute path, so stored node attachments keep working.
     */
    fun migrateLegacy(context: Context): Map<String, String> {
        val base = root ?: return emptyMap()
        val target = File(base, "Attachments/Imported")
        if (!target.exists()) target.mkdirs()
        val mapping = HashMap<String, String>()
        listOf("pics", "files").forEach legacy@{ legacyName ->
            val legacy = File(context.filesDir, legacyName)
            if (!legacy.isDirectory) return@legacy
            legacy.listFiles()?.forEach file@{ file ->
                if (!file.isFile) return@file
                val destination = File(target, uniqueName(target, sanitize(file.name)))
                if (file.renameTo(destination)) {
                    mapping[file.absolutePath] = destination.absolutePath
                }
            }
        }
        if (mapping.isNotEmpty()) {
            AppLog.i("Drive", "migrated " + mapping.size + " legacy attachment(s) into the Drive")
        }
        return mapping
    }

    // ---- pure rules live in DrivePaths so they can be unit-tested on the JVM ----

    fun kindOf(name: String): DriveKind = DrivePaths.kindOf(name)

    fun sanitize(name: String): String = DrivePaths.sanitize(name)

    fun uniqueName(dir: File, desired: String): String = DrivePaths.uniqueName(dir, desired)

    fun formatSize(bytes: Long): String = DrivePaths.formatSize(bytes)

    fun formatDate(millis: Long): String = DrivePaths.formatDate(millis)

    fun join(parent: String, name: String): String = DrivePaths.join(parent, name)

    fun parentOf(relative: String): String = DrivePaths.parentOf(relative)

    // ---- internals ---------------------------------------------------------

    /** A Drive entry as a File, refusing anything that tries to climb out of the Drive root. */
    private fun resolve(relative: String): File? {
        val base = root ?: return null
        val clean = relative.trim().trimStart('/')
        if (clean.contains("..")) {
            AppLog.w("Drive", "refused a path outside the Drive: " + relative)
            return null
        }
        val file = if (clean.isEmpty()) base else File(base, clean)
        return if (file.absolutePath.startsWith(base.absolutePath)) file else null
    }

    private fun queryName(context: Context, uri: Uri): String? = try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
    } catch (t: Throwable) {
        AppLog.w("Drive", "could not read the picked name: " + t.message)
        null
    }
}