package com.saeed.ideasmo.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.saeed.ideasmo.core.log.AppLog
import java.io.File

/**
 * Copies a picked image into app storage so it survives restarts with no URI permission.
 * Returns the absolute file path, or null when the copy failed.
 */
fun copyPictureIntoAppStorage(context: Context, uri: Uri): String? {
    return try {
        val dir = File(context.filesDir, "pics")
        if (!dir.exists() && !dir.mkdirs()) {
            AppLog.w("Images", "cannot create pics dir: " + dir.absolutePath)
            return null
        }
        val target = File(dir, "pic-" + System.currentTimeMillis() + ".jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
        AppLog.i("Images", "picture stored: " + target.absolutePath + " (" + target.length() + " bytes)")
        target.absolutePath
    } catch (t: Throwable) {
        AppLog.e("Images", "picture copy failed", t)
        null
    }
}

/** Manual thumbnail decode — no image-loading dependency needed for a small preview. */
fun decodeThumbnail(context: Context, pathOrUri: String, maxPx: Int = 256): Bitmap? {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openStream(context, pathOrUri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / sample > maxPx || bounds.outHeight / sample > maxPx) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        openStream(context, pathOrUri)?.use { BitmapFactory.decodeStream(it, null, options) }
    } catch (t: Throwable) {
        AppLog.e("Images", "thumbnail decode failed for " + pathOrUri, t)
        null
    }
}

private fun openStream(context: Context, pathOrUri: String) =
    if (pathOrUri.startsWith("content://")) {
        context.contentResolver.openInputStream(Uri.parse(pathOrUri))
    } else {
        File(pathOrUri).inputStream()
    }