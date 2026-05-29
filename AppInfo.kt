package com.sr.openbyd.services

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Persists the current toggled state of hardware controls so that TOGGLE_* button
 * actions can flip the real current state rather than always sending the same command.
 *
 * Stored in a dedicated SharedPreferences file so it survives app restarts.
 */
class CarStateRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("CarState", Context.MODE_PRIVATE)

    /** Whether the AC is considered ON. Defaults to false (off). */
    var isAcOn: Boolean
        get() = prefs.getBoolean(KEY_AC_POWER, false)
        set(value) = prefs.edit { putBoolean(KEY_AC_POWER, value) }

    /** Whether the driver (left-front) window is considered open. Defaults to false (closed). */
    var isDriverWindowOpen: Boolean
        get() = prefs.getBoolean(KEY_WINDOW_DRIVER, false)
        set(value) = prefs.edit { putBoolean(KEY_WINDOW_DRIVER, value) }

    /** Whether   */
    var isAuxiliaryLightOn: Boolean
        get() = prefs.getBoolean(KEY_AUXILIARY_LIGHT, false)
        set(value) = prefs.edit { putBoolean(KEY_AUXILIARY_LIGHT, value) }

    companion object {
        private const val KEY_AC_POWER      = "ac_power"
        private const val KEY_WINDOW_DRIVER = "window_driver_open"
        private const val KEY_AUXILIARY_LIGHT = "auxiliary_light"
    }
}
