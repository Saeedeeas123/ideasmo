package com.saeed.ideasmo.ui.board

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.saeed.ideasmo.R
import com.saeed.ideasmo.core.model.IdeaNode
import com.saeed.ideasmo.core.model.LinkColor
import com.saeed.ideasmo.core.model.NodeStyle
import com.saeed.ideasmo.ui.components.ChoiceChip
import com.saeed.ideasmo.ui.components.SectionTitle
import com.saeed.ideasmo.ui.theme.Radius
import com.saeed.ideasmo.ui.theme.Sizes
import com.saeed.ideasmo.ui.theme.Spacing
import kotlin.math.roundToInt
/**
 * The node's style: size, colour and shape.
 *
 * Resize, Color and Shape all open this one sheet, because they are three views of the same thing —
 * how the card looks — and splitting them into three separate screens would only hide options from
 * each other. Every option carries its own description from [NodeStyle], so the choice is explained
 * rather than guessed.
 */
@Composable
internal fun NodeStylePanel(
    node: IdeaNode,
    onSize: (Float, Float) -> Unit,
    onColor: (String?) -> Unit,
    onShape: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                        text = stringResource(R.string.node_style_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = node.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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
                item(key = "size_header") { SectionTitle(stringResource(R.string.style_size)) }
                item(key = "size_row") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NodeStyle.SIZES.forEach { option ->
                            ChoiceChip(
                                text = option.label,
                                selected = NodeStyle.presetFor(node.width, node.height)?.label == option.label,
                                onClick = { onSize(option.w, option.h) },
                            )
                        }
                    }
                }
                item(key = "size_detail") {
                    Text(
                        text = NodeStyle.presetFor(node.width, node.height)?.detail
                            ?: stringResource(
                                R.string.style_size_custom,
                                node.width.roundToInt(),
                                node.height.roundToInt(),
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.s),
                    )
                }

                item(key = "colour_header") { SectionTitle(stringResource(R.string.style_colour)) }
                item(key = "colour_row") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ChoiceChip(
                            text = stringResource(R.string.style_default),
                            selected = node.color == null,
                            onClick = { onColor(null) },
                            leading = { ColourDot(color = MaterialTheme.colorScheme.surfaceVariant) },
                        )
                        NodeStyle.COLORS.forEach { option ->
                            val role = LinkColor.values().firstOrNull { it.name.equals(option.key, true) }
                            ChoiceChip(
                                text = option.label,
                                selected = node.color == option.key,
                                onClick = { onColor(option.key) },
                                leading = {
                                    ColourDot(
                                        color = if (role != null) {
                                            themeRoleColor(MaterialTheme.colorScheme, role)
                                        } else {
                                            MaterialTheme.colorScheme.outline
                                        },
                                    )
                                },
                            )
                        }
                    }
                }
                item(key = "colour_detail") {
                    Text(
                        text = NodeStyle.COLORS.firstOrNull { it.key == node.color }?.detail
                            ?: stringResource(R.string.style_colour_default_detail),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.s),
                    )
                }

                item(key = "shape_header") { SectionTitle(stringResource(R.string.style_shape)) }
                item(key = "shape_row") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NodeStyle.SHAPES.forEach { option ->
                            ChoiceChip(
                                text = option.label,
                                selected = node.shape == option.key,
                                onClick = { onShape(option.key) },
                            )
                        }
                    }
                }
                item(key = "shape_detail") {
                    Text(
                        text = NodeStyle.SHAPES.firstOrNull { it.key == node.shape }?.detail ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.s),
                    )
                }

                item(key = "note") {
                    Column(modifier = Modifier.padding(top = Spacing.xl)) {
                        Text(
                            text = stringResource(R.string.style_note),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColourDot(color: Color) {
    Box(
        modifier = Modifier
            .size(14.dp)
            .clip(CircleShape)
            .background(color),
    )
}

/** The card's shape for a stored key — plain Compose shapes, no custom drawing. */
internal fun nodeShapeOf(key: String) = when (key) {
    NodeStyle.SHAPE_SQUARE -> RoundedCornerShape(2.dp)
    NodeStyle.SHAPE_PILL -> CircleShape
    NodeStyle.SHAPE_CUT -> androidx.compose.foundation.shape.CutCornerShape(18.dp)
    else -> RoundedCornerShape(Radius.medium)
}