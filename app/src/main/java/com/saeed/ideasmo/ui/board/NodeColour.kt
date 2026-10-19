package com.saeed.ideasmo.ui.board

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.saeed.ideasmo.R
import com.saeed.ideasmo.core.model.LinkColor
import com.saeed.ideasmo.core.model.NodeStyle
import com.saeed.ideasmo.ui.theme.Radius
import com.saeed.ideasmo.ui.theme.Sizes
import com.saeed.ideasmo.ui.theme.Spacing

/**
 * **Node colour** (v0.17.0).
 *
 * Saeed asked for two things: *"more base colours"* and *"an RGB palette so any colour can be made"*.
 * Both are here, and neither needed a model change: a colour has always been a **string key** on the
 * node, so the presets are plain hex strings and a custom colour is just `#RRGGBB`. A role key
 * ("primary") still follows day/night and the wallpaper colour; a fixed colour does what it says.
 *
 * The card's tone is always the surface blended with the chosen colour, never the colour at full
 * strength — that is what keeps the text on a card readable whatever colour the user picks, including
 * black or white from the custom picker.
 */
internal fun nodeColorOf(scheme: androidx.compose.material3.ColorScheme, key: String?): Color? {
    if (key == null) return null
    val role = LinkColor.values().firstOrNull { it.name.equals(key, true) }
    if (role != null) return themeRoleColor(scheme, role)
    val argb = NodeStyle.parseHex(key) ?: return null
    return Color(argb.toInt())
}

/** The card's own container colour: the surface, tinted by the node's colour when it has one. */
internal fun nodeContainerColor(
    scheme: androidx.compose.material3.ColorScheme,
    key: String?,
    strength: Float = 0.30f,
): Color {
    val tint = nodeColorOf(scheme, key) ?: return scheme.surfaceVariant
    return lerp(scheme.surfaceVariant, tint, strength)
}

private fun Color.luminance(): Float =
    0.2126f * red + 0.7152f * green + 0.0722f * blue

/**
 * The colour chooser: the five theme roles, the twelve preset colours, and **Custom…** which opens the
 * RGB builder. One row, scrollable, used by both the node's action box and the style sheet, so a colour
 * is chosen the same way in both.
 */
@Composable
internal fun NodeColourRow(
    current: String?,
    onPick: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var custom by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ColourSwatch(
            label = stringResource(R.string.style_default),
            color = MaterialTheme.colorScheme.surfaceVariant,
            selected = current == null,
            onClick = { onPick(null) },
        )
        NodeStyle.COLORS.forEach { option ->
            val role = LinkColor.values().firstOrNull { it.name.equals(option.key, true) }
            ColourSwatch(
                label = option.label,
                color = if (role != null) {
                    themeRoleColor(MaterialTheme.colorScheme, role)
                } else {
                    MaterialTheme.colorScheme.outline
                },
                selected = current == option.key,
                onClick = { onPick(option.key) },
            )
        }
        NodeStyle.PRESETS.forEach { option ->
            ColourSwatch(
                label = option.label,
                color = nodeColorOf(MaterialTheme.colorScheme, option.key)
                    ?: MaterialTheme.colorScheme.outline,
                selected = current == option.key,
                onClick = { onPick(option.key) },
            )
        }
        // A custom colour that is not one of the presets shows as its own selected swatch, so the
        // choice is never invisible.
        if (NodeStyle.isCustomColor(current) && NodeStyle.PRESETS.none { it.key == current }) {
            ColourSwatch(
                label = stringResource(R.string.colour_custom),
                color = nodeColorOf(MaterialTheme.colorScheme, current)!!,
                selected = true,
                onClick = { custom = true },
            )
        }
        ColourSwatch(
            label = stringResource(R.string.colour_custom),
            color = MaterialTheme.colorScheme.tertiary,
            selected = false,
            onClick = { custom = true },
            showDot = false,
        )
    }

    if (custom) {
        CustomColourDialog(
            initial = current,
            onPick = { key ->
                onPick(key)
                custom = false
            },
            onDismiss = { custom = false },
        )
    }
}

@Composable
private fun ColourSwatch(
    label: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    showDot: Boolean = true,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.small))
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.s, vertical = Spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (showDot) {
            Box(
                modifier = Modifier
                    .size(if (selected) 26.dp else 22.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
        )
    }
}

/**
 * The RGB builder: three sliders, a live preview and the hex it will store. Any colour at all is
 * reachable — including the ones a fixed palette can never contain.
 */
@Composable
internal fun CustomColourDialog(
    initial: String?,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val start = NodeStyle.rgbOf(initial) ?: Triple(0x1E, 0x88, 0xE5)
    var red by remember { mutableStateOf(start.first) }
    var green by remember { mutableStateOf(start.second) }
    var blue by remember { mutableStateOf(start.third) }
    val hex = NodeStyle.hexOf(red, green, blue)
    val preview = Color(0xFF000000.toInt() or (red shl 16) or (green shl 8) or blue)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.colour_custom_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(Radius.medium))
                        .background(preview),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = hex,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (preview.luminance() > 0.6f) Color(0xFF1B1B1B) else Color.White,
                    )
                }
                Spacer(Modifier.height(Spacing.m))
                Channel(stringResource(R.string.colour_red), red) { red = it }
                Channel(stringResource(R.string.colour_green), green) { green = it }
                Channel(stringResource(R.string.colour_blue), blue) { blue = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { onPick(hex) }) {
                Text(stringResource(R.string.colour_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun Channel(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(56.dp),
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt().coerceIn(0, 255)) },
            valueRange = 0f..255f,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(36.dp),
        )
    }
}

/** The colour of a swatch as an ARGB int — used by callers that need a raw value (screenshots, tests). */
internal fun argbOf(color: Color): Int = color.toArgb()