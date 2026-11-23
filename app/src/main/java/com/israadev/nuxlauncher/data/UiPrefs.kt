package com.israadev.nuxlauncher.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.israadev.nuxlauncher.ui.theme.AmberUi

/** Pengaturan tampilan: warna aksen dan redup latar. Disimpan di SharedPreferences "fibrus". */
object UiPrefs {
    private const val PREFS = "fibrus"
    private const val KEY_ACCENT = "accent"
    private const val KEY_DIM = "dim"
    const val DEFAULT_ACCENT = "red" // sama dengan tampilan Home Amber di screenshot
    const val DEFAULT_DIM = 50

    var accentId by mutableStateOf(DEFAULT_ACCENT)
        private set
    var dimPercent by mutableIntStateOf(DEFAULT_DIM)
        private set

    fun init(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        accentId = p.getString(KEY_ACCENT, DEFAULT_ACCENT) ?: DEFAULT_ACCENT
        dimPercent = p.getInt(KEY_DIM, DEFAULT_DIM).coerceIn(0, 100)
        apply()
    }

    fun setAccent(context: Context, id: String) {
        accentId = id
        apply()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_ACCENT, id).apply()
    }

    /** Dipanggil terus saat slider digeser (tanpa menulis ke disk); [save] menulis bila jari dilepas. */
    fun setDim(context: Context, percent: Int, save: Boolean) {
        dimPercent = percent.coerceIn(0, 100)
        apply()
        if (save) context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY_DIM, dimPercent).apply()
    }

    private fun apply() {
        AmberUi.setAccent(accentId)
        AmberUi.applyDim(dimPercent / 100f)
    }
}
