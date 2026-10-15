package com.saeed.ideasmo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.saeed.ideasmo.core.log.AppLog
import com.saeed.ideasmo.core.log.LogViewerScreen
import com.saeed.ideasmo.ui.home.HomeScreen
import com.saeed.ideasmo.ui.home.RecentItem
import com.saeed.ideasmo.ui.theme.IdeasmoTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Log first, so everything after this line is recoverable.
        AppLog.init(applicationContext)
        AppLog.i("Lifecycle", "onCreate")
        enableEdgeToEdge()
        setContent {
            IdeasmoTheme {
                IdeasmoApp()
            }
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

@Composable
private fun IdeasmoApp() {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showLogs by remember { mutableStateOf(false) }

    // No project store exists yet, so the recent list is honestly empty and the
    // home screen shows its empty state instead of fake rows.
    val recent = remember { emptyList<RecentItem>() }

    if (showLogs) {
        BackHandler {
            showLogs = false
            AppLog.i("UI", "log viewer closed (back)")
        }
        LogViewerScreen(
            onClose = {
                showLogs = false
                AppLog.i("UI", "log viewer closed")
            },
        )
    } else {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            HomeScreen(
                recent = recent,
                snackbarHostState = snackbarHostState,
                onOpenLogs = {
                    showLogs = true
                    AppLog.i("UI", "log viewer opened (long-press on title)")
                },
                onEntry = { entry ->
                    val label = context.getString(entry.labelRes)
                    AppLog.i("UI", "home entry tapped: " + entry.name + " / " + label)
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.not_built_yet, label),
                        )
                    }
                },
            )
        }
    }
}