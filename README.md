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
tombol hub, dan menu pop-up bawaannya (patch `fibrus1/0005-amber-controls.diff`). Kontrol sendiri dipasang di
`StarlingOverlay.attach()` lewat `StarlingBridge`, atau, lebih baik untuk performa, lewat patch Rust milikmu.

## Dua jenis kontrol: Fibrus Pad dan kontrol Amber

Pilih di Settings > Controls: **Fibrus Pad** (bawaan, paling ringan), **Amber layout**, atau **Off**. Yang menyala
hanya satu. Dua-duanya digambar dan diproses di dalam mesin, tanpa jendela Android di atas game.

**Amber layout** memakai sistem kontrol AmberLauncher (tata letak ala Zalith: tombol kustom, joystick, preset). Saat PLAY,
`core/horizon/RustTouchLayout.kt` (disalin dari Amber) menerjemahkan tata letak yang dipilih ke `files/starling/horizon-touch.json`
(gaya `goy-control`), lalu kontrol sentuh bawaan mesin menggambarnya: kaca gelap, garis putih tipis, kuning saat ditekan.
Mesin tidak berubah untuk ini (`AMBER_CONTROLS` dan `FIBRUS_PAD` tidak ditulis; `FIBRUS_NO_TOUCH_TEXTURES=1` ditulis supaya
mesin tidak mengunduh tekstur tombol Bedrock, patch `0009`).

- Preset bawaan Amber: Standar (joystick), WASD tanpa analog, Panah tanpa analog. Dipilih di Settings > Controls.
- **Import layout**: berkas layout dari Amber, PojavLauncher, Zalith, atau Mojo (diubah otomatis oleh
  `ForeignLayoutConverter`), disimpan sebagai preset. Tombol sistem yang kurang (ESC/MENU, HIDE GUI) ditambahkan lagi supaya
  menu game tetap terjangkau.
- Kode Amber yang dibawa: `core/controls/` (model tombol, preset, impor, konverter) dan `core/horizon/RustTouchLayout.kt`.
  Berkas mesin asli disalin ke `horizon-touch.amber-backup.json` dan dikembalikan bila Amber layout dimatikan.
- Yang tidak dibawa: editor tata letak di dalam game (Amber memakai jendela terpisah yang membebani GPU), pemutar makro
  (tombol makro dilewati, kombo tombol tetap jalan), sidebar Amber (MENU = Esc), dan tombol FPS/keyboard layar. Kecepatan
  kamera dan opsi Pad tidak berlaku di mode ini; kecepatan kamera ikut setelan mesin.
- Geser dari tombol serang/pakai/lompat ikut memutar kamera (bisa dimatikan di Settings).

## Kontrol: Fibrus Pad (di dalam mesin, tanpa beban)

Kontrol bawaan mesin (Bedrock) tidak dipakai di mode ini (sistem kontrol mesin, editor, hub, dan overlay Android mati). Fibrus
Pad adalah modul kecil di mesin (`third_party/horizon-patches/fibrus1/0007-fibrus-pad.diff`, file
`game/mobile_controls/fibrus_pad.rs` dan `fibrus_pad/logic.rs`):

- Sentuhan sudah tiba di thread game mesin, jadi langsung diolah di sana: tanpa JNI, tanpa jendela Android di atas game,
  tanpa antrean/kunci, tanpa alokasi per sentuhan (larik tetap di tumpukan). Menyala hanya bila `FIBRUS_PAD` ada di
  `horizon-args.txt` (launcher menulisnya; matikan di Settings > Controls). `FIBRUS_PAD_SENS` mengatur kecepatan kamera.
- Tampilan: pelat datar semua, tanpa tekstur. Empat tombol bulat di kanan (JUMP, SNEAK, HIT, USE), dua baris tombol
  kecil di kanan atas (ESC, INV, CHAT, DROP, VIEW, SWAP, LIST, CMD, F3, F2, dan panah hotbar kiri/kanan), dan stick atau
  D-pad di kiri.
- Layar kiri (42%), mode **Stick** (bawaan): stick mengambang, di mana pun jari mendarat. Lewat sepertiga radius = jalan
  (8 arah); lewat radius ke depan = lari. Mode **D-pad** (gaya PojavLauncher/Zalith): 3 x 3 tombol tetap di kiri bawah
  (8 arah, tengahnya RUN yang menyala-padam). Pilih di Settings > Controls > Movement.
- Sisa layar: geser = putar kamera, ketuk = pakai/taruh, tahan 280 ms = tambang/pukul (kamera tetap bisa diputar).
  Ketuk hotbar = pilih slot. Menu dan layar terbuka = sentuhan kembali ditangani mesin seperti biasa dan semua tombol
  yang tertahan dilepas.
- Ukuran tombol (60 sampai 130%) dan kepekatan pelat (5 sampai 60%) diatur di Settings > Controls
  (`FIBRUS_PAD_SCALE`, `FIBRUS_PAD_ALPHA`, patch `0008-fibrus-pad-dpad.diff`). Mode D-pad: `FIBRUS_PAD_MODE=dpad`.
- Logika murni (`logic.rs`, tanpa tipe mesin) punya 23 uji. Jalankan: `rustc --edition 2021 --test
  crates/horizon-client/src/game/mobile_controls/fibrus_pad/logic.rs -o t && ./t` di sumber mesin yang sudah dipatch.
- Mengubah tata letak atau tombol: `Screen::rect` dan `BUTTONS` di `logic.rs`. Tombol memakai token tombol milik game
  (`key.keyboard.space`, dst.), jadi tombol baru cukup satu entri di `Btn`.

## FPS tidak terkunci dan start ringan

- `FIBRUS_UNLOCK_FPS=1` (patch `0010-fibrus-unlock-fps.diff`; Settings > Performance > Unlocked, bawaan menyala).
  Slider Max Framerate di Minecraft tidak mematikan VSync, jadi FPS tetap ikut laju layar (60 Hz di kebanyakan HP).
  Switch ini mematikan VSync, memakai batas 260, dan melewati batas mode baterai (60 di dalam dunia, 30 di luar).
  Batas untuk jendela diminimalkan atau lama menganggur tetap berlaku. Tidak menulis apa pun ke `options.txt`.
- `0009-fibrus-lean-start.diff`: bila `AMBER_CONTROLS=1`, mesin tidak lagi mengunduh tekstur tombol sentuh Bedrock
  (satu unduhan jaringan di jalur start, dan utas yang mencoba lagi tiap start bila HP tidak punya jaringan). Tidak ada
  yang menggambar tombol itu karena Fibrus Pad memakai pelat datar.
- Sistem lain yang sudah mati di build ini: telemetri dan Discord (`android::main`), cek pembaruan (`0002`), dan utas
  heartbeat (hanya jalan bila alamat telemetri aktif). Pemangkasan lebih jauh perlu profil nyata dari HP, bukan
  tebakan: mesin punya perekam performa (`0004-amber-performance-record.diff`).

## Memodif mesin Rust

1. Salin `third_party/horizon-patches/fibrus1` menjadi `fibrus1`, ganti `patch_set=fibrus1` di `starling-engine.properties`.
2. Tambah/ubah file `.diff` (diterapkan berurutan nama). Hash isi patch ikut di tanda Release, jadi mengedit patch =
   mesin baru; jalankan lagi workflow mesin.
3. Perubahan besar: pertimbangkan fork sumber mesin dan ganti `source_repo=` dan `commit=`.

## Mengganti nama paket

`namespace` tetap `com.israadev.nuxlauncher` karena patch `fibrus1/0001` dan `0005` meng-import `R`, `BuildConfig`,
`starling.*`, dan `discord.*` dari paket itu. Untuk menggantinya: ubah import di kedua diff, pindahkan folder Kotlin,
lalu ganti `namespace` dan nama kelas di `AndroidManifest.xml` bila perlu. `applicationId` (`com.itsvynzzzz.fibrus`)
bebas diganti.

## Belum ada (sengaja)

Tab Mod, instance/versi lain, akun Microsoft/Ely.by, skin kustom (figur memakai Steve), tampilan 3D skin,
pembaruan otomatis, dan pelaporan crash. Semuanya ada di AmberLauncher bila mau dipindahkan bertahap.

Sudah ada: unduhan berkas game berjalan di layanan latar dengan notifikasi progres (`core/DownloadService.kt`),
serta pengaturan aksen dan redup latar di tab Settings (`data/UiPrefs.kt`).
