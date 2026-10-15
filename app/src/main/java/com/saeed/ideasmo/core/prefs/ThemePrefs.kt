package com.saeed.ideasmo.core.prefs

import android.content.Context
import com.saeed.ideasmo.core.log.AppLog

/**
 * Persisted day / night choice for the whole app.
 * Absent value means "follow the system".
 */
object ThemePrefs {

    private const val FILE = "ideasmo_prefs"
    private const val KEY_DARK = "dark_theme"
    private const val UNSET = -1

    fun load(context: Context): Boolean? {
        val value = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getInt(KEY_DARK, UNSET)
        return when (value) {
            1 -> true
            0 -> false
            else -> null
        }
    }

    fun save(context: Context, dark: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_DARK, if (dark) 1 else 0)
            .apply()
        AppLog.i("Theme", "day/night set to " + (if (dark) "night" else "day"))
    }
}