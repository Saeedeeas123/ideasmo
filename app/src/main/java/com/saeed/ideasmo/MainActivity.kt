package com.saeed.ideasmo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.saeed.ideasmo.core.drive.DriveStore
import com.saeed.ideasmo.core.log.AppLog
import com.saeed.ideasmo.core.log.LogViewerScreen
import com.saeed.ideasmo.core.model.NodeStore
import com.saeed.ideasmo.core.prefs.BoardPrefs
import com.saeed.ideasmo.core.prefs.ThemePrefs
import com.saeed.ideasmo.ui.board.BoardScreen
import com.saeed.ideasmo.ui.components.NoticeBar
import com.saeed.ideasmo.ui.drive.DriveScreen
import com.saeed.ideasmo.ui.help.HelpScreen
import com.saeed.ideasmo.ui.home.HomeEntry
import com.saeed.ideasmo.ui.home.HomeScreen
import com.saeed.ideasmo.ui.home.RecentItem
import com.saeed.ideasmo.ui.project.ProjectEntry
import com.saeed.ideasmo.ui.project.ProjectScreen
import com.saeed.ideasmo.ui.settings.SettingsScreen
import com.saeed.ideasmo.ui.theme.IdeasmoTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Log first, so everything after this line is recoverable.
        AppLog.init(applicationContext)
        AppLog.i("Lifecycle", "onCreate")
        NodeStore.init(applicationContext)
        // The Drive is the app's one file store (v0.13.0). Initialise it before anything can attach
        // a file, and move the pre-Drive attachment folders into it so no node loses its pictures.
        DriveStore.init(applicationContext)
        val moved = DriveStore.migrateLegacy(applicationContext)
        if (moved.isNotEmpty()) NodeStore.remapAttachments(applicationContext, moved)
        BoardPrefs.init(applicationContext)
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
    data object Drive : Screen
    data object Board : Screen
    data object Settings : Screen
    data object Help : Screen
    data object Logs : Screen
}

@Composable
private fun IdeasmoApp() {
    val context = LocalContext.current

    // Everything that used to be a floating toast is an inline strip now, owned here so every
    // screen reports the same way. See NoticeBar.
    var notice by remember { mutableStateOf<String?>(null) }

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
        notice = context.getString(if (next) R.string.theme_is_night else R.string.theme_is_day)
    }

    val notBuilt: (String) -> Unit = { label ->
        AppLog.i("Nav", "entry tapped: " + label + " (not built)")
        notice = context.getString(R.string.not_built_yet, label)
    }

    BackHandler(enabled = backStack.size > 1) { pop() }

    IdeasmoTheme(darkTheme = isDark) {
        // The screen fills the space; anything the app has to say sits below it, in the layout —
        // never floating over the content.
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                when (current) {
            Screen.Home -> Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface,
            ) {
                HomeScreen(
                    recent = remember { emptyList<RecentItem>() },
                    onOpenLogs = {
                        push(Screen.Logs)
                        AppLog.i("UI", "log viewer opened (long-press on title)")
                    },
                    onEntry = { entry ->
                        AppLog.i("UI", "home entry tapped: " + entry.name)
                        when (entry) {
                            HomeEntry.Project -> push(Screen.Project)
                            HomeEntry.Drive -> push(Screen.Drive)
                            HomeEntry.Settings -> push(Screen.Settings)
                            else -> notBuilt(context.getString(entry.labelRes))
                        }
                    },
                )
            }

            Screen.Project -> ProjectScreen(
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
                isDark = isDark,
                onToggleTheme = toggleTheme,
                onBack = { pop() },
            )

            Screen.Drive -> DriveScreen(onBack = { pop() })
            Screen.Settings -> SettingsScreen(
                isDark = isDark,
                onToggleTheme = toggleTheme,
                onOpenHelp = { push(Screen.Help) },
                onOpenLogs = { push(Screen.Logs) },
                onBack = { pop() },
            )

            Screen.Help -> HelpScreen(onBack = { pop() })

            Screen.Logs -> LogViewerScreen(
                onClose = {
                    pop()
                    AppLog.i("UI", "log viewer closed")
                },
            )
                }
            }
            // The strip is the LAST thing in the layout and it is a custom surface, so it has to carry
            // the system-bar inset itself — otherwise it hides under the home indicator (Saeed's
            // report of 2026-10-14).
            //
            // THE INSET IS APPLIED ONLY WHILE THE STRIP IS THERE. That is the whole fix for Saeed's
            // next report (2026-10-17: "the bottom bar is far too high, it used to sit behind the home
            // and back buttons"): v0.14.0 wrapped the strip in `Box(navigationBarsPadding())` with the
            // `notice?.let` INSIDE it, so the inset was reserved at every moment. Two things followed
            // from that, and both are exactly what he saw —
            //   1. the screen above it (weight 1f) was shortened by the navigation bar's height even
            //      with nothing to say, so every screen's bottom edge floated up;
            //   2. a screen that already carries the inset on its own bottom bar (the board's toolbar
            //      through its Scaffold, the Drive's custom bottom bar) was padded a second time.
            // A padding for a view that is not on screen is not padding, it is a gap.
            notice?.let { text ->
                Box(modifier = Modifier.navigationBarsPadding()) {
                    NoticeBar(text = text, onDismiss = { notice = null })
                }
            }
        }
    }
}