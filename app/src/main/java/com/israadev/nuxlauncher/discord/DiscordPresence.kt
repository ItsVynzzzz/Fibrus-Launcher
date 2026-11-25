package com.israadev.nuxlauncher.discord

import android.app.Activity

/**
 * Stub. Kelas Java mesin Starling (patch amber5/0001) memanggil ini hanya bila BuildConfig.HAS_DISCORD_SDK true;
 * di Fibrus nilainya selalu false. Kelas ini ada supaya kode hasil patch tetap bisa dikompilasi.
 */
object DiscordPresence {
    @Suppress("UNUSED_PARAMETER")
    fun onEngineStart(activity: Activity, requested: Boolean, mcVersion: String) = Unit

    @Suppress("UNUSED_PARAMETER")
    fun stopBlocking(timeoutMs: Long = 800L) = Unit
}
