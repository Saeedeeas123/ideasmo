package com.saeed.ideasmo.core.drive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * JVM tests for the Drive's pure rules.
 *
 * What they protect: a file is classified by its name (so the Drive opens it the right way), a name is
 * made safe before it reaches the file system, two files can never overwrite each other, and a size and
 * a date read like a human wrote them. None of this needs a device — [DrivePaths] has no Android in it.
 */
class DrivePathsTest {

    @Test
    fun everyKindOfFileIsClassifiedFromItsName() {
        assertEquals(DriveKind.Image, DrivePaths.kindOf("photo.JPG"))
        assertEquals(DriveKind.Image, DrivePaths.kindOf("shot.webp"))
        assertEquals(DriveKind.Text, DrivePaths.kindOf("notes.md"))
        assertEquals(DriveKind.Text, DrivePaths.kindOf("Main.kt"))
        assertEquals(DriveKind.Audio, DrivePaths.kindOf("voice.m4a"))
        assertEquals(DriveKind.Video, DrivePaths.kindOf("clip.mp4"))
        assertEquals(DriveKind.Pdf, DrivePaths.kindOf("paper.pdf"))
        assertEquals(DriveKind.Document, DrivePaths.kindOf("plan.docx"))
        assertEquals(DriveKind.Archive, DrivePaths.kindOf("backup.zip"))
        assertEquals(DriveKind.Other, DrivePaths.kindOf("mystery"))
        assertEquals(DriveKind.Other, DrivePaths.kindOf("no-extension-at-all"))
    }

    @Test
    fun aNameIsMadeSafeBeforeItReachesTheFileSystem() {
        assertEquals("a_b", DrivePaths.sanitize("a/b"))
        assertEquals("a_b", DrivePaths.sanitize("a\\b"))
        // A leading dot would make the file invisible; the Drive keeps its own hidden folders.
        assertEquals("secret.txt", DrivePaths.sanitize(".secret.txt"))
        assertEquals("", DrivePaths.sanitize("   "))
        assertTrue("a very long name is cut", DrivePaths.sanitize("x".repeat(500)).length <= 96)
    }

    @Test
    fun aSecondFileWithTheSameNameGetsACounter() {
        val dir = File(System.getProperty("java.io.tmpdir"), "drive-test-" + System.nanoTime())
        dir.mkdirs()
        try {
            assertEquals("photo.jpg", DrivePaths.uniqueName(dir, "photo.jpg"))
            File(dir, "photo.jpg").writeText("one")
            assertEquals("photo (2).jpg", DrivePaths.uniqueName(dir, "photo.jpg"))
            File(dir, "photo (2).jpg").writeText("two")
            assertEquals("photo (3).jpg", DrivePaths.uniqueName(dir, "photo.jpg"))
            // No extension: the counter is simply appended.
            File(dir, "notes").writeText("x")
            assertEquals("notes (2)", DrivePaths.uniqueName(dir, "notes"))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun sizesReadLikeAHumanWroteThem() {
        assertEquals("900 B", DrivePaths.formatSize(900))
        assertEquals("1 KB", DrivePaths.formatSize(1024))
        assertEquals("4.2 MB", DrivePaths.formatSize((4.2 * 1024 * 1024).toLong()))
        assertTrue(DrivePaths.formatSize(3L * 1024 * 1024 * 1024).endsWith("GB"))
    }

    @Test
    fun pathsJoinAndStepUpCorrectly() {
        assertEquals("Attachments", DrivePaths.join("", "Attachments"))
        assertEquals("Attachments/First idea", DrivePaths.join("Attachments", "First idea"))
        assertEquals("Attachments/First idea", DrivePaths.join("Attachments/", "First idea"))
        assertEquals("Attachments", DrivePaths.parentOf("Attachments/First idea"))
        assertEquals("", DrivePaths.parentOf("Attachments"))
        assertEquals("", DrivePaths.parentOf(""))
    }

    @Test
    fun aRowSubtitleIsNeverEmpty() {
        val folder = DriveEntry("Attachments", "Attachments", true, 0L, 0L, DriveKind.Folder)
        assertEquals("Folder", folder.detail)
        val file = DriveEntry("a.txt", "a.txt", false, 2048L, 0L, DriveKind.Text)
        assertTrue(file.detail.startsWith("2 KB"))
    }
}