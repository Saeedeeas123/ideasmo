package com.saeed.ideasmo.core.util

import android.content.Context
import android.net.Uri
import com.saeed.ideasmo.core.log.AppLog
import java.io.File

/**
 * Copies any picked document into app storage, so a node's files survive restarts with no URI
 * permission (the same rule the pictures follow). Returns the absolute path, or null on failure.
 *
 * [displayName] is kept as part of the stored file name so the list in the UI reads like the file the
 * user picked instead of a timestamp.
 */
fun copyFileIntoAppStorage(context: Context, uri: Uri, displayName: String?): String? {
    return try {
        val dir = File(context.filesDir, "files")
        if (!dir.exists() && !dir.mkdirs()) {
            AppLog.w("Files", "cannot create files dir: " + dir.absolutePath)
            return null
        }
        val safe = (displayName ?: "file")
            .replace('/', '_')
            .replace('\\', '_')
            .takeLast(64)
        val target = File(dir, System.currentTimeMillis().toString() + "-" + safe)
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
        AppLog.i("Files", "file stored: " + target.absolutePath + " (" + target.length() + " bytes)")
        target.absolutePath
    } catch (t: Throwable) {
        AppLog.e("Files", "file copy failed", t)
        null
    }
}

/** The name a stored file shows in the UI: everything after the timestamp prefix. */
fun storedFileName(path: String): String {
    val name = path.substringAfterLast('/')
    val dash = name.indexOf('-')
    return if (dash in 1..14) name.substring(dash + 1) else name
}
