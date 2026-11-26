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
    private const val KEY_PAD = "pad_on"
    private const val KEY_SENS = "look_sens"
    const val DEFAULT_SENS = 100

    var accentId by mutableStateOf(DEFAULT_ACCENT)
        private set
    var dimPercent by mutableIntStateOf(DEFAULT_DIM)
        private set

    /**
     * Lab performa: pilihan yang hanya menulis variabel lingkungan mesin yang SUDAH ADA (tanpa membangun ulang mesin),
     * untuk uji A/B lewat F3. 0 = bawaan mesin. Dibaca saat game dibuka.
     */
    var labSubmit by mutableIntStateOf(0) // 1 = HORIZON_SUBMIT_THREAD=1, 2 = =0
        private set
    var labUpscale by mutableIntStateOf(0) // 1 = HORIZON_WORLD_UPSCALE=off (tanpa filter tajam)
        private set
    var labShadow by mutableIntStateOf(0) // n >= 2 = HORIZON_SHADER_SHADOW_EVERY=n
        private set
    var labCull by mutableIntStateOf(0) // 1 = HORIZON_GPU_CULL=0

    /** Kontrol sentuh Fibrus Pad (di dalam mesin) menyala. Dibaca saat game dibuka. */
    var padOn by mutableStateOf(true)
        private set

    /** Kecepatan putaran kamera dalam persen (40 sampai 250). Dibaca saat game dibuka. */
    var lookSensPercent by mutableIntStateOf(DEFAULT_SENS)
        private set

    fun init(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        accentId = p.getString(KEY_ACCENT, DEFAULT_ACCENT) ?: DEFAULT_ACCENT
        dimPercent = p.getInt(KEY_DIM, DEFAULT_DIM).coerceIn(0, 100)
        labSubmit = p.getInt("lab_submit", 0)
        labUpscale = p.getInt("lab_upscale", 0)
        labShadow = p.getInt("lab_shadow", 0)
        labCull = p.getInt("lab_cull", 0)
        padOn = p.getBoolean(KEY_PAD, true)
        lookSensPercent = p.getInt(KEY_SENS, DEFAULT_SENS).coerceIn(40, 250)
        apply()
    }

    fun setLab(context: Context, key: String, value: Int) {
        when (key) {
            "lab_submit" -> labSubmit = value
            "lab_upscale" -> labUpscale = value
            "lab_shadow" -> labShadow = value
            "lab_cull" -> labCull = value
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(key, value).apply()
    }

    /** Baris horizon-args.txt dari pilihan lab. */
    fun labArgs(): String = buildString {
        when (labSubmit) { 1 -> append("HORIZON_SUBMIT_THREAD=1\n"); 2 -> append("HORIZON_SUBMIT_THREAD=0\n") }
        if (labUpscale == 1) append("HORIZON_WORLD_UPSCALE=off\n")
        if (labShadow >= 2) append("HORIZON_SHADER_SHADOW_EVERY=$labShadow\n")
        if (labCull == 1) append("HORIZON_GPU_CULL=0\n")
    }

    fun setPadOn(context: Context, on: Boolean) {
        padOn = on
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_PAD, on).apply()
    }

    fun setLookSens(context: Context, percent: Int, save: Boolean) {
        lookSensPercent = percent.coerceIn(40, 250)
        if (save) context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY_SENS, lookSensPercent).apply()
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
