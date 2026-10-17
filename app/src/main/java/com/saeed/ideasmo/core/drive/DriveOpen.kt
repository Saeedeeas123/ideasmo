package com.saeed.ideasmo.core.drive

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.saeed.ideasmo.core.log.AppLog
import java.io.File

/**
 * Hands a Drive file to whatever app on the phone knows how to read it.
 *
 * Saeed's order (v0.14.0): the Drive must be able to read files of different kinds — and the honest way
 * to do that for a PDF, a document or an archive is to let its own app open it. The file goes out as a
 * `content://` URI through the app's [FileProvider] (declared in the manifest with
 * `authorities = com.saeed.ideasmo.files`), so no storage permission is involved and nothing has to be
 * copied again.
 */
object DriveOpen {

    /** The mime type the rest of the phone needs to know what app should open [name]. */
    fun mimeOf(name: String): String = when (DrivePaths.kindOf(name)) {
        DriveKind.Image -> "image/*"
        DriveKind.Text -> "text/plain"
        DriveKind.Audio -> "audio/*"
        DriveKind.Video -> "video/*"
        DriveKind.Pdf -> "application/pdf"
        DriveKind.Archive -> "application/zip"
        DriveKind.Document -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        DriveKind.Folder, DriveKind.Other -> "*/*"
    }

    /** True when [name] is something the app itself can show without help. */
    fun readableInside(name: String): Boolean =
        DrivePaths.kindOf(name) == DriveKind.Image || DrivePaths.kindOf(name) == DriveKind.Text

    /**
     * Opens [absolutePath] in another app. Returns false when nothing on the phone can open it, so the
     * caller can say so instead of failing silently.
     */
    fun openWith(context: Context, absolutePath: String): Boolean {
        val file = File(absolutePath)
        if (!file.exists()) {
            AppLog.w("Drive", "open refused, file is gone: " + absolutePath)
            return false
        }
        return try {
            val uri: Uri = FileProvider.getUriForFile(context, "com.saeed.ideasmo.files", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeOf(file.name))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            AppLog.i("Drive", "handed to another app: " + file.name + " (" + mimeOf(file.name) + ")")
            true
        } catch (t: Throwable) {
            AppLog.w("Drive", "nothing can open " + file.name + ": " + t.message)
            false
        }
    }
}