package com.sr.openbyd.ui.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.toColorInt
import com.sr.openbyd.data.AppDisplayConfig
import com.sr.openbyd.data.AppPreferences
import com.sr.openbyd.data.ClusterLayoutMode
import com.sr.openbyd.data.ProjectionGradientMode
import com.sr.openbyd.data.ThemeMode
import com.sr.openbyd.models.ProjectionAutomationRepository
import com.sr.openbyd.proxy.ProxyManager
import com.sr.openbyd.services.ClusterProjectionService
import com.sr.openbyd.utils.DiLinkGeneration
import com.sr.openbyd.utils.DiLinkHelper
import com.sr.openbyd.utils.ShellCommandExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.lang.ref.WeakReference


/**
 * Manages the overlay and app casting on the cluster (instrument cluster) display.
 */
object ClusterOverlayManager {

    private const val FEATURE_INSTRUMENT_PRIMARY = 1086337074
    private const val FEATURE_INSTRUMENT_SECONDARY = 1276157976
    private const val INSTRUMENT_MODE_FULLSCREEN = 4
    private const val INSTRUMENT_MODE_MINI = 3

    private const val FEATURE_SETTING_UI7_NAV_SCREEN_STATE = 1276174357
    private const val FEATURE_INSTRUMENT_UI7_NAV_CONTROL_30 = 1276313665

    private const val TAG = "ClusterOverlayManager"

    private var overlayViewRef: WeakReference<View>? = null
    private var remoteDisplayId: Int = -1
    var clusterWidth: Int = 1920
    var clusterHeight: Int = 720

    private val _currentProjectedPackage = MutableStateFlow<String?>(null)
    /** The package name of the app currently projected to the cluster. */
    val currentProjectedPackage = _currentProjectedPackage.asStateFlow()

    /**
     * Checks if the app has overlay permissions and attempts to grant them via proxy if missing.
     */
    private suspend fun ensureOverlayPermission(context: Context): Boolean {
        if (Settings.canDrawOverlays(context)) return true

        Log.w(TAG, "SYSTEM_ALERT_WINDOW permission missing, attempting grant via proxy")
        val control = ProxyManager.carControl
        if (control == null) {
            Log.e(TAG, "Proxy not connected, cannot grant overlay permission")
            return false
        }

        ShellCommandExecutor.grantSystemAlertWindow(context.packageName)

        // Polling check for permission grant (up to 2 seconds)
        for (i in 1..10) {
            delay(200)
            if (Settings.canDrawOverlays(context)) {
                Log.i(TAG, "Overlay permission granted via proxy")
                return true
            }
        }

        return false
    }

    /**
     * Finds the cluster display ID by searching for "fission" or "cluster" in display names.
     * Also updates the cached resolution.
     */
    fun getClusterDisplayId(context: Context): Int {
        val prefs = AppPreferences(context)
        val overrideId = prefs.overrideClusterDisplayId
        if (overrideId != -1) {
            val dm = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            dm.getDisplay(overrideId)?.let { d ->
                val size = android.graphics.Point()
                @Suppress("DEPRECATION")
                d.getRealSize(size)
                clusterWidth = size.x
                clusterHeight = size.y
                Log.i(TAG, "Overridden cluster resolution detected: ${clusterWidth}x${clusterHeight} for display ID $overrideId")
            }
            return overrideId
        }

        val dm = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val displays = dm.displays
        var foundId = -1
        for (d in displays) {
            Log.d(TAG, "Found display: ID=${d.displayId}, Name=${d.name}")
            if (d.name.contains("fission", ignoreCase = true) || d.name.contains(
                    "cluster",
                    ignoreCase = true
                )
            ) {
                foundId = d.displayId
                break
            }
        }

        if (foundId == -1) foundId = 2 // Default fallback

        dm.getDisplay(foundId)?.let { d ->
            val size = android.graphics.Point()
            @Suppress("DEPRECATION")
            d.getRealSize(size)
            clusterWidth = size.x
            clusterHeight = size.y
            Log.i(TAG, "Cluster resolution detected: ${clusterWidth}x${clusterHeight}")
        }

        return foundId
    }

    /**
     * Removes the current overlay and virtual display.
     */
    fun hideOverlay() {
        if (remoteDisplayId != -1) {
            val result = ProxyManager.carControl?.releaseVirtualDisplay(remoteDisplayId)
            Log.d(TAG, "Release Virtual Display result: $result")
            remoteDisplayId = -1
        }

        Handler(Looper.getMainLooper()).post {
            _currentProjectedPackage.value = null
        }

        overlayViewRef?.get()?.let { view ->
            // FIX: Ensure UI operations (visibility change and view removal)
            // always happen on the Main thread to avoid CalledFromWrongThreadException.
            Handler(Looper.getMainLooper()).post {
                try {
                    view.visibility = View.GONE
                    val wm = view.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                    wm.removeView(view)
                    overlayViewRef = null
                    Log.i(TAG, "Overlay removed on main thread")

                    // Stop service after view is gone
                    ClusterProjectionService.stop(view.context)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to remove overlay on main thread", e)
                }
            }
        } ?: run {
            // If no view ref, at least ensure service is stopped
            // This handles cases where hideOverlay is called without a visible overlay
        }
    }

    /**
     * "Casts" an application to the cluster display by creating an overlay.
     * Uses a SurfaceView in an overlay to host a VirtualDisplay for the target app.
     */
    fun castAppToCluster(appContext: Context, packageName: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                if (ProxyManager.carControl == null) {
                    Toast.makeText(appContext, "Proxy not connected!", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                if (!ensureOverlayPermission(appContext)) {
                    Toast.makeText(appContext, "Overlay permission required!", Toast.LENGTH_LONG)
                        .show()
                    return@launch
                }

                val targetId = getClusterDisplayId(appContext)
                val dm = appContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
                val targetDisplay = dm.getDisplay(targetId) ?: run {
                    Toast.makeText(
                        appContext,
                        "Error: Cluster display $targetId not found!",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                val current = _currentProjectedPackage.value
                if (current != null && current != packageName) {
                    Log.i(TAG, "Casting $packageName to cluster while $current is active. Pulling $current back to main silently.")
                    pullAppBackToMain(current, silent = true)
                    delay(300)
                }

                val activeMainProjected = MainOverlayManager.currentProjectedPackage.value
                val primaryPkg = MainOverlayManager.primarySplitPackage
                val isAmongSplitScreenApps = (packageName == activeMainProjected || packageName == primaryPkg)

                if (activeMainProjected != null && isAmongSplitScreenApps) {
                    Log.i(TAG, "Casting $packageName to cluster while it is part of the main split-screen. Cancelling split-screen first.")
                    MainOverlayManager.hideOverlay(focusPrimary = true)
                    
                    val control = ProxyManager.carControl
                    if (control != null) {
                        try {
                            val taskId = control.getTaskId(packageName).lines()
                                .firstOrNull { it.startsWith("RESULT_ID:") }
                                ?.substringAfter("RESULT_ID:")
                                ?.toIntOrNull() ?: -1
                            if (taskId != -1) {
                                Log.d(TAG, "Resetting split-screen task $taskId bounds before casting to cluster")
                                control.setTaskWindowingMode(taskId, 1) // 1 = WINDOWING_MODE_FULLSCREEN
                                control.setTaskBounds(taskId, 0, 0, 0, 0)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to reset task bounds before casting", e)
                        }
                    }
                    delay(300)
                }

                hideOverlay() // Clear previous

                val displayContext = appContext.createDisplayContext(targetDisplay)
                val windowManager = displayContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

                val appPrefs = AppPreferences(appContext)
                val isMiniMode = determineMiniMode(appPrefs)
                val config = appPrefs.getAppDisplayConfig(packageName, isMiniMode, clusterWidth, clusterHeight)

                val container = FrameLayout(displayContext).apply {
                    clipChildren = true
                }

                val surfaceView = SurfaceView(displayContext).apply {
                    setupSurfaceCallback(packageName, config.renderWidth, config.renderHeight, config.densityDpi)
                }

                // Add SurfaceView to FrameLayout
                container.addView(surfaceView, createSurfaceLayoutParams(config))

                // Add gradient overlay views if enabled
                if (appPrefs.projectionGradientsEnabled) {
                    addGradientOverlays(displayContext, container, config)
                }

                // Add FrameLayout to WindowManager
                windowManager.addView(container, createOverlayLayoutParams())
                overlayViewRef = WeakReference(container)

                // Start foreground service to maintain projection priority
                ClusterProjectionService.start(appContext)

                Toast.makeText(
                    appContext,
                    "Projecting mode ${if(isMiniMode) "Mini" else "FullScreen"}\n$packageName",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {
                Log.e(TAG, "Cast failed: ${e.message}", e)
                Toast.makeText(appContext, "Cast failed", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun determineMiniMode(appPrefs: AppPreferences): Boolean {
        when (appPrefs.clusterLayoutMode) {
            ClusterLayoutMode.FORCE_MINI -> {
                Log.d(TAG, "determineMiniMode: Forced to Mini layout mode")
                return true
            }
            ClusterLayoutMode.FORCE_FULLSCREEN -> {
                Log.d(TAG, "determineMiniMode: Forced to Fullscreen layout mode")
                return false
            }
            ClusterLayoutMode.AUTO -> {
                Log.d(TAG, "determineMiniMode: Using Auto-detect mode")
            }
        }
        var isMiniMode = appPrefs.isMiniMode
        try {
            ProxyManager.carControl?.let { control ->
                when (DiLinkHelper.getDiLinkGeneration(appPrefs)) {
                    DiLinkGeneration.UI_7 -> {
                        // UI 7 compatibility mode
                        
                        // 1. Try reading instrument feature ID 1276313665 (NavControl30) first
                        val navControl = control.getInstrumentFeatureValue(FEATURE_INSTRUMENT_UI7_NAV_CONTROL_30)
                        Log.d(TAG, "UI 7 NavControl30 (1276313665) response: $navControl")
                        
                        if (navControl == 3) {
                            isMiniMode = true
                            Log.d(TAG, "UI 7 layout resolved via NavControl30: Card/Mini mode")
                        } else if (navControl == 2 || navControl == 1 || navControl == 4) {
                            isMiniMode = false
                            Log.d(TAG, "UI 7 layout resolved via NavControl30: Fullscreen/Off/TBD mode ($navControl)")
                        } else {
                            // 2. Fallback: Try reading setting feature ID 1276174357 (NavScreenState)
                            val settingStr = control.getSettingFeatureValue(FEATURE_SETTING_UI7_NAV_SCREEN_STATE) ?: ""
                            Log.d(TAG, "UI 7 NavScreenState (1276174357) feature response: $settingStr")
                            val parsedVal = settingStr.substringAfter("RESULT_VALUE:").trim().toIntOrNull()
                            
                            if (parsedVal != null && parsedVal != 0) {
                                isMiniMode = (parsedVal != 2)
                                Log.d(TAG, "UI 7 layout resolved via NavScreenState fallback: value=$parsedVal, isMiniMode=$isMiniMode")
                            } else {
                                Log.d(TAG, "UI 7 Dynamic layout cast: all queries returned 0/invalid, fallback to appPrefs: $isMiniMode")
                            }
                        }
                    }
                    DiLinkGeneration.LEGACY -> {
                        // Older DiLink platform
                        var value = control.getInstrumentFeatureValue(FEATURE_INSTRUMENT_PRIMARY)
                        if (value == 0) {
                            value = control.getInstrumentFeatureValue(FEATURE_INSTRUMENT_SECONDARY)
                        }

                        if (value != 0) {
                            isMiniMode = (value == INSTRUMENT_MODE_MINI)
                            Log.d(TAG, "Dynamic layout cast: Using SDK instrument mode value=$value, isMiniMode=$isMiniMode")
                        } else {
                            Log.d(TAG, "Dynamic layout cast: SDK value is 0 (uncached), fallback to appPrefs: $isMiniMode")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query instrument feature value", e)
        }
        return isMiniMode
    }

    /**
     * Creates the layout parameters for the WindowManager overlay.
     */
    private fun createOverlayLayoutParams(): WindowManager.LayoutParams {
        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
    }

    /**
     * Generates sizing and margins for the projection surface based on the target mode.
     */
    private fun createSurfaceLayoutParams(config: AppDisplayConfig): FrameLayout.LayoutParams {
        return FrameLayout.LayoutParams(
            config.renderWidth,
            config.renderHeight,
            Gravity.START or Gravity.TOP
        ).apply {
            leftMargin = config.xOffset
            topMargin = config.yOffset
        }
    }

    /**
     * Adds smooth dark-to-transparent edge gradients to the top and bottom of the projection
     * surface to improve readability of the instrument cluster's physical white lettering.
     */
    private fun addGradientOverlays(
        context: Context,
        container: FrameLayout,
        config: AppDisplayConfig
    ) {
        val gradientHeightPx = (90 * context.resources.displayMetrics.density).toInt()

        val appPrefs = AppPreferences(context)
        val useLightGradients = when (appPrefs.projectionGradientMode) {
            ProjectionGradientMode.LIGHT -> true
            ProjectionGradientMode.DARK -> false
            ProjectionGradientMode.AUTO -> {
                // Determine based on theme mode: Light or Dark
                when (appPrefs.themeMode) {
                    ThemeMode.LIGHT -> true
                    ThemeMode.DARK -> false
                    ThemeMode.DEFAULT -> {
                        // Fire a diagnostic probe via the proxy (async, logs everything)
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                withTimeoutOrNull(3000) {
                                    com.sr.openbyd.ui.theme.probeThemeViaProxy(context)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Theme probe failed", e)
                            }
                        }
                        // Use standard detection (includes proxy cache as 4th check)
                        val isDark = com.sr.openbyd.ui.theme.isSystemDarkTheme(context)
                        Log.d(TAG, "Gradient AUTO+DEFAULT: isSystemDarkTheme=$isDark, cachedProxyDarkTheme=${com.sr.openbyd.ui.theme.cachedProxyDarkTheme}")
                        !isDark
                    }
                }
            }
        }

        // Color definitions based on intensity:
        // Dark theme: pure black (0x000000) to match the dark cluster background seamlessly
        // Light theme: instrument cluster light grey-blue (0xD5DCEE) to blend perfectly
        val intensity = appPrefs.projectionGradientIntensity.coerceIn(0, 100)
        val alpha = (intensity * 255 / 100).coerceIn(0, 255)
        val rgb = if (useLightGradients) 0xD5DCEE else 0x000000
        val startColor = (alpha shl 24) or rgb
        val endColor = rgb // alpha = 0

        // Top gradient: startColor → endColor (top-to-bottom direction)
        val topGradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(startColor, endColor)
        )
        val topView = android.view.View(context).apply {
            background = topGradientDrawable
        }
        container.addView(topView, FrameLayout.LayoutParams(
            config.renderWidth,
            gradientHeightPx,
            Gravity.START or Gravity.TOP
        ).apply {
            leftMargin = config.xOffset
            topMargin = config.yOffset
        })

        // Bottom gradient: startColor → endColor (bottom-to-top direction)
        val bottomGradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.BOTTOM_TOP,
            intArrayOf(startColor, endColor)
        )
        val bottomView = View(context).apply {
            background = bottomGradientDrawable
        }
        container.addView(bottomView, FrameLayout.LayoutParams(
            config.renderWidth,
            gradientHeightPx,
            Gravity.START or Gravity.TOP
        ).apply {
            leftMargin = config.xOffset
            topMargin = config.yOffset + config.renderHeight - gradientHeightPx
        })

        // Optional Left gradient: startColor → endColor (left-to-right direction)
        if (appPrefs.projectionLeftGradientEnabled) {
            val leftGradientWidthPx = (120 * context.resources.displayMetrics.density).toInt()
            val leftGradientDrawable = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(startColor, endColor)
            )
            val leftView = View(context).apply {
                background = leftGradientDrawable
            }
            container.addView(leftView, FrameLayout.LayoutParams(
                leftGradientWidthPx,
                config.renderHeight,
                Gravity.START or Gravity.TOP
            ).apply {
                leftMargin = config.xOffset
                topMargin = config.yOffset
            })
        }

        Log.d(TAG, "Projection gradients applied: intensity=$intensity%, leftGradientEnabled=${appPrefs.projectionLeftGradientEnabled}, ${if (useLightGradients) "light" else "dark"} theme")
    }

    /**
     * Extension function to keep the surface callback logic clean and isolated.
     */
    private fun SurfaceView.setupSurfaceCallback(packageName: String, vdWidth: Int, vdHeight: Int, densityDpi: Int) {
        holder.setFixedSize(vdWidth, vdHeight)
        holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                Log.d(TAG, "Surface created, launching VirtualDisplay")
                launchOnVirtualDisplay(this@setupSurfaceCallback.context, packageName, holder.surface, vdWidth, vdHeight, densityDpi)
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                // No-op (Handle dynamic resizing here in the future if needed)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                Log.d(TAG, "Surface destroyed")
                releaseActiveVirtualDisplay()
            }
        })
    }

    private fun releaseActiveVirtualDisplay() {
        if (remoteDisplayId != -1) {
            val result = ProxyManager.carControl?.releaseVirtualDisplay(remoteDisplayId)
            Log.d(TAG, "Release Virtual Display result: $result")
            remoteDisplayId = -1
        }
        Handler(Looper.getMainLooper()).post {
            _currentProjectedPackage.value = null
        }
    }

    private fun launchOnVirtualDisplay(
        context: Context,
        packageName: String,
        surface: Surface,
        width: Int,
        height: Int,
        densityDpi: Int
    ) {
        try {
            val control = ProxyManager.carControl ?: run {
                Log.e(TAG, "Proxy not connected, cannot create VirtualDisplay")
                return
            }

            // Match flags from decompiled app (320 = DESTROY_CONTENT_ON_REMOVAL | SUPPORTS_TOUCH)
            // Re-adding VIRTUAL_DISPLAY_FLAG_PRESENTATION (2)
            val flags = 256 or 64 or 2

            // Dynamically resolve target display density
            val targetId = getClusterDisplayId(context)

            Log.i(TAG, "Creating cluster virtual display. ID=$targetId resolution=${width}x${height} densityDpi=$densityDpi")

            // Create a virtual display targeting the surface of our overlay on the cluster.
            val resp = control.createVirtualDisplay(
                "OpenBYD_Cluster_VD",
                width, height, densityDpi,
                surface,
                flags
            )

            Log.d(TAG, "createVirtualDisplay response:\n$resp")
            remoteDisplayId = resp.lines()
                .firstOrNull { it.startsWith("RESULT_ID:") }
                ?.substringAfter("RESULT_ID:")
                ?.toIntOrNull() ?: -1

            if (remoteDisplayId == -1 || remoteDisplayId == 0) {
                Log.e(TAG, "Failed to create remote Virtual Display via proxy (returned ID $remoteDisplayId)")
                return
            }

            Log.i(TAG, "Virtual Display created with ID: $remoteDisplayId")

            // NEW STRATEGY: Use the proxy's launchAndForce to handle the entire lifecycle.
            // This is more robust as it runs entirely in the privileged proxy process.
            Log.d(TAG, "Starting launchAndForce via proxy for $packageName")

            Handler(Looper.getMainLooper()).post {
                _currentProjectedPackage.value = packageName
            }

            val forceResult = control.launchAndForce(packageName, remoteDisplayId, width, height)
            Log.d(TAG, "launchAndForce output:\n$forceResult")

            // Run app-specific automation hooks
            performAppSpecificAutomations(context, packageName, remoteDisplayId, width, height)

            // If the app successfully moved, we are done. 
            // In the UI, the user can click 'Cast' again to 'Pull Back' if they want to interact.

        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch on virtual display: ${e.message}", e)
        }
    }

    /**
     * Executes UI interactions (clicks, etc.) for specific apps after they are projected.
     */
    private fun performAppSpecificAutomations(context: Context, pkg: String, displayId: Int, w: Int, h: Int) {
        val control = ProxyManager.carControl ?: return
        val repository = ProjectionAutomationRepository(context)
        val actions = repository.getForApp(pkg).sortedBy { it.index }
        
        if (actions.isEmpty()) {
            Log.d(TAG, "No automations configured for $pkg")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            Log.d(TAG, "Running ${actions.size} automation tasks for $pkg sequentially...")
            actions.forEach { action ->
                if (action.delayMs > 0) {
                    delay(action.delayMs)
                }
                val clickX = (w * (action.xPercent / 100f)).toInt()
                val clickY = (h * (action.yPercent / 100f)).toInt()
                val resp = ShellCommandExecutor.simulateTapOnDisplay(displayId, clickX, clickY)
                Log.d(TAG, "Automation action #${action.index} ($clickX, $clickY) -> $resp")
            }
        }
    }

    /**
     * Helper to pull the currently projected app back to the main display.
     */
    fun pullAppBackToMain(packageName: String, silent: Boolean = false) {
        val control = ProxyManager.carControl ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val taskId = control.getTaskId(packageName).lines()
                    .firstOrNull { it.startsWith("RESULT_ID:") }
                    ?.substringAfter("RESULT_ID:")
                    ?.toIntOrNull() ?: -1

                if (taskId != -1) {
                    control.moveTaskToDisplay(taskId, 0)
                    control.setTaskBounds(taskId, 0, 0, 0, 0)
                    if (!silent) {
                        control.setFocusedTask(taskId)
                    }
                    Handler(Looper.getMainLooper()).post {
                        _currentProjectedPackage.value = null
                    }
                    hideOverlay()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Pull back failed", e)
            }
        }
    }

    /**
     * Refreshes the active projection silently by pulling it back and re-casting it.
     */
    fun refreshClusterProjection(appContext: Context, packageName: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                if (ProxyManager.carControl == null) {
                    Toast.makeText(appContext, "Proxy not connected!", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                Log.i(TAG, "Refreshing cluster projection for $packageName silently")
                pullAppBackToMain(packageName, silent = true)
                delay(500)
                castAppToCluster(appContext, packageName)
            } catch (e: Exception) {
                Log.e(TAG, "Refresh cluster projection failed: ${e.message}", e)
                Toast.makeText(appContext, "Refresh failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Simple test overlay remains for basic verification.
     */
    fun testOverlayLaunch(context: Context, displayId: Int = getClusterDisplayId(context)) {
        try {
            val dm = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            val targetDisplay = dm.getDisplay(displayId) ?: return
            
            // If overlay is already showing, remove it first
            hideOverlay()

            val displayContext = context.createDisplayContext(targetDisplay)
            val wm = displayContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

            val view = TextView(displayContext).apply {
                text = "Display ID: $displayId Active"
                textSize = 28f
                setTextColor(Color.WHITE)
                setBackgroundColor("#CC006600".toColorInt()) // Semi-transparent dark green
                gravity = Gravity.CENTER
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 2038 else 2003,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )

            wm.addView(view, params)
            overlayViewRef = WeakReference(view)
            Handler(Looper.getMainLooper()).postDelayed({ hideOverlay() }, 5000)

        } catch (e: Exception) {
            Log.e(TAG, "Overlay test failed", e)
        }
    }
}
