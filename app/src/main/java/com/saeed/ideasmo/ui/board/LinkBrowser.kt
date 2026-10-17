package com.saeed.ideasmo.ui.board

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.saeed.ideasmo.R
import com.saeed.ideasmo.core.model.LinkCatalog
import com.saeed.ideasmo.core.model.LinkFamily
import com.saeed.ideasmo.core.model.LinkType
import com.saeed.ideasmo.ui.components.ChoiceChip
import com.saeed.ideasmo.ui.theme.Radius
import com.saeed.ideasmo.ui.theme.Sizes
import com.saeed.ideasmo.ui.theme.Spacing

/**
 * **The shared link chooser (v0.15.0).**
 *
 * Saeed's order for this round: "the links have not had much attention — keep what they do, but tidy
 * it up and organise it so that choosing, sorting into groups, looking at them and editing them is
 * easier."
 *
 * The catalog holds 42 relations in seven families, which is the right model and the wrong browsing
 * experience on a phone: before this, both the picker and the inspector printed all 42 in one long
 * scroll. These three pieces — the search field, the family chips and the grouped results — are used
 * by **both** screens, so choosing a New relation and changing an existing one behave identically,
 * and there is exactly one place to fix if that changes.
 */

/** Free-text search over the whole catalog: name, phrase, example, meaning, key or family. */
@Composable
internal fun LinkSearchField(
    query: String,
    onQuery: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQuery,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(Radius.medium),
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                modifier = Modifier.size(Sizes.iconSm),
            )
        },
        // The same field filters the 42 relation types and a node's own relations, so the hint is a
        // parameter: a catalog search and a list filter are the same gesture, worded for the place
        // they are in.
        placeholder = {
            Text(placeholder.ifEmpty { stringResource(R.string.link_search_hint) })
        },
    )
}

/**
 * One chip per family, plus **All**. The chips carry how many relations are in each family, so the
 * user knows what they are choosing between before tapping.
 */
@Composable
internal fun LinkFamilyChips(
    selected: LinkFamily?,
    onSelect: (LinkFamily?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val counts = LinkCatalog.countByFamily()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChoiceChip(
            text = stringResource(R.string.link_family_all, LinkCatalog.all.size),
            selected = selected == null,
            onClick = { onSelect(null) },
        )
        LinkFamily.values().forEach { family ->
            ChoiceChip(
                text = family.title + " " + (counts[family] ?: 0),
                selected = selected == family,
                onClick = { onSelect(family) },
            )
        }
    }
}

/**
 * The catalog itself, as LazyColumn items: a heading per family (with the family's own question) and
 * one row per relation. Families with nothing left after filtering are not emitted at all, so no
 * empty heading can appear.
 *
 * It is a [LazyListScope] extension rather than a composable so that the picker and the inspector can
 * drop it into their own scroll containers — the inspector has its sections above it, the picker has
 * the node's existing links.
 */
internal fun LazyListScope.linkResults(
    query: String,
    family: LinkFamily?,
    selectedKey: String?,
    onPick: (LinkType) -> Unit,
) {
    val groups = LinkCatalog.browse(query, family)
    if (groups.isEmpty()) {
        item(key = "link_no_match") {
            Column(modifier = Modifier.padding(vertical = Spacing.l)) {
                Text(
                    text = stringResource(R.string.link_no_match),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.link_no_match_detail),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }
    groups.forEach { (group, types) ->
        item(key = "family_" + group.name) {
            FamilyHeader(title = group.title, question = group.question)
        }
        items(types, key = { "type_" + it.key }) { type ->
            LinkTypeRow(
                type = type,
                selected = type.key == selectedKey,
                onClick = { onPick(type) },
            )
        }
    }
}
