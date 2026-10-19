package com.saeed.ideasmo.ui.board

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.saeed.ideasmo.R
import com.saeed.ideasmo.core.model.LayoutSpacing
import com.saeed.ideasmo.core.model.LinkCatalog
import com.saeed.ideasmo.core.model.LinkFamily
import com.saeed.ideasmo.core.model.NodeStyle
import com.saeed.ideasmo.ui.components.ChoiceChip
import com.saeed.ideasmo.ui.components.NavRow
import com.saeed.ideasmo.ui.components.SectionTitle
import com.saeed.ideasmo.ui.theme.Radius
import com.saeed.ideasmo.ui.theme.Sizes
import com.saeed.ideasmo.ui.theme.Spacing

/**
 * Project settings — opened from the top-left button on the board.
 *
 * **One page per kind of setting (v0.17.0).** Saeed: *"separate the categories — each one's settings must
 * be its own place now; do not scatter them all in one screen. Board settings in one place, link settings
 * in one place, node settings in one place, and the app's general settings in one place."*
 *
 * So this screen is an **index**, and every category opens its own page:
 *
 * | Page | What it holds |
 * |---|---|
 * | **Board** | the room the arrangement leaves between levels, and what is drawn on top of the board |
 * | **Modes** | the eight board modes as live miniatures, and the six themes |
 * | **Links** | relation names on the board, and the relation a new link starts as |
 * | **Nodes** | what a NEW node is born with: priority, shape, colour, size |
 * | **App** | the door to the app's own settings (theme, help, logs, version) |
 *
 * The **Structure** group is gone: the arrangement moved into the node's own menu in v0.15.0. The
 * board-wide **commands** that used to live there (Arrange the map, Collapse all, Expand all) are
 * features, not settings, and sit in the board's own menu in the top bar.
 */
private enum class SettingsPage(val labelRes: Int, val detailRes: Int, val icon: ImageVector) {
    Board(R.string.settings_board, R.string.settings_board_page_detail, Icons.Outlined.Style),
    Modes(R.string.settings_board_mode, R.string.settings_modes_page_detail, Icons.Outlined.GridOn),
    Links(R.string.settings_links, R.string.settings_links_page_detail, Icons.Outlined.Link),
    Nodes(R.string.settings_nodes, R.string.settings_nodes_page_detail, Icons.Outlined.Memory),
    App(R.string.settings_app, R.string.settings_app_page_detail, Icons.Outlined.Settings),
}

@Composable
internal fun ProjectSettingsPanel(
    pattern: BoardPattern,
    palette: BoardPalette,
    spacing: LayoutSpacing,
    showLabels: Boolean,
    showMinimap: Boolean,
    defaultLinkKey: String,
    nodePriority: Int,
    nodeShape: String,
    nodeColor: String?,
    nodeWidth: Float,
    nodeHeight: Float,
    rootCount: Int,
    hiddenCount: Int,
    arrangedCount: Int,
    onPattern: (BoardPattern) -> Unit,
    onPalette: (BoardPalette) -> Unit,
    onSpacing: (LayoutSpacing) -> Unit,
    onShowLabels: (Boolean) -> Unit,
    onShowMinimap: (Boolean) -> Unit,
    onDefaultLink: (String) -> Unit,
    onNodePriority: (Int) -> Unit,
    onNodeShape: (String) -> Unit,
    onNodeColor: (String?) -> Unit,
    onNodeSize: (Float, Float) -> Unit,
    onOpenAppSettings: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var page by remember { mutableStateOf<SettingsPage?>(null) }
    val colors = boardColorsOf(palette)

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = Spacing.s, end = Spacing.s, top = Spacing.s, bottom = Spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (page != null) {
                    IconButton(onClick = { page = null }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back_to_hub),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = page?.let { stringResource(it.labelRes) }
                            ?: stringResource(R.string.project_settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = page?.let { stringResource(it.detailRes) }
                            ?: stringResource(R.string.settings_hub_detail),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.link_close),
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    start = Spacing.screenH,
                    end = Spacing.screenH,
                    bottom = Spacing.xxxl,
                ),
            ) {
                when (page) {
                    // ---- the index: one row per kind of setting -------------------
                    null -> {
                        SettingsPage.entries.forEach { candidate ->
                            item(key = "page_" + candidate.name) {
                                NavRow(
                                    title = stringResource(candidate.labelRes),
                                    subtitle = stringResource(candidate.detailRes),
                                    icon = candidate.icon,
                                    onClick = { page = candidate },
                                )
                            }
                        }
                    }

                    // ---- board -----------------------------------------------------
                    SettingsPage.Board -> {
                        item(key = "board_detail") {
                            Text(
                                text = stringResource(R.string.settings_board_detail, rootCount, hiddenCount),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        item(key = "spacing") {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Spacer(Modifier.height(Spacing.s))
                                Text(
                                    text = stringResource(R.string.settings_spacing),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = stringResource(R.string.settings_spacing_detail, arrangedCount),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(Spacing.s))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    LayoutSpacing.values().forEach { candidate ->
                                        ChoiceChip(
                                            text = candidate.label,
                                            selected = candidate == spacing,
                                            onClick = { onSpacing(candidate) },
                                        )
                                    }
                                }
                            }
                        }
                        item(key = "minimap") {
                            ToggleRow(
                                title = stringResource(R.string.settings_minimap),
                                detail = stringResource(R.string.settings_minimap_detail),
                                checked = showMinimap,
                                onCheckedChange = onShowMinimap,
                            )
                        }
                    }

                    // ---- modes -----------------------------------------------------
                    SettingsPage.Modes -> {
                        item(key = "mode_header") {
                            SectionTitle(stringResource(R.string.settings_board_mode))
                        }
                        items(BoardPattern.values().toList(), key = { "pat_" + it.key }) { candidate ->
                            PatternRow(
                                pattern = candidate,
                                colors = colors,
                                selected = candidate == pattern,
                                onClick = { onPattern(candidate) },
                            )
                        }
                        item(key = "theme_header") {
                            SectionTitle(stringResource(R.string.settings_board_theme))
                        }
                        items(BoardPalette.values().toList(), key = { "pal_" + it.key }) { candidate ->
                            PaletteRow(
                                palette = candidate,
                                selected = candidate == palette,
                                onClick = { onPalette(candidate) },
                            )
                        }
                    }

                    // ---- links -----------------------------------------------------
                    SettingsPage.Links -> {
                        item(key = "labels") {
                            ToggleRow(
                                title = stringResource(R.string.settings_labels),
                                detail = stringResource(R.string.settings_labels_detail),
                                checked = showLabels,
                                onCheckedChange = onShowLabels,
                            )
                        }
                        item(key = "default_link") {
                            var choosing by remember { mutableStateOf(false) }
                            NavRow(
                                title = stringResource(R.string.settings_default_link),
                                subtitle = stringResource(
                                    R.string.settings_default_link_detail,
                                    LinkCatalog.defaultFor(defaultLinkKey).name,
                                ),
                                icon = Icons.Outlined.Link,
                                onClick = { choosing = true },
                            )
                            if (choosing) {
                                DefaultLinkDialog(
                                    current = defaultLinkKey,
                                    onPick = {
                                        onDefaultLink(it)
                                        choosing = false
                                    },
                                    onDismiss = { choosing = false },
                                )
                            }
                        }
                    }

                    // ---- nodes -----------------------------------------------------
                    SettingsPage.Nodes -> {
                        item(key = "nodes_detail") {
                            Text(
                                text = stringResource(R.string.node_defaults_detail),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        item(key = "node_priority") {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Spacer(Modifier.height(Spacing.s))
                                Text(
                                    text = stringResource(R.string.node_defaults_priority),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(Modifier.height(Spacing.s))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    (1..5).forEach { level ->
                                        ChoiceChip(
                                            text = level.toString(),
                                            selected = nodePriority == level,
                                            onClick = { onNodePriority(level) },
                                        )
                                    }
                                }
                            }
                        }
                        item(key = "node_shape") {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Spacer(Modifier.height(Spacing.l))
                                Text(
                                    text = stringResource(R.string.node_defaults_shape),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(Modifier.height(Spacing.s))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    NodeStyle.SHAPES.forEach { option ->
                                        ChoiceChip(
                                            text = option.label,
                                            selected = nodeShape == option.key,
                                            onClick = { onNodeShape(option.key) },
                                        )
                                    }
                                }
                            }
                        }
                        item(key = "node_colour") {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Spacer(Modifier.height(Spacing.l))
                                Text(
                                    text = stringResource(R.string.node_defaults_colour),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(Modifier.height(Spacing.s))
                                NodeColourRow(current = nodeColor, onPick = onNodeColor)
                            }
                        }
                        item(key = "node_size") {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Spacer(Modifier.height(Spacing.l))
                                Text(
                                    text = stringResource(R.string.node_defaults_size),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(Modifier.height(Spacing.s))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    NodeStyle.SIZES.forEach { option ->
                                        ChoiceChip(
                                            text = option.label,
                                            selected = NodeStyle.presetFor(nodeWidth, nodeHeight)?.label ==
                                                option.label,
                                            onClick = { onNodeSize(option.w, option.h) },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ---- the app ---------------------------------------------------
                    SettingsPage.App -> {
                        item(key = "app_row") {
                            NavRow(
                                title = stringResource(R.string.settings_app_open),
                                subtitle = stringResource(R.string.settings_app_open_detail),
                                icon = Icons.Outlined.Settings,
                                onClick = onOpenAppSettings,
                            )
                        }
                    }
                }

                item(key = "footer") {
                    HorizontalDivider(modifier = Modifier.padding(top = Spacing.l))
                }
            }
        }
    }
}

/** The shared chooser, in a dialog: the same searchable list the picker and the inspector use. */
@Composable
private fun DefaultLinkDialog(
    current: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var family by remember { mutableStateOf<LinkFamily?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_default_link)) },
        text = {
            Column(modifier = Modifier.heightIn(max = 460.dp)) {
                LinkSearchField(query = query, onQuery = { query = it })
                Spacer(Modifier.height(Spacing.s))
                LinkFamilyChips(selected = family, onSelect = { family = it })
                Spacer(Modifier.height(Spacing.s))
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    linkResults(
                        query = query,
                        family = family,
                        selectedKey = current,
                        onPick = { type -> onPick(type.key) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

/** One board mode: a live miniature of the pattern in the current theme, its name and its detail. */
@Composable
private fun PatternRow(
    pattern: BoardPattern,
    colors: BoardColors,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.medium))
            .clickable(onClick = onClick)
            .heightIn(min = Sizes.listRowMin)
            .padding(vertical = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = RoundedCornerShape(Radius.small),
            color = colors.background ?: MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            ),
        ) {
            BoardPatternPreview(pattern = pattern, colors = colors)
        }
        Spacer(Modifier.width(Spacing.l))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = pattern.label,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = pattern.detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** One board theme: three colour bars showing the surface, the lines and the accent. */
@Composable
private fun PaletteRow(
    palette: BoardPalette,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = boardColorsOf(palette)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.medium))
            .clickable(onClick = onClick)
            .heightIn(min = Sizes.listRowMin)
            .padding(vertical = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(width = 64.dp, height = 44.dp)
                .background(colors.background ?: MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(colors.major),
                )
                Spacer(Modifier.width(Spacing.xs))
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(colors.accent),
                )
            }
        }
        Spacer(Modifier.width(Spacing.l))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = palette.label,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = palette.detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    detail: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .heightIn(min = Sizes.listRowMin)
            .padding(vertical = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(Spacing.l))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}