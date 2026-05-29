package com.sr.openbyd

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.sr.openbyd.proxy.ProxyManager
import com.sr.openbyd.data.AppPreferences
import com.sr.openbyd.services.AccessibilitySetupHelper
import com.sr.openbyd.ui.screens.MainScreen
import com.sr.openbyd.ui.theme.OpenBYDTheme
import com.sr.openbyd.ui.viewmodel.MainViewModel
import com.sr.openbyd.utils.LocaleHelper

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    companion object {
        private const val TAG = "MainActivity"

        /** Human-readable names for all known BYD steering wheel keycodes.
         * Shared with ViewModel for item generation. */
        val KNOWN_KEYCODES = linkedMapOf(
            310 to R.string.key_surround_view,
            320 to R.string.key_voice_assistant,
            321 to R.string.key_custom_left,
            383 to R.string.key_custom_right,
        )
    }

    override fun attachBaseContext(newBase: Context) {
        val prefs = AppPreferences(newBase)
        val langCode = prefs.languageMode.code
        super.attachBaseContext(LocaleHelper.wrap(newBase, langCode))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        ProxyManager.startProxy(this)

        setContent {
            OpenBYDTheme(themeMode = viewModel.currentThemeMode) {
                MainScreen(
                    viewModel = viewModel,
                    onLanguageChange = { lang ->
                        viewModel.updateLanguage(lang)
                        recreate()
                    }
                )
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (viewModel.showDebugDialog) {
            viewModel.lastPressedKeyCode = keyCode
        }
        return super.onKeyDown(keyCode, event)
    }
}
