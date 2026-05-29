package com.sr.openbyd.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sr.openbyd.data.InstalledAppsRepository
import com.sr.openbyd.models.StartupAction
import com.sr.openbyd.models.StartupActionRepository
import com.sr.openbyd.services.ActionExecutor
import kotlinx.coroutines.launch

class StartupViewModel(application: Application) : AndroidViewModel(application) {
    private val startupRepository = StartupActionRepository(application)

    var startupActions by mutableStateOf<List<StartupAction>>(emptyList())
        private set

    var installedApps by mutableStateOf<List<AppInfo>>(emptyList())
        private set

    init {
        loadStartupActions()
        loadInstalledApps()
    }

    fun loadStartupActions() {
        startupActions = startupRepository.getAll()
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            installedApps = InstalledAppsRepository.getInstance(getApplication()).getInstalledApps()
        }
    }

    fun addStartupAction(action: StartupAction) {
        startupRepository.append(action)
        loadStartupActions()
    }

    fun updateStartupAction(action: StartupAction) {
        startupRepository.update(action)
        loadStartupActions()
    }

    fun deleteStartupAction(index: Int) {
        startupRepository.delete(index)
        loadStartupActions()
    }

    fun runStartupSequence() {
        viewModelScope.launch {
            ActionExecutor.runStartupSequence(getApplication<Application>())
        }
    }
}
