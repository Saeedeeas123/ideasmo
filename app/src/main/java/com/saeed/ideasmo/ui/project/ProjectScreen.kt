package com.saeed.ideasmo.ui.project

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.saeed.ideasmo.R
import com.saeed.ideasmo.ui.components.GridTile
import com.saeed.ideasmo.ui.theme.Spacing

/**
 * Project screen (wireframe 1, 2026-10-15): four create actions on the left,
 * their libraries on the right. Only Idea is implemented so far.
 */
enum class ProjectEntry(@StringRes val labelRes: Int, val creates: Boolean) {
    Idea(R.string.entry_idea, true),
    IdeaLibrary(R.string.entry_idea_library, false),
    Mindmap(R.string.entry_mindmap, true),
    MindmapLibrary(R.string.entry_mindmap_library, false),
    Knowledge(R.string.entry_knowledge, true),
    KnowledgeLibrary(R.string.entry_knowledge_library, false),
    Note(R.string.entry_note, true),
    NoteLibrary(R.string.entry_note_library, false),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectScreen(
    onBack: () -> Unit,
    onEntry: (ProjectEntry) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.project_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenH),
        ) {
            Spacer(Modifier.height(Spacing.l))

            EntryRow(
                left = ProjectEntry.Idea to Icons.Outlined.Lightbulb,
                right = ProjectEntry.IdeaLibrary to Icons.Outlined.Bookmarks,
                onEntry = onEntry,
            )
            Spacer(Modifier.height(Spacing.m))
            EntryRow(
                left = ProjectEntry.Mindmap to Icons.Outlined.AccountTree,
                right = ProjectEntry.MindmapLibrary to Icons.Outlined.CollectionsBookmark,
                onEntry = onEntry,
            )
            Spacer(Modifier.height(Spacing.m))
            EntryRow(
                left = ProjectEntry.Knowledge to Icons.AutoMirrored.Outlined.MenuBook,
                right = ProjectEntry.KnowledgeLibrary to Icons.Outlined.LocalLibrary,
                onEntry = onEntry,
            )
            Spacer(Modifier.height(Spacing.m))
            EntryRow(
                left = ProjectEntry.Note to Icons.AutoMirrored.Outlined.StickyNote2,
                right = ProjectEntry.NoteLibrary to Icons.AutoMirrored.Outlined.LibraryBooks,
                onEntry = onEntry,
            )

            Spacer(Modifier.height(Spacing.xl))
        }
    }
}

@Composable
private fun EntryRow(
    left: Pair<ProjectEntry, ImageVector>,
    right: Pair<ProjectEntry, ImageVector>,
    onEntry: (ProjectEntry) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
        GridTile(
            label = stringResource(left.first.labelRes),
            icon = left.second,
            showPlus = left.first.creates,
            onClick = { onEntry(left.first) },
            modifier = Modifier.weight(1f),
        )
        GridTile(
            label = stringResource(right.first.labelRes),
            icon = right.second,
            showPlus = right.first.creates,
            onClick = { onEntry(right.first) },
            modifier = Modifier.weight(1f),
        )
    }
}