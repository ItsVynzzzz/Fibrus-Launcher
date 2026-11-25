package com.israadev.nuxlauncher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.israadev.nuxlauncher.BuildConfig
import com.israadev.nuxlauncher.core.DownloadController
import com.israadev.nuxlauncher.data.AccountStore
import com.israadev.nuxlauncher.ui.theme.AmberUi

@Composable
fun AddAccountDialog(onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    FibrusDialog(onDismiss, "Add offline account") {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(16); error = null },
            singleLine = true,
            label = { Text("Player name") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = AmberUi.Text, unfocusedTextColor = AmberUi.Text,
                focusedBorderColor = AmberUi.AccentSoft, unfocusedBorderColor = AmberUi.Border,
                focusedLabelColor = AmberUi.AccentSoft, unfocusedLabelColor = AmberUi.TextDim,
                cursorColor = AmberUi.AccentSoft
            ),
            modifier = Modifier.fillMaxWidth()
        )
        error?.let { Text(it, color = AmberUi.Danger, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp)) }
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.End) {
            PillButton("Cancel", onDismiss, filled = false)
            Spacer(Modifier.width(8.dp))
            PillButton("Add", {
                val e = AccountStore.addOffline(name)
                if (e == null) onDismiss() else error = e
            })
        }
    }
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    FibrusDialog(onDismiss, "Fibrus Launcher") {
        Text("Version ${BuildConfig.VERSION_NAME}", color = AmberUi.TextDim, fontSize = 13.sp)
        Text(
            "Built on AmberLauncher (GPL-3.0) and the Starling Rust engine (MIT). " +
                "This is an unofficial project: it is not made by, or affiliated with, Mojang or Microsoft. " +
                "You need your own copy of Minecraft.",
            color = AmberUi.Text, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp)
        )
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.End) {
            PillButton("Close", onDismiss)
        }
    }
}

private fun mb(bytes: Long) = bytes / (1024L * 1024L)

/** Dialog unduhan berkas game; letakkan sekali di akar aplikasi. */
@Composable
fun DownloadDialogHost() {
    val state by DownloadController.state.collectAsState()
    when (val s = state) {
        is DownloadController.State.Running -> FibrusDialog({}, "Downloading game files", dismissable = false) {
            val p = s.progress
            Text(
                if (p.preparing) "Preparing the file list..." else "${p.filesDone} / ${p.filesTotal} files",
                color = AmberUi.Text, fontSize = 14.sp
            )
            if (p.preparing) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 10.dp), color = AmberUi.AccentSoft)
            else LinearProgressIndicator(progress = { p.fraction }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp), color = AmberUi.AccentSoft)
            if (!p.preparing) {
                val speed = if (p.bytesPerSecond > 0) " · %.1f MB/s".format(p.bytesPerSecond / (1024f * 1024f)) else ""
                Text("${mb(p.bytesDone)} / ${mb(p.bytesTotal)} MB$speed", color = AmberUi.TextDim, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            }
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.End) {
                PillButton("Cancel", { DownloadController.cancel() }, filled = false)
            }
        }
        is DownloadController.State.Failed -> FibrusDialog({ DownloadController.dismissError() }, "Download failed") {
            Text(s.message, color = AmberUi.Text, fontSize = 14.sp)
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.End) {
                PillButton("OK", { DownloadController.dismissError() })
            }
        }
        DownloadController.State.Idle -> Unit
    }
}
