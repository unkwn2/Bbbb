package com.sr.openbyd.models

import android.content.Context
import android.content.SharedPreferences

data class ButtonMapping(
    val keyCode: Int,
    val actionType: ActionType,
    val targetPackageName: String? = null,
    val secondPackageName: String? = null,
    val customDescription: String? = null, // For user-added buttons
    val isLeft: Boolean = true,
    val projectFirstApp: Boolean = true,
    val splitRatio: String = "1/2"
)

class ButtonMappingRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ButtonMappings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ALL_KEYCODES = "all_configured_keycodes"
        private const val PREFIX_TYPE = "mapping_"
        private const val PREFIX_PKG = "package_"
        private const val PREFIX_PKG2 = "package2_"
        private const val PREFIX_DESC = "desc_"
        private const val PREFIX_IS_LEFT = "is_left_"
        private const val PREFIX_PROJECT_FIRST = "project_first_"
        private const val PREFIX_RATIO = "ratio_"
    }

    /** Returns all keycodes that have been configured or added by the user. */
    fun getAllConfiguredKeyCodes(): Set<Int> {
        return prefs.getStringSet(KEY_ALL_KEYCODES, emptySet())
            ?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
    }

    fun getMapping(keyCode: Int): ButtonMapping {
        val typeStr = prefs.getString("${PREFIX_TYPE}${keyCode}", ActionType.NONE.name)
        val targetPackage = prefs.getString("${PREFIX_PKG}${keyCode}", null)
        val secondPackage = prefs.getString("${PREFIX_PKG2}${keyCode}", null)
        val customDesc = prefs.getString("${PREFIX_DESC}${keyCode}", null)
        val isLeft = prefs.getBoolean("${PREFIX_IS_LEFT}${keyCode}", true)
        val projectFirst = prefs.getBoolean("${PREFIX_PROJECT_FIRST}${keyCode}", false)
        val ratio = prefs.getString("${PREFIX_RATIO}${keyCode}", "1/2") ?: "1/2"

        val type = try {
            ActionType.valueOf(typeStr ?: ActionType.NONE.name)
        } catch (e: Exception) {
            ActionType.NONE
        }

        return ButtonMapping(keyCode, type, targetPackage, secondPackage, customDesc, isLeft, projectFirst, ratio)
    }

    fun saveMapping(mapping: ButtonMapping) {
        val keyCodes = getAllConfiguredKeyCodes().toMutableSet()
        keyCodes.add(mapping.keyCode)

        prefs.edit().apply {
            putStringSet(KEY_ALL_KEYCODES, keyCodes.map { it.toString() }.toSet())
            putString("${PREFIX_TYPE}${mapping.keyCode}", mapping.actionType.name)
            putString("${PREFIX_PKG}${mapping.keyCode}", mapping.targetPackageName)
            putString("${PREFIX_PKG2}${mapping.keyCode}", mapping.secondPackageName)
            putString("${PREFIX_DESC}${mapping.keyCode}", mapping.customDescription)
            putBoolean("${PREFIX_IS_LEFT}${mapping.keyCode}", mapping.isLeft)
            putBoolean("${PREFIX_PROJECT_FIRST}${mapping.keyCode}", mapping.projectFirstApp)
            putString("${PREFIX_RATIO}${mapping.keyCode}", mapping.splitRatio)
        }.apply()
    }

    fun deleteMapping(keyCode: Int) {
        val keyCodes = getAllConfiguredKeyCodes().toMutableSet()
        keyCodes.remove(keyCode)

        prefs.edit().apply {
            putStringSet(KEY_ALL_KEYCODES, keyCodes.map { it.toString() }.toSet())
            remove("${PREFIX_TYPE}${keyCode}")
            remove("${PREFIX_PKG}${keyCode}")
            remove("${PREFIX_PKG2}${keyCode}")
            remove("${PREFIX_DESC}${keyCode}")
            remove("${PREFIX_IS_LEFT}${keyCode}")
            remove("${PREFIX_PROJECT_FIRST}${keyCode}")
            remove("${PREFIX_RATIO}${keyCode}")
        }.apply()
    }
}
