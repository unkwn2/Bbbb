package com.sr.openbyd.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.sr.openbyd.ipc.ICarControl
import com.sr.openbyd.ipc.ProxyBinderParcelable
import com.sr.openbyd.proxy.ProxyManager

class ProxyReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "ProxyReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.sr.openbyd.PROXY_CONNECTED") {
            Log.d(TAG, "Received proxy connected broadcast!")
            val parcelable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra("proxy_binder", ProxyBinderParcelable::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra("proxy_binder")
            }

            parcelable?.binder?.let { binder ->
                val carControl = ICarControl.Stub.asInterface(binder)
                Log.d(TAG, "Successfully extracted ICarControl binder.")
                ProxyManager.onProxyConnected(context, carControl)
            } ?: run {
                Log.e(TAG, "Parcelable or binder was null.")
            }
        }
    }
}
