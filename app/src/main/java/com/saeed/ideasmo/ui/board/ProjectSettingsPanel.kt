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
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.saeed.ideasmo.R
import com.saeed.ideasmo.core.model.LayoutDirection
import com.saeed.ideasmo.core.model.LayoutSpacing
import com.saeed.ideasmo.ui.components.ChoiceChip
import com.saeed.ideasmo.ui.components.SectionTitle
import com.saeed.ideasmo.ui.theme.Radius
import com.saeed.ideasmo.ui.theme.Sizes
import com.saeed.ideasmo.ui.theme.Spacing

/**
 * Project settings — opened from the top-left button on the board, as ordered.
 *
 * The **first** setting is the board mode: eight surfaces, from the plain sheet this app shipped
 * with to the patterns infinite-canvas and drafting tools use, each shown as a live miniature so it
 * is chosen by looking at it. The board theme follows, then the structure of the map (direction,
 * spacing, arrange, collapse) and finally what is drawn on top of the board.
 */
@Composable
internal fun ProjectSettingsPanel(
    pattern: BoardPattern,
    palette: BoardPalette,
    direction: LayoutDirection,
    spacing: LayoutSpacing,
    showLabels: Boolean,
    showMinimap: Boolean,
    showOrigin: Boolean,
    rootCount: Int,
    hiddenCount: Int,
    onPattern: (BoardPattern) -> Unit,
    onPalette: (BoardPalette) -> Unit,
    onDirection: (LayoutDirection) -> Unit,
    onSpacing: (LayoutSpacing) -> Unit,
    onShowLabels: (Boolean) -> Unit,
    onShowMinimap: (Boolean) -> Unit,
    onShowOrigin: (Boolean) -> Unit,
    onArrange: () -> Unit,
    onCollapseAll: () -> Unit,
    onExpandAll: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = boardColorsOf(palette)

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
                // ---- 1. board mode -------------------------------------------
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

                // ---- 2. board theme ------------------------------------------
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

                // ---- 3. structure --------------------------------------------
                item(key = "structure_header") {
                    SectionTitle(stringResource(R.string.settings_structure))
                }
                item(key = "structure_hint") {
                    Text(
                        text = stringResource(R.string.settings_structure_detail, rootCount, hiddenCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item(key = "direction") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(Modifier.height(Spacing.s))
                        Text(
                            text = stringResource(R.string.settings_direction),
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
                            LayoutDirection.values().forEach { candidate ->
                                ChoiceChip(
                                    text = candidate.label,
                                    selected = candidate == direction,
                                    onClick = { onDirection(candidate) },
                                )
                            }
                        }
                        Spacer(Modifier.height(Spacing.l))
                        Text(
                            text = stringResource(R.string.settings_spacing),
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
                            LayoutSpacing.values().forEach { candidate ->
                                ChoiceChip(
                                    text = candidate.label,
                                    selected = candidate == spacing,
                                    onClick = { onSpacing(candidate) },
                                )
                            }
                        }
                        Spacer(Modifier.height(Spacing.l))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedButton(
                                onClick = onArrange,
                                modifier = Modifier.heightIn(min = Sizes.touchMin),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AutoFixHigh,
                                    contentDescription = null,
                                    modifier = Modifier.size(Sizes.iconSm),
                                )
                                Spacer(Modifier.width(Spacing.s))
                                Text(stringResource(R.string.arrange_now))
                            }
                            TextButton(
                                onClick = onCollapseAll,
                                modifier = Modifier.heightIn(min = Sizes.touchMin),
                            ) {
                                Text(stringResource(R.string.collapse_all))
                            }
                            TextButton(
                                onClick = onExpandAll,
                                modifier = Modifier.heightIn(min = Sizes.touchMin),
                            ) {
                                Text(stringResource(R.string.expand_all))
                            }
                        }
                    }
                }

                // ---- 4. display ----------------------------------------------
                item(key = "display_header") {
                    SectionTitle(stringResource(R.string.settings_display))
                }
                item(key = "labels") {
                    ToggleRow(
                        title = stringResource(R.string.settings_labels),
                        detail = stringResource(R.string.settings_labels_detail),
                        checked = showLabels,
                        onCheckedChange = onShowLabels,
                    )
                }
                item(key = "minimap") {
                    ToggleRow(
                        title = stringResource(R.string.settings_minimap),
                        detail = stringResource(R.string.settings_minimap_detail),
                        checked = showMinimap,
                        onCheckedChange = onShowMinimap,
                    )
                }
                item(key = "origin") {
                    ToggleRow(
                        title = stringResource(R.string.settings_origin),
                        detail = stringResource(R.string.settings_origin_detail),
                        checked = showOrigin,
                        onCheckedChange = onShowOrigin,
                    )
                }

                item(key = "footer") {
                    HorizontalDivider(modifier = Modifier.padding(top = Spacing.l))
                }
            }
        }
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