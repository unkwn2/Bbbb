package com.sr.openbyd.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.sr.openbyd.models.ActionType
import com.sr.openbyd.models.ButtonMapping
import com.sr.openbyd.models.ButtonMappingRepository
import com.sr.openbyd.ui.overlay.MainOverlayManager
import com.sr.openbyd.ui.overlay.ClusterOverlayManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SteeringWheelAccessibilityService : AccessibilityService() {

    private lateinit var mappingRepository: ButtonMappingRepository
    private lateinit var carState: CarStateRepository

    companion object {
        private const val TAG = "SteeringWheelService"

        /**
         * True only while the OS has the service bound and running.
         * Set in [onServiceConnected], cleared in [onDestroy].
         * Use this in the UI for a live running check — checking settings alone
         * only tells you the service is *configured*, not *active*.
         */
        @Volatile
        var isConnected: Boolean = false
            private set

        /** Global debug flag. If true, all key events are intercepted and logged but not executed. */
        @Volatile
        var isDebugMode: Boolean = false

        /** Stores the last keycode captured for UI display. */
        @Volatile
        var lastCapturedKeyCode: Int? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        mappingRepository = ButtonMappingRepository(this)
        carState = CarStateRepository(this)
        isConnected = true

        val info = serviceInfo ?: AccessibilityServiceInfo()
        // FLAG_REQUEST_FILTER_KEY_EVENTS = 32
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        serviceInfo = info

        Log.d(TAG, "Steering Wheel Accessibility Service Connected and filtering key events")
    }

    override fun onDestroy() {
        isConnected = false
        Log.d(TAG, "Steering Wheel Accessibility Service Destroyed")
        super.onDestroy()
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        // We only care about keys down for triggering, to avoid double execution on key up
        Log.d(TAG, "KeyEvent: keycode=${event.keyCode} action=${event.action} when=${event.eventTime}")

        val keyCode = event.keyCode

        // Always update last captured key for the debug console
        if (event.action == KeyEvent.ACTION_DOWN) {
            lastCapturedKeyCode = keyCode
        }

        // Handle debug mode first - capture ALL keys if in debug
        if (isDebugMode) {
            return true // Intercept and consume in debug mode to disable original car behavior
        }

        val mapping = mappingRepository.getMapping(keyCode)

        // If no custom mapping is set, let the original car system handle it
        if (mapping.actionType == ActionType.NONE) {
            return false
        }

        // Suppression logic for mapped keys (including ACTION_UP)
        if (event.action != KeyEvent.ACTION_DOWN) {
            return true
        }

        Log.d(TAG, "Intercepted mapped keycode $keyCode -> Action: ${mapping.actionType}")

        // Execute the mapped action via shared ActionExecutor
        executeAction(mapping)

        // Return true to consume the event and suppress the original car behavior
        return true
    }

    private fun executeAction(mapping: ButtonMapping) {
        val appContext = applicationContext
        GlobalScope.launch {
            ActionExecutor.execute(
                actionType          = mapping.actionType,
                targetPackageName   = mapping.targetPackageName,
                secondPackageName   = mapping.secondPackageName,
                context             = appContext,
                carState            = carState,
                isLeft              = mapping.isLeft,
                projectFirstApp     = mapping.projectFirstApp,
                splitRatio          = mapping.splitRatio
            )
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val focusedPkg = event.packageName?.toString() ?: return
            
            val activeMainProjected = MainOverlayManager.currentProjectedPackage.value
            val primaryPkg = MainOverlayManager.primarySplitPackage
            
            if (activeMainProjected != null) {
                val isIME = focusedPkg.contains("input", ignoreCase = true) ||
                            focusedPkg.contains("keyboard", ignoreCase = true) ||
                            focusedPkg.contains("ime", ignoreCase = true) ||
                            focusedPkg.contains("sogou", ignoreCase = true)

                val activeClusterProjected = ClusterOverlayManager.currentProjectedPackage.value
                if (focusedPkg != activeMainProjected &&
                    focusedPkg != primaryPkg &&
                    focusedPkg != packageName &&
                    focusedPkg != activeClusterProjected &&
                    focusedPkg != "com.android.systemui" &&
                    focusedPkg != "com.byd.launchermap" &&
                    focusedPkg != "com.example.amapservice" &&
                    focusedPkg != "com.byd.sr" &&
                    focusedPkg != "android" &&
                    !isIME
                ) {
                    Log.d(TAG, "onAccessibilityEvent: Focus shifted to $focusedPkg from ($activeMainProjected, $primaryPkg). Silently cancelling split-screen projection.")
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        MainOverlayManager.hideOverlay(focusPrimary = false)
                    }
                }
            }
        }
    }

    override fun onInterrupt() {
        // Service interrupted
        Log.w(TAG, "Steering Wheel Accessibility Service Interrupted")
    }
}
