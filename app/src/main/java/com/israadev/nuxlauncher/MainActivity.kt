package com.israadev.nuxlauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.israadev.nuxlauncher.ui.FibrusColors
import com.israadev.nuxlauncher.ui.FibrusTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FibrusTheme { HomeScreen() } }
    }
}

private const val PREFS = "fibrus"
private const val KEY_NAME = "player_name"

/** Layar tunggal: nama pemain offline dan tombol Main. Semua selebihnya sengaja belum ada. */
@Composable
private fun HomeScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, 0) }
    var name by remember { mutableStateOf(prefs.getString(KEY_NAME, "") ?: "") }
    var message by remember { mutableStateOf<String?>(null) }
    val status = remember { StarlingLauncher.availability(context) }
    val ready = status is StarlingLauncher.Availability.Ready

    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Fibrus", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = FibrusColors.Amber)
        Text("Minecraft ${StarlingLauncher.MC_VERSION} · mesin Rust", color = FibrusColors.TextDim)

        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it.take(16)
                prefs.edit().putString(KEY_NAME, name).apply()
                message = null
            },
            label = { Text("Nama pemain") },
            singleLine = true,
            modifier = Modifier.padding(top = 24.dp).widthIn(max = 360.dp).fillMaxWidth()
        )

        Button(
            onClick = { StarlingLauncher.launch(context, name) { message = it } },
            enabled = ready,
            modifier = Modifier.padding(top = 16.dp).widthIn(max = 360.dp).fillMaxWidth()
        ) {
            Text("Main")
        }

        val note = message ?: (status as? StarlingLauncher.Availability.Unavailable)?.reason
        if (note != null) {
            Text(note, color = FibrusColors.Danger, modifier = Modifier.padding(top = 12.dp))
        }
    }
}
