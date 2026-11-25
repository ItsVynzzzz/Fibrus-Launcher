package com.israadev.nuxlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.israadev.nuxlauncher.StarlingLauncher
import com.israadev.nuxlauncher.core.DownloadController
import com.israadev.nuxlauncher.core.GameFiles
import com.israadev.nuxlauncher.data.AccountStore
import com.israadev.nuxlauncher.ui.theme.AmberUi
import com.israadev.nuxlauncher.ui.theme.LocalNuxScale
import com.israadev.nuxlauncher.ui.theme.NuxColors
import com.israadev.nuxlauncher.ui.theme.amberGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Home: langsung tampil saat aplikasi dibuka (tanpa layar pilih akun). Tata letaknya mengikuti Home AmberLauncher:
 * pil pemain kiri atas, versi aktif besar kiri bawah, bar navigasi, tombol PLAY, dan figur skin di kanan.
 */
@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accountState by AccountStore.state.collectAsState()
    val account = accountState.active
    val dl by DownloadController.state.collectAsState()
    val downloading = dl is DownloadController.State.Running
    val installed = remember(dl) { GameFiles.installed(context) }
    val engine = remember { StarlingLauncher.availability(context) }
    val freeRamMb = rememberFreeRamMb()

    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var accountMenuOpen by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }

    fun launchGame() {
        message = null
        if (StarlingLauncher.launch(context) { message = it }) {
            busy = true
            scope.launch { delay(2_500); busy = false } // cegah ketukan ganda saat mesin dibuka
        }
    }

    fun onPlay() {
        if (account == null) { showAdd = true; return }
        if (!installed) {
            if (GameFiles.pins(context) == null) message = "Game file list is missing from this build."
            else DownloadController.start(context) { launchGame() }
        } else launchGame()
    }

    val config = LocalConfiguration.current
    val uiScale = LocalNuxScale.current.scaleFactor
    val designWidthDp = config.screenWidthDp / uiScale
    val designHeightDp = config.screenHeightDp / uiScale
    val compact = designWidthDp < 760f
    val nameSize = if (designHeightDp < 372f) 40.sp else 48.sp
    val textShadow = remember { Shadow(NuxColors.Ink.copy(alpha = 0.40f), Offset(0f, 2f), 12f) }
    val textStyle = LocalTextStyle.current.copy(shadow = textShadow)

    Box(Modifier.fillMaxSize()) {
        // Gradien bawah dan kiri: teks putih terbaca di latar terang
        Box(
            Modifier.fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to NuxColors.Ink.copy(alpha = 0.18f),
                        0.24f to Color.Transparent,
                        0.58f to NuxColors.Ink.copy(alpha = 0.18f),
                        1f to NuxColors.Ink.copy(alpha = 0.78f)
                    )
                )
                .background(Brush.horizontalGradient(0f to NuxColors.Ink.copy(alpha = 0.40f), 0.55f to Color.Transparent))
        )

        // Figur skin di kolom yang sama dengan tombol PLAY, di belakang kolom UI (tidak menghalangi ketukan)
        if (account != null) {
            val rowWidthDp = designWidthDp - 32f
            val fit = ((rowWidthDp - MIN_GAP_DP) / (NAV_ITEM_DP * NAV_ITEM_COUNT + NAV_CHROME_DP + MAIN_BUTTON_DP).toFloat()).coerceIn(0.7f, 1f)
            SkinFigure(
                Modifier.align(Alignment.BottomEnd)
                    .padding(end = 16.dp, top = 16.dp + 43.dp + 8.dp, bottom = 16.dp + MAIN_BUTTON_HEIGHT_DP.dp + 10.dp)
                    .fillMaxHeight()
                    .width((MAIN_BUTTON_DP * fit).dp)
            )
        }

        Column(Modifier.fillMaxSize().padding(16.dp)) {
            // ---- Baris atas: pil pemain (kiri), tombol Tentang (kanan) ----
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.height(42.dp).clip(CircleShape).amberGlass(CircleShape)
                        .clickable { if (accountState.accounts.isEmpty()) showAdd = true else accountMenuOpen = !accountMenuOpen }
                        .padding(start = 6.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (account != null) {
                        AccountAvatar(Modifier.size(30.dp))
                    } else {
                        Box(Modifier.size(30.dp).background(AmberUi.AccentBrush, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, null, tint = AmberUi.Text, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        account?.username ?: "Add account", color = AmberUi.Text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 150.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        if (accountMenuOpen) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        "Switch account", tint = AmberUi.Text.copy(alpha = 0.8f), modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.size(43.dp).clip(CircleShape).amberGlass(CircleShape).clickable { showAbout = true },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.Info, "About", tint = AmberUi.Text, modifier = Modifier.size(22.dp)) }
            }

            Spacer(Modifier.weight(1f))

            // ---- Kiri bawah: versi aktif, lalu navigasi + PLAY ----
            Column(Modifier.fillMaxWidth()) {
                Text(
                    "ACTIVE VERSION", color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp, letterSpacing = 2.6.sp, maxLines = 1, style = textStyle
                )
                Text(
                    StarlingLauncher.DISPLAY_NAME, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = nameSize,
                    lineHeight = nameSize * 1.1f, letterSpacing = (-1.5).sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = textStyle,
                    modifier = Modifier.padding(top = 4.dp).fillMaxWidth(if (!compact) 0.7f else if (account != null) 0.64f else 1f)
                )
                Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(StarlingLauncher.MC_VERSION, color = Color.White.copy(alpha = 0.92f), fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1, style = textStyle)
                    MetaDivider()
                    Text("Starling (Rust)", color = AmberUi.AccentSoft, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
                    MetaDivider()
                    Text("No Java", color = Color.White.copy(alpha = 0.92f), fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1, style = textStyle)
                }
                val ramColor = when (ramLevel(freeRamMb)) { 2 -> AmberUi.AccentSoft; 1 -> NuxColors.Amber; else -> NuxColors.ErrorRed }
                Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).background(ramColor, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text(ramText(freeRamMb), color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Medium, fontSize = 13.sp, maxLines = 1, style = textStyle)
                }
                val note = message ?: (engine as? StarlingLauncher.Availability.Unavailable)?.reason
                if (note != null) Text(note, color = AmberUi.Danger, fontSize = 13.sp, maxLines = 2, modifier = Modifier.padding(top = 6.dp), style = textStyle)

                BoxWithConstraints(Modifier.padding(top = 16.dp).fillMaxWidth()) {
                    val ideal = (NAV_ITEM_DP * NAV_ITEM_COUNT + NAV_CHROME_DP + MAIN_BUTTON_DP).toFloat()
                    val fit = ((maxWidth.value - MIN_GAP_DP) / ideal).coerceIn(0.7f, 1f)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        NavBar("home", onNavigate, (NAV_ITEM_DP * fit).dp)
                        MainButton(
                            text = when { busy -> "LOADING"; downloading -> "DOWNLOADING"; !installed -> "DOWNLOAD"; else -> "PLAY" },
                            icon = if (installed) Icons.Default.PlayArrow else Icons.Default.Download,
                            onClick = ::onPlay,
                            enabled = !busy && !downloading,
                            modifier = Modifier.width((MAIN_BUTTON_DP * fit).dp)
                        )
                    }
                }
            }
        }

        // Menu akun cepat di bawah pil pemain
        if (accountMenuOpen) {
            Box(
                Modifier.fillMaxSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { accountMenuOpen = false }
            )
            val shape = RoundedCornerShape(18.dp)
            Column(
                Modifier.padding(start = 16.dp, top = 16.dp + 42.dp + 6.dp).widthIn(min = 220.dp, max = 300.dp)
                    .clip(shape).background(AmberUi.MenuCard, shape).padding(6.dp)
            ) {
                accountState.accounts.forEach { a ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(if (a.id == account?.id) AmberUi.SurfaceRaised else Color.Transparent)
                            .clickable { AccountStore.select(a.id); accountMenuOpen = false }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AccountAvatar(Modifier.size(26.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(a.username, color = AmberUi.Text, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Icon(
                            Icons.Default.Close, "Remove", tint = AmberUi.TextDim,
                            modifier = Modifier.size(18.dp).clickable { AccountStore.remove(a.id) }
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .clickable { accountMenuOpen = false; showAdd = true }.padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, null, tint = AmberUi.AccentSoft, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Add account", color = AmberUi.AccentSoft, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }
    }

    if (showAdd) AddAccountDialog { showAdd = false }
    if (showAbout) AboutDialog { showAbout = false }
}

@Composable
private fun MetaDivider() {
    Box(Modifier.padding(horizontal = 12.dp).width(1.dp).height(14.dp).background(Color.White.copy(alpha = 0.40f)))
}
