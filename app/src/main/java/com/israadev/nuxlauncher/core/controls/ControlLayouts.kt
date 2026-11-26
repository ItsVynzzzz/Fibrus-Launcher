package com.israadev.nuxlauncher.core.controls

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.israadev.nuxlauncher.core.controls.models.ControlShowMode
import com.israadev.nuxlauncher.core.controls.models.CustomControlButton
import com.movtery.inputmap.keycodes.LwjglGlfwKeycode
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Bagian ControlLayoutManager milik Amber yang dibutuhkan Fibrus: tata letak bawaan, pelengkap tombol sistem, impor
 * berkas layout (Amber, PojavLauncher, Zalith, Mojo), dan daftar preset. Isi fungsi disalin apa adanya dari Amber
 * (core/controls/ControlLayoutManager.kt); yang tidak dibawa: layout global di penyimpanan, cadangan, preset per instance,
 * dan editor di dalam game (Fibrus tidak punya jendela kontrol di atas game).
 *
 * Semua fungsi membaca atau menulis berkas: panggil dari thread latar.
 */
object ControlLayouts {
    /**
     * Tata letak bawaan: sedikit tombol, besar, mudah dijangkau jempol.
     * Kiri: joystick + jongkok. Kanan: serang, pakai, lompat, gulir hotbar.
     * Atas: lencana FPS, ESC/chat/inventori/F5 di tengah, tombol sistem kecil di kanan.
     * Dua mode: tombol gerak dan aksi hanya tampil saat main; saat menu (inventori, chat, jeda)
     * tersisa ESC, inventori, dan tombol Shift untuk memindah barang cepat.
     */
    fun defaultButtons(): List<CustomControlButton> {
        return listOf(
            CustomControlButton(
                id = "sys_fps", name = "FPS",
                xPercent = 11f, yPercent = 9f, widthDp = 84, heightDp = 26,
                opacity = 0.9f, cornerRadiusDp = 13,
                isSystem = true, systemAction = "FPS"
            ),

            // Tombol pintas di tengah atas
            CustomControlButton(
                id = "key_esc", name = "ESC", icon = "pause",
                keyCode = LwjglGlfwKeycode.GLFW_KEY_ESCAPE,
                xPercent = 41f, yPercent = 9f, widthDp = 38, heightDp = 34,
                opacity = 0.9f, cornerRadiusDp = 12
            ),
            CustomControlButton(
                id = "key_t", name = "CHAT", icon = "chat",
                keyCode = LwjglGlfwKeycode.GLFW_KEY_T,
                xPercent = 47f, yPercent = 9f, widthDp = 38, heightDp = 34,
                opacity = 0.9f, cornerRadiusDp = 12,
                showMode = ControlShowMode.GAME
            ),
            CustomControlButton(
                id = "key_inv", name = "INV", icon = "inventory",
                keyCode = LwjglGlfwKeycode.GLFW_KEY_E,
                xPercent = 53f, yPercent = 9f, widthDp = 38, heightDp = 34,
                opacity = 0.9f, cornerRadiusDp = 12
            ),
            CustomControlButton(
                id = "key_f5", name = "F5",
                keyCode = LwjglGlfwKeycode.GLFW_KEY_F5,
                xPercent = 59f, yPercent = 9f, widthDp = 38, heightDp = 34,
                opacity = 0.9f, cornerRadiusDp = 12,
                showMode = ControlShowMode.GAME
            ),

            // Tombol sistem kecil di kanan atas
            CustomControlButton(
                id = "sys_keyboard", name = "KEYBOARD",
                xPercent = 87f, yPercent = 9f, widthDp = 32, heightDp = 32,
                opacity = 0.9f, cornerRadiusDp = 16,
                isSystem = true, systemAction = "KEYBOARD"
            ),
            CustomControlButton(
                id = "sys_hide_gui", name = "HIDE GUI",
                xPercent = 91.5f, yPercent = 9f, widthDp = 32, heightDp = 32,
                opacity = 0.9f, cornerRadiusDp = 16,
                isSystem = true, systemAction = "HIDE_GUI"
            ),
            CustomControlButton(
                id = "sys_close", name = "MENU",
                xPercent = 96f, yPercent = 9f, widthDp = 32, heightDp = 32,
                opacity = 0.9f, cornerRadiusDp = 16,
                isSystem = true, systemAction = "MENU"
            ),

            // Kiri: gerak
            CustomControlButton(
                id = "joy_move", name = "JOYSTICK", isJoystick = true,
                xPercent = 15f, yPercent = 70f, widthDp = 120, heightDp = 120,
                opacity = 0.9f, cornerRadiusDp = 60,
                showMode = ControlShowMode.GAME
            ),
            CustomControlButton(
                id = "key_sneak", name = "SNEAK", icon = "sneak",
                keyCode = LwjglGlfwKeycode.GLFW_KEY_LEFT_SHIFT, isToggle = true,
                xPercent = 29.5f, yPercent = 82f, widthDp = 52, heightDp = 52,
                opacity = 0.9f, cornerRadiusDp = 26,
                showMode = ControlShowMode.GAME
            ),

            // Saat menu terbuka (inventori, peti): tahan Shift lalu ketuk barang = pindah cepat.
            CustomControlButton(
                id = "key_shift_menu", name = "SHIFT",
                keyCode = LwjglGlfwKeycode.GLFW_KEY_LEFT_SHIFT, isToggle = true,
                xPercent = 12f, yPercent = 80f, widthDp = 56, heightDp = 44,
                opacity = 0.9f, cornerRadiusDp = 14,
                showMode = ControlShowMode.MENU
            ),

            // Kanan: aksi utama di sekitar jempol
            CustomControlButton(
                id = "mouse_attack", name = "SERANG", icon = "attack",
                isMouseButton = true, mouseButton = LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT,
                xPercent = 84f, yPercent = 62f, widthDp = 72, heightDp = 72,
                opacity = 0.9f, cornerRadiusDp = 36,
                showMode = ControlShowMode.GAME
            ),
            CustomControlButton(
                id = "mouse_use", name = "PAKAI", icon = "use",
                isMouseButton = true, mouseButton = LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT,
                xPercent = 75.5f, yPercent = 84f, widthDp = 60, heightDp = 60,
                opacity = 0.9f, cornerRadiusDp = 30,
                showMode = ControlShowMode.GAME
            ),
            CustomControlButton(
                id = "key_jump", name = "JUMP", icon = "jump",
                keyCode = LwjglGlfwKeycode.GLFW_KEY_SPACE,
                xPercent = 92f, yPercent = 80f, widthDp = 70, heightDp = 70,
                opacity = 0.9f, cornerRadiusDp = 35,
                showMode = ControlShowMode.GAME
            ),
            CustomControlButton(
                id = "widget_scroll", name = "HOTBAR", isScroll = true,
                xPercent = 96.5f, yPercent = 36f, widthDp = 40, heightDp = 92,
                opacity = 0.9f, cornerRadiusDp = 14,
                showMode = ControlShowMode.GAME
            )
        )
    }

    /** [imported] ditambah tombol sistem bawaan yang belum ada di dalamnya (menu game tetap terjangkau). */
    fun withSystemButtons(imported: List<CustomControlButton>): List<CustomControlButton> {
        fun actionOf(b: CustomControlButton) = if (b.systemAction == "CLOSE") "MENU" else b.systemAction

        val result = ArrayList<CustomControlButton>(imported)
        val present = result.filter { it.isSystem }.map { actionOf(it) }.toSet()
        for (d in defaultButtons()) {
            if (!d.isSystem || actionOf(d) in present) continue
            var id = d.id
            var n = 2
            while (result.any { it.id == id }) id = d.id + "_" + n++
            result.add(if (id == d.id) d else d.copy(id = id))
        }
        return result
    }

    /** Membaca berkas layout yang dipilih pemain (SAF). Posisi tombol dari launcher lain dihitung terhadap layar ini. */
    fun readImport(context: Context, uri: Uri): ControlLayoutIO.ImportResult {
        return try {
            val input = context.contentResolver.openInputStream(uri)
                ?: return ControlLayoutIO.ImportResult.Failed(ControlLayoutIO.Reason.NOT_LAYOUT)
            val out = ByteArrayOutputStream()
            input.use {
                val buf = ByteArray(8 * 1024)
                while (out.size() <= ControlLayoutIO.MAX_FILE_BYTES) {
                    val n = it.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n)
                }
            }
            if (out.size() > ControlLayoutIO.MAX_FILE_BYTES) {
                return ControlLayoutIO.ImportResult.Failed(ControlLayoutIO.Reason.TOO_LARGE)
            }
            // Posisi tombol dari launcher lain dihitung terhadap layar perangkat ini (dalam bentuk landscape)
            val dm = context.resources.displayMetrics
            val density = if (dm.density > 0f) dm.density else 1f
            val longDp = maxOf(dm.widthPixels, dm.heightPixels) / density
            val shortDp = minOf(dm.widthPixels, dm.heightPixels) / density
            ControlLayoutIO.parse(String(out.toByteArray(), Charsets.UTF_8).removePrefix("﻿"), longDp, shortDp)
        } catch (_: Exception) {
            ControlLayoutIO.ImportResult.Failed(ControlLayoutIO.Reason.NOT_LAYOUT)
        }
    }

    /** Nama berkas yang dipilih tanpa ekstensi (nama preset hasil impor), atau "Imported" bila tidak terbaca. */
    fun displayName(context: Context, uri: Uri): String {
        val raw = try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        } catch (_: Exception) {
            null
        }
        val base = (raw ?: uri.lastPathSegment ?: "").substringAfterLast('/').substringBeforeLast('.')
        return ControlPresetManager.cleanName(base).ifEmpty { "Imported" }
    }

    /** Folder preset; preset bawaan (Standar, WASD, Panah) disalin ke sana sekali. */
    fun presetDir(context: Context): File {
        val dir = ControlPresetManager.dir(context)
        ControlPresetManager.ensureBuiltins(dir, defaultButtons())
        return dir
    }

    /** Semua preset, yang terbaru di atas (bawaan paling bawah). */
    fun presets(context: Context): List<ControlPresetManager.Preset> = ControlPresetManager.list(presetDir(context))

    /**
     * Tombol milik preset [presetId] siap pakai (tombol sistem dilengkapi), atau null bila id kosong, preset sudah
     * dihapus, atau isinya rusak. Tidak menulis apa pun.
     */
    fun presetButtons(context: Context, presetId: String?): List<CustomControlButton>? {
        if (presetId.isNullOrBlank()) return null
        val buttons = ControlPresetManager.read(ControlPresetManager.dir(context), presetId)
        if (buttons.isNullOrEmpty()) return null
        return withSystemButtons(buttons)
    }

    /** Tombol yang dipakai saat PLAY: preset pilihan pemain, atau tata letak bawaan bila tidak ada atau rusak. */
    fun activeButtons(context: Context, presetId: String?): List<CustomControlButton> =
        presetButtons(context, presetId) ?: defaultButtons()

    /** Menyimpan hasil impor sebagai preset bernama [name]; tombol sistem yang kurang dilengkapi lebih dulu. */
    fun saveImported(context: Context, name: String, buttons: List<CustomControlButton>): ControlPresetManager.SaveResult =
        ControlPresetManager.saveButtons(presetDir(context), name, withSystemButtons(buttons))

    fun deletePreset(context: Context, id: String): Boolean = ControlPresetManager.delete(ControlPresetManager.dir(context), id)
}
