package com.sr.openbyd.ui.theme

import android.app.Activity
import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.sr.openbyd.data.AppPreferences
import com.sr.openbyd.data.ThemeMode
import com.sr.openbyd.proxy.ProxyManager
import com.sr.openbyd.utils.DiLinkGeneration
import com.sr.openbyd.utils.DiLinkHelper
import com.sr.openbyd.utils.ShellCommandExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val DarkColorScheme = darkColorScheme(
    primary = CyanNeon,
    secondary = SlateGrey,
    tertiary = ElectricBlue,
    background = DeepObsidian,
    surface = DarkSlate,
    onPrimary = Color(0xFF070B13),
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF1F5F9)
)

private val LightColorScheme = lightColorScheme(
    primary = IndigoDeep,
    secondary = SlateLight,
    tertiary = CyanDark,
    background = PureIce,
    surface = SurfaceWhite,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A)
)



private const val THEME_TAG = "OpenBYD_Theme"

/**
 * Cached result from the last proxy-based theme probe.
 * Updated asynchronously by [probeThemeViaProxy] or Settings ContentObserver.
 * Declared as a delegated Compose State to automatically trigger recomposition when updated.
 */
var cachedProxyDarkTheme: Boolean? by mutableStateOf(null)
    internal set

/**
 * Detects whether the system is in dark theme.
 *
 * For non-UI7 vehicles, standard system theme detection is used.
 * For UI7 vehicles, standard Android [Configuration.uiMode] is stuck, so we must
 * use the proxy / Settings.Secure `key_byd_night_mode` to read the true theme state:
 *   - `1` = **Dark** (night mode)
 *   - `2` = **Light** (day mode)
 */
fun isSystemDarkTheme(context: Context): Boolean {
    val appPrefs = AppPreferences(context)
    val isUi7 = DiLinkHelper.getDiLinkGeneration(appPrefs) == DiLinkGeneration.UI_7

    if (isUi7) {
        Log.d(THEME_TAG, "isSystemDarkTheme: UI7 vehicle detected, using BYD proxy theme detection")
        // ── 1. BYD proprietary: key_byd_night_mode (1 = Dark, 2 = Light) ──
        // On some firmware/vehicles, this is in Settings.System, on others in Settings.Secure or Settings.Global
        try {
            var bydNightMode = Settings.System.getInt(context.contentResolver, "key_byd_night_mode", -1)
            if (bydNightMode == -1) {
                bydNightMode = Settings.Secure.getInt(context.contentResolver, "key_byd_night_mode", -1)
            }
            if (bydNightMode == -1) {
                bydNightMode = Settings.Global.getInt(context.contentResolver, "key_byd_night_mode", -1)
            }
            Log.d(THEME_TAG, "isSystemDarkTheme (UI7): key_byd_night_mode=$bydNightMode (1=Dark, 2=Light)")
            if (bydNightMode == 1) return true   // Dark / Night
            if (bydNightMode == 2) return false  // Light / Day
        } catch (e: Exception) {
            Log.d(THEME_TAG, "isSystemDarkTheme (UI7): key_byd_night_mode threw: ${e.message}")
        }

        // ── 2. Proxy cache (populated async by probeThemeViaProxy) ──
        cachedProxyDarkTheme?.let { proxyDark ->
            Log.d(THEME_TAG, "isSystemDarkTheme (UI7) → $proxyDark (proxy cache)")
            return proxyDark
        }
        Log.d(THEME_TAG, "isSystemDarkTheme (UI7): proxy cache is null (not yet probed)")

        // ── 3. Configuration.uiMode fallback ──
        val uiMode = context.resources.configuration.uiMode
        val nightMask = uiMode and Configuration.UI_MODE_NIGHT_MASK
        val result = nightMask == Configuration.UI_MODE_NIGHT_YES
        Log.d(THEME_TAG, "isSystemDarkTheme (UI7 fallback) → $result")
        return result
    } else {
        // Non-UI7: Use standard system/configuration theme detection
        val uiMode = context.resources.configuration.uiMode
        val nightMask = uiMode and Configuration.UI_MODE_NIGHT_MASK
        val result = nightMask == Configuration.UI_MODE_NIGHT_YES
        Log.d(THEME_TAG, "isSystemDarkTheme (Non-UI7 system theme) → $result")
        return result
    }
}

/**
 * Probes theme/night mode via the privileged proxy (UID 2000).
 * Reads `key_byd_night_mode` from Settings (checking system, secure, and global tables) and caches the result.
 *
 * **Must be called from a background thread** (makes blocking IPC calls).
 */
fun probeThemeViaProxy(context: Context) {
    val appPrefs = AppPreferences(context)
    val isUi7 = DiLinkHelper.getDiLinkGeneration(appPrefs) == DiLinkGeneration.UI_7
    if (!isUi7) {
        Log.d(THEME_TAG, "probeThemeViaProxy: non-UI7 vehicle, skipping proxy theme probe")
        return
    }

    val control = ProxyManager.carControl
    if (control == null) {
        Log.w(THEME_TAG, "probeThemeViaProxy: ProxyManager.carControl is null, skipping")
        return
    }

    try {
        var value: String? = null
        // Query system first, then secure, then global settings tables
        for (table in listOf("system", "secure", "global")) {
            val parsed = ShellCommandExecutor.getSettingsValue(table, "key_byd_night_mode")
            if (parsed != null && parsed != "null" && parsed.isNotEmpty()) {
                value = parsed
                Log.d(THEME_TAG, "probeThemeViaProxy — Found key_byd_night_mode='$value' in table '$table'")
                break
            }
        }

        Log.d(THEME_TAG, "probeThemeViaProxy — Parsed key_byd_night_mode='$value'")

        // key_byd_night_mode: 1 = Dark/Night, 2 = Light/Day
        val isDark = when (value) {
            "1" -> true
            "2" -> false
            else -> null
        }
        if (isDark != null) {
            cachedProxyDarkTheme = isDark
            Log.d(THEME_TAG, "probeThemeViaProxy — Cached: isDark=$isDark")
        } else {
            Log.d(THEME_TAG, "probeThemeViaProxy — Could not determine theme, cache unchanged")
        }
    } catch (e: Exception) {
        Log.e(THEME_TAG, "probeThemeProxy failed", e)
    }
}


@Composable
fun OpenBYDTheme(
    themeMode: ThemeMode = ThemeMode.DEFAULT,
    dynamicColor: Boolean = false, // Disabled dynamicColor by default to fix buggy/incorrect system palettes on cars
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    
    // Automatically trigger a background settings probe via proxy when app's theme is set to AUTO (DEFAULT) on launch
    if (themeMode == ThemeMode.DEFAULT) {
        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                probeThemeViaProxy(context)
            }
        }
    }

    val darkTheme = when (themeMode) {
        ThemeMode.DEFAULT -> isSystemDarkTheme(context)
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
