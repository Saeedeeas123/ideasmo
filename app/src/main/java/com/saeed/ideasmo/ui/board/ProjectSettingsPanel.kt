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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.saeed.ideasmo.R
import com.saeed.ideasmo.core.model.LayoutSpacing
import com.saeed.ideasmo.core.model.LinkCatalog
import com.saeed.ideasmo.core.model.LinkFamily
import com.saeed.ideasmo.ui.components.ChoiceChip
import com.saeed.ideasmo.ui.components.NavRow
import com.saeed.ideasmo.ui.components.SectionTitle
import com.saeed.ideasmo.ui.theme.Radius
import com.saeed.ideasmo.ui.theme.Sizes
import com.saeed.ideasmo.ui.theme.Spacing

/**
 * Project settings — opened from the top-left button on the board.
 *
 * **What belongs here, and what does not (v0.16.0).** Saeed's rule for this round: *"a feature is
 * different from a setting"*, and the settings should be about the app in general, the board, the modes
 * and the links. So this screen holds **preferences that are remembered**, in four groups:
 *
 * - **Board** — how the workspace behaves: the spacing the arrangement leaves between levels, and what
 *   is drawn on top of the board (the travelling minimap).
 * - **Modes** — the eight board modes, each shown as a live miniature, and the six themes.
 * - **Links** — relation names on the board, and which relation a new link starts as.
 * - **App** — the door to the app's own settings (theme, help, logs, version), so the app-wide
 *   preferences have exactly one home and this screen does not duplicate them.
 *
 * The **Structure** group is gone: the arrangement moved into the node's own menu in v0.15.0, so the
 * section had nothing left of its own. The board-wide **commands** that used to sit in it — Arrange the
 * map, Collapse all, Expand all — are features, not settings, and now live in the board's own menu in
 * the top bar. Nothing was dropped; each thing moved to where it belongs.
 */
@Composable
internal fun ProjectSettingsPanel(
    pattern: BoardPattern,
    palette: BoardPalette,
    spacing: LayoutSpacing,
    showLabels: Boolean,
    showMinimap: Boolean,
    defaultLinkKey: String,
    rootCount: Int,
    hiddenCount: Int,
    arrangedCount: Int,
    onPattern: (BoardPattern) -> Unit,
    onPalette: (BoardPalette) -> Unit,
    onSpacing: (LayoutSpacing) -> Unit,
    onShowLabels: (Boolean) -> Unit,
    onShowMinimap: (Boolean) -> Unit,
    onDefaultLink: (String) -> Unit,
    onOpenAppSettings: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = boardColorsOf(palette)
    var choosingDefaultLink by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = Spacing.l, end = Spacing.s, top = Spacing.s, bottom = Spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.project_settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.project_settings_detail),
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
                // ---- 1. the board: how the workspace behaves -------------------
                item(key = "board_header") {
                    SectionTitle(stringResource(R.string.settings_board))
                }
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

                // ---- 2. the modes ----------------------------------------------
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

                // ---- 3. the links ----------------------------------------------
                item(key = "links_header") {
                    SectionTitle(stringResource(R.string.settings_links))
                }
                item(key = "labels") {
                    ToggleRow(
                        title = stringResource(R.string.settings_labels),
                        detail = stringResource(R.string.settings_labels_detail),
                        checked = showLabels,
                        onCheckedChange = onShowLabels,
                    )
                }
                item(key = "default_link") {
                    NavRow(
                        title = stringResource(R.string.settings_default_link),
                        subtitle = stringResource(
                            R.string.settings_default_link_detail,
                            LinkCatalog.defaultFor(defaultLinkKey).name,
                        ),
                        icon = Icons.Outlined.Link,
                        onClick = { choosingDefaultLink = true },
                    )
                }

                // ---- 4. the app itself -----------------------------------------
                item(key = "app_header") {
                    SectionTitle(stringResource(R.string.settings_app))
                }
                item(key = "app_row") {
                    NavRow(
                        title = stringResource(R.string.settings_app_open),
                        subtitle = stringResource(R.string.settings_app_open_detail),
                        icon = Icons.Outlined.Settings,
                        onClick = onOpenAppSettings,
                    )
                }

                item(key = "footer") {
                    HorizontalDivider(modifier = Modifier.padding(top = Spacing.l))
                }
            }
        }
    }

    if (choosingDefaultLink) {
        // The same chooser the picker and the inspector use (v0.15.0), so "which relation" is found the
        // same way everywhere in the app.
        var query by remember { mutableStateOf("") }
        var family by remember { mutableStateOf<LinkFamily?>(null) }
        AlertDialog(
            onDismissRequest = { choosingDefaultLink = false },
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
                            selectedKey = defaultLinkKey,
                            onPick = { type ->
                                onDefaultLink(type.key)
                                choosingDefaultLink = false
                            },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { choosingDefaultLink = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
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
        Row(
            modifier = Modifier
                .size(width = 64.dp, height = 44.dp)
                .clip(RoundedCornerShape(Radius.small)),
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