package com.saeed.ideasmo.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Subject
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.saeed.ideasmo.R
import com.saeed.ideasmo.ui.components.NavRow
import com.saeed.ideasmo.ui.components.SectionTitle
import com.saeed.ideasmo.ui.theme.Spacing

/**
 * Settings. Only real entries are listed — nothing placeholder. Help lives here, as ordered,
 * together with the theme switch and the app's own log viewer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenLogs: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val version = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
        } catch (t: Throwable) {
            "?"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
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
            SectionTitle(stringResource(R.string.settings_appearance))
            NavRow(
                title = if (isDark) {
                    stringResource(R.string.theme_is_night)
                } else {
                    stringResource(R.string.theme_is_day)
                },
                subtitle = stringResource(R.string.settings_appearance_detail),
                icon = if (isDark) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                onClick = onToggleTheme,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.s))

            SectionTitle(stringResource(R.string.settings_help))
            NavRow(
                title = stringResource(R.string.settings_help),
                subtitle = stringResource(R.string.settings_help_detail),
                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                onClick = onOpenHelp,
            )
            NavRow(
                title = stringResource(R.string.settings_logs),
                subtitle = stringResource(R.string.settings_logs_detail),
                icon = Icons.AutoMirrored.Outlined.Subject,
                onClick = onOpenLogs,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.s))

            SectionTitle(stringResource(R.string.about_title))
            Text(
                text = stringResource(R.string.about_version, version),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = stringResource(R.string.about_line),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(Spacing.xxl))
        }
    }
}