package com.sr.openbyd.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sr.openbyd.MainActivity
import com.sr.openbyd.data.AppPreferences
import com.sr.openbyd.data.InstalledAppsRepository
import com.sr.openbyd.models.ActionType
import com.sr.openbyd.models.ButtonMapping
import com.sr.openbyd.models.ButtonMappingRepository
import kotlinx.coroutines.launch

class SteeringWheelViewModel(application: Application) : AndroidViewModel(application) {
    private val mappingRepository = ButtonMappingRepository(application)
    private val appPrefs = AppPreferences(application)

    var activeKeyCodes by mutableStateOf<List<Int>>(emptyList())
        private set

    var installedApps by mutableStateOf<List<AppInfo>>(emptyList())
        private set

    init {
        loadActiveKeyCodes()
        loadInstalledApps()
    }

    fun loadActiveKeyCodes() {
        if (!appPrefs.keyCodesInitialized) {
            val defaults = MainActivity.KNOWN_KEYCODES.keys
            val configured = mappingRepository.getAllConfiguredKeyCodes()
            defaults.forEach { keyCode ->
                if (!configured.contains(keyCode)) {
                    mappingRepository.saveMapping(ButtonMapping(keyCode, ActionType.NONE))
                }
            }
            appPrefs.keyCodesInitialized = true
        }

        val finalConfigured = mappingRepository.getAllConfiguredKeyCodes()
        activeKeyCodes = finalConfigured.toList().sorted()
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            installedApps = InstalledAppsRepository.getInstance(getApplication()).getInstalledApps()
        }
    }

    fun saveMapping(mapping: ButtonMapping) {
        mappingRepository.saveMapping(mapping)
        loadActiveKeyCodes()
    }

    fun deleteMapping(keyCode: Int) {
        mappingRepository.deleteMapping(keyCode)
        loadActiveKeyCodes()
    }

    fun getMapping(keyCode: Int): ButtonMapping {
        return mappingRepository.getMapping(keyCode)
    }
}
