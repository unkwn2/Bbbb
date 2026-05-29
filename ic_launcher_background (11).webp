package com.sr.openbyd.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sr.openbyd.data.AppDisplayConfig
import com.sr.openbyd.data.AppPreferences
import com.sr.openbyd.data.InstalledAppsRepository
import com.sr.openbyd.models.ProjectionAutomation
import com.sr.openbyd.models.ProjectionAutomationRepository
import com.sr.openbyd.proxy.ProxyManager
import com.sr.openbyd.ui.overlay.ClusterOverlayManager
import com.sr.openbyd.ui.overlay.MainOverlayManager
import com.sr.openbyd.utils.AppConstants
import com.sr.openbyd.utils.ShellCommandExecutor
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProjectionViewModel(application: Application) : AndroidViewModel(application) {
    private val appPrefs = AppPreferences(application)
    private val projectionAutomationRepository = ProjectionAutomationRepository(application)

    var currentProjectedPackage by mutableStateOf<String?>(null)
        private set

    var currentMainProjectedPackage by mutableStateOf<String?>(null)
        private set

    var installedApps by mutableStateOf<List<AppInfo>>(emptyList())
        private set

    // Main Display (UI 7 Split Screen with ADAS) Configuration
    var activeMainWidth by mutableIntStateOf(AppConstants.DEFAULT_MAIN_WIDTH)
    var activeMainHeight by mutableIntStateOf(AppConstants.DEFAULT_MAIN_HEIGHT)
    var activeMainXOffset by mutableIntStateOf(AppConstants.DEFAULT_MAIN_X_OFFSET)
    var activeMainYOffset by mutableIntStateOf(AppConstants.DEFAULT_MAIN_Y_OFFSET)
    var activeMainIsLeft by mutableStateOf(true)

    // Fullscreen Configuration
    var activeFullWidth by mutableIntStateOf(AppConstants.DEFAULT_FULL_WIDTH)
    var activeFullHeight by mutableIntStateOf(AppConstants.DEFAULT_FULL_HEIGHT)
    var activeFullXOffset by mutableIntStateOf(AppConstants.DEFAULT_FULL_X_OFFSET)
    var activeFullYOffset by mutableIntStateOf(AppConstants.DEFAULT_FULL_Y_OFFSET)

    // Mini Mode Configuration
    var activeMiniWidth by mutableIntStateOf(AppConstants.DEFAULT_MINI_WIDTH)
    var activeMiniHeight by mutableIntStateOf(AppConstants.DEFAULT_MINI_HEIGHT)
    var activeMiniXOffset by mutableIntStateOf(AppConstants.DEFAULT_MINI_X_OFFSET)
    var activeMiniYOffset by mutableIntStateOf(AppConstants.DEFAULT_MINI_Y_OFFSET)

    // DPI Configuration
    var activeMainDpi by mutableIntStateOf(240)
    var activeFullDpi by mutableIntStateOf(320)
    var activeMiniDpi by mutableIntStateOf(320)

    // App projection automations
    var activeProjectionAutomations by mutableStateOf<List<ProjectionAutomation>>(emptyList())
        private set

    init {
        loadInstalledApps()

        // Observe cluster projection state
        viewModelScope.launch {
            ClusterOverlayManager.currentProjectedPackage.collectLatest { pkg ->
                currentProjectedPackage = pkg
            }
        }

        // Observe main screen projection state
        viewModelScope.launch {
            MainOverlayManager.currentProjectedPackage.collectLatest { pkg ->
                currentMainProjectedPackage = pkg
            }
        }
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            installedApps = InstalledAppsRepository.getInstance(getApplication()).getInstalledApps()
        }
    }

    fun grantOverlayPermission() {
        ShellCommandExecutor.grantOverlayPermission(AppConstants.PACKAGE_NAME)
    }

    fun loadConfigsFor(packageName: String) {
        // Load Fullscreen config
        val fullConfig = appPrefs.getAppDisplayConfig(packageName, false, ClusterOverlayManager.clusterWidth, ClusterOverlayManager.clusterHeight)
        activeFullWidth = fullConfig.renderWidth
        activeFullHeight = fullConfig.renderHeight
        activeFullXOffset = fullConfig.xOffset
        activeFullYOffset = fullConfig.yOffset
        activeFullDpi = fullConfig.densityDpi

        // Load Mini config
        val miniConfig = appPrefs.getAppDisplayConfig(packageName, true, ClusterOverlayManager.clusterWidth, ClusterOverlayManager.clusterHeight)
        activeMiniWidth = miniConfig.renderWidth
        activeMiniHeight = miniConfig.renderHeight
        activeMiniXOffset = miniConfig.xOffset
        activeMiniYOffset = miniConfig.yOffset
        activeMiniDpi = miniConfig.densityDpi

        // Load Main config
        val mainConfig = appPrefs.getMainDisplayConfig(packageName)
        activeMainWidth = mainConfig.renderWidth
        activeMainHeight = mainConfig.renderHeight
        activeMainXOffset = mainConfig.xOffset
        activeMainYOffset = mainConfig.yOffset
        activeMainDpi = mainConfig.densityDpi
        activeMainIsLeft = appPrefs.getMainDisplayProjectionSideLeft(packageName)

        // Load automations
        loadAutomationsFor(packageName)
    }

    // Fullscreen updates
    fun updateFullWidth(packageName: String, width: Int) {
        if (width < 1) return
        activeFullWidth = width
        saveFullConfig(packageName)
    }
    fun updateFullHeight(packageName: String, height: Int) {
        if (height < 1) return
        activeFullHeight = height
        saveFullConfig(packageName)
    }
    fun updateFullXOffset(packageName: String, offset: Int) {
        activeFullXOffset = offset
        saveFullConfig(packageName)
    }
    fun updateFullYOffset(packageName: String, offset: Int) {
        activeFullYOffset = offset
        saveFullConfig(packageName)
    }
    fun updateFullDpi(packageName: String, dpi: Int) {
        if (dpi < 1) return
        activeFullDpi = dpi
        saveFullConfig(packageName)
    }
    private fun saveFullConfig(packageName: String) {
        appPrefs.saveAppDisplayConfig(packageName, false, AppDisplayConfig(activeFullWidth, activeFullHeight, activeFullXOffset, activeFullYOffset, activeFullDpi))
    }

    // Mini updates
    fun updateMiniWidth(packageName: String, width: Int) {
        if (width < 1) return
        activeMiniWidth = width
        saveMiniConfig(packageName)
    }
    fun updateMiniHeight(packageName: String, height: Int) {
        if (height < 1) return
        activeMiniHeight = height
        saveMiniConfig(packageName)
    }
    fun updateMiniXOffset(packageName: String, offset: Int) {
        activeMiniXOffset = offset
        saveMiniConfig(packageName)
    }
    fun updateMiniYOffset(packageName: String, offset: Int) {
        activeMiniYOffset = offset
        saveMiniConfig(packageName)
    }
    fun updateMiniDpi(packageName: String, dpi: Int) {
        if (dpi < 1) return
        activeMiniDpi = dpi
        saveMiniConfig(packageName)
    }
    private fun saveMiniConfig(packageName: String) {
        appPrefs.saveAppDisplayConfig(packageName, true, AppDisplayConfig(activeMiniWidth, activeMiniHeight, activeMiniXOffset, activeMiniYOffset, activeMiniDpi))
    }

    fun castAppToCluster(packageName: String) {
        grantOverlayPermission()
        ClusterOverlayManager.castAppToCluster(getApplication(), packageName)
    }

    fun refreshClusterProjection(packageName: String) {
        grantOverlayPermission()
        ClusterOverlayManager.refreshClusterProjection(getApplication(), packageName)
    }

    fun pullAppBackToCluster(packageName: String) {
        ClusterOverlayManager.pullAppBackToMain(packageName)
    }

    fun pullAppBackToMain(packageName: String) {
        pullAppBackToCluster(packageName)
    }

    fun resetAppConfig(packageName: String) {
        appPrefs.resetAppConfig(packageName)
        loadConfigsFor(packageName)
    }

    fun updateMainProjectionSideLeft(packageName: String, isLeft: Boolean) {
        activeMainIsLeft = isLeft
        appPrefs.saveMainDisplayProjectionSideLeft(packageName, isLeft)
        val newX = if (isLeft) AppConstants.DEFAULT_MAIN_X_OFFSET else AppConstants.DEFAULT_MAIN_X_OFFSET_RIGHT
        updateMainXOffset(packageName, newX)
    }

    // Main updates
    fun updateMainWidth(packageName: String, width: Int) {
        if (width < 1) return
        activeMainWidth = width
        saveMainConfig(packageName)
    }
    fun updateMainHeight(packageName: String, height: Int) {
        if (height < 1) return
        activeMainHeight = height
        saveMainConfig(packageName)
    }
    fun updateMainXOffset(packageName: String, offset: Int) {
        activeMainXOffset = offset
        saveMainConfig(packageName)
    }
    fun updateMainYOffset(packageName: String, offset: Int) {
        activeMainYOffset = offset
        saveMainConfig(packageName)
    }
    fun updateMainDpi(packageName: String, dpi: Int) {
        if (dpi < 1) return
        activeMainDpi = dpi
        saveMainConfig(packageName)
    }
    private fun saveMainConfig(packageName: String) {
        appPrefs.saveMainDisplayConfig(packageName, AppDisplayConfig(activeMainWidth, activeMainHeight, activeMainXOffset, activeMainYOffset, activeMainDpi))
    }

    fun applyMainAspectRatioPreset(packageName: String) {
        activeMainWidth = AppConstants.DEFAULT_MAIN_WIDTH
        activeMainHeight = AppConstants.DEFAULT_MAIN_HEIGHT
        val isLeft = appPrefs.getMainDisplayProjectionSideLeft(packageName)
        activeMainXOffset = if (isLeft) AppConstants.DEFAULT_MAIN_X_OFFSET else AppConstants.DEFAULT_MAIN_X_OFFSET_RIGHT
        activeMainYOffset = AppConstants.DEFAULT_MAIN_Y_OFFSET
        val mainSpecs = appPrefs.getPhysicalMainSpecs()
        activeMainDpi = mainSpecs.densityDpi
        saveMainConfig(packageName)
    }

    fun testMainOverlayLaunch() {
        grantOverlayPermission()
        MainOverlayManager.testOverlayLaunch(getApplication())
    }

    fun castAppToMainScreen(packageName: String) {
        grantOverlayPermission()
        MainOverlayManager.castAppToMainScreen(getApplication(), packageName)
    }

    fun pullAppBackToMainScreen(packageName: String) {
        MainOverlayManager.pullAppBackToMain(packageName)
    }

    // App Projection Automations
    fun loadAutomationsFor(packageName: String) {
        activeProjectionAutomations = projectionAutomationRepository.getForApp(packageName)
    }

    fun addProjectionAutomation(packageName: String, action: ProjectionAutomation) {
        val current = projectionAutomationRepository.getForApp(packageName).toMutableList()
        val newAction = action.copy(index = current.size)
        current.add(newAction)
        projectionAutomationRepository.saveForApp(packageName, current)
        loadAutomationsFor(packageName)
    }

    fun updateProjectionAutomation(packageName: String, action: ProjectionAutomation) {
        val current = projectionAutomationRepository.getForApp(packageName).toMutableList()
        if (action.index in current.indices) {
            current[action.index] = action
            projectionAutomationRepository.saveForApp(packageName, current)
            loadAutomationsFor(packageName)
        }
    }

    fun deleteProjectionAutomation(packageName: String, index: Int) {
        val current = projectionAutomationRepository.getForApp(packageName).toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            val reindexed = current.mapIndexed { i, a -> a.copy(index = i) }
            projectionAutomationRepository.saveForApp(packageName, reindexed)
            loadAutomationsFor(packageName)
        }
    }
}
