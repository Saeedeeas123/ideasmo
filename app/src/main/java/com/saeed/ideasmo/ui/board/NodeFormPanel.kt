package com.saeed.ideasmo.ui.board

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.saeed.ideasmo.R
import com.saeed.ideasmo.core.log.AppLog
import com.saeed.ideasmo.core.util.decodeThumbnail
import com.saeed.ideasmo.ui.components.PrimaryAction
import com.saeed.ideasmo.ui.theme.Radius
import com.saeed.ideasmo.ui.theme.Sizes
import com.saeed.ideasmo.ui.theme.Spacing

private const val PIC_BOX = 112

/**
 * The node creation panel. Fixed at the bottom of the board (it must not pan away),
 * with keyboard insets and internal scrolling.
 */
@Composable
internal fun NodeFormPanel(
    modifier: Modifier = Modifier,
    name: String,
    onNameChange: (String) -> Unit,
    priority: Int,
    onPriorityChange: (Int) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    picPath: String?,
    onPickImage: () -> Unit,
    onAi: () -> Unit,
    onCancel: () -> Unit,
    onCreate: () -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .imePadding(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(topStart = Radius.large, topEnd = Radius.large),
        tonalElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier
                .heightIn(max = 460.dp)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.l),
        ) {
            Text(
                text = stringResource(R.string.priority),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.xs))
            Row {
                (1..5).forEach { level ->
                    Box(
                        modifier = Modifier
                            .size(Sizes.touchMin)
                            .clip(CircleShape)
                            .clickable {
                                onPriorityChange(level)
                                AppLog.i("Board", "priority set to " + level)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(
                                    if (level <= priority) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                ),
                        )
                    }
                }
            }

            Spacer(Modifier.height(Spacing.m))

            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(Radius.medium),
                label = { Text(stringResource(R.string.node_name)) },
            )

            Spacer(Modifier.height(Spacing.l))

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(R.string.description),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FilledTonalButton(
                            onClick = onAi,
                            modifier = Modifier.heightIn(min = Sizes.touchMin),
                        ) {
                            Text(stringResource(R.string.ai_assist))
                        }
                    }
                    OutlinedTextField(
                        value = description,
                        onValueChange = onDescriptionChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 96.dp),
                        shape = RoundedCornerShape(Radius.medium),
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.pic),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    PicBox(picPath = picPath, onPick = onPickImage)
                }
            }

            Spacer(Modifier.height(Spacing.l))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.cancel))
                }
                Spacer(Modifier.width(Spacing.s))
                PrimaryAction(text = stringResource(R.string.create_node), onClick = onCreate)
            }
        }
    }
}

@Composable
private fun PicBox(picPath: String?, onPick: () -> Unit) {
    val context = LocalContext.current
    val bitmap = remember(picPath) { picPath?.let { decodeThumbnail(context, it) } }

    Box(
        modifier = Modifier
            .size(PIC_BOX.dp)
            .clip(RoundedCornerShape(Radius.medium))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onPick),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = stringResource(R.string.cd_pick_image),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Sizes.iconMd),
            )
        }
    }
}