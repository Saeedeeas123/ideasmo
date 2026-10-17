package com.saeed.ideasmo.core.prefs

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.saeed.ideasmo.core.log.AppLog

/**
 * Project-level board settings, persisted per app (there is one board per module and the look is a
 * workspace choice, not a per-node one).
 *
 * Keys are plain strings on purpose: this file belongs to `core`, so it must not know about the
 * `ui.board` enums that own the labels and the drawing. The UI maps key -> enum, and an unknown
 * key from an older build falls back to the default instead of crashing.
 */
object BoardPrefs {

    private const val FILE = "ideasmo_board"
    private const val KEY_PATTERN = "board_pattern"
    private const val KEY_PALETTE = "board_palette"
    private const val KEY_SPACING = "layout_spacing"
    private const val KEY_LABELS = "show_link_labels"
    private const val KEY_MINIMAP = "show_minimap"
    private const val KEY_ORIGIN = "show_origin"

    const val DEFAULT_PATTERN = "grid"
    const val DEFAULT_PALETTE = "default"
    const val DEFAULT_SPACING = "normal"

    private val patternState = mutableStateOf(DEFAULT_PATTERN)
    private val paletteState = mutableStateOf(DEFAULT_PALETTE)
    private val spacingState = mutableStateOf(DEFAULT_SPACING)
    private val labelsState = mutableStateOf(true)
    private val minimapState = mutableStateOf(true)
    private val originState = mutableStateOf(true)

    private var loaded = false

    val patternKey: String get() = patternState.value
    val paletteKey: String get() = paletteState.value
    val spacingKey: String get() = spacingState.value
    val showLinkLabels: Boolean get() = labelsState.value
    val showMinimap: Boolean get() = minimapState.value
    val showOrigin: Boolean get() = originState.value

    fun init(context: Context) {
        if (loaded) return
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        patternState.value = prefs.getString(KEY_PATTERN, DEFAULT_PATTERN) ?: DEFAULT_PATTERN
        paletteState.value = prefs.getString(KEY_PALETTE, DEFAULT_PALETTE) ?: DEFAULT_PALETTE
        spacingState.value = prefs.getString(KEY_SPACING, DEFAULT_SPACING) ?: DEFAULT_SPACING
        labelsState.value = prefs.getBoolean(KEY_LABELS, true)
        minimapState.value = prefs.getBoolean(KEY_MINIMAP, true)
        originState.value = prefs.getBoolean(KEY_ORIGIN, true)
        loaded = true
        AppLog.i(
            "BoardPrefs",
            "board settings loaded: pattern=" + patternState.value + " palette=" + paletteState.value +
                " spacing=" + spacingState.value +
                " labels=" + labelsState.value + " minimap=" + minimapState.value +
                " origin=" + originState.value,
        )
    }

    fun setPattern(context: Context, key: String) {
        patternState.value = key
        write(context, KEY_PATTERN, key)
        AppLog.i("BoardPrefs", "board pattern -> $key")
    }

    fun setPalette(context: Context, key: String) {
        paletteState.value = key
        write(context, KEY_PALETTE, key)
        AppLog.i("BoardPrefs", "board theme -> $key")
    }

    fun setSpacing(context: Context, key: String) {
        spacingState.value = key
        write(context, KEY_SPACING, key)
        AppLog.i("BoardPrefs", "structure spacing -> $key")
    }

    fun setShowLinkLabels(context: Context, on: Boolean) {
        labelsState.value = on
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putBoolean(KEY_LABELS, on).apply()
        AppLog.i("BoardPrefs", "link labels on board -> $on")
    }

    fun setShowMinimap(context: Context, on: Boolean) {
        minimapState.value = on
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putBoolean(KEY_MINIMAP, on).apply()
        AppLog.i("BoardPrefs", "minimap -> $on")
    }

    fun setShowOrigin(context: Context, on: Boolean) {
        originState.value = on
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putBoolean(KEY_ORIGIN, on).apply()
        AppLog.i("BoardPrefs", "origin marker -> $on")
    }

    private fun write(context: Context, key: String, value: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(key, value).apply()
    }
}