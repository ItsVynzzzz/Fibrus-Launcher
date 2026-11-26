package com.israadev.nuxlauncher.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.israadev.nuxlauncher.core.controls.ControlLayoutIO
import com.israadev.nuxlauncher.core.controls.ControlLayouts
import com.israadev.nuxlauncher.core.controls.ControlPresetManager
import com.israadev.nuxlauncher.data.UiPrefs
import com.israadev.nuxlauncher.ui.theme.AmberUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Hasil impor: pesan untuk pemain dan id preset yang baru dibuat (null bila gagal). */
private class ImportOutcome(val message: String, val presetId: String?)

private fun importFrom(context: Context, uri: Uri): ImportOutcome {
    return when (val r = ControlLayouts.readImport(context, uri)) {
        is ControlLayoutIO.ImportResult.Ok -> {
            val name = ControlLayouts.displayName(context, uri)
            when (val saved = ControlLayouts.saveImported(context, name, r.buttons)) {
                is ControlPresetManager.SaveResult.Saved -> {
                    val from = if (r.source.isNotEmpty()) " from ${r.source}" else ""
                    val extra = (if (r.skipped > 0) ", ${r.skipped} skipped" else "") +
                        (if (r.macroCommands > 0) ", ${r.macroCommands} macro buttons are ignored" else "")
                    ImportOutcome("Imported \"${saved.preset.name}\"$from: ${saved.preset.buttonCount} buttons$extra.", saved.preset.id)
                }
                ControlPresetManager.SaveResult.TooMany -> ImportOutcome("Too many layouts saved. Delete one first.", null)
                else -> ImportOutcome("Could not save the layout.", null)
            }
        }
        is ControlLayoutIO.ImportResult.Failed -> ImportOutcome(
            when (r.reason) {
                ControlLayoutIO.Reason.TOO_LARGE -> "That file is too large to be a control layout."
                ControlLayoutIO.Reason.NO_BUTTONS -> "No buttons were found in that file."
                ControlLayoutIO.Reason.NOT_LAYOUT -> "That file is not a control layout."
            },
            null
        )
    }
}

/**
 * Pilihan kontrol Amber di Settings > Controls: preset (bawaan Amber, atau hasil impor dari Amber, PojavLauncher, Zalith,
 * Mojo) dan impor berkas layout. Dipakai saat game dibuka: lihat StarlingLauncher.launch dan RustTouchLayout.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AmberControlsSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var presets by remember { mutableStateOf(emptyList<ControlPresetManager.Preset>()) }
    var reload by remember { mutableIntStateOf(0) }
    var status by remember { mutableStateOf("") }
    LaunchedEffect(reload) {
        presets = withContext(Dispatchers.IO) { ControlLayouts.presets(context) }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val outcome = withContext(Dispatchers.IO) { importFrom(context, uri) }
                status = outcome.message
                outcome.presetId?.let { UiPrefs.setAmberPreset(context, it) }
                reload++
            }
        }
    }

    Column {
        Text(
            "Zalith-style layout drawn by the engine's own touch controls (no overlay window). Pick a layout, or import one " +
                "from Amber, PojavLauncher or Zalith. Camera speed and the Fibrus Pad options above do not apply here.",
            color = AmberUi.TextDim, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp)
        )
        Text("Layout", color = AmberUi.TextDim, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            PillButton("Default", { UiPrefs.setAmberPreset(context, "") }, filled = UiPrefs.amberPresetId.isEmpty())
            presets.forEach { p ->
                PillButton(p.name, { UiPrefs.setAmberPreset(context, p.id) }, filled = UiPrefs.amberPresetId == p.id)
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 10.dp)
        ) {
            PillButton("Import layout", { picker.launch(arrayOf("*/*")) }, filled = false)
            val selected = presets.firstOrNull { it.id == UiPrefs.amberPresetId }
            if (selected != null) {
                PillButton("Delete", {
                    scope.launch {
                        withContext(Dispatchers.IO) { ControlLayouts.deletePreset(context, selected.id) }
                        UiPrefs.setAmberPreset(context, "")
                        status = "Deleted \"${selected.name}\"."
                        reload++
                    }
                }, filled = false, danger = true)
            }
        }
        if (status.isNotEmpty()) {
            Text(status, color = AmberUi.AccentSoft, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }
        Text("Drag from the attack, use and jump buttons turns the camera", color = AmberUi.TextDim, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            PillButton("On", { UiPrefs.setLookDragOn(context, true) }, filled = UiPrefs.lookDragOn)
            PillButton("Off", { UiPrefs.setLookDragOn(context, false) }, filled = !UiPrefs.lookDragOn)
        }
    }
}
