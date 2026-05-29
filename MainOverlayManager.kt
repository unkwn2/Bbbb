package com.sr.openbyd.models

import android.content.Context
import android.content.SharedPreferences

/**
 * Represents a single automated action for a projected app.
 *
 * @param index    Position in the sequence (0-based). Used as the storage key.
 * @param delayMs  Delay in milliseconds before this action is executed.
 * @param xPercent X coordinate in percentage (0..100) within the projected window.
 * @param yPercent Y coordinate in percentage (0..100) within the projected window.
 */
data class ProjectionAutomation(
    val index: Int,
    val delayMs: Long = 2000L,
    val xPercent: Float,
    val yPercent: Float
)

/**
 * Persists the projection app-specific automation sequences in SharedPreferences.
 */
class ProjectionAutomationRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ProjectionAutomations", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_COUNT   = "_count"
        private const val PREFIX_DELAY = "_delay_"
        private const val PREFIX_X     = "_x_"
        private const val PREFIX_Y     = "_y_"
    }

    /**
     * Returns all saved automations for a given app package, in index order.
     * If no configuration exists, pre-populates default settings for known apps.
     */
    fun getForApp(packageName: String): List<ProjectionAutomation> {
        val countKey = "${packageName}${KEY_COUNT}"
        
        // Pre-populate default values on first access
        if (!prefs.contains(countKey)) {
            if (packageName == "com.waze") {
                val defaults = listOf(ProjectionAutomation(0, 2000L, 52.0f, 10.0f))
                saveForApp(packageName, defaults)
                return defaults
            } else if (packageName == "app.revanced.android.apps.youtube.music" || 
                       packageName == "com.google.android.apps.youtube.music") {
                val defaults = listOf(ProjectionAutomation(0, 2000L, 50.0f, 85.0f))
                saveForApp(packageName, defaults)
                return defaults
            }
        }

        val count = prefs.getInt(countKey, 0)
        return (0 until count).map { i -> read(packageName, i) }
    }

    /**
     * Overwrites all automations for a given app package. Indices are reassigned 0..N-1.
     */
    fun saveForApp(packageName: String, actions: List<ProjectionAutomation>) {
        val countKey = "${packageName}${KEY_COUNT}"
        val oldCount = prefs.getInt(countKey, 0)
        
        prefs.edit().apply {
            // Clear old keys beyond the new count
            for (i in actions.size until oldCount) {
                remove("${packageName}${PREFIX_DELAY}$i")
                remove("${packageName}${PREFIX_X}$i")
                remove("${packageName}${PREFIX_Y}$i")
            }
            putInt(countKey, actions.size)
            actions.forEachIndexed { i, action ->
                putLong("${packageName}${PREFIX_DELAY}$i", action.delayMs)
                putFloat("${packageName}${PREFIX_X}$i", action.xPercent)
                putFloat("${packageName}${PREFIX_Y}$i", action.yPercent)
            }
        }.apply()
    }

    private fun read(packageName: String, i: Int): ProjectionAutomation {
        return ProjectionAutomation(
            index = i,
            delayMs = prefs.getLong("${packageName}${PREFIX_DELAY}$i", 2000L),
            xPercent = prefs.getFloat("${packageName}${PREFIX_X}$i", 0f),
            yPercent = prefs.getFloat("${packageName}${PREFIX_Y}$i", 0f)
        )
    }
}
