package com.saeed.ideasmo.ui.help

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.saeed.ideasmo.R
import com.saeed.ideasmo.core.model.LinkCatalog
import com.saeed.ideasmo.core.model.LinkFamily
import com.saeed.ideasmo.ui.board.LinkTypeRow
import com.saeed.ideasmo.ui.components.SectionTitle
import com.saeed.ideasmo.ui.theme.Spacing

/**
 * Help. The relations section is generated from [LinkCatalog], so this page can never fall out
 * of step with what the board actually supports.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onBack: () -> Unit) {
    val gestures = listOf(
        stringResource(R.string.help_gesture_pan),
        stringResource(R.string.help_gesture_zoom),
        stringResource(R.string.help_gesture_reset),
        stringResource(R.string.help_gesture_minimap),
        stringResource(R.string.help_gesture_node),
        stringResource(R.string.help_gesture_select),
        stringResource(R.string.help_gesture_create),
        stringResource(R.string.help_gesture_list),
        stringResource(R.string.help_gesture_band),
        stringResource(R.string.help_gesture_multi),
        stringResource(R.string.help_gesture_back),
        stringResource(R.string.help_gesture_undo),
        stringResource(R.string.help_gesture_tools),
    )
    val families = LinkCatalog.byFamily()
    val linkEditing = listOf(
        stringResource(R.string.help_link_edit_1),
        stringResource(R.string.help_link_edit_2),
        stringResource(R.string.help_link_edit_3),
        stringResource(R.string.help_link_edit_4),
        stringResource(R.string.help_link_edit_5),
    )
    val structure = listOf(
        stringResource(R.string.help_structure_1),
        stringResource(R.string.help_structure_2),
        stringResource(R.string.help_structure_3),
        stringResource(R.string.help_structure_4),
        stringResource(R.string.help_structure_5),
        stringResource(R.string.help_origin_1),
        stringResource(R.string.help_origin_2),
        stringResource(R.string.help_origin_3),
    )
    val boardMode = listOf(
        stringResource(R.string.help_board_mode_1),
        stringResource(R.string.help_board_mode_2),
        stringResource(R.string.help_board_mode_3),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.help_title),
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = PaddingValues(
                start = Spacing.screenH,
                end = Spacing.screenH,
                bottom = Spacing.xxl,
            ),
        ) {
            item(key = "board_title") {
                SectionTitle(stringResource(R.string.help_board_title))
            }
            items(gestures, key = { it }) { line ->
                HelpLine(text = line)
            }
            item(key = "link_edit_title") {
                SectionTitle(stringResource(R.string.help_link_edit_title))
            }
            items(linkEditing, key = { "le_" + it }) { line ->
                HelpLine(text = line)
            }

            item(key = "structure_title") {
                SectionTitle(stringResource(R.string.help_structure_title))
            }
            items(structure, key = { "st_" + it }) { line ->
                HelpLine(text = line)
            }

            item(key = "board_mode_title") {
                SectionTitle(stringResource(R.string.help_board_mode_title))
            }
            items(boardMode, key = { "bm_" + it }) { line ->
                HelpLine(text = line)
            }

            item(key = "links_title") {
                SectionTitle(stringResource(R.string.help_links_title))
            }
            item(key = "links_intro") {
                Text(
                    text = stringResource(R.string.help_links_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Spacing.s),
                )
            }

            families.forEach { (family, types) ->
                item(key = "family_${family.name}") {
                    FamilyBlock(family = family)
                }
                items(types, key = { it.key }) { type ->
                    LinkTypeRow(type = type)
                }
            }

            item(key = "divider") {
                HorizontalDivider(modifier = Modifier.padding(top = Spacing.xl))
            }
        }
    }
}

@Composable
private fun HelpLine(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun FamilyBlock(family: LinkFamily) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.xl, bottom = Spacing.xs),
    ) {
        Text(
            text = family.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = family.question,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.xs))
    }
}