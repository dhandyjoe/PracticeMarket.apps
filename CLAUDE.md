# CLAUDE.md — PracticeMart

Panduan untuk agent (Claude Code) dan kontributor yang bekerja di repo ini.

## Project
- **PracticeMart**: aplikasi marketplace Android untuk **latihan automation testing mobile** (Appium, Maestro). **PracticePay** adalah fitur dompet digital (tab) di dalamnya.
- Package `com.automation.practice`, satu modul `:app`, `minSdk 24`, `targetSdk/compileSdk 35`.
- **Full offline**: tanpa backend, tanpa permission `INTERNET`, tanpa Firebase/analytics/Google Play Services.
- Data aplikasi disimpan **permanen di database lokal** (Room `practice.db`): bertahan saat app di-kill dan saat update APK; hilang hanya lewat *Clear data* Android, uninstall, "Reset semua data", atau toggle "Reset data setiap app dibuka".

## Sumber kebenaran
| Dokumen | Isi |
|---|---|
| `PRD.md` | Fitur, FR, skenario E2E, test data, kontrak locator |
| `ARCHITECTURE.md` | Keputusan teknis, tech stack, skema database (§6) |
| `DESIGN.md` | Token warna/tipografi/spacing, spesifikasi komponen & layar |
| `docs/locators/*.md` | Kontrak locator per area (public API untuk learner) |

Urutan bila konflik: **PRD → DESIGN → canvas desain**. Keputusan teknis mengikuti ARCHITECTURE.

## Build & test
JDK tidak ada di PATH. Selalu set `JAVA_HOME` dulu:
```
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
./gradlew :app:testDebugUnitTest      # unit test
./gradlew :app:assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:lintDebug
```
- Tidak ada emulator. HP fisik hanya kadang tersambung: cek `adb devices` sebelum test on-device.
- Pasang update tanpa hapus data: `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
- Inspeksi database (build debug): `adb shell run-as com.automation.practice sqlite3 databases/practice.db` (bila `sqlite3` tersedia di device) atau Android Studio › App Inspection › Database Inspector.

## Struktur kode
```
app/src/main/java/com/automation/practice/
├── App.kt, MainActivity.kt      # Application + host bottom navigation
├── di/AppContainer.kt           # manual DI (tanpa Hilt)
├── core/ui/                     # komponen bersama, Compose (PracticePay), LocatorHints
├── data/db/                     # AppDatabase (Room), entity, DAO, RoomShopStore
├── data/prefs/SettingsStore.kt  # DataStore: pengaturan & Developer Options
├── data/repo/                   # AuthRepository, CatalogRepository, ShopSession, OrderSimulator
├── domain/                      # aturan bisnis Kotlin murni (harga, voucher, kartu, wallet, hash)
└── feature/auth, feature/shop   # layar (Views + ViewBinding; PracticePay pakai Compose)
```
Seed default di `app/src/main/assets/seed/` (`users.json`, `catalog.json`) dan konstanta `SEED_*` di `domain/`.

## Aturan data & database
- Semua data aplikasi (user, sesi login, keranjang, wishlist, riwayat pencarian, voucher aktif, stok, pesanan, alamat, profil, saldo & PIN PracticePay, transaksi) ada di **Room `practice.db`**. Pengaturan & Developer Options ada di **`SettingsStore` (DataStore)**. Daftar lengkap tabel: ARCHITECTURE.md §6.4.
- State di memori (`StateFlow` di repository) adalah cache; setiap mutasi **langsung ditulis** ke Room (write-through, sinkron) supaya data aman walau app di-kill sesaat setelah aksi.
- Data per akun selalu dikunci dengan `username`.
- **Password & PIN tidak pernah disimpan plaintext.** Pakai `Hashing.hash()` / `Hashing.verify()` (PBKDF2WithHmacSHA1 + salt; SHA256 butuh API 26).
- Uang selalu `Long` Rupiah tanpa desimal.
- **Ubah entity = naikkan `version` di `AppDatabase` + tulis `Migration`/`AutoMigration` + commit file `app/schemas/`.** Dilarang `fallbackToDestructiveMigration`: update APK tidak boleh menghapus data user.
- Unit test JVM memakai fake `ShopStore` di memori; jangan bergantung pada Room di `src/test`.

## Aturan UI & locator
- Layar mayoritas: Android Views + Material 3 + ViewBinding. Compose hanya PracticePay, root dengan `testTagsAsResourceId = true` (Riwayat sengaja tanpa `testTag`).
- resource-id, `testTag`, content-desc, dan teks assertion adalah **public API**. id baru/berubah wajib dicatat di `docs/locators/<area>.md`; id statis didaftarkan di `res/values/ids.xml`.
- Warna, ukuran teks, radius, spacing diambil dari token (`colors.xml`, `dimens.xml`, `themes.xml`, Compose `PmCompose.kt`). Tanpa hex atau dp langsung di layout.
- Teks UI default bahasa Indonesia (`values/`), terjemahan di `values-en/`.

## Konvensi kode
- Ikuti gaya sekitar: KDoc singkat berbahasa Indonesia yang merujuk PRD/ARCHITECTURE (mis. `(PRD FR-PAY-06)`).
- Jalan pintas yang disengaja diberi komentar `// shortcut: <batas>, <kapan upgrade>`.
- Dependency minimum: tanpa Hilt/Dagger, Retrofit/OkHttp, Firebase, Play Services, WorkManager.
- Aturan bisnis yang menjadi sumber assertion (harga, voucher, kartu, wallet, hash) wajib punya unit test di `app/src/test`.
- Commit: Conventional Commits, mis. `feat(db): …`, `fix(cart): …`.
