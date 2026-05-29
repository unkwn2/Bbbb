package com.sr.openbyd.data

import android.content.Context
import android.content.SharedPreferences
import com.sr.openbyd.R
import com.sr.openbyd.utils.AppConstants
import com.sr.openbyd.utils.DiLinkGeneration
import com.sr.openbyd.utils.DiLinkHelper
import com.sr.openbyd.utils.ScreenParams

enum class ThemeMode(val labelRes: Int) {
    DEFAULT(R.string.theme_default),
    LIGHT(R.string.theme_light),
    DARK(R.string.theme_dark)
}

enum class ProjectionGradientMode(val labelRes: Int) {
    AUTO(R.string.projection_gradient_mode_auto),
    DARK(R.string.projection_gradient_mode_dark),
    LIGHT(R.string.projection_gradient_mode_light)
}

enum class LanguageMode(val code: String, val label: String) {
    ENGLISH("en", "English"),
    SPANISH("es", "Español"),
    RUSSIAN("ru", "Русский"),
    PORTUGUESE("pt", "Português"),
    CHINESE("zh", "中文")
}

enum class SplitScreenMode(val labelRes: Int) {
    NATIVE(R.string.split_screen_mode_native),
    COMPAT_TAP(R.string.split_screen_mode_compat_tap),
    PROJECTED(R.string.split_screen_mode_projected),
    NATIVE_UI7_DILINK_6(R.string.split_screen_mode_dilink6_native)
}

enum class DiLinkMode(val labelRes: Int) {
    AUTO(R.string.dilink_mode_auto),
    FORCE_UI_7(R.string.dilink_mode_7),
    FORCE_DILINK_5(R.string.dilink_mode_5)
}

enum class ClusterLayoutMode(val labelRes: Int) {
    AUTO(R.string.cluster_layout_auto),
    FORCE_MINI(R.string.cluster_layout_force_mini),
    FORCE_FULLSCREEN(R.string.cluster_layout_force_fullscreen)
}

data class AppDisplayConfig(
    val renderWidth: Int,
    val renderHeight: Int,
    val xOffset: Int,
    val yOffset: Int,
    val densityDpi: Int
)

class AppPreferences(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

    var themeMode: ThemeMode
        get() = ThemeMode.valueOf(prefs.getString("theme_mode", ThemeMode.DEFAULT.name) ?: ThemeMode.DEFAULT.name)
        set(value) = prefs.edit().putString("theme_mode", value.name).apply()

    var languageMode: LanguageMode
        get() = LanguageMode.valueOf(prefs.getString("language_mode", LanguageMode.ENGLISH.name) ?: LanguageMode.ENGLISH.name)
        set(value) = prefs.edit().putString("language_mode", value.name).apply()

    var isMiniMode: Boolean
        get() = prefs.getBoolean("is_mini_mode", false)
        set(value) = prefs.edit().putBoolean("is_mini_mode", value).apply()

    var cachedDiLinkGeneration: String?
        get() = prefs.getString("cached_dilink_generation", null)
        set(value) = prefs.edit().putString("cached_dilink_generation", value).apply()

    var splitScreenMode: SplitScreenMode
        get() {
            val savedMode = prefs.getString("split_screen_mode", null)
            if (savedMode != null) {
                try {
                    return SplitScreenMode.valueOf(savedMode)
                } catch (e: Exception) {
                    // Fallback to default
                }
            }
            val defaultMode = if (DiLinkHelper.getDiLinkGeneration(this) == DiLinkGeneration.UI_7) {
                SplitScreenMode.NATIVE_UI7_DILINK_6
            } else {
                SplitScreenMode.NATIVE
            }
            return defaultMode
        }
        set(value) = prefs.edit().putString("split_screen_mode", value.name).apply()

    var diLinkMode: DiLinkMode
        get() {
            val raw = prefs.getString("dilink_mode", DiLinkMode.AUTO.name) ?: DiLinkMode.AUTO.name
            return if (raw == "FORCE_DILINK_6") DiLinkMode.FORCE_UI_7 else DiLinkMode.valueOf(raw)
        }
        set(value) = prefs.edit().putString("dilink_mode", value.name).apply()

    var clusterLayoutMode: ClusterLayoutMode
        get() = ClusterLayoutMode.valueOf(prefs.getString("cluster_layout_mode", ClusterLayoutMode.AUTO.name) ?: ClusterLayoutMode.AUTO.name)
        set(value) = prefs.edit().putString("cluster_layout_mode", value.name).apply()

    var projectionGradientsEnabled: Boolean
        get() = prefs.getBoolean("projection_gradients_enabled", false)
        set(value) = prefs.edit().putBoolean("projection_gradients_enabled", value).apply()

    var projectionGradientMode: ProjectionGradientMode
        get() = ProjectionGradientMode.valueOf(prefs.getString("projection_gradient_mode", ProjectionGradientMode.AUTO.name) ?: ProjectionGradientMode.AUTO.name)
        set(value) = prefs.edit().putString("projection_gradient_mode", value.name).apply()

    var projectionGradientIntensity: Int
        get() = prefs.getInt("projection_gradient_intensity", 80)
        set(value) = prefs.edit().putInt("projection_gradient_intensity", value).apply()

    var projectionLeftGradientEnabled: Boolean
        get() = prefs.getBoolean("projection_left_gradient_enabled", false)
        set(value) = prefs.edit().putBoolean("projection_left_gradient_enabled", value).apply()

    var overrideMainDisplayId: Int
        get() = prefs.getInt("override_main_display_id", -1)
        set(value) = prefs.edit().putInt("override_main_display_id", value).apply()

    var overrideClusterDisplayId: Int
        get() = prefs.getInt("override_cluster_display_id", -1)
        set(value) = prefs.edit().putInt("override_cluster_display_id", value).apply()

    var keyCodesInitialized: Boolean
        get() = prefs.getBoolean("key_codes_initialized", false)
        set(value) = prefs.edit().putBoolean("key_codes_initialized", value).apply()

    var splitScreenCompatTapX: Int
        get() = prefs.getInt("split_screen_compat_tap_x", 831)
        set(value) = prefs.edit().putInt("split_screen_compat_tap_x", value).apply()

    var splitScreenCompatTapY: Int
        get() = prefs.getInt("split_screen_compat_tap_y", 1031)
        set(value) = prefs.edit().putInt("split_screen_compat_tap_y", value).apply()

    var runStartupOnConnect: Boolean
        get() = prefs.getBoolean("run_startup_on_connect", false)
        set(value) = prefs.edit().putBoolean("run_startup_on_connect", value).apply()

    fun getPhysicalMainSpecs(): ScreenParams {
        return ScreenParams.getMetrics(context)
    }

    fun getPhysicalClusterSpecs(): ScreenParams {
        return ScreenParams.getClusterMetrics(context)
    }

    fun getAppDisplayConfig(
        packageName: String,
        isMiniMode: Boolean,
        defaultW: Int? = null,
        defaultH: Int? = null,
        defaultDpi: Int? = null
    ): AppDisplayConfig {
        val prefix = if (isMiniMode) "mini" else "full"
        val clusterSpecs = getPhysicalClusterSpecs()

        val dW = if (isMiniMode) AppConstants.DEFAULT_MINI_WIDTH else (defaultW ?: clusterSpecs.width)
        val dH = if (isMiniMode) AppConstants.DEFAULT_MINI_HEIGHT else (defaultH ?: clusterSpecs.height)
        val dX = if (isMiniMode) AppConstants.DEFAULT_MINI_X_OFFSET else AppConstants.DEFAULT_FULL_X_OFFSET
        val dY = if (isMiniMode) AppConstants.DEFAULT_MINI_Y_OFFSET else AppConstants.DEFAULT_FULL_Y_OFFSET
        val dDpi = if (isMiniMode) AppConstants.DEFAULT_MINI_DPI else (defaultDpi ?: clusterSpecs.densityDpi)

        val width = prefs.getInt("${prefix}_mode_width_$packageName", dW)
        val height = prefs.getInt("${prefix}_mode_height_$packageName", dH)
        val xOffset = prefs.getInt("${prefix}_mode_x_offset_$packageName", dX)
        val yOffset = prefs.getInt("${prefix}_mode_y_offset_$packageName", dY)
        val densityDpi = prefs.getInt("${prefix}_mode_density_dpi_$packageName", dDpi)
        return AppDisplayConfig(width, height, xOffset, yOffset, densityDpi)
    }

    fun saveAppDisplayConfig(packageName: String, isMiniMode: Boolean, config: AppDisplayConfig) {
        val prefix = if (isMiniMode) "mini" else "full"
        prefs.edit().apply {
            putInt("${prefix}_mode_width_$packageName", config.renderWidth)
            putInt("${prefix}_mode_height_$packageName", config.renderHeight)
            putInt("${prefix}_mode_x_offset_$packageName", config.xOffset)
            putInt("${prefix}_mode_y_offset_$packageName", config.yOffset)
            putInt("${prefix}_mode_density_dpi_$packageName", config.densityDpi)
            apply()
        }
    }

    fun getMainDisplayProjectionSideLeft(packageName: String): Boolean {
        return prefs.getBoolean("main_mode_is_left_$packageName", true)
    }

    fun saveMainDisplayProjectionSideLeft(packageName: String, isLeft: Boolean) {
        prefs.edit().putBoolean("main_mode_is_left_$packageName", isLeft).apply()
    }

    fun getMainDisplayConfig(
        packageName: String,
        defaultW: Int? = null,
        defaultH: Int? = null,
        defaultDpi: Int? = null
    ): AppDisplayConfig {
        val mainSpecs = getPhysicalMainSpecs()
        val isLeft = getMainDisplayProjectionSideLeft(packageName)

        // Calculate dynamic defaults
        val dW = defaultW ?: ((mainSpecs.width * 1275) / 1920)
        val dH = defaultH ?: (mainSpecs.height - AppConstants.TOTAL_BAR_HEIGHTS)
        val defaultX = if (isLeft) 0 else (mainSpecs.width - dW)
        val defaultY = AppConstants.STATUS_BAR_HEIGHT
        val dDpi = defaultDpi ?: mainSpecs.densityDpi

        val width = prefs.getInt("main_mode_width_$packageName", dW)
        val height = prefs.getInt("main_mode_height_$packageName", dH)
        val xOffset = prefs.getInt("main_mode_x_offset_$packageName", defaultX)
        val yOffset = prefs.getInt("main_mode_y_offset_$packageName", defaultY)
        val densityDpi = prefs.getInt("main_mode_density_dpi_$packageName", dDpi)
        return AppDisplayConfig(width, height, xOffset, yOffset, densityDpi)
    }

    fun saveMainDisplayConfig(packageName: String, config: AppDisplayConfig) {
        prefs.edit().apply {
            putInt("main_mode_width_$packageName", config.renderWidth)
            putInt("main_mode_height_$packageName", config.renderHeight)
            putInt("main_mode_x_offset_$packageName", config.xOffset)
            putInt("main_mode_y_offset_$packageName", config.yOffset)
            putInt("main_mode_density_dpi_$packageName", config.densityDpi)
            apply()
        }
    }

    fun resetAppConfig(packageName: String) {
        prefs.edit().apply {
            remove("mini_mode_width_$packageName")
            remove("mini_mode_height_$packageName")
            remove("mini_mode_x_offset_$packageName")
            remove("mini_mode_y_offset_$packageName")
            remove("mini_mode_density_dpi_$packageName")
            remove("full_mode_width_$packageName")
            remove("full_mode_height_$packageName")
            remove("full_mode_x_offset_$packageName")
            remove("full_mode_y_offset_$packageName")
            remove("full_mode_density_dpi_$packageName")
            remove("main_mode_is_left_$packageName")
            remove("main_mode_width_$packageName")
            remove("main_mode_height_$packageName")
            remove("main_mode_x_offset_$packageName")
            remove("main_mode_y_offset_$packageName")
            remove("main_mode_density_dpi_$packageName")
            apply()
        }
    }
}
