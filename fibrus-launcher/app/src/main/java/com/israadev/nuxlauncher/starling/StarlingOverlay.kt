package com.israadev.nuxlauncher.starling

import android.app.Activity

/**
 * Sengaja kosong, dan harus tetap ada: GameActivity hasil patch memanggilnya. Kontrol Fibrus TIDAK lewat sini lagi:
 * Fibrus Pad berjalan di dalam mesin (patch fibrus1/0007-fibrus-pad.diff), tanpa jendela Android di atas game, tanpa JNI.
 * Kelas ini hanya dipakai bila kamu ingin kontrol berbasis Android di atas game (lebih berat; Amber dulu begitu).
 *
 * Titik pasang kontrol Android. Tidak menggambar apa pun di atas game.
 *
 * Alurnya: launcher menulis AMBER_CONTROLS=1 ke horizon-args.txt (lihat StarlingLauncher). Dengan itu mesin
 * mematikan kontrol sentuh, tombol hub, dan menu pop-up miliknya sendiri (patch amber5/0005, game/mobile_controls.rs)
 * dan GameActivity milik Starling memanggil [attach] di proses :starling begitu game dibuka, lalu [detach] saat ditutup.
 *
 * Kontrolmu nanti memakai [bridge]; semuanya dipanggil dari thread UI:
 *  - bridge.token("key.keyboard.w", true/false)   tombol keyboard atau mouse (token milik game)
 *  - bridge.look(dx, dy)                          putaran kamera dalam piksel
 *  - bridge.state()                               bit 1 = game jalan, 2 = menu/layar terbuka, 4 = penunjuk ditangkap
 *  - bridge.exit()                                menutup mesin
 *
 * Catatan dari Amber: kontrol di jendela Dialog terpisah (Amber dulu begitu) membebani GPU. Pertimbangkan
 * menggambar kontrol langsung di dalam mesin Rust lewat patch fibrus sendiri.
 */
object StarlingOverlay {
    @Volatile
    var bridge: StarlingBridge? = null
        private set

    @Suppress("UNUSED_PARAMETER")
    fun attach(activity: Activity, bridge: StarlingBridge) {
        this.bridge = bridge
    }

    fun detach() {
        bridge = null
    }
}
