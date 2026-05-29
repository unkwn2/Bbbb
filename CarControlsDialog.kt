package com.sr.openbyd.models

import android.content.Context
import android.content.SharedPreferences

/**
 * Represents a single action in the startup sequence.
 *
 * @param index             Position in the sequence (0-based). Used as the storage key.
 * @param actionType        The action to execute.
 * @param delayMs           Delay in milliseconds before this action is executed.
 *                          Range: 0–15 000 ms (0–15 s) in steps of 500 ms.
 * @param targetPackageName Primary package for LAUNCH_APP / LAUNCH_APP_ON_SPLIT_SCREEN.
 * @param secondPackageName Secondary package for LAUNCH_APP_ON_SPLIT_SCREEN.
 */
data class StartupAction(
    val index: Int,
    val actionType: ActionType,
    val delayMs: Long = 0L,
    val targetPackageName: String? = null,
    val secondPackageName: String? = null,
    val isLeft: Boolean = true,
    val projectFirstApp: Boolean = true,
    val splitRatio: String = "1/2"
)

/**
 * Persists the startup action sequence in a dedicated SharedPreferences file.
 */
class StartupActionRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("StartupActions", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_COUNT             = "sequence_count"
        private const val PREFIX_TYPE           = "action_type_"
        private const val PREFIX_DELAY          = "action_delay_ms_"
        private const val PREFIX_PKG            = "action_pkg_"
        private const val PREFIX_PKG2           = "action_pkg2_"
        private const val PREFIX_IS_LEFT        = "action_is_left_"
        private const val PREFIX_PROJECT_FIRST  = "action_project_first_"
        private const val PREFIX_RATIO          = "action_ratio_"
    }

    /** Returns all saved startup actions in order. */
    fun getAll(): List<StartupAction> {
        val count = prefs.getInt(KEY_COUNT, 0)
        return (0 until count).map { i -> read(i) }
    }

    /** Overwrites the entire sequence with [actions]. Indices are reassigned 0..N-1. */
    fun saveAll(actions: List<StartupAction>) {
        prefs.edit().apply {
            // Clear old keys beyond the new count
            val oldCount = prefs.getInt(KEY_COUNT, 0)
            for (i in actions.size until oldCount) {
                remove("${PREFIX_TYPE}$i")
                remove("${PREFIX_DELAY}$i")
                remove("${PREFIX_PKG}$i")
                remove("${PREFIX_PKG2}$i")
                remove("${PREFIX_IS_LEFT}$i")
                remove("${PREFIX_PROJECT_FIRST}$i")
                remove("${PREFIX_RATIO}$i")
            }
            putInt(KEY_COUNT, actions.size)
            actions.forEachIndexed { i, action ->
                putString("${PREFIX_TYPE}$i", action.actionType.name)
                putLong("${PREFIX_DELAY}$i", action.delayMs)
                putString("${PREFIX_PKG}$i", action.targetPackageName)
                putString("${PREFIX_PKG2}$i", action.secondPackageName)
                putBoolean("${PREFIX_IS_LEFT}$i", action.isLeft)
                putBoolean("${PREFIX_PROJECT_FIRST}$i", action.projectFirstApp)
                putString("${PREFIX_RATIO}$i", action.splitRatio)
            }
        }.apply()
    }

    /** Appends a new action at the end of the sequence. Returns its assigned index. */
    fun append(action: StartupAction): Int {
        val all = getAll().toMutableList()
        val newIndex = all.size
        all.add(action.copy(index = newIndex))
        saveAll(all)
        return newIndex
    }

    /** Updates a single action (identified by [action.index]) in-place. */
    fun update(action: StartupAction) {
        val all = getAll().toMutableList()
        if (action.index in all.indices) {
            all[action.index] = action
            saveAll(all)
        }
    }

    /** Removes the action at [index] and re-sequences the remaining actions. */
    fun delete(index: Int) {
        val all = getAll().toMutableList()
        if (index in all.indices) {
            all.removeAt(index)
            // Re-assign indices after removal
            val reindexed = all.mapIndexed { i, a -> a.copy(index = i) }
            saveAll(reindexed)
        }
    }

    private fun read(i: Int): StartupAction {
        val typeStr = prefs.getString("${PREFIX_TYPE}$i", ActionType.NONE.name)
        val type = try {
            ActionType.valueOf(typeStr ?: ActionType.NONE.name)
        } catch (e: Exception) {
            ActionType.NONE
        }
        return StartupAction(
            index               = i,
            actionType          = type,
            delayMs             = prefs.getLong("${PREFIX_DELAY}$i", 0L),
            targetPackageName   = prefs.getString("${PREFIX_PKG}$i", null),
            secondPackageName   = prefs.getString("${PREFIX_PKG2}$i", null),
            isLeft              = prefs.getBoolean("${PREFIX_IS_LEFT}$i", true),
            projectFirstApp     = prefs.getBoolean("${PREFIX_PROJECT_FIRST}$i", false),
            splitRatio          = prefs.getString("${PREFIX_RATIO}$i", "1/2") ?: "1/2"
        )
    }
}
