package com.israadev.nuxlauncher

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.israadev.nuxlauncher.data.AccountStore
import com.israadev.nuxlauncher.data.UiPrefs
import com.israadev.nuxlauncher.ui.FibrusApp
import com.israadev.nuxlauncher.ui.theme.AmberUi
import com.israadev.nuxlauncher.ui.theme.NuxColors
import com.israadev.nuxlauncher.ui.theme.NuxResponsiveTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()
        AccountStore.init(this)
        UiPrefs.init(this)
        setContent {
            NuxResponsiveTheme {
                MaterialTheme(
                    colorScheme = darkColorScheme(
                        primary = AmberUi.Accent, onPrimary = NuxColors.Ink,
                        background = NuxColors.Background, onBackground = AmberUi.Text,
                        surface = NuxColors.Background, onSurface = AmberUi.Text
                    )
                ) { FibrusApp() }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    /** Layar penuh imersif; poni (cutout) dipakai sampai tepi pendek layar. */
    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val c = WindowCompat.getInsetsController(window, window.decorView)
        c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        c.hide(WindowInsetsCompat.Type.systemBars())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val p = window.attributes
            p.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = p
        }
    }
}
