package com.saeed.ideasmo.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import com.saeed.ideasmo.R
import com.saeed.ideasmo.ui.components.EmptyState
import com.saeed.ideasmo.ui.components.GridTile
import com.saeed.ideasmo.ui.components.NavRow
import com.saeed.ideasmo.ui.components.SectionTitle
import com.saeed.ideasmo.ui.theme.Radius
import com.saeed.ideasmo.ui.theme.Sizes
import com.saeed.ideasmo.ui.theme.Spacing

/** Every entry on the home screen. Modules are built one by one. */
enum class HomeEntry(@StringRes val labelRes: Int) {
    Calendar(R.string.cd_calendar),
    Template(R.string.entry_template),
    Project(R.string.entry_project),
    Drive(R.string.entry_drive),
    Knowledge(R.string.entry_knowledge),
    Settings(R.string.cd_settings),
}

data class RecentItem(val title: String, val date: String, val tag: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    recent: List<RecentItem>,
    onEntry: (HomeEntry) -> Unit,
    onOpenLogs: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.pointerInput(Unit) {
                            detectTapGestures(onLongPress = { onOpenLogs() })
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onEntry(HomeEntry.Calendar) }) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = stringResource(R.string.cd_calendar),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onEntry(HomeEntry.Settings) }) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.cd_settings),
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
            HeaderTabs(selected = selectedTab, onSelect = { selectedTab = it })

            Spacer(Modifier.height(Spacing.xl))

            // 2 x 2 entry grid (as drawn on the wireframe)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                GridTile(
                    label = stringResource(R.string.entry_template),
                    icon = Icons.Outlined.Description,
                    showPlus = true,
                    onClick = { onEntry(HomeEntry.Template) },
                    modifier = Modifier.weight(1f),
                )
                GridTile(
                    label = stringResource(R.string.entry_project),
                    icon = Icons.Outlined.FolderOpen,
                    showPlus = true,
                    onClick = { onEntry(HomeEntry.Project) },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(Spacing.m))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                GridTile(
                    label = stringResource(R.string.entry_drive),
                    icon = Icons.Outlined.Cloud,
                    onClick = { onEntry(HomeEntry.Drive) },
                    modifier = Modifier.weight(1f),
                )
                GridTile(
                    label = stringResource(R.string.entry_knowledge),
                    icon = Icons.AutoMirrored.Outlined.MenuBook,
                    onClick = { onEntry(HomeEntry.Knowledge) },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(Spacing.xl))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(Radius.medium),
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = null)
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onEntry(HomeEntry.Knowledge) }),
            )

            SectionTitle(stringResource(R.string.recent_title))

            if (recent.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.Inbox,
                    title = stringResource(R.string.empty_title),
                    detail = stringResource(R.string.empty_body),
                    actionLabel = stringResource(R.string.empty_action),
                    onAction = { onEntry(HomeEntry.Project) },
                )
            } else {
                recent.forEach { item ->
                    NavRow(
                        title = item.title,
                        subtitle = item.date + "  ·  " + item.tag,
                        icon = Icons.Outlined.Description,
                        onClick = { onEntry(HomeEntry.Project) },
                    )
                }
            }

            Spacer(Modifier.height(Spacing.xl))
        }
    }
}

@Composable
private fun HeaderTabs(selected: Int, onSelect: (Int) -> Unit) {
    val labels = listOf(
        stringResource(R.string.tab_home),
        stringResource(R.string.tab_menu),
        stringResource(R.string.tab_history),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.s),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        labels.forEachIndexed { index, label ->
            val isSelected = index == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.small))
                    .clickable { onSelect(index) }
                    .heightIn(min = Sizes.touchMin)
                    .padding(horizontal = Spacing.l),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}