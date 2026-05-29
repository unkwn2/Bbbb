package com.sr.openbyd.utils

import android.util.Log
import com.sr.openbyd.proxy.ProxyManager

/**
 * Centralized executor for running privileged shell commands via the ADB ProxyManager.
 * Decouples individual components from raw string command construction and execution.
 */
object ShellCommandExecutor {
    private const val TAG = "ShellCommandExecutor"

    /**
     * Executes a raw shell command via the ProxyManager.
     *
     * @param command The raw command string to run.
     * @return The output of the command, or null if the proxy is disconnected or failed.
     */
    fun execute(command: String): String? {
        Log.d(TAG, "Executing: $command")
        return ProxyManager.runShellCommand(command)
    }

    /**
     * Grants the SYSTEM_ALERT_WINDOW (draw overlays) permission to a package.
     */
    fun grantSystemAlertWindow(packageName: String): String? {
        return execute("appops set $packageName SYSTEM_ALERT_WINDOW allow")
    }

    /**
     * Grants the PROJECT_MEDIA permission to a package.
     */
    fun grantProjectMedia(packageName: String): String? {
        return execute("appops set $packageName PROJECT_MEDIA allow")
    }

    /**
     * Grants the SYSTEM_ALERT_WINDOW (draw overlays) and PROJECT_MEDIA permissions to a package.
     */
    fun grantOverlayPermission(packageName: String): Boolean {
        val r1 = grantSystemAlertWindow(packageName)
        Log.d(TAG, "Granting SYSTEM_ALERT_WINDOW permissions via proxy...: $r1")
        val r2 = grantProjectMedia(packageName)
        Log.d(TAG, "Granting PROJECT_MEDIA permissions via proxy...: $r2")
        return r1 != null && r2 != null
    }

    /**
     * Copies a file from source to destination.
     */
    fun copyFile(sourcePath: String, destPath: String): String? {
        return execute("cp $sourcePath $destPath")
    }

    /**
     * Changes permission mode on a file (e.g. 666).
     */
    fun chmod(mode: String, path: String): String? {
        return execute("chmod $mode $path")
    }

    /**
     * Deletes a file.
     */
    fun removeFile(path: String): String? {
        return execute("rm $path")
    }

    /**
     * Uninstalls a package.
     */
    fun uninstallApp(packageName: String): String? {
        return execute("pm uninstall $packageName")
    }

    /**
     * Creates an atomic installation session and returns the session details containing the session ID.
     */
    fun createInstallSession(): String? {
        return execute("pm install-create")
    }

    /**
     * Writes an APK split path to an active installation session.
     */
    fun writeToInstallSession(sessionId: String, splitName: String, path: String): String? {
        return execute("pm install-write $sessionId $splitName $path")
    }

    /**
     * Commits/finalizes an active installation session.
     */
    fun commitInstallSession(sessionId: String): String? {
        return execute("pm install-commit $sessionId")
    }

    /**
     * Abandons/discards an active installation session.
     */
    fun abandonInstallSession(sessionId: String): String? {
        return execute("pm install-abandon $sessionId")
    }

    /**
     * Performs a standard single-APK installation.
     */
    fun installApp(apkPath: String): String? {
        return execute("pm install $apkPath")
    }

    /**
     * Simulates a tap/click at coordinate (x, y) on the default display.
     */
    fun simulateTap(x: Int, y: Int): String? {
        return execute("input tap $x $y")
    }

    /**
     * Simulates a tap/click at coordinate (x, y) on a specific display.
     */
    fun simulateTapOnDisplay(displayId: Int, x: Int, y: Int): String? {
        return execute("input -d $displayId tap $x $y")
    }

    /**
     * Launches an app's primary/launcher activity using monkey.
     */
    fun launchAppViaMonkey(packageName: String): String? {
        return execute("monkey -p $packageName -c android.intent.category.LAUNCHER 1")
    }

    /**
     * Queries a key's value from the specified settings table (system, secure, global).
     * Includes helper logic to parse result tokens standard for BYD ADB outputs.
     */
    fun getSettingsValue(table: String, key: String): String? {
        val result = execute("settings get $table $key")
        return result?.lines()
            ?.lastOrNull { it.contains("runShellCommand result:") }
            ?.substringAfter("runShellCommand result:")
            ?.trim() ?: result?.trim()
    }

    /**
     * Reads logcat output, optionally filtering by PID.
     */
    fun readLogcat(pid: Int, tailCount: Int = 1200): String? {
        return execute("logcat -d -t $tailCount --pid=$pid")
    }
}
