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
    private const val KEY_DPAD = "pad_dpad"
    private const val KEY_PAD_SCALE = "pad_scale"
    private const val KEY_PAD_ALPHA = "pad_alpha"
    private const val KEY_UNLOCK_FPS = "unlock_fps"
    /** Ukuran tombol dalam persen (60 sampai 130) dan kepekatan pelat (5 sampai 60). Sama dengan batas di mesin. */
    const val DEFAULT_PAD_SCALE = 100
    const val DEFAULT_PAD_ALPHA = 16

    var accentId by mutableStateOf(DEFAULT_ACCENT)
        private set
    var dimPercent by mutableIntStateOf(DEFAULT_DIM)
        private set

    /** Kontrol sentuh Fibrus Pad (di dalam mesin) menyala. Dibaca saat game dibuka. */
    var padOn by mutableStateOf(true)
        private set

    /** Kecepatan putaran kamera dalam persen (40 sampai 250). Dibaca saat game dibuka. */
    var lookSensPercent by mutableIntStateOf(DEFAULT_SENS)
        private set

    /** Tombol arah kiri berupa D-pad tetap 3 x 3 (gaya PojavLauncher dan Zalith), bukan stik yang muncul di bawah jari. */
    var dpadOn by mutableStateOf(false)
        private set

    /** Ukuran tombol Fibrus Pad dalam persen (60 sampai 130). Dibaca saat game dibuka. */
    var padScalePercent by mutableIntStateOf(DEFAULT_PAD_SCALE)
        private set

    /** Kepekatan pelat tombol dalam persen (5 sampai 60). Dibaca saat game dibuka. */
    var padAlphaPercent by mutableIntStateOf(DEFAULT_PAD_ALPHA)
        private set

    /**
     * Buka kunci FPS: VSync mati dan tidak ada batas bingkai, termasuk batas mode baterai. Slider "Max Framerate" di
     * Minecraft sendiri tidak mematikan VSync, jadi FPS tetap ikut laju layar. Menyala sejak awal karena itu keluhan utama.
     */
    var unlockFps by mutableStateOf(true)
        private set

    fun init(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        accentId = p.getString(KEY_ACCENT, DEFAULT_ACCENT) ?: DEFAULT_ACCENT
        dimPercent = p.getInt(KEY_DIM, DEFAULT_DIM).coerceIn(0, 100)
        padOn = p.getBoolean(KEY_PAD, true)
        lookSensPercent = p.getInt(KEY_SENS, DEFAULT_SENS).coerceIn(40, 250)
        dpadOn = p.getBoolean(KEY_DPAD, false)
        padScalePercent = p.getInt(KEY_PAD_SCALE, DEFAULT_PAD_SCALE).coerceIn(60, 130)
        padAlphaPercent = p.getInt(KEY_PAD_ALPHA, DEFAULT_PAD_ALPHA).coerceIn(5, 60)
        unlockFps = p.getBoolean(KEY_UNLOCK_FPS, true)
        apply()
    }

    fun setDpadOn(context: Context, on: Boolean) {
        dpadOn = on
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_DPAD, on).apply()
    }

    /** Dipanggil terus saat slider digeser (tanpa menulis ke disk); [save] menulis bila jari dilepas. */
    fun setPadScale(context: Context, percent: Int, save: Boolean) {
        padScalePercent = percent.coerceIn(60, 130)
        if (save) context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY_PAD_SCALE, padScalePercent).apply()
    }

    fun setPadAlpha(context: Context, percent: Int, save: Boolean) {
        padAlphaPercent = percent.coerceIn(5, 60)
        if (save) context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY_PAD_ALPHA, padAlphaPercent).apply()
    }

    fun setUnlockFps(context: Context, on: Boolean) {
        unlockFps = on
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_UNLOCK_FPS, on).apply()
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
