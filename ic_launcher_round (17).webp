package com.sr.openbyd.utils

import android.util.Log
import com.sr.openbyd.data.AppPreferences
import com.sr.openbyd.data.DiLinkMode
import com.sr.openbyd.proxy.ProxyManager

enum class DiLinkGeneration {
    LEGACY,
    UI_7
}

object DiLinkHelper {
    private const val TAG = "DiLinkHelper"

    fun getDiLinkGeneration(appPrefs: AppPreferences): DiLinkGeneration {
        return when (appPrefs.diLinkMode) {
            DiLinkMode.FORCE_UI_7 -> DiLinkGeneration.UI_7
            DiLinkMode.FORCE_DILINK_5 -> DiLinkGeneration.LEGACY
            DiLinkMode.AUTO -> {
                val cached = appPrefs.cachedDiLinkGeneration
                if (cached != null) {
                    try {
                        return DiLinkGeneration.valueOf(cached)
                    } catch (e: Exception) {
                        Log.e(TAG, "Invalid cached DiLink generation: $cached", e)
                    }
                }
                refreshDiLinkGeneration(appPrefs)
            }
        }
    }

    fun refreshDiLinkGeneration(appPrefs: AppPreferences): DiLinkGeneration {
        val detected = detectDiLinkGeneration()
        appPrefs.cachedDiLinkGeneration = detected.name
        Log.d(TAG, "Refreshed cached DiLink generation to $detected")
        return detected
    }

    private fun detectDiLinkGeneration(): DiLinkGeneration {
        // Try reading via local reflection first (instantaneous on launch)
        var prop = getLocalSystemProperty("ro.vehicle.type")
        if (prop.isEmpty()) {
            // Fallback to proxy if reflection fails/is not accessible
            prop = ProxyManager.carControl?.getSystemProperty("ro.vehicle.type") ?: ""
        }
        Log.d(TAG, "detectDiLinkGeneration Auto-detect: ro.vehicle.type = $prop")
        return if ( prop.contains("7.0UI", ignoreCase = true)) {
            DiLinkGeneration.UI_7
        } else {
            DiLinkGeneration.LEGACY
        }
    }

    private fun getLocalSystemProperty(name: String): String {
        return try {
            val systemPropertiesClass = Class.forName("android.os.SystemProperties")
            val getMethod = systemPropertiesClass.getMethod("get", String::class.java)
            (getMethod.invoke(null, name) as? String) ?: ""
        } catch (e: Exception) {
            Log.d(TAG, "Failed to read system property locally via reflection: ${e.message}")
            ""
        }
    }
}
