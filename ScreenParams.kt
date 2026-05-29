package com.sr.openbyd.ui.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import com.sr.openbyd.data.AppDisplayConfig
import com.sr.openbyd.data.AppPreferences
import com.sr.openbyd.proxy.ProxyManager
import com.sr.openbyd.services.MainProjectionService
import com.sr.openbyd.utils.ScreenParams
import com.sr.openbyd.utils.ShellCommandExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * Manages the overlay and app casting on the main split-screen display (Display 0).
 */
object MainOverlayManager {

    private const val TAG = "MainOverlayManager"

    private data class TouchEventData(
        val displayId: Int,
        val action: Int,
        val downTime: Long,
        val eventTime: Long,
        val x: Float,
        val y: Float
    )

    private val touchEventQueue = kotlinx.coroutines.channels.Channel<TouchEventData>(kotlinx.coroutines.channels.Channel.UNLIMITED)

    init {
        startTouchEventProcessor()
    }

    private fun startTouchEventProcessor() {
        CoroutineScope(Dispatchers.IO).launch {
            for (eventData in touchEventQueue) {
                val control = ProxyManager.carControl ?: continue
                try {
                    val result = control.injectTouchEvent(
                        eventData.displayId,
                        eventData.action,
                        eventData.downTime,
                        eventData.eventTime,
                        eventData.x,
                        eventData.y
                    )
                    if (eventData.action == MotionEvent.ACTION_DOWN ||
                        eventData.action == MotionEvent.ACTION_UP ||
                        !result.contains("SUCCESS")) {
                        Log.d(TAG, "injectTouchEvent (action=${eventData.action}) result:\n$result")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to stream touch event: ${e.message}")
                }
            }
        }
    }

    private var overlayViewRef: WeakReference<View>? = null
    private var remoteDisplayId: Int = -1

    private val _currentProjectedPackage = MutableStateFlow<String?>(null)
    /** The package name of the app currently projected to the main screen split screen. */
    val currentProjectedPackage = _currentProjectedPackage.asStateFlow()

    @JvmField
    var primarySplitPackage: String? = null

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
     * Removes the current overlay and virtual display.
     */
    fun hideOverlay(focusPrimary: Boolean = true) {
        val primaryPkg = primarySplitPackage
        primarySplitPackage = null

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val control = ProxyManager.carControl
                if (control != null) {
                    if (primaryPkg != null) {
                        val taskId = control.getTaskId(primaryPkg).lines()
                            .firstOrNull { it.startsWith("RESULT_ID:") }
                            ?.substringAfter("RESULT_ID:")
                            ?.toIntOrNull() ?: -1
                        if (taskId != -1) {
                            Log.d(TAG, "Restoring primary split app $primaryPkg task $taskId to fullscreen windowing mode (focusPrimary=$focusPrimary)")
                            control.setTaskWindowingMode(taskId, 1) // 1 = WINDOWING_MODE_FULLSCREEN
                            control.setTaskBounds(taskId, 0, 0, 0, 0)
                            if (focusPrimary) {
                                control.setFocusedTask(taskId)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore primary split app bounds", e)
            }
        }

        if (remoteDisplayId != -1) {
            val result = ProxyManager.carControl?.releaseVirtualDisplay(remoteDisplayId)
            Log.d(TAG, "Release Virtual Display result: $result")
            remoteDisplayId = -1
        }

        Handler(Looper.getMainLooper()).post {
            _currentProjectedPackage.value = null
        }

        overlayViewRef?.get()?.let { view ->
            Handler(Looper.getMainLooper()).post {
                try {
                    view.visibility = View.GONE
                    val wm = view.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                    wm.removeView(view)
                    overlayViewRef = null
                    Log.i(TAG, "Main screen overlay removed on main thread")

                    // Stop service after view is gone
                    MainProjectionService.stop(view.context)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to remove main screen overlay on main thread", e)
                }
            }
        }
    }

    /**
     * Resolves the Main Screen Display ID, respecting any manual user overrides.
     */
    fun getMainDisplayId(context: Context): Int {
        val overrideId = com.sr.openbyd.data.AppPreferences(context).overrideMainDisplayId
        return if (overrideId != -1) overrideId else android.view.Display.DEFAULT_DISPLAY
    }

    /**
     * Casts an application to the main display as a customizable floating window overlay.
     */
    fun castAppToMainScreen(
        appContext: Context,
        packageName: String,
        customConfig: AppDisplayConfig? = null,
        primarySplitPkg: String? = null,
        screenParams: ScreenParams = ScreenParams.getMetrics(appContext)
    ) {
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

                val dm = appContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
                val mainDisplayId = getMainDisplayId(appContext)
                val targetDisplay = dm.getDisplay(mainDisplayId) ?: run {
                    Toast.makeText(appContext, "Error: Resolved main display not found!", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                hideOverlay() // Clear previous

                if (primarySplitPkg != null) {
                    primarySplitPackage = primarySplitPkg
                }

                val displayContext = appContext.createDisplayContext(targetDisplay)
                val windowManager = displayContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

                val appPrefs = AppPreferences(appContext)
                val config = customConfig ?: appPrefs.getMainDisplayConfig(packageName)

                val container = FrameLayout(displayContext).apply {
                    clipChildren = true
                }

                val surfaceView = SurfaceView(displayContext).apply {
                    // Render at actual config size with dynamically adjusted density
                    // to prevent deformation while keeping full-screen tablet features/resolution!
                    setupSurfaceCallback(packageName, config.renderWidth, config.renderHeight, config.densityDpi)
                }

                // Add SurfaceView matching exact config size
                container.addView(surfaceView, FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                ))

                // Configure touch listener for tap & swipe injection on surfaceView, mapping actual width & height
                setupTouchForwarder(surfaceView, config.renderWidth, config.renderHeight)

                // Create a circular close button at the division edge
                val density = displayContext.resources.displayMetrics.density
                val btnSize = (40 * density).toInt()
                // Determine if overlay is on the left half of the display to position the exit button exactly on the center division line.
                val isLeft = (config.xOffset + config.renderWidth / 2) < (screenParams.width / 2)
                val btnGravity = if (isLeft) Gravity.CENTER_VERTICAL or Gravity.END else Gravity.CENTER_VERTICAL or Gravity.START
                val btnMargin = (8 * density).toInt()

                val circleBg = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(Color.parseColor("#B32C2C2C")) // Semi-transparent dark gray
                    setStroke(2, Color.parseColor("#80FFFFFF")) // Premium thin white border
                }

                val closeButton = TextView(displayContext).apply {
                    text = "✕"
                    textSize = 18f
                    setTextColor(Color.WHITE)
                    gravity = Gravity.CENTER
                    background = circleBg
                    setOnClickListener {
                        pullAppBackToMain(packageName)
                    }
                }

                val btnParams = FrameLayout.LayoutParams(btnSize, btnSize).apply {
                    gravity = btnGravity
                    if (isLeft) {
                        rightMargin = btnMargin
                    } else {
                        leftMargin = btnMargin
                    }
                }

                container.addView(closeButton, btnParams)

                // Add FrameLayout directly at specific size/offset
                windowManager.addView(container, createOverlayLayoutParams(config))
                overlayViewRef = WeakReference(container)

                // Start foreground service to maintain projection priority
                MainProjectionService.start(appContext)

                Toast.makeText(
                    appContext,
                    "Projecting Split Screen\n$packageName",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {
                Log.e(TAG, "Main Screen Cast failed: ${e.message}", e)
                Toast.makeText(appContext, "Main Screen Cast failed", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Simple test overlay remains for basic main screen verification.
     */
    fun testOverlayLaunch(context: Context) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                if (!ensureOverlayPermission(context)) {
                    Toast.makeText(context, "Overlay permission required!", Toast.LENGTH_LONG).show()
                    return@launch
                }

                hideOverlay() // Clear previous

                val dm = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
                val targetDisplay = dm.getDisplay(android.view.Display.DEFAULT_DISPLAY) ?: run {
                    Toast.makeText(context, "Error: Default display not found!", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val displayContext = context.createDisplayContext(targetDisplay)
                val windowManager = displayContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

                val container = FrameLayout(displayContext).apply {
                    setBackgroundColor(Color.parseColor("#80FF0000"))
                }
                val textView = TextView(displayContext).apply {
                    text = "UI 7 Main Screen Overlay Active"
                    textSize = 24f
                    setTextColor(Color.WHITE)
                    gravity = Gravity.CENTER
                }
                container.addView(textView, FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                ))

                // Use current config or default config for package name
                val appPrefs = AppPreferences(context)
                val config = appPrefs.getMainDisplayConfig("com.sr.openbyd")

                windowManager.addView(container, createOverlayLayoutParams(config))
                overlayViewRef = WeakReference(container)

                Toast.makeText(context, "Testing Overlay (UI 7)", Toast.LENGTH_SHORT).show()
                Handler(Looper.getMainLooper()).postDelayed({ hideOverlay() }, 5000)

            } catch (e: Exception) {
                Log.e(TAG, "Overlay test failed: ${e.message}", e)
                Toast.makeText(context, "Overlay test failed", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Intercepts touch gestures and forwards them to the Virtual Display in real-time via Binder IPC.
     */
    private fun setupTouchForwarder(view: View, vdWidth: Int, vdHeight: Int) {
        val forwardEvent = { event: MotionEvent ->
            val viewWidth = view.width
            val viewHeight = view.height
            if (viewWidth > 0 && viewHeight > 0) {
                val displayId = remoteDisplayId
                if (displayId != -1 && displayId != 0) {
                    val clampX = { x: Float -> x.coerceIn(0f, viewWidth.toFloat()) }
                    val clampY = { y: Float -> y.coerceIn(0f, viewHeight.toFloat()) }

                    // Map local coordinates to VirtualDisplay space
                    val mappedX = (clampX(event.x) / viewWidth) * vdWidth
                    val mappedY = (clampY(event.y) / viewHeight) * vdHeight

                    // Offer to our sequential FIFO channel immediately (retains ordering and bypasses recycled object corruptions)
                    touchEventQueue.trySend(TouchEventData(
                        displayId,
                        event.action,
                        event.downTime,
                        event.eventTime,
                        mappedX,
                        mappedY
                    ))
                }
            }
        }

        view.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                forwardEvent(event)
                return true
            }
        })

        view.setOnGenericMotionListener(object : View.OnGenericMotionListener {
            override fun onGenericMotion(v: View, event: MotionEvent): Boolean {
                forwardEvent(event)
                return true
            }
        })
    }


    /**
     * Creates the layout parameters for the WindowManager overlay.
     */
    private fun createOverlayLayoutParams(config: AppDisplayConfig): WindowManager.LayoutParams {
        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        return WindowManager.LayoutParams(
            config.renderWidth,
            config.renderHeight,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.START or Gravity.TOP
            x = config.xOffset
            y = config.yOffset
        }
    }

    private fun SurfaceView.setupSurfaceCallback(packageName: String, vdWidth: Int, vdHeight: Int, densityDpi: Int) {
        holder.setFixedSize(vdWidth, vdHeight)
        holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                Log.d(TAG, "Main screen surface created, launching VirtualDisplay")
                launchOnVirtualDisplay(this@setupSurfaceCallback.context, packageName, holder.surface, vdWidth, vdHeight, densityDpi)
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                Log.d(TAG, "Main screen surface destroyed")
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

            // Flags: 256 or 64 or 2 (MATCH_CONTENT_ON_REMOVAL | SUPPORTS_TOUCH | PRESENTATION)
            val flags = 256 or 64 or 2

            Log.d(TAG, "Creating main screen virtual display resolution=${width}x${height} densityDpi=$densityDpi")

            val resp = control.createVirtualDisplay(
                "OpenBYD_Main_VD",
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
                Log.e(TAG, "Failed to create remote Virtual Display (returned ID $remoteDisplayId)")
                return
            }

            Log.i(TAG, "Virtual Display created with ID: $remoteDisplayId")

            Handler(Looper.getMainLooper()).post {
                _currentProjectedPackage.value = packageName
            }

            val forceResult = control.launchAndForce(packageName, remoteDisplayId, width, height)
            Log.d(TAG, "launchAndForce output:\n$forceResult")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch on virtual display: ${e.message}", e)
        }
    }

    /**
     * Pulls the projected app back to the main display at normal status.
     */
    fun pullAppBackToMain(packageName: String) {
        val control = ProxyManager.carControl ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val taskId = control.getTaskId(packageName).lines()
                    .firstOrNull { it.startsWith("RESULT_ID:") }
                    ?.substringAfter("RESULT_ID:")
                    ?.toIntOrNull() ?: -1

                if (taskId != -1) {
                    control.moveTaskToDisplay(taskId, 0)
                    control.setTaskWindowingMode(taskId, 1) // 1 = WINDOWING_MODE_FULLSCREEN
                    control.setTaskBounds(taskId, 0, 0, 0, 0)
                    control.setFocusedTask(taskId)
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
}
