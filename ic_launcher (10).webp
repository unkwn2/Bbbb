package com.sr.openbyd.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sr.openbyd.R
import com.sr.openbyd.data.AppPreferences
import com.sr.openbyd.data.ClusterLayoutMode
import com.sr.openbyd.data.DiLinkMode
import com.sr.openbyd.data.LanguageMode
import com.sr.openbyd.data.ProjectionGradientMode
import com.sr.openbyd.data.SplitScreenMode
import com.sr.openbyd.data.ThemeMode
import com.sr.openbyd.proxy.ProxyManager
import com.sr.openbyd.ui.overlay.ClusterOverlayManager
import com.sr.openbyd.utils.AppConstants
import com.sr.openbyd.utils.DiLinkGeneration
import com.sr.openbyd.utils.DiLinkHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val TAG = "MainViewModel"

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val appPrefs = AppPreferences(application)

    var currentThemeMode by mutableStateOf(appPrefs.themeMode)
        private set

    var currentLanguageMode by mutableStateOf(appPrefs.languageMode)
        private set

    var isMiniMode by mutableStateOf(appPrefs.isMiniMode)
        private set

    var splitScreenMode by mutableStateOf(appPrefs.splitScreenMode)
        private set

    var splitScreenCompatTapX by mutableIntStateOf(appPrefs.splitScreenCompatTapX)
        private set

    var splitScreenCompatTapY by mutableIntStateOf(appPrefs.splitScreenCompatTapY)
        private set

    var diLinkMode by mutableStateOf(appPrefs.diLinkMode)
        private set

    var clusterLayoutMode by mutableStateOf(appPrefs.clusterLayoutMode)
        private set

    var projectionGradientsEnabled by mutableStateOf(appPrefs.projectionGradientsEnabled)
        private set

    var projectionGradientMode by mutableStateOf(appPrefs.projectionGradientMode)
        private set

    var projectionGradientIntensity by mutableIntStateOf(appPrefs.projectionGradientIntensity)
        private set

    var projectionLeftGradientEnabled by mutableStateOf(appPrefs.projectionLeftGradientEnabled)
        private set

    val diLinkStatusRes: Int
        get() {
            return when (diLinkMode) {
                DiLinkMode.FORCE_UI_7 -> R.string.dilink_status_dilink6_forced
                DiLinkMode.FORCE_DILINK_5 -> R.string.dilink_status_dilink5_forced
                DiLinkMode.AUTO -> {
                    val prop = ProxyManager.carControl?.getSystemProperty("ro.vehicle.type")
                    if (prop.isNullOrBlank()) {
                        R.string.dilink_status_proxy_offline
                    } else {
                        val gen = DiLinkHelper.getDiLinkGeneration(appPrefs)
                        if (gen == DiLinkGeneration.UI_7) R.string.dilink_status_dilink6_auto else R.string.dilink_status_dilink5_auto
                    }
                }
            }
        }

    var lastPressedKeyCode by mutableStateOf<Int?>(null)
    var showDebugDialog by mutableStateOf(false)
    var showDiagnosticDialog by mutableStateOf(false)
    var showCarControlsDialog by mutableStateOf(false)

    init {
        ClusterOverlayManager.getClusterDisplayId(application)

        // Asynchronously detect and cache DiLink generation on startup, then update split screen mode reactive state
        viewModelScope.launch(Dispatchers.IO) {
            DiLinkHelper.refreshDiLinkGeneration(appPrefs)
        }

        // Delayed check to alert the user if proxy is not running
        viewModelScope.launch(Dispatchers.Main) {
            delay(AppConstants.PROXY_CHECK_DELAY_MS) // Wait for local ADB connection and proxy handshake
            if (ProxyManager.carControl == null) {
                android.widget.Toast.makeText(
                    getApplication(),
                    "ADB proxy is not running!\nPlease make sure ADB debugging is enabled and authorized for the app, otherwise most functionalities will not work.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun updateTheme(mode: ThemeMode) {
        appPrefs.themeMode = mode
        currentThemeMode = mode
    }

    fun updateLanguage(lang: LanguageMode) {
        appPrefs.languageMode = lang
        currentLanguageMode = lang
    }

    fun updateMiniMode(enabled: Boolean) {
        appPrefs.isMiniMode = enabled
        isMiniMode = enabled
    }

    fun updateSplitScreenMode(mode: SplitScreenMode) {
        appPrefs.splitScreenMode = mode
        splitScreenMode = mode
    }

    fun updateSplitScreenCompatTapX(x: Int) {
        appPrefs.splitScreenCompatTapX = x
        splitScreenCompatTapX = x
    }

    fun updateSplitScreenCompatTapY(y: Int) {
        appPrefs.splitScreenCompatTapY = y
        splitScreenCompatTapY = y
    }

    fun updateDiLinkMode(mode: DiLinkMode) {
        appPrefs.diLinkMode = mode
        diLinkMode = mode
    }

    fun updateClusterLayoutMode(mode: ClusterLayoutMode) {
        appPrefs.clusterLayoutMode = mode
        clusterLayoutMode = mode
    }

    fun updateProjectionGradientsEnabled(enabled: Boolean) {
        appPrefs.projectionGradientsEnabled = enabled
        projectionGradientsEnabled = enabled
        // Re-project the current app live if custom overlays are active
        val pkg = ClusterOverlayManager.currentProjectedPackage.value
        if (pkg != null) {
            ClusterOverlayManager.castAppToCluster(getApplication(), pkg)
        }
    }

    fun updateProjectionGradientMode(mode: ProjectionGradientMode) {
        appPrefs.projectionGradientMode = mode
        projectionGradientMode = mode
        // Re-project the current app live if custom overlays are active
        val pkg = ClusterOverlayManager.currentProjectedPackage.value
        if (pkg != null) {
            ClusterOverlayManager.castAppToCluster(getApplication(), pkg)
        }
    }

    fun updateProjectionGradientIntensity(intensity: Int) {
        appPrefs.projectionGradientIntensity = intensity
        projectionGradientIntensity = intensity
        // Re-project the current app live if custom overlays are active
        val pkg = ClusterOverlayManager.currentProjectedPackage.value
        if (pkg != null) {
            ClusterOverlayManager.castAppToCluster(getApplication(), pkg)
        }
    }

    fun updateProjectionLeftGradientEnabled(enabled: Boolean) {
        appPrefs.projectionLeftGradientEnabled = enabled
        projectionLeftGradientEnabled = enabled
        // Re-project the current app live if custom overlays are active
        val pkg = ClusterOverlayManager.currentProjectedPackage.value
        if (pkg != null) {
            ClusterOverlayManager.castAppToCluster(getApplication(), pkg)
        }
    }
}
