package com.israadev.nuxlauncher

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.os.Build
import com.israadev.nuxlauncher.data.AccountStore
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException

/**
 * Membuka mesin Rust Starling (dasarnya StarlingEngine milik AmberLauncher, dipangkas).
 *
 * Antarmuka ke mesin (tanpa patch Rust):
 *  - files/starling/accounts/accounts.json  akun offline dan yang dipilih (diambil dari [AccountStore])
 *  - files/starling/horizon-args.txt         satu variabel lingkungan per baris
 *  - files/starling/minecraft/starling-files.json  penanda berkas game lengkap (ditulis core/GameFiles.kt)
 * Karena akun sudah dipilih dan berkas sudah ada, mesin langsung membuka game: tanpa layar pilih akun, tanpa unduhan.
 */
object StarlingLauncher {
    /** Nama kelas WAJIB persis ini: fungsi JNI di libstarling_android.so terikat padanya. */
    private const val ACTIVITY_CLASS = "dev.horizon.android.GameActivity"
    private const val LIBRARY_FILE = "libstarling_android.so"
    private const val HOME_DIR = "starling"
    private const val ARGS_FILE = "horizon-args.txt"
    private const val MIN_SDK = 28

    // Kunci Intent yang dibaca GameActivity (patch amber5).
    private const val EXTRA_MC_VERSION = "amber_mc_version"
    private const val EXTRA_INSTANCE_NAME = "amber_instance"

    /** Versi Minecraft yang dikerjakan Starling (dipatok di assets/starling-pins.json). */
    const val MC_VERSION = "26.3"
    const val DISPLAY_NAME = "$MC_VERSION Starling"

    sealed interface Availability {
        data object Ready : Availability
        data class Unavailable(val reason: String) : Availability
    }

    fun availability(context: Context): Availability {
        if (Build.VERSION.SDK_INT < MIN_SDK) return Availability.Unavailable("Requires Android 9 (API 28) or newer.")
        if (!android.os.Process.is64Bit() || "arm64-v8a" !in Build.SUPPORTED_64_BIT_ABIS) {
            return Availability.Unavailable("Only 64-bit (arm64) phones are supported.")
        }
        if (!File(context.applicationInfo.nativeLibraryDir, LIBRARY_FILE).isFile) {
            return Availability.Unavailable("The Starling engine is not included in this build (see docs/rust-engine.md).")
        }
        return Availability.Ready
    }

    /** Nama pemain offline: 1 sampai 16 huruf, angka, atau garis bawah. */
    fun validName(name: String): Boolean =
        name.length in 1..16 && name.all { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it == '_' }

    /** Folder data mesin: sama dengan getFilesDir()/starling milik GameActivity di proses :starling. */
    fun home(context: Context) = File(context.filesDir, HOME_DIR)

    /** true bila peluncuran dimulai. Pesan kegagalan dikembalikan lewat [onError]. */
    fun launch(context: Context, onError: (String) -> Unit): Boolean {
        val status = availability(context)
        if (status is Availability.Unavailable) { onError(status.reason); return false }
        val account = AccountStore.state.value.active
        if (account == null) { onError("Add an account first."); return false }
        return try {
            val dir = home(context)
            if (!dir.isDirectory && !dir.mkdirs()) throw IOException("Cannot create the engine data folder")
            writeAccountsFile(dir, AccountStore.state.value.accounts.map { it.username }, account.username)
            // AMBER_EMBEDDED=1: mesin tahu ia dijalankan launcher (pemberitahuan pembaruan bawaannya mati).
            // AMBER_CONTROLS=1: kontrol sentuh, tombol hub, dan menu pop-up bawaan mesin MATI. Layar kosong;
            //   kontrolmu sendiri dipasang lewat starling/StarlingOverlay.kt.
            writePrivate(File(dir, ARGS_FILE), "AMBER_EMBEDDED=1\nAMBER_CONTROLS=1\n")
            context.startActivity(
                Intent()
                    .setClassName(context.packageName, ACTIVITY_CLASS)
                    .putExtra(EXTRA_MC_VERSION, MC_VERSION)
                    .putExtra(EXTRA_INSTANCE_NAME, DISPLAY_NAME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
                ActivityOptions.makeCustomAnimation(context, 0, 0).toBundle()
            )
            true
        } catch (e: Exception) {
            onError("Could not open Starling: ${e.message}")
            false
        }
    }

    /** accounts/accounts.json milik mesin. Kunci lain yang sudah ada (akun Microsoft, dll.) dipertahankan. */
    private fun writeAccountsFile(home: File, offlineNames: List<String>, selected: String) {
        val folder = File(home, "accounts")
        if (!folder.isDirectory && !folder.mkdirs()) throw IOException("Cannot create the accounts folder")
        val file = File(folder, "accounts.json")
        val json = try { JSONObject(file.readText()) } catch (_: Exception) { JSONObject() }
        if (!json.has("accounts")) json.put("accounts", JSONArray())
        json.put("offline", JSONArray(offlineNames))
        json.put("selected", JSONObject.NULL)
        json.put("offline_selected", selected)
        writePrivate(file, json.toString())
    }

    /** Menulis lewat berkas sementara, izin hanya untuk aplikasi ini. */
    private fun writePrivate(target: File, text: String) {
        val temp = File(target.parentFile, "${target.name}.tmp")
        temp.writeText(text)
        temp.setReadable(false, false); temp.setReadable(true, true)
        temp.setWritable(false, false); temp.setWritable(true, true)
        target.delete()
        if (!temp.renameTo(target)) { temp.delete(); throw IOException("${target.name} could not be saved") }
    }
}
