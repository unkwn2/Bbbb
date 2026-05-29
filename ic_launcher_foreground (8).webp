package com.sr.openbyd.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.sr.openbyd.WindowState
import com.sr.openbyd.WindowType
import com.sr.openbyd.proxy.ProxyManager

class CarControlsViewModel(application: Application) : AndroidViewModel(application) {
    fun setAcPower(isOn: Boolean): String? {
        return ProxyManager.carControl?.setAcPower(isOn)
    }

    fun setWindow(windowType: WindowType, state: WindowState): String? {
        return ProxyManager.carControl?.setWindow(windowType.id, state.value)
    }
}
