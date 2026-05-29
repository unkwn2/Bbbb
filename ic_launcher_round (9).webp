package com.sr.openbyd.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sr.openbyd.data.AppPreferences
import com.sr.openbyd.proxy.ProxyManager
import com.sr.openbyd.services.SteeringWheelAccessibilityService
import com.sr.openbyd.ui.overlay.ClusterOverlayManager
import com.sr.openbyd.utils.AppConstants
import com.sr.openbyd.utils.ShellCommandExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DiagnosticViewModel(application: Application) : AndroidViewModel(application) {
    private val appPrefs = AppPreferences(application)



    var diagnosticLogs by mutableStateOf("")
        private set
    var isExecutingLogcat by mutableStateOf(false)
        private set

    var hasSdkAccess by mutableStateOf(false)
        private set

    val isProxyConnected: Boolean
        get() = ProxyManager.carControl != null

    val isAccessibilityServiceRunning: Boolean
        get() = SteeringWheelAccessibilityService.isConnected

    private val _overrideMainDisplayId = mutableIntStateOf(appPrefs.overrideMainDisplayId)
    val overrideMainDisplayId: Int get() = _overrideMainDisplayId.intValue

    private val _overrideClusterDisplayId = mutableIntStateOf(appPrefs.overrideClusterDisplayId)
    val overrideClusterDisplayId: Int get() = _overrideClusterDisplayId.intValue

    init {
        refreshDiagnostics()
    }

    fun refreshDiagnostics() {
        Log.d(TAG, "Refreshing... 1")
        viewModelScope.launch {
            hasSdkAccess = try {
                Log.d(TAG, "Refreshing...")
                Log.d(TAG, ProxyManager.carControl?.ping() ?: "Ping no output")
                ProxyManager.carControl?.ping()?.contains("SUCCESS") ?: false
            } catch (e: Exception) {
                false
            }
        }
    }

    fun setOverrideMainDisplayId(id: Int) {
        appPrefs.overrideMainDisplayId = id
        _overrideMainDisplayId.intValue = id
    }

    fun setOverrideClusterDisplayId(id: Int) {
        appPrefs.overrideClusterDisplayId = id
        _overrideClusterDisplayId.intValue = id
    }

    fun resetDisplayOverrides() {
        appPrefs.overrideMainDisplayId = -1
        appPrefs.overrideClusterDisplayId = -1
        _overrideMainDisplayId.intValue = -1
        _overrideClusterDisplayId.intValue = -1
    }

    fun testOverlayLaunch(displayId: Int) {
        ShellCommandExecutor.grantOverlayPermission(AppConstants.PACKAGE_NAME)
        ClusterOverlayManager.testOverlayLaunch(getApplication(), displayId)
    }

    fun executeLogcat() {
        viewModelScope.launch(Dispatchers.IO) {
            isExecutingLogcat = true
            diagnosticLogs = "Executing logcat..."
            try {
                val myPid = android.os.Process.myPid()
                val pidStr = myPid.toString()
                val pkgName = "com.sr.openbyd"
                
                // Try running via privileged proxy
                var output = ShellCommandExecutor.readLogcat(myPid)
                
                if (output.isNullOrBlank()) {
                    Log.d(TAG, "Proxy returned empty logs or is disconnected. Falling back to local logcat.")
                    val process = Runtime.getRuntime().exec("logcat -d -t 1200 --pid=$myPid")
                    output = process.inputStream.bufferedReader().use { it.readText() }
                }
                
                diagnosticLogs = if (output.isNullOrBlank()) {
                    "Logcat executed successfully but returned no output."
                } else {
                    val filteredLines = output.lines().filter { line ->
                        line.contains(pkgName, ignoreCase = true) ||
                        line.contains(pidStr) ||
                        line.contains("OpenBYD", ignoreCase = true) ||
                        line.contains("ClusterOverlayManager") ||
                        line.contains("MainOverlayManager") ||
                        line.contains("ProxyManager") ||
                        line.contains("ActionExecutor") ||
                        line.contains("SteeringWheelAccessibilityService")
                    }
                    
                    if (filteredLines.isEmpty()) {
                        "No app logs found in the last 1200 lines. Try triggering some actions and executing logcat again."
                    } else {
                        filteredLines.joinToString("\n")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to execute logcat", e)
                diagnosticLogs = "Error executing logcat: ${e.localizedMessage}"
            } finally {
                isExecutingLogcat = false
            }
        }
    }
}
