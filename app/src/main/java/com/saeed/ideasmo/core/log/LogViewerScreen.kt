package com.saeed.ideasmo.core.log

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import com.saeed.ideasmo.R
import com.saeed.ideasmo.ui.theme.Spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/**
 * In-app log viewer (standing hard rule). Live-updating, filterable, with session
 * picker, copy and share. Shows a visible banner when the log file could not be opened.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewerScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var sessions by remember { mutableStateOf(AppLog.sessions(context)) }
    var selected by remember { mutableStateOf(sessions.firstOrNull()) }
    var lines by remember { mutableStateOf(listOf<String>()) }
    var filter by remember { mutableStateOf(0) }
    var autoScroll by remember { mutableStateOf(true) }
    var showSessions by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(selected, filter, autoScroll) {
        while (true) {
            val file = selected
            val source = if (file != null && file.exists()) {
                AppLog.readTail(file, 2000)
            } else {
                AppLog.memoryTail
            }
            lines = source.filter { keepLine(it, filter) }
            if (autoScroll && lines.isNotEmpty()) {
                listState.scrollToItem(lines.size - 1)
            }
            delay(1000L)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.logs_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onClose) {
                        Text(stringResource(R.string.logs_close))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = Spacing.screenH),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                TextButton(onClick = {
                    copyLog(context, lines)
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.logs_copied)) }
                }) { Text(stringResource(R.string.logs_copy)) }

                TextButton(onClick = { shareLog(context, lines) }) {
                    Text(stringResource(R.string.logs_share))
                }

                TextButton(onClick = {
                    sessions = AppLog.sessions(context)
                    showSessions = true
                }) { Text(stringResource(R.string.logs_sessions)) }

                TextButton(onClick = { autoScroll = !autoScroll }) {
                    Text(
                        if (autoScroll) {
                            stringResource(R.string.logs_autoscroll_on)
                        } else {
                            stringResource(R.string.logs_autoscroll_off)
                        }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                FilterButton(stringResource(R.string.filter_all), filter == 0) { filter = 0 }
                FilterButton(stringResource(R.string.filter_info), filter == 1) { filter = 1 }
                FilterButton(stringResource(R.string.filter_warn), filter == 2) { filter = 2 }
                FilterButton(stringResource(R.string.filter_error), filter == 3) { filter = 3 }
            }

            val openError = AppLog.openError
            if (openError != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.s),
                ) {
                    Text(
                        text = stringResource(R.string.log_open_failed) + "\n" + openError,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(Spacing.m),
                    )
                }
            }

            val file = selected
            Text(
                text = (file?.absolutePath ?: AppLog.logFile(context).absolutePath) +
                    "   ·   " + lines.size + " lines",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(vertical = Spacing.s),
            )

            if (lines.isEmpty()) {
                Text(
                    text = stringResource(R.string.logs_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = Spacing.l),
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    items(lines) { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }

    if (showSessions) {
        AlertDialog(
            onDismissRequest = { showSessions = false },
            title = { Text(stringResource(R.string.logs_sessions)) },
            text = {
                Column {
                    if (sessions.isEmpty()) {
                        Text(stringResource(R.string.logs_empty))
                    } else {
                        sessions.forEach { session ->
                            TextButton(onClick = {
                                selected = session
                                showSessions = false
                                AppLog.i("LogViewer", "session selected: " + session.name)
                            }) {
                                Text(session.name + "  (" + session.length() / 1024 + " KB)")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSessions = false }) {
                    Text(stringResource(R.string.logs_close))
                }
            },
        )
    }
}

@Composable
private fun FilterButton(label: String, active: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(
            text = label,
            color = if (active) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

private fun keepLine(line: String, filter: Int): Boolean = when (filter) {
    1 -> line.contains(" I [") || line.contains(" W [") || line.contains(" E [")
    2 -> line.contains(" W [") || line.contains(" E [")
    3 -> line.contains(" E [")
    else -> true
}

private fun copyLog(context: Context, lines: List<String>) {
    try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Ideasmo log", lines.joinToString("\n")))
        AppLog.i("LogViewer", "copied " + lines.size + " lines to clipboard")
    } catch (t: Throwable) {
        AppLog.e("LogViewer", "copy failed", t)
    }
}

private fun shareLog(context: Context, lines: List<String>) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Ideasmo log")
            putExtra(Intent.EXTRA_TEXT, lines.joinToString("\n"))
        }
        context.startActivity(Intent.createChooser(intent, "Share log"))
        AppLog.i("LogViewer", "share intent launched")
    } catch (t: Throwable) {
        AppLog.e("LogViewer", "share failed", t)
    }
}