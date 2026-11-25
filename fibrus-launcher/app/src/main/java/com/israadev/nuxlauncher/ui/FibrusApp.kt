package com.israadev.nuxlauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.israadev.nuxlauncher.R
import com.israadev.nuxlauncher.ui.theme.AmberUi
import com.israadev.nuxlauncher.ui.theme.NuxColors
import com.israadev.nuxlauncher.ui.theme.amberScrimBrush

/** Akar aplikasi: langsung Home, tanpa gerbang pilih akun. Tab: home, accounts, settings. */
@Composable
fun FibrusApp() {
    var tab by rememberSaveable { mutableStateOf("home") }
    BackHandler(enabled = tab != "home") { tab = "home" }
    Box(Modifier.fillMaxSize().background(NuxColors.Background)) {
        Image(painterResource(R.drawable.mc_hero_bg), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        if (tab != "home") Box(Modifier.fillMaxSize().background(amberScrimBrush(AmberUi.dim)))
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.displayCutout)) {
            when (tab) {
                "accounts" -> AccountsScreen { tab = it }
                "settings" -> SettingsScreen { tab = it }
                else -> HomeScreen { tab = it }
            }
        }
        DownloadDialogHost()
    }
}
