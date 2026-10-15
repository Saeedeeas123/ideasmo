package com.saeed.ideasmo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.saeed.ideasmo.core.log.AppLog
import com.saeed.ideasmo.core.log.LogViewerScreen
import com.saeed.ideasmo.core.model.NodeStore
import com.saeed.ideasmo.core.prefs.ThemePrefs
import com.saeed.ideasmo.ui.home.HomeEntry
import com.saeed.ideasmo.ui.home.HomeScreen
import com.saeed.ideasmo.ui.home.RecentItem
import com.saeed.ideasmo.ui.board.BoardScreen
import com.saeed.ideasmo.ui.project.ProjectEntry
import com.saeed.ideasmo.ui.project.ProjectScreen
import com.saeed.ideasmo.ui.theme.IdeasmoTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Log first, so everything after this line is recoverable.
        AppLog.init(applicationContext)
        AppLog.i("Lifecycle", "onCreate")
        NodeStore.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            IdeasmoApp()
        }
    }

    override fun onStart() {
        super.onStart()
        AppLog.i("Lifecycle", "onStart")
        AppLog.i("Mem", AppLog.memoryLine(this))
    }

    override fun onResume() {
        super.onResume()
        AppLog.i("Lifecycle", "onResume")
    }

    override fun onPause() {
        AppLog.i("Lifecycle", "onPause")
        AppLog.flush()
        super.onPause()
    }

    override fun onStop() {
        AppLog.i("Lifecycle", "onStop")
        super.onStop()
    }

    override fun onDestroy() {
        AppLog.i("Lifecycle", "onDestroy")
        super.onDestroy()
    }
}

/** Simple state-based navigation (no navigation library is available on this device). */
private sealed interface Screen {
    data object Home : Screen
    data object Project : Screen
    data object Board : Screen
    data object Logs : Screen
}

@Composable
private fun IdeasmoApp() {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var darkOverride by remember { mutableStateOf(ThemePrefs.load(context)) }
    val isDark = darkOverride ?: isSystemInDarkTheme()

    val backStack = remember { mutableStateListOf<Screen>(Screen.Home) }
    val current = backStack.last()

    fun push(screen: Screen) {
        backStack.add(screen)
        AppLog.i("Nav", "push " + screen + " (depth " + backStack.size + ")")
    }

    fun pop() {
        if (backStack.size > 1) {
            val removed = backStack.removeAt(backStack.lastIndex)
            AppLog.i("Nav", "pop " + removed + " (depth " + backStack.size + ")")
        }
    }

    val toggleTheme: () -> Unit = {
        val next = !isDark
        darkOverride = next
        ThemePrefs.save(context, next)
        scope.launch {
            snackbarHostState.showSnackbar(
                context.getString(if (next) R.string.theme_is_night else R.string.theme_is_day),
            )
        }
    }

    val notBuilt: (String) -> Unit = { label ->
        AppLog.i("Nav", "entry tapped: " + label + " (not built)")
        scope.launch {
            snackbarHostState.showSnackbar(context.getString(R.string.not_built_yet, label))
        }
    }

    BackHandler(enabled = backStack.size > 1) { pop() }

    IdeasmoTheme(darkTheme = isDark) {
        when (current) {
            Screen.Home -> Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface,
            ) {
                HomeScreen(
                    recent = remember { emptyList<RecentItem>() },
                    snackbarHostState = snackbarHostState,
                    onOpenLogs = {
                        push(Screen.Logs)
                        AppLog.i("UI", "log viewer opened (long-press on title)")
                    },
                    onEntry = { entry ->
                        AppLog.i("UI", "home entry tapped: " + entry.name)
                        when (entry) {
                            HomeEntry.Project -> push(Screen.Project)
                            else -> notBuilt(context.getString(entry.labelRes))
                        }
                    },
                )
            }

            Screen.Project -> ProjectScreen(
                snackbarHostState = snackbarHostState,
                onBack = { pop() },
                onEntry = { entry ->
                    AppLog.i("UI", "project entry tapped: " + entry.name)
                    when (entry) {
                        ProjectEntry.Idea -> push(Screen.Board)
                        else -> notBuilt(context.getString(entry.labelRes))
                    }
                },
            )

            Screen.Board -> BoardScreen(
                kind = NodeStore.KIND_IDEA,
                snackbarHostState = snackbarHostState,
                isDark = isDark,
                onToggleTheme = toggleTheme,
                onBack = { pop() },
            )

            Screen.Logs -> LogViewerScreen(
                onClose = {
                    pop()
                    AppLog.i("UI", "log viewer closed")
                },
            )
        }
    }
}