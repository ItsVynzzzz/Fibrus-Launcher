package com.israadev.nuxlauncher.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.israadev.nuxlauncher.BuildConfig
import com.israadev.nuxlauncher.StarlingLauncher
import com.israadev.nuxlauncher.core.DownloadController
import com.israadev.nuxlauncher.core.GameFiles
import com.israadev.nuxlauncher.data.AccountStore
import com.israadev.nuxlauncher.data.UiPrefs
import com.israadev.nuxlauncher.ui.theme.AMBER_ACCENTS
import com.israadev.nuxlauncher.ui.theme.AmberUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Kerangka layar selain Home: judul, isi yang bisa digulir, dan bar navigasi yang sama di kiri bawah. */
@Composable
private fun Page(title: String, selected: String, onNavigate: (String) -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, letterSpacing = (-0.5).sp)
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(top = 12.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
        BoxWithConstraints(Modifier.padding(top = 12.dp).fillMaxWidth()) {
            val fit = ((maxWidth.value - MIN_GAP_DP) / (NAV_ITEM_DP * NAV_ITEM_COUNT + NAV_CHROME_DP + MAIN_BUTTON_DP).toFloat()).coerceIn(0.7f, 1f)
            NavBar(selected, onNavigate, (NAV_ITEM_DP * fit).dp)
        }
    }
}

@Composable
fun AccountsScreen(onNavigate: (String) -> Unit) {
    val state by AccountStore.state.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    Page("Accounts", "accounts", onNavigate) {
        if (state.accounts.isEmpty()) {
            GlassCard {
                Text("No accounts yet", color = AmberUi.Text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text("Add an offline account to start playing.", color = AmberUi.TextDim, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
        state.accounts.forEach { a ->
            val active = a.id == state.active?.id
            GlassCard(Modifier.clickable { AccountStore.select(a.id) }) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    AccountAvatar(Modifier.size(40.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(a.username, color = AmberUi.Text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 1)
                        Text(if (active) "Active · Offline" else "Offline", color = if (active) AmberUi.AccentSoft else AmberUi.TextDim, fontSize = 12.sp)
                    }
                    if (!active) PillButton("Use", { AccountStore.select(a.id) }, filled = false)
                    Spacer(Modifier.width(8.dp))
                    PillButton("Remove", { AccountStore.remove(a.id) }, filled = false, danger = true)
                }
            }
        }
        Row { PillButton("Add offline account", { showAdd = true }) }
    }
    if (showAdd) AddAccountDialog { showAdd = false }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val dl by DownloadController.state.collectAsState()
    val downloading = dl is DownloadController.State.Running
    val installed = remember(dl) { GameFiles.installed(context) }
    var sizeMb by remember { mutableLongStateOf(-1L) }
    var confirmDelete by remember { mutableStateOf(false) }
    LaunchedEffect(dl, confirmDelete) {
        sizeMb = withContext(Dispatchers.IO) { GameFiles.sizeOnDisk(context) / (1024L * 1024L) }
    }
    val engine = remember { StarlingLauncher.availability(context) }

    Page("Settings", "settings", onNavigate) {
        GlassCard {
            Text("Appearance", color = AmberUi.Text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text("Accent color", color = AmberUi.TextDim, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AMBER_ACCENTS.forEach { a ->
                    val selected = a.id == UiPrefs.accentId
                    Box(
                        Modifier.size(32.dp).clip(CircleShape).background(a.brush, CircleShape)
                            .border(if (selected) 2.5.dp else 1.dp, if (selected) Color.White else Color.White.copy(alpha = 0.3f), CircleShape)
                            .clickable { UiPrefs.setAccent(context, a.id) }
                    )
                }
            }
            Text("Dim background: ${UiPrefs.dimPercent}%", color = AmberUi.TextDim, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
            Slider(
                value = UiPrefs.dimPercent.toFloat(),
                onValueChange = { UiPrefs.setDim(context, it.toInt(), save = false) },
                onValueChangeFinished = { UiPrefs.setDim(context, UiPrefs.dimPercent, save = true) },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(thumbColor = AmberUi.AccentSoft, activeTrackColor = AmberUi.AccentSoft, inactiveTrackColor = AmberUi.Border)
            )
        }
        GlassCard {
            Text("Controls", color = AmberUi.Text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(
                "Touch controls drawn and handled inside the engine, with no overlay. Takes effect the next time the game opens.",
                color = AmberUi.TextDim, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp)
            )
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton("On", { UiPrefs.setPadOn(context, true) }, filled = UiPrefs.padOn)
                PillButton("Off", { UiPrefs.setPadOn(context, false) }, filled = !UiPrefs.padOn)
            }
            Text("Camera speed: ${UiPrefs.lookSensPercent}%", color = AmberUi.TextDim, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
            Slider(
                value = UiPrefs.lookSensPercent.toFloat(),
                onValueChange = { UiPrefs.setLookSens(context, it.toInt(), save = false) },
                onValueChangeFinished = { UiPrefs.setLookSens(context, UiPrefs.lookSensPercent, save = true) },
                valueRange = 40f..250f,
                colors = SliderDefaults.colors(thumbColor = AmberUi.AccentSoft, activeTrackColor = AmberUi.AccentSoft, inactiveTrackColor = AmberUi.Border)
            )
        }
        GlassCard {
            Text("Performance lab (experimental)", color = AmberUi.Text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(
                "Engine switches to compare with F3 (look at GPU/CPU ms and the pass list). Each is off by default. Takes effect the next time the game opens.",
                color = AmberUi.TextDim, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp)
            )
            LabRow("Submit thread (CPU-bound frames)", "lab_submit", UiPrefs.labSubmit, listOf("Default" to 0, "On" to 1, "Off" to 2))
            LabRow("World upscale filter", "lab_upscale", UiPrefs.labUpscale, listOf("Sharp" to 0, "Fast" to 1))
            LabRow("Shadow map reused for N frames", "lab_shadow", UiPrefs.labShadow, listOf("Default" to 0, "3" to 3, "4" to 4))
            LabRow("GPU occlusion culling", "lab_cull", UiPrefs.labCull, listOf("Default" to 0, "Off" to 1))
        }
        GlassCard {
            Text("Engine", color = AmberUi.Text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text("Starling (Rust) · Minecraft ${StarlingLauncher.MC_VERSION}", color = AmberUi.TextDim, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            Text(
                if (engine is StarlingLauncher.Availability.Ready) "Ready" else (engine as StarlingLauncher.Availability.Unavailable).reason,
                color = if (engine is StarlingLauncher.Availability.Ready) AmberUi.AccentSoft else AmberUi.Danger,
                fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp)
            )
        }
        GlassCard {
            Text("Game files", color = AmberUi.Text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(
                (if (installed) "Installed" else "Not downloaded") + if (sizeMb > 0) " · $sizeMb MB on disk" else "",
                color = AmberUi.TextDim, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp)
            )
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton(
                    if (installed) "Verify / repair" else "Download",
                    {
                        if (installed) GameFiles.invalidate(context)
                        DownloadController.start(context)
                    },
                    enabled = !downloading && GameFiles.pins(context) != null
                )
                if (installed) PillButton("Delete", { confirmDelete = true }, filled = false, danger = true, enabled = !downloading)
            }
        }
        GlassCard {
            Text("Storage", color = AmberUi.Text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(StarlingLauncher.home(context).absolutePath, color = AmberUi.TextDim, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }
        GlassCard {
            Text("Fibrus Launcher ${BuildConfig.VERSION_NAME}", color = AmberUi.Text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text("Built on AmberLauncher (GPL-3.0) and the Starling engine (MIT).", color = AmberUi.TextDim, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
    if (confirmDelete) {
        FibrusDialog({ confirmDelete = false }, "Delete game files?") {
            Text("Everything under the game files folder is removed. You can download it again later.", color = AmberUi.Text, fontSize = 14.sp)
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.End) {
                PillButton("Cancel", { confirmDelete = false }, filled = false)
                Spacer(Modifier.width(8.dp))
                PillButton("Delete", { GameFiles.deleteAll(context); confirmDelete = false }, danger = true, filled = false)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LabRow(title: String, key: String, selected: Int, options: List<Pair<String, Int>>) {
    val context = LocalContext.current
    Text(title, color = AmberUi.TextDim, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (label, value) ->
            PillButton(label, { UiPrefs.setLab(context, key, value) }, filled = selected == value)
        }
    }
}
