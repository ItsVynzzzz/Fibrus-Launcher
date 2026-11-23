# Fibrus Launcher

Launcher Minecraft berbasis mesin Rust **Starling**, sengaja kosongan: tanpa Fabric/Forge, tanpa JVM, tanpa kontrol
sentuh. Satu layar (nama pemain + Main). Kontrol dan fitur lain dibangun sendiri di atasnya.

Diturunkan dari [AmberLauncher](https://github.com/ItsVynzzzz/AmberLauncher) (GPL-3.0). Mesin: Starling (MIT),
lihat `third_party/horizon/`.

## Status

**Belum pernah dibangun.** Kerangka ini ditulis tanpa Android SDK/Gradle, jadi build pertama mungkin butuh perbaikan kecil.

## Isi

| Bagian | Fungsi |
|---|---|
| `app/.../MainActivity.kt` | Satu layar Compose: nama pemain offline + tombol Main |
| `app/.../StarlingLauncher.kt` | Menulis `accounts.json` + `horizon-args.txt`, lalu membuka `dev.horizon.android.GameActivity` |
| `app/.../starling/StarlingOverlay.kt` | **Titik pasang kontrol.** Kosong; menerima `StarlingBridge` (token/look/state/exit) |
| `app/.../discord/DiscordPresence.kt` | Stub supaya kelas Java mesin hasil patch bisa dikompilasi |
| `starling-engine.properties` | Sumber mesin (repo + commit), patch, mod native |
| `third_party/horizon-patches/amber5/` | Patch pada sumber mesin (dari Amber) |
| `scripts/` | Membangun mesin (CI) dan memasangnya ke APK (Gradle) |
| `.github/workflows/build.yml` | Satu workflow: bangun mesin bila perlu, lalu APK |

## Langkah pertama

1. Upload isi folder ini ke repo `Fibrus-Launcher`.
2. Actions > **Build** > Run workflow (atau cukup push ke `main`). Satu run mengerjakan semuanya:
   - Release mesin untuk konfigurasi sekarang belum ada: mesin dibangun dulu (lama, butuh banyak RAM).
     Untuk percobaan cepat, isi `profile=fast` di `starling-engine.properties`.
   - Sudah ada: dipakai langsung, jadi build berikutnya hanya beberapa menit.
3. Unduh artefak **Fibrus-APK** dan pasang di HP arm64 (Android 9+). Centang `publish_apk` bila mau APK-nya
   muncul di halaman Releases.
4. Buka, isi nama, tekan Main. Pertama kali, layar Starling menyiapkan game (unduhan sekitar 500 MiB).

Mengubah `starling-engine.properties` atau patch di `third_party/horizon-patches/` otomatis berarti mesin baru.
Bila mesin gagal dibangun, APK tetap dibangun (tanpa mesin) dan run ditandai gagal.

## Kontrol kosong

`StarlingLauncher` menulis `AMBER_CONTROLS=1` ke `horizon-args.txt`. Dengan itu mesin mematikan kontrol sentuh,
tombol hub, dan menu pop-up bawaannya (patch `amber5/0005-amber-controls.diff`). Kontrol sendiri dipasang di
`StarlingOverlay.attach()` lewat `StarlingBridge`, atau, lebih baik untuk performa, lewat patch Rust milikmu.

## Memodif mesin Rust

1. Salin `third_party/horizon-patches/amber5` menjadi `fibrus1`, ganti `patch_set=fibrus1` di `starling-engine.properties`.
2. Tambah/ubah file `.diff` (diterapkan berurutan nama). Hash isi patch ikut di tanda Release, jadi mengedit patch =
   mesin baru; jalankan lagi workflow mesin.
3. Perubahan besar: pertimbangkan fork sumber mesin dan ganti `source_repo=` dan `commit=`.

## Mengganti nama paket

`namespace` tetap `com.israadev.nuxlauncher` karena patch `amber5/0001` dan `0005` meng-import `R`, `BuildConfig`,
`starling.*`, dan `discord.*` dari paket itu. Untuk menggantinya: ubah import di kedua diff, pindahkan folder Kotlin,
lalu ganti `namespace` dan nama kelas di `AndroidManifest.xml` bila perlu. `applicationId` (`com.itsvynzzzz.fibrus`)
bebas diganti.

## Belum ada (sengaja)

Akun Microsoft, daftar instance, unduhan berkas game dengan UI sendiri (memakai layar bawaan Starling), pengaturan,
pembaruan otomatis, mod, kontrol. Semua bisa ditambah bertahap; sumbernya ada di AmberLauncher.
