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

    @Test
    fun everyKindGetsTheMimeTypeAnotherAppNeeds() {
        // v0.14.0: the Drive hands a file to whatever app can read it, so the mime type has to be right.
        assertEquals("image/*", DriveOpen.mimeOf("photo.jpg"))
        assertEquals("text/plain", DriveOpen.mimeOf("notes.md"))
        assertEquals("application/pdf", DriveOpen.mimeOf("paper.pdf"))
        assertEquals("audio/*", DriveOpen.mimeOf("voice.m4a"))
        assertEquals("video/*", DriveOpen.mimeOf("clip.mp4"))
        assertTrue(DriveOpen.mimeOf("mystery.bin").isNotEmpty())
        // Images and text are readable inside the app, so they do not need another app at all.
        assertTrue(DriveOpen.readableInside("shot.png"))
        assertTrue(DriveOpen.readableInside("readme.txt"))
        assertTrue(!DriveOpen.readableInside("paper.pdf"))
    }

    @Test
    fun aFileIsReadByWhatItIsNotByTheFolderItSitsIn() {
        // v0.15.0 — the Drive is read file-first, so the shelf of a file comes from its NAME only.
        assertEquals(DrivePaths.DriveShelf.Image, DrivePaths.shelfOfName("photo.JPG"))
        assertEquals(DrivePaths.DriveShelf.Text, DrivePaths.shelfOfName("notes.txt"))
        assertEquals(DrivePaths.DriveShelf.Document, DrivePaths.shelfOfName("paper.pdf"))
        assertEquals(DrivePaths.DriveShelf.Document, DrivePaths.shelfOfName("plan.docx"))
        assertEquals(DrivePaths.DriveShelf.Audio, DrivePaths.shelfOfName("voice.m4a"))
        assertEquals(DrivePaths.DriveShelf.Video, DrivePaths.shelfOfName("clip.mp4"))
        assertEquals(DrivePaths.DriveShelf.Archive, DrivePaths.shelfOfName("backup.zip"))
        assertEquals(DrivePaths.DriveShelf.Other, DrivePaths.shelfOfName("mystery"))
        // A PDF and a Word file share one shelf: what the user thinks is "documents".
        assertEquals(DrivePaths.DriveShelf.Document, DrivePaths.shelfOf(DriveKind.Pdf))
        assertEquals(DrivePaths.DriveShelf.Document, DrivePaths.shelfOf(DriveKind.Document))
        // And each shelf knows the folder an import of that kind is filed into.
        assertEquals("Images", DrivePaths.DriveShelf.Image.folder)
        assertEquals("Images", DrivePaths.folderFor(DriveKind.Image))
        assertEquals("Documents", DrivePaths.folderFor(DriveKind.Pdf))
        // Free-form keys survive a round trip; an unknown key is not a shelf.
        DrivePaths.DriveShelf.order.forEach { shelf ->
            assertEquals(shelf, DrivePaths.DriveShelf.fromKey(shelf.key))
        }
        assertEquals(null, DrivePaths.DriveShelf.fromKey("screenshots"))
    }

    @Test
    fun aShelfFindsItsFilesWhereverTheySitAndNeverInTheTrash() {
        val root = File(System.getProperty("java.io.tmpdir"), "shelf-test-" + System.nanoTime())
        val images = File(root, "Images")
        val nodeFolder = File(root, "Attachments/First idea")
        val nodeDeep = File(root, "Attachments/First idea/nested")
        val trash = File(root, ".trash")
        listOf(images, nodeFolder, nodeDeep, trash).forEach { it.mkdirs() }
        try {
            File(images, "a.jpg").writeText("a")
            // The same picture, filed as a node attachment three levels down: it is still a picture.
            File(nodeDeep, "b.png").writeText("b")
            File(nodeFolder, "notes.txt").writeText("c")
            File(root, "loose.webp").writeText("d")
            File(trash, "deleted.jpg").writeText("e")

            val found = DrivePaths.filesOnShelf(root, DrivePaths.DriveShelf.Image, setOf(".trash"))
                .map { it.name }
                .sorted()
            assertEquals(listOf("a.jpg", "b.png", "loose.webp"), found)

            // The trash never leaks onto a shelf, and a shelf with nothing on it is simply empty.
            assertEquals(
                emptyList<String>(),
                DrivePaths.filesOnShelf(root, DrivePaths.DriveShelf.Audio, setOf(".trash")),
            )
            assertEquals(
                listOf("notes.txt"),
                DrivePaths.filesOnShelf(root, DrivePaths.DriveShelf.Text, setOf(".trash")).map { it.name },
            )
        } finally {
            root.deleteRecursively()
        }
    }
}