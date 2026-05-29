package com.sr.openbyd.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.sr.openbyd.data.AppPreferences
import com.sr.openbyd.proxy.ProxyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Receives [Intent.ACTION_BOOT_COMPLETED] (and the HTC/Huawei quick-boot variant)
 * so Open BYD automatically starts the background proxy when the car powers on.
 *
 * On boot:
 * 1. Starts the ADB proxy ([com.sr.openbyd.proxy.ProxyManager.startProxy]).
 * 2. The proxy, once connected, auto-re-enables the accessibility service if the user
 *    previously enabled it.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                Log.i(TAG, "Boot completed - setting boot flag and launching Proxy")
                val prefs = AppPreferences(context)
                prefs.runStartupOnConnect = true
                
                // Keep the receiver alive long enough for the proxy script to run
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        ProxyManager.startProxy(context.applicationContext)
                        // Give it 3 seconds to finish sending the command over local ADB
                        delay(3000)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
