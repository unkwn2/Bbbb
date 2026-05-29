package com.sr.openbyd.utils

import android.content.Context
import android.hardware.display.DisplayManager
import android.util.DisplayMetrics
import android.util.Log

data class ScreenParams(
    val width: Int,
    val height: Int,
    val densityDpi: Int
) {
    companion object {
        private const val TAG = "ScreenParams"
        private const val DEFAULT_WIDTH = 1920
        private const val DEFAULT_HEIGHT = 1080
        private const val DEFAULT_DPI = 240

        private const val DEFAULT_CLUSTER_WIDTH = 1920
        private const val DEFAULT_CLUSTER_HEIGHT = 720
        private const val DEFAULT_CLUSTER_DPI = 320

        /**
         * Dynamically fetches the physical/real metrics of the default display.
         * Uses the maximum dimension as logical width (landscape) to ensure consistency.
         */
        @JvmStatic
        fun getMetrics(context: Context): ScreenParams {
            try {
                val dm = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
                val overrideId = com.sr.openbyd.data.AppPreferences(context).overrideMainDisplayId
                val mainDisplayId = if (overrideId != -1) overrideId else android.view.Display.DEFAULT_DISPLAY
                val defaultDisplay = dm.getDisplay(mainDisplayId)
                if (defaultDisplay != null) {
                    val metrics = DisplayMetrics()
                    @Suppress("DEPRECATION")
                    defaultDisplay.getRealMetrics(metrics)
                    val realWidth = maxOf(metrics.widthPixels, metrics.heightPixels)
                    val realHeight = minOf(metrics.widthPixels, metrics.heightPixels)
                    Log.d(TAG, "Fetched main display metrics dynamically for ID $mainDisplayId: ${realWidth}x${realHeight}, dpi: ${metrics.densityDpi}")
                    return ScreenParams(realWidth, realHeight, metrics.densityDpi)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch default display metrics dynamically, using default values", e)
            }
            return ScreenParams(DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_DPI)
        }

        /**
         * Dynamically fetches the physical/real metrics of the cluster display.
         * Uses the maximum dimension as logical width (landscape) to ensure consistency.
         */
        @JvmStatic
        fun getClusterMetrics(context: Context): ScreenParams {
            try {
                val dm = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
                val prefs = com.sr.openbyd.data.AppPreferences(context)
                val overrideId = prefs.overrideClusterDisplayId
                var targetId = -1
                if (overrideId != -1) {
                    targetId = overrideId
                } else {
                    val displays = dm.displays
                    for (d in displays) {
                        if (d.name.contains("fission", ignoreCase = true) || d.name.contains("cluster", ignoreCase = true)) {
                            targetId = d.displayId
                            break
                        }
                    }
                    if (targetId == -1) {
                        targetId = 2 // Default cluster fallback ID
                    }
                }
                val targetDisplay = dm.getDisplay(targetId)
                if (targetDisplay != null) {
                    val metrics = DisplayMetrics()
                    @Suppress("DEPRECATION")
                    targetDisplay.getRealMetrics(metrics)
                    val realWidth = maxOf(metrics.widthPixels, metrics.heightPixels)
                    val realHeight = minOf(metrics.widthPixels, metrics.heightPixels)
                    Log.d(TAG, "Fetched cluster display metrics dynamically for ID $targetId: ${realWidth}x${realHeight}, dpi: ${metrics.densityDpi}")
                    return ScreenParams(realWidth, realHeight, metrics.densityDpi)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch cluster display metrics dynamically, using default values", e)
            }
            return ScreenParams(DEFAULT_CLUSTER_WIDTH, DEFAULT_CLUSTER_HEIGHT, DEFAULT_CLUSTER_DPI)
        }
    }
}
