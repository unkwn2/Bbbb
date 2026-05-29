package com.sr.openbyd.services

import android.content.Context
import android.view.accessibility.AccessibilityManager
import android.accessibilityservice.AccessibilityServiceInfo
import android.provider.Settings
import android.util.Log
import androidx.core.content.edit
import com.sr.openbyd.utils.ShellCommandExecutor

/**
 * Helper object that uses the privileged ADB proxy (UID 2000 / shell) to automatically
 * enable the [SteeringWheelAccessibilityService] without requiring the user to navigate
 * to Settings > Accessibility.
 *
 * The proxy process holds the WRITE_SECURE_SETTINGS permission, which allows it to
 * directly write to the system's secure settings database — something a normal app
 * cannot do.
 *
 * Usage: call [buildEnableCommands] to get the shell commands to send via the proxy,
 * or call [isServiceEnabled] to check the current state.
 */
object AccessibilitySetupHelper {

    private const val TAG = "AccessibilitySetupHelper"
    private const val PREFS_NAME = "AccessibilityPrefs"
    private const val KEY_USER_ENABLED = "user_enabled_remapper"

    /** The fully-qualified component name of our accessibility service. */
    const val SERVICE_COMPONENT = "com.sr.openbyd/com.sr.openbyd.services.SteeringWheelAccessibilityService"

    /**
     * Persists whether the user has intentionally enabled the remapper service.
     * Call this when the user taps "Enable Service" in the UI.
     * Used by [autoEnableIfNeeded] to re-enable the service after a car reboot.
     */
    fun markUserEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putBoolean(KEY_USER_ENABLED, enabled) }
        Log.d(TAG, "User preference saved: enabled=$enabled")
    }

    /** Returns true if the user has previously tapped "Enable Service". */
    fun hasUserEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_USER_ENABLED, false)

    /**
     * If the user previously enabled the remapper and the service is not yet running,
     * re-sends the enable commands via the proxy. Call this inside
     * [ProxyManager.onProxyConnected] so the service is always activated when the
     * proxy comes online (e.g. after a car reboot).
     */
    fun autoEnableIfNeeded(context: Context) {
        if (getStatus(context) == ServiceStatus.RUNNING) return

        Log.i(TAG, "Ensuring accessibility service is enabled via proxy")
        try {
            buildEnableCommands().forEach { cmd ->
                ShellCommandExecutor.execute(cmd)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Auto-enable failed", e)
        }
    }

    /**
     * Returns the list of shell commands that the proxy should execute via
     * [com.sr.openbyd.proxy.ProxyManager] to enable the accessibility service.
     *
     * These commands require `WRITE_SECURE_SETTINGS`, which is available to shell (UID 2000).
     *
     * The approach mirrors DiPlus's `KDService` — it directly writes the component name
     * to `enabled_accessibility_services` and flips `accessibility_enabled` to 1.
     */
    fun buildEnableCommands(): List<String> = listOf(
        // To force Android to actually start the service (especially after a reboot),
        // we must force a state change. If it's already in the string but not running,
        // writing the exact same string won't wake up the OS.
        // We first remove our component, save, and then add it back.
        """
        CURRENT=$(settings get secure enabled_accessibility_services)
        CLEANED=$(echo "${'$'}CURRENT" | sed 's/$SERVICE_COMPONENT//g' | sed 's/::/:/g' | sed 's/^://' | sed 's/:${'$'}//')
        
        # Force a state change by temporarily removing it
        settings put secure enabled_accessibility_services "${'$'}CLEANED"
        sleep 0.2
        
        # Add it back to trigger the OS to bind the service
        if [ -z "${'$'}CLEANED" ] || [ "${'$'}CLEANED" = "null" ]; then
          settings put secure enabled_accessibility_services "$SERVICE_COMPONENT"
        else
          settings put secure enabled_accessibility_services "${'$'}CLEANED:$SERVICE_COMPONENT"
        fi
        
        settings put secure accessibility_enabled 1
        echo "SteeringWheelAccessibilityService force-enabled"
        """.trimIndent()
    )

    /**
     * Returns the shell commands to DISABLE the service (e.g., on uninstall or user request).
     * Removes only our service component from the colon-separated list.
     */
    fun buildDisableCommands(): List<String> = listOf(
        """
        CURRENT=$(settings get secure enabled_accessibility_services)
        NEW=$(echo "${'$'}CURRENT" | sed 's/$SERVICE_COMPONENT://g' | sed 's/:$SERVICE_COMPONENT//g' | sed 's/^$SERVICE_COMPONENT${'$'}//g')
        settings put secure enabled_accessibility_services "${'$'}NEW"
        echo "SteeringWheelAccessibilityService disabled"
        """.trimIndent()
    )

    /**
     * Three-state status for the UI to display:
     * - [NOT_ENABLED]  — not in the secure settings list at all
     * - [ENABLED_NOT_RUNNING] — in the settings list but the OS hasn't bound the service yet
     * - [RUNNING]      — the OS has bound the service and [onServiceConnected] has fired
     */
    enum class ServiceStatus(val labelRes: Int) {
        NOT_ENABLED(com.sr.openbyd.R.string.status_not_enabled),
        ENABLED_NOT_RUNNING(com.sr.openbyd.R.string.status_enabled_not_running),
        RUNNING(com.sr.openbyd.R.string.status_running);
    }

    /**
     * Returns the current [ServiceStatus] by combining:
     * 1. The in-process static flag from [SteeringWheelAccessibilityService.isConnected]
     * 2. The OS active-service list from [AccessibilityManager]
     * 3. The secure settings string (last resort)
     *
     * Call this in [MainActivity.onResume] to keep the badge fresh.
     */
    fun getStatus(context: Context): ServiceStatus {
        // Fast path: if our service instance is live in this process we know for sure
        if (SteeringWheelAccessibilityService.isConnected) {
            return ServiceStatus.RUNNING
        }

        // Check the OS's active-service list (more reliable than reading the settings string)
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        val isActiveInOS = am?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            ?.any { it.resolveInfo.serviceInfo.let { si ->
                si.packageName == "com.sr.openbyd" &&
                si.name == "com.sr.openbyd.services.SteeringWheelAccessibilityService"
            } } ?: false

        if (isActiveInOS) {
            // OS has it active but our flag isn't set — process restarted between checks
            return ServiceStatus.RUNNING
        }

        // Fall back to settings string check
        return if (isServiceEnabled(context)) ServiceStatus.ENABLED_NOT_RUNNING
        else ServiceStatus.NOT_ENABLED
    }

    /**
     * Checks whether the service is currently listed in the system's enabled accessibility
     * services. This can be called from the UI to reflect current state.
     *
     * Note: This reads the secure setting directly and does NOT require any special
     * permission (only WRITE requires the privilege).
     */
    fun isServiceEnabled(context: Context): Boolean {
        return try {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""
            val enabled = enabledServices.contains(SERVICE_COMPONENT)
            Log.d(TAG, "Service enabled: $enabled (current list: '$enabledServices')")
            enabled
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read enabled_accessibility_services", e)
            false
        }
    }
}
