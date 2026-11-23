# Fibrus Launcher

Launcher Minecraft berbasis mesin Rust **Starling**: tanpa Fabric/Forge, tanpa JVM. UI-nya dari AmberLauncher
(tema, kaca, Home), ditulis ulang tanpa kode Java. Aplikasi langsung membuka **Home** (tanpa layar pilih akun);
akun diambil dari pil pemain di Home. Kontrol di dalam game sengaja kosong, dibuat sendiri nanti.

Diturunkan dari [AmberLauncher](https://github.com/ItsVynzzzz/AmberLauncher) (GPL-3.0). Mesin: Starling (MIT),
lihat `third_party/horizon/`.

## Status

**Belum pernah dibangun.** Ditulis tanpa Android SDK/Gradle. Sintaks Kotlin sudah diperiksa dengan kompiler, tetapi
tipe dan API Compose belum, jadi build pertama mungkin butuh perbaikan kecil.

## Isi

| Bagian | Fungsi |
|---|---|
| `ui/theme/` | Tema Amber apa adanya (warna, kaca, skala UI); hanya `NuxResponsive.kt` dilepas dari SettingsManager |
| `ui/HomeScreen.kt` | Home seperti Amber: pil pemain + menu akun, versi aktif, RAM, bar navigasi, PLAY/DOWNLOAD, figur skin |
| `ui/Pages.kt` | Tab Accounts dan Settings (info mesin, unduh/verifikasi/hapus berkas game) |
| `ui/Dialogs.kt`, `ui/Common.kt` | Dialog (tambah akun, unduhan, tentang), tombol, bar navigasi |
| `data/AccountStore.kt` | Akun offline + akun aktif (disimpan di `files/fibrus/accounts.json`) |
| `core/GameFiles.kt`, `core/Http.kt` | **Logic unduh sendiri**: berkas resmi Minecraft 26.3 (~500 MiB), cek SHA-1, bisa dilanjutkan |
| `core/DownloadController.kt` | Menjalankan unduhan di luar siklus layar (rotasi/ganti tab tidak mengulang) |
| `StarlingLauncher.kt` | Menulis akun + `horizon-args.txt` lalu membuka `dev.horizon.android.GameActivity` |
| `starling/StarlingOverlay.kt` | **Titik pasang kontrol.** Kosong; menerima `StarlingBridge` |
| `starling-engine.properties`, `third_party/horizon-patches/` | Sumber mesin dan patch-nya |

## Tanpa pilih akun, tanpa unduhan dari mesin

Mesin menanyakan akun dan mengunduh berkas sendiri bila tidak ada penanda. Fibrus menghindarinya: (1) akun aktif
ditulis ke `accounts.json` mesin sebelum game dibuka, (2) `GameFiles` mengunduh semua berkas lalu menulis
`starling-files.json` (penanda yang dibaca mesin). Tata letak dan aturan hash harus sama dengan sisi mesin
(lihat komentar di `GameFiles.kt`). Patokan versi ada di `app/src/main/assets/starling-pins.json` dan dicek build mesin.

## Langkah pertama

1. Upload isi folder ini ke repo `Fibrus-Launcher`.
2. Actions > **Build** > Run workflow (atau cukup push ke `main`). Satu run mengerjakan semuanya:
   - Release mesin untuk konfigurasi sekarang belum ada: mesin dibangun dulu (lama, butuh banyak RAM).
     Untuk percobaan cepat, isi `profile=fast` di `starling-engine.properties`.
   - Sudah ada: dipakai langsung, jadi build berikutnya hanya beberapa menit.
3. Unduh artefak **Fibrus-APK** dan pasang di HP arm64 (Android 9+). Centang `publish_apk` bila mau APK-nya
   muncul di halaman Releases.
4. Buka, tambah akun di pil pemain, tekan DOWNLOAD (sekitar 500 MiB, hanya sekali), lalu PLAY.

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

Tab Mod, instance/versi lain, akun Microsoft/Ely.by, skin kustom (figur memakai Steve), tampilan 3D skin,
pembaruan otomatis, dan pelaporan crash. Semuanya ada di AmberLauncher bila mau dipindahkan bertahap.

Sudah ada: unduhan berkas game berjalan di layanan latar dengan notifikasi progres (`core/DownloadService.kt`),
serta pengaturan aksen dan redup latar di tab Settings (`data/UiPrefs.kt`).
