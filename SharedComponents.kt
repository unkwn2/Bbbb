package com.sr.openbyd.proxy

import android.content.Context
import android.util.Log
import com.sr.openbyd.adb.AdbConnectionManager
import com.sr.openbyd.ipc.ICarControl
import com.sr.openbyd.data.AppPreferences
import com.sr.openbyd.services.AccessibilitySetupHelper
import com.sr.openbyd.services.ActionExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


object ProxyManager {
    private const val TAG = "ProxyManager"

    var carControl: ICarControl? = null
        private set

    private var appContext: Context? = null

    fun onProxyConnected(context: Context, control: ICarControl) {
        appContext = context.applicationContext
        carControl = control
        Log.i(TAG, "Proxy connected. API Version: ${control.apiVersion}")

        try {
            control.ping()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to ping proxy", e)
        }

        // Trigger a theme probe when the proxy connects!
        CoroutineScope(Dispatchers.IO).launch {
            com.sr.openbyd.ui.theme.probeThemeViaProxy(context.applicationContext)
        }

        // Auto-re-enable accessibility service if the user had previously enabled it.
        CoroutineScope(Dispatchers.IO).launch {
            AccessibilitySetupHelper.autoEnableIfNeeded(context.applicationContext)
        }

        // Execute startup action sequence only if triggered from boot receiver.
        val prefs = AppPreferences(context)
        if (prefs.runStartupOnConnect) {
            Log.i(TAG, "Boot flag detected. Executing startup sequence.")
            prefs.runStartupOnConnect = false // Consume flag

            CoroutineScope(Dispatchers.IO).launch {
                ActionExecutor.runStartupSequence(context.applicationContext)
            }
        } else {
            Log.i(TAG, "Skipping startup sequence (not started from boot receiver)")
        }
    }

    fun startProxy(context: Context) {
        appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Connect to Local ADB
                val manager = AdbConnectionManager.getInstance(context)

                // Connect if not already connected
                var connected = false
                try {
                    connected = manager.connect("127.0.0.1", 5555)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to connect to local ADB", e)
                }

                if (!connected) {
                    Log.e(TAG, "Could not establish local ADB connection")
                    return@launch
                }

                val apkPath = context.applicationInfo.sourceDir
                val command = """
                    apkPath=$apkPath
                    
                    pkill -9 -f openbyd_proxy
                    pidof openbyd_proxy | xargs kill -9 2>/dev/null
                    
                    nohup app_process \
                      -Djava.class.path=/system/framework/services.jar:/system/framework/dilink-services.jar:${'$'}apkPath \
                      -Djava.library.path=/system/lib64:/product/lib64:${'$'}apkPath!/lib/arm64-v8a \
                      /system/bin \
                      --nice-name=openbyd_proxy \
                      com.sr.openbyd.proxy.EntryPoint \
                      --uid=2000 \
                      > /dev/null 2>&1 &
                    """.trimIndent()
                // 2. Open Stream and Execute Command
                val stream = manager.openStream(io.github.muntashirakon.adb.LocalServices.SHELL)
                stream.openOutputStream().use { os ->
                    os.write("$command\n".toByteArray(Charsets.UTF_8))
                    os.flush()
                    os.write("\n".toByteArray(Charsets.UTF_8))
                }

                Log.d(TAG, "Proxy launch script executed via libadb-android")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start proxy", e)
            }
        }
    }

    /**
     * Executes a shell command via the privileged proxy (if connected).
     * Returns null if proxy is disconnected.
     */
    fun runShellCommand(command: String): String? {
        return try {
            carControl?.runShellCommand(command)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to run shell command: ${e.message}")
            null
        }
    }
}
