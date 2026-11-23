package com.israadev.nuxlauncher.ui

import android.app.ActivityManager
import android.content.Context
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.israadev.nuxlauncher.ui.theme.AmberUi
import com.israadev.nuxlauncher.ui.theme.NuxColors
import com.israadev.nuxlauncher.ui.theme.amberGlass
import kotlinx.coroutines.delay

// Ukuran tetap baris bawah Home (dp pada skala UI), sama dengan Amber tetapi dengan 3 item (tanpa tab Mod).
const val NAV_ITEM_DP = 100
const val NAV_CHROME_DP = 16
const val MAIN_BUTTON_DP = 216
const val MAIN_BUTTON_HEIGHT_DP = 64
const val MIN_GAP_DP = 16f
const val NAV_ITEM_COUNT = 3

private val NAV_ITEMS = listOf(
    Triple("home", "Home", Icons.Outlined.Home),
    Triple("accounts", "Accounts", Icons.Outlined.Person),
    Triple("settings", "Settings", Icons.Outlined.Tune)
)

/** Bar navigasi berbentuk pil: ikon di atas label. */
@Composable
fun NavBar(selected: String, onSelect: (String) -> Unit, itemWidth: Dp, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.height(61.dp).clip(CircleShape).amberGlass(CircleShape).padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        NAV_ITEMS.forEach { (id, label, icon) ->
            val isSelected = id == selected
            Column(
                modifier = Modifier
                    .width(itemWidth).fillMaxHeight().clip(CircleShape)
                    .background(if (isSelected) AmberUi.SurfaceRaised else Color.Transparent, CircleShape)
                    .border(1.dp, if (isSelected) AmberUi.BorderStrong else Color.Transparent, CircleShape)
                    .clickable { if (!isSelected) onSelect(id) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(icon, null, tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.78f), modifier = Modifier.size(20.dp))
                Spacer(Modifier.height(3.dp))
                Text(label, color = if (isSelected) Color.White else Color.White.copy(alpha = 0.78f), fontWeight = FontWeight.Medium, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

/** Tombol utama (PLAY / DOWNLOAD) dengan bayangan keras di bawah; menekan = turun. */
@Composable
fun MainButton(text: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val lift by animateDpAsState(if (pressed && enabled) 0.dp else 4.dp, label = "mainLift")
    Box(
        modifier = modifier.height(64.dp).alpha(if (enabled) 1f else 0.65f)
            .clickable(enabled = enabled, interactionSource = interaction, indication = null) { onClick() }
    ) {
        Box(Modifier.fillMaxWidth().height(60.dp).align(Alignment.BottomCenter).background(AmberUi.AccentDeep, CircleShape))
        Row(
            modifier = Modifier.fillMaxWidth().height(60.dp).align(Alignment.TopCenter).offset(y = 4.dp - lift)
                .clip(CircleShape).background(AmberUi.AccentBrush, CircleShape).border(1.5.dp, AmberUi.AccentEdge, CircleShape),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(12.dp))
            Text(text, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, letterSpacing = 3.sp, maxLines = 1)
        }
    }
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(modifier.fillMaxWidth().clip(shape).amberGlass(shape).padding(16.dp), content = content)
}

/** Tombol pil kecil: terisi aksen, atau kaca (outline). */
@Composable
fun PillButton(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, filled: Boolean = true, danger: Boolean = false
) {
    val bg = if (filled) Modifier.background(AmberUi.AccentBrush, CircleShape) else Modifier.amberGlass(CircleShape)
    Box(
        modifier = modifier.height(40.dp).alpha(if (enabled) 1f else 0.5f).clip(CircleShape).then(bg)
            .clickable(enabled = enabled) { onClick() }.padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (danger) AmberUi.Danger else Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
    }
}

@Composable
fun FibrusDialog(onDismiss: () -> Unit, title: String, dismissable: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    Dialog(
        onDismissRequest = { if (dismissable) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = dismissable, dismissOnClickOutside = dismissable)
    ) {
        val shape = RoundedCornerShape(22.dp)
        Column(
            Modifier.widthIn(max = 440.dp).padding(16.dp).clip(shape).background(NuxColors.Panel).border(1.dp, NuxColors.CardBorder, shape).padding(20.dp)
        ) {
            Text(title, color = AmberUi.Text, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

private fun readFreeRamMb(context: Context): Int {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val info = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
    return ((info.availMem / (1024L * 1024L)) / 100L * 100L).toInt()
}

/** Sisa RAM HP (dibulatkan 100 MB), diperbarui tiap 5 detik selama layar tampil. */
@Composable
fun rememberFreeRamMb(): Int {
    val context = LocalContext.current
    var mb by remember { mutableIntStateOf(readFreeRamMb(context)) }
    LaunchedEffect(Unit) {
        while (true) { delay(5_000); mb = readFreeRamMb(context) }
    }
    return mb
}

/** 2 = lapang, 1 = menipis, 0 = sempit. */
fun ramLevel(mb: Int): Int = when { mb >= 2000 -> 2; mb >= 1000 -> 1; else -> 0 }
fun ramText(mb: Int): String = if (mb >= 1000) "%.1f GB RAM available".format(mb / 1000f) else "$mb MB RAM available"
