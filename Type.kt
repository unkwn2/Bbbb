package com.sr.openbyd.services

import android.content.Context
import android.content.Intent
import android.util.Log
import com.sr.openbyd.WindowState
import com.sr.openbyd.WindowType
import com.sr.openbyd.ipc.ICarControl
import com.sr.openbyd.models.ActionType
import com.sr.openbyd.models.StartupActionRepository
import com.sr.openbyd.proxy.ProxyManager
import com.sr.openbyd.data.AppPreferences
import com.sr.openbyd.data.AppDisplayConfig
import com.sr.openbyd.data.SplitScreenMode
import com.sr.openbyd.ui.overlay.ClusterOverlayManager
import com.sr.openbyd.ui.overlay.MainOverlayManager
import com.sr.openbyd.utils.AppConstants.PACKAGE_NAME
import com.sr.openbyd.utils.AppConstants.STATUS_BAR_HEIGHT
import com.sr.openbyd.utils.AppConstants.BOTTOM_NAV_BAR_HEIGHT
import com.sr.openbyd.utils.AppConstants.TOTAL_BAR_HEIGHTS
import com.sr.openbyd.utils.ScreenParams
import com.sr.openbyd.utils.DiLinkHelper
import com.sr.openbyd.utils.DiLinkGeneration
import com.sr.openbyd.utils.ShellCommandExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Shared action executor used by both [SteeringWheelAccessibilityService] (button mappings)
 * and [ProxyManager] (startup sequence).
 *
 * All suspend functions are safe to call from any coroutine context — IO-bound work
 * is dispatched to [Dispatchers.IO] internally.
 */
object ActionExecutor {

    private const val TAG = "ActionExecutor"

    /**
     * Executes a single action.
     *
     * @param actionType        The action to perform.
     * @param targetPackageName Primary package (for LAUNCH_APP / LAUNCH_APP_ON_SPLIT_SCREEN).
     * @param secondPackageName Secondary package (for LAUNCH_APP_ON_SPLIT_SCREEN).
     * @param context           Application context used for launching activities.
     * @param carState          Mutable toggle-state holder (AC, window, etc.).
     */
    suspend fun execute(
        actionType: ActionType,
        targetPackageName: String?,
        secondPackageName: String?,
        context: Context,
        carState: CarStateRepository,
        isLeft: Boolean = true,
        projectFirstApp: Boolean = false,
        splitRatio: String = "1/2"
    ) {
        val control = ProxyManager.carControl

        when (actionType) {

            ActionType.LAUNCH_APP -> {
                val pkg = targetPackageName ?: run {
                    Log.e(TAG, "LAUNCH_APP: no package name set")
                    return
                }
                launchApp(pkg, context, control)
            }

            ActionType.LAUNCH_APP_ON_CLUSTER -> {
                val pkg = targetPackageName ?: run {
                    Log.e(TAG, "LAUNCH_APP_ON_CLUSTER: no package name set")
                    return
                }
                val current = ClusterOverlayManager.currentProjectedPackage.value
                if (current == pkg) {
                    Log.i(TAG, "LAUNCH_APP_ON_CLUSTER: $pkg is currently projected on cluster. Toggling OFF.")
                    ClusterOverlayManager.pullAppBackToMain(pkg)
                } else {
                    Log.i(TAG, "LAUNCH_APP_ON_CLUSTER: casting $pkg to cluster.")
                    ClusterOverlayManager.castAppToCluster(context, pkg)
                }
            }

            ActionType.LAUNCH_APP_ON_SPLIT_SCREEN -> {
                if (targetPackageName != null && secondPackageName != null) {
                    val appPrefs = AppPreferences(context)
                    when (appPrefs.splitScreenMode) {
                        SplitScreenMode.PROJECTED -> launchAppsInSplitScreenProjected(targetPackageName, secondPackageName, context, control, projectFirstApp, splitRatio)
                        SplitScreenMode.COMPAT_TAP -> launchAppsInSplitScreenCompat(targetPackageName, secondPackageName, context, control)
                        SplitScreenMode.NATIVE -> launchAppsInSplitScreenNativeLegacy(targetPackageName, secondPackageName, context, control)
                        SplitScreenMode.NATIVE_UI7_DILINK_6 -> launchAppsInSplitScreenNativeUi7(targetPackageName, secondPackageName, context, splitRatio)
                    }
                } else {
                    Log.e(TAG, "LAUNCH_APP_ON_SPLIT_SCREEN: missing packages: $targetPackageName, $secondPackageName")
                }
            }

            ActionType.TOGGLE_AC -> {
                if (control != null) {
                    withContext(Dispatchers.IO) {
                        val turnOn = !carState.isAcOn
                        runCatching { control.setAcPower(turnOn) }
                            .onSuccess {
                                Log.d(TAG, "TOGGLE_AC result: $it (new state=$turnOn)")
                                carState.isAcOn = turnOn
                            }
                            .onFailure { Log.e(TAG, "TOGGLE_AC failed", it) }
                    }
                } else {
                    Log.w(TAG, "TOGGLE_AC: proxy not connected")
                }
            }

            ActionType.TOGGLE_WINDOW_DRIVER -> {
                if (control != null) {
                    withContext(Dispatchers.IO) {
                        val open = !carState.isDriverWindowOpen
                        val targetState = if (open) WindowState.OPEN.value else WindowState.CLOSE.value
                        runCatching { control.setWindow(WindowType.LEFT_FRONT.id, targetState) }
                            .onSuccess {
                                Log.d(TAG, "TOGGLE_WINDOW_DRIVER result: $it (new state=$open)")
                                carState.isDriverWindowOpen = open
                            }
                            .onFailure { Log.e(TAG, "TOGGLE_WINDOW_DRIVER failed", it) }
                    }
                } else {
                    Log.w(TAG, "TOGGLE_WINDOW_DRIVER: proxy not connected")
                }
            }

            ActionType.TOGGLE_CLUSTER_CAST -> {
                if (control == null) {
                    Log.w(TAG, "TOGGLE_CLUSTER_CAST: proxy not connected")
                    return
                }
                withContext(Dispatchers.IO) {
                    val current = ClusterOverlayManager.currentProjectedPackage.value
                    if (current != null) {
                        ClusterOverlayManager.pullAppBackToMain(current)
                    } else {
                        val activeMainProjected = MainOverlayManager.currentProjectedPackage.value
                        val appToCast = if (activeMainProjected != null) {
                            Log.i(TAG, "TOGGLE_CLUSTER_CAST: Main split-screen projection active ($activeMainProjected). Moving it to cluster.")
                            activeMainProjected
                        } else {
                            val topPkg = runCatching { control.topActivityPackage }.getOrElse { "unknown" }
                            if (topPkg != PACKAGE_NAME && topPkg != "unknown" && topPkg != "none") {
                                topPkg
                            } else {
                                null
                            }
                        }

                        if (appToCast != null) {
                            ClusterOverlayManager.castAppToCluster(context, appToCast)
                        }
                    }
                }
            }

            ActionType.NONE -> {
                Log.d(TAG, "NONE action — nothing to do")
            }
        }
    }

    // -------------------------------------------------------------------------
    // App-launch helpers
    // -------------------------------------------------------------------------

    /**
     * Tries to launch [packageName] using three tiers:
     * 1. Standard [getLaunchIntentForPackage].
     * 2. Query for any MAIN+LAUNCHER activity in the package.
     * 3. Privileged proxy `launchApp()`.
     */
    suspend fun launchApp(packageName: String, context: Context, control: ICarControl?) {
        // Tier 1
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Log.d(TAG, "Launched '$packageName' via getLaunchIntentForPackage")
                return
            }
        } catch (e: Exception) {
            Log.w(TAG, "Tier 1 launch failed for '$packageName'", e)
        }

        // Tier 2
        try {
            val query = Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setPackage(packageName)
            val resolved = context.packageManager.queryIntentActivities(query, 0)
            if (resolved.isNotEmpty()) {
                val si = resolved.first().activityInfo
                val launchIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    setClassName(si.packageName, si.name)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(launchIntent)
                Log.d(TAG, "Launched '$packageName' via queryIntentActivities (${si.name})")
                return
            }
        } catch (e: Exception) {
            Log.w(TAG, "Tier 2 launch failed for '$packageName'", e)
        }

        // Tier 3
        if (control != null) {
            withContext(Dispatchers.IO) {
                val result = runCatching { control.launchApp(packageName) }
                Log.d(TAG, "Tier 3 proxy.launchApp('$packageName'): ${result.getOrElse { it.message }}")
            }
        } else {
            Log.e(TAG, "All launch tiers failed for '$packageName' - proxy not connected")
        }
    }

    private suspend fun launchAppsInSplitScreenNativeUi7(
        pkg1: String,
        pkg2: String,
        context: Context,
        splitRatio: String = "1/2"
    ) {
        try {
            if (pkg2.isEmpty()) {
                // Fullscreen fallback
                val intent = context.packageManager.getLaunchIntentForPackage(pkg1)
                if (intent != null) {
                    intent.addCategory("byd.intent.category.START_IVI_FULL")
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    withContext(Dispatchers.Main) {
                        Log.d(TAG, "DiLink 6 Native Fullscreen: Launching '$pkg1'")
                        context.startActivity(intent)
                    }
                } else {
                    Log.e(TAG, "DiLink 6 Native Fullscreen: Could not resolve intent for $pkg1")
                }
            } else {
                // Split screen launch (either 2/3 pane or 1/3 pane depending on splitRatio)
                val intent1 = context.packageManager.getLaunchIntentForPackage(pkg1)
                val intent2 = context.packageManager.getLaunchIntentForPackage(pkg2)

                if (intent1 != null && intent2 != null) {
                    val isOneThird = splitRatio == "1/3"
                    val cat1 = if (isOneThird) "byd.intent.category.START_IVI_PRIMARY" else "byd.intent.category.START_IVI_SECOND"
                    val cat2 = if (isOneThird) "byd.intent.category.START_IVI_SECOND" else "byd.intent.category.START_IVI_PRIMARY"
                    val label1 = if (isOneThird) "1/3" else "2/3"
                    val label2 = if (isOneThird) "2/3" else "1/3"

                    intent1.addCategory(cat1)
                    intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    intent2.addCategory(cat2)
                    intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)

                    withContext(Dispatchers.Main) {
                        Log.d(TAG, "DiLink 6 Native Split: Launching $label1 app '$pkg1' first")
                        context.startActivity(intent1)
                        delay(400) // Delay to let window manager capture and process first pane layout
                        Log.d(TAG, "DiLink 6 Native Split: Launching $label2 app '$pkg2' second")
                        context.startActivity(intent2)
                    }
                    Log.d(TAG, "DiLink 6 Native Split complete: $pkg1 ($label1) + $pkg2 ($label2)")
                } else {
                    Log.e(TAG, "DiLink 6 Native Split: Could not resolve intents for $pkg1 or $pkg2")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "DiLink 6 Native launch failed", e)
        }
    }

    private suspend fun launchAppsInSplitScreenNativeLegacy(
        pkg1: String,
        pkg2: String,
        context: Context,
        control: ICarControl?,
    ) {
        try {
            if (pkg2.isEmpty()) {
                launchApp(pkg1, context, control)
            } else {
                val intent1 = context.packageManager.getLaunchIntentForPackage(pkg1)
                val intent2 = context.packageManager.getLaunchIntentForPackage(pkg2)

                if (intent1 != null && intent2 != null) {
                    intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent1)

                    withContext(Dispatchers.Main) {
                        delay(500)
                        intent2.addFlags(
                            Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or Intent.FLAG_ACTIVITY_NEW_TASK
                        )
                        context.startActivity(intent2)
                    }
                    Log.d(TAG, "Launched split screen: $pkg1 + $pkg2")
                } else {
                    Log.e(TAG, "Split screen: could not resolve intents for $pkg1/$pkg2")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Split screen launch failed", e)
        }
    }

    private suspend fun launchAppsInSplitScreenCompat(
        pkg1: String,
        pkg2: String,
        context: Context,
        control: ICarControl?
    ) {
        try {
            val appPrefs = AppPreferences(context)
            val x = appPrefs.splitScreenCompatTapX
            val y = appPrefs.splitScreenCompatTapY

            Log.d(TAG, "Split screen compat: Launching first app: $pkg1")
            // Launch first app
            launchApp(pkg1, context, control)

            withContext(Dispatchers.IO) {
                // Wait for app to be in foreground
                delay(1000)

                // Trigger split screen button click
                Log.d(TAG, "Split screen compat: Toggling split screen mode via coordinates ($x, $y)")
                ShellCommandExecutor.simulateTap(x, y)

                // Wait for split-screen transition
                delay(500)

                // Launch second app via monkey command in background
                Log.d(TAG, "Split screen compat: Launching second app: $pkg2 via monkey")
                ShellCommandExecutor.launchAppViaMonkey(pkg2)
            }

            Log.d(TAG, "Launched compat tap split screen: $pkg1 + $pkg2 via coordinates ($x, $y)")
        } catch (e: Exception) {
            Log.e(TAG, "Compat tap split screen launch failed", e)
        }
    }

    private suspend fun launchAppsInSplitScreenProjected(
        pkg1: String,
        pkg2: String,
        context: Context,
        control: ICarControl?,
        projectFirstApp: Boolean = false,
        splitRatio: String = "1/2"
    ) {
        val activeProjectedPkg = MainOverlayManager.currentProjectedPackage.value
        if (activeProjectedPkg != null) {
            Log.i(TAG, "LAUNCH_APP_ON_SPLIT_SCREEN: split-screen active with $activeProjectedPkg. Toggling OFF.")
            MainOverlayManager.pullAppBackToMain(activeProjectedPkg)
        } else {
            launchAppsInSplitScreenUI7(
                pkg1,
                pkg2,
                context,
                control,
                projectFirstApp = projectFirstApp,
                splitRatio = splitRatio
            )
        }
    }

    private suspend fun launchAppsInSplitScreenUI7(
        pkg1: String,
        pkg2: String,
        context: Context,
        control: ICarControl?,
        projectFirstApp: Boolean = false,
        splitRatio: String = "1/3"
    ) {
        try {
            // First, hide any active main screen overlay
            withContext(Dispatchers.Main) {
                MainOverlayManager.hideOverlay()
            }

            // Fetch the physical width and height dynamically
            val screenParams = ScreenParams.getMetrics(context)

            // Determine which package is the primary one on main display, and which is projected as the overlay
            val primarySplitPkg = if (projectFirstApp) pkg2 else pkg1
            val projectedPkg = if (projectFirstApp) pkg1 else pkg2

            // Map split ratios to partition coordinates based on default display metrics
            val xSplit = when (splitRatio) {
                "2/3" -> (screenParams.width * 2) / 3
                "1/3" -> screenParams.width / 3
                else -> screenParams.width / 3 // Default to 1/3 split
            }

            // Override isLeft to be linked directly to projectFirstApp for physical split screen consistency:
            // First App is ALWAYS on the Left pane, Second App is ALWAYS on the Right pane.
            val effectiveIsLeft = projectFirstApp

            // Left pane: [0, xSplit], Right pane: [xSplit, screenParams.width]
            // The primary app will occupy the side opposite to the projected overlay (which is on 'effectiveIsLeft' side)
            val left = if (effectiveIsLeft) xSplit else 0
            val top = STATUS_BAR_HEIGHT
            val right = if (effectiveIsLeft) screenParams.width else xSplit
            val bottom = screenParams.height - BOTTOM_NAV_BAR_HEIGHT

            // Custom config for the overlay
            val appPrefs = AppPreferences(context)
            val customMainConfig = appPrefs.getMainDisplayConfig(projectedPkg)
            val xOffsetOverlay = if (effectiveIsLeft) 0 else xSplit
            val renderWidthOverlay = if (effectiveIsLeft) xSplit else (screenParams.width - xSplit)
            val overlayConfig = AppDisplayConfig(
                renderWidth = renderWidthOverlay,
                renderHeight = screenParams.height - TOTAL_BAR_HEIGHTS,
                xOffset = xOffsetOverlay,
                yOffset = STATUS_BAR_HEIGHT,
                densityDpi = customMainConfig.densityDpi
            )

            // 1. Launch the primary app normally on the main screen (Display 0)
            launchApp(primarySplitPkg, context, control)

            // 2. Poll/wait for the primary app task to appear and apply task bounds
            if (control != null) {
                withContext(Dispatchers.IO) {
                    var taskId = -1
                    // Poll for up to 15 times
                    for (i in 1..15) {
                        val taskLogs = control.getTaskId(primarySplitPkg)
                        taskId = taskLogs.lines()
                            .firstOrNull { it.startsWith("RESULT_ID:") }
                            ?.substringAfter("RESULT_ID:")
                            ?.toIntOrNull() ?: -1

                        if (taskId != -1 && taskId != 0) {
                            Log.d(TAG, "UI 7: Found taskId $taskId for $primarySplitPkg on attempt $i")
                            break
                        }
                        delay(500)
                    }

                    if (taskId != -1 && taskId != 0) {
                        // Change windowing mode to FREEFORM (5)
                        Log.d(TAG, "UI 7: Setting windowing mode of task $taskId for $primarySplitPkg to FREEFORM (5)")
                        control.setTaskWindowingMode(taskId, 5)
                        delay(300)

                        // Apply split-screen bounds to primary app
                        Log.d(TAG, "UI 7: Resizing task $taskId for $primarySplitPkg to bounds ($left, $top, $right, $bottom)")
                        control.setTaskBounds(taskId, left, top, right, bottom)
                        control.setFocusedTask(taskId)
                    } else {
                        Log.e(TAG, "UI 7: Failed to find taskId for $primarySplitPkg to resize")
                    }
                }
            }

            // 3. Wait a bit and project the projected app via MainOverlayManager
            withContext(Dispatchers.Main) {
                delay(300)
                MainOverlayManager.castAppToMainScreen(context, projectedPkg, overlayConfig, primarySplitPkg, screenParams)
            }

            Log.d(TAG, "Launched UI 7 split screen: $primarySplitPkg + $projectedPkg (via MainOverlayManager)")
        } catch (e: Exception) {
            Log.e(TAG, "UI 7 split screen launch failed", e)
        }
    }

    // -------------------------------------------------------------------------
    // Startup sequence runner
    // -------------------------------------------------------------------------

    /**
     * Reads the saved startup action sequence and executes each action in order,
     * respecting per-action delays.
     *
     * Called by [com.sr.openbyd.proxy.ProxyManager] after the proxy connects so the
     * car API is guaranteed to be available.
     */
    suspend fun runStartupSequence(context: Context) {
        val repo = StartupActionRepository(context)
        val actions = repo.getAll()

        if (actions.isEmpty()) {
            Log.d(TAG, "Startup sequence: no actions configured")
            return
        }

        Log.i(TAG, "Startup sequence: executing ${actions.size} action(s)")
        val carState = CarStateRepository(context)

        for (action in actions) {
            if (action.delayMs > 0) {
                Log.d(TAG, "Startup sequence: waiting ${action.delayMs} ms before #${action.index}")
                delay(action.delayMs)
            }

            if (action.actionType == ActionType.NONE) {
                Log.d(TAG, "Startup sequence: #${action.index} is NONE — skipping")
                continue
            }

            Log.i(TAG, "Startup sequence: executing #${action.index} → ${action.actionType}")
            runCatching {
                execute(
                    actionType          = action.actionType,
                    targetPackageName   = action.targetPackageName,
                    secondPackageName   = action.secondPackageName,
                    context             = context,
                    carState            = carState,
                    isLeft              = action.isLeft,
                    projectFirstApp     = action.projectFirstApp,
                    splitRatio          = action.splitRatio
                )
            }.onFailure { e ->
                Log.e(TAG, "Startup sequence: #${action.index} failed", e)
            }
        }

        Log.i(TAG, "Startup sequence: complete")
    }
}
