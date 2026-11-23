package com.israadev.nuxlauncher

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException

/**
 * Membuka mesin Rust Starling. Dasarnya StarlingEngine milik AmberLauncher, dipangkas: hanya akun offline, tanpa
 * Microsoft, tanpa instance, tanpa kontrol, tanpa Discord.
 *
 * Antarmuka ke mesin (tanpa patch Rust):
 *  - files/starling/accounts/accounts.json  pemain offline yang dipilih
 *  - files/starling/horizon-args.txt         satu argumen/variabel lingkungan per baris
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

    /** Versi Minecraft yang dikerjakan Starling (dipatok di research-lock.json sumbernya). */
    const val MC_VERSION = "26.3"

    sealed interface Availability {
        data object Ready : Availability
        data class Unavailable(val reason: String) : Availability
    }

    fun availability(context: Context): Availability {
        if (Build.VERSION.SDK_INT < MIN_SDK) return Availability.Unavailable("Butuh Android 9 (API 28) ke atas.")
        if (!android.os.Process.is64Bit() || "arm64-v8a" !in Build.SUPPORTED_64_BIT_ABIS) {
            return Availability.Unavailable("Hanya untuk HP 64-bit (arm64).")
        }
        if (!File(context.applicationInfo.nativeLibraryDir, LIBRARY_FILE).isFile) {
            return Availability.Unavailable("Mesin Starling belum ada di APK ini. Lihat docs/rust-engine.md.")
        }
        return Availability.Ready
    }

    /** Nama pemain offline: 1 sampai 16 huruf, angka, atau garis bawah (aturan akun offline Starling). */
    fun validName(name: String): Boolean =
        name.length in 1..16 && name.all { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it == '_' }

    /** Folder data mesin: sama dengan getFilesDir() milik GameActivity di proses :starling. */
    private fun home(context: Context) = File(context.filesDir, HOME_DIR)

    /** true bila peluncuran dimulai. Pesan kegagalan dikembalikan lewat [onError]. */
    fun launch(context: Context, playerName: String, onError: (String) -> Unit): Boolean {
        val status = availability(context)
        if (status is Availability.Unavailable) {
            onError(status.reason)
            return false
        }
        if (!validName(playerName)) {
            onError("Nama harus 1 sampai 16 huruf, angka, atau garis bawah.")
            return false
        }
        return try {
            val dir = home(context)
            if (!dir.isDirectory && !dir.mkdirs()) throw IOException("Folder data mesin tidak bisa dibuat")
            writeAccountsFile(dir, playerName)
            // AMBER_EMBEDDED=1: mesin tahu ia dijalankan launcher (pemberitahuan pembaruan bawaannya mati).
            // AMBER_CONTROLS=1: kontrol sentuh, tombol hub, dan menu pop-up bawaan mesin MATI. Layar kosong;
            //   kontrolmu sendiri dipasang lewat starling/StarlingOverlay.kt.
            writePrivate(File(dir, ARGS_FILE), "AMBER_EMBEDDED=1\nAMBER_CONTROLS=1\n")
            context.startActivity(
                Intent()
                    .setClassName(context.packageName, ACTIVITY_CLASS)
                    .putExtra(EXTRA_MC_VERSION, MC_VERSION)
                    .putExtra(EXTRA_INSTANCE_NAME, "Fibrus")
                    // Bila mesin masih berjalan di latar, kembali ke sana, bukan membuka yang kedua.
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
                ActivityOptions.makeCustomAnimation(context, 0, 0).toBundle()
            )
            true
        } catch (e: Exception) {
            onError("Starling gagal dibuka: ${e.message}")
            false
        }
    }

    /** accounts/accounts.json milik mesin: memilih [offlineName] sebagai pemain. Akun yang sudah ada dipertahankan. */
    private fun writeAccountsFile(home: File, offlineName: String) {
        val folder = File(home, "accounts")
        if (!folder.isDirectory && !folder.mkdirs()) throw IOException("Folder akun mesin tidak bisa dibuat")
        val file = File(folder, "accounts.json")
        val json = try {
            JSONObject(file.readText())
        } catch (_: Exception) {
            JSONObject()
        }
        val offline = json.optJSONArray("offline") ?: JSONArray()
        if ((0 until offline.length()).none { offline.optString(it) == offlineName }) offline.put(offlineName)
        if (!json.has("accounts")) json.put("accounts", JSONArray())
        json.put("offline", offline)
        json.put("selected", JSONObject.NULL)
        json.put("offline_selected", offlineName)
        writePrivate(file, json.toString())
    }

    /** Menulis lewat berkas sementara, izin hanya untuk aplikasi ini. */
    private fun writePrivate(target: File, text: String) {
        val temp = File(target.parentFile, "${target.name}.tmp")
        temp.writeText(text)
        temp.setReadable(false, false)
        temp.setReadable(true, true)
        temp.setWritable(false, false)
        temp.setWritable(true, true)
        target.delete()
        if (!temp.renameTo(target)) {
            temp.delete()
            throw IOException("${target.name} tidak bisa disimpan")
        }
    }
}
