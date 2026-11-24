# CLAUDE.md — PracticeMart

Panduan untuk agent (Claude Code) dan kontributor yang bekerja di repo ini.

## Project
- **PracticeMart**: aplikasi marketplace Android untuk **latihan automation testing mobile** (Appium, Maestro). **PracticePay** adalah fitur dompet digital (tab) di dalamnya.
- Package `com.automation.practice`, satu modul `:app`, `minSdk 24`, `targetSdk/compileSdk 35`.
- **Backend = Firebase** (plan Spark): Firebase Authentication (email/password) + Cloud Firestore. Tanpa Cloud Functions, Storage, FCM, Analytics, App Check, dan tanpa Google Play Services. Butuh permission `INTERNET`.
- Akun & data dipakai bersama dengan web app PracticeMart (`../PracticeMart.web`). Bentuk data dan aturan tulis ditetapkan di `docs/contract.md`.
- **PRD M6 (v0.3.0):** Room sudah dihapus; data di Firestore (`data/firebase/FirestoreShopStore`). Data Room v0.2.x tidak dimigrasi.

## Sumber kebenaran
| Dokumen | Isi |
|---|---|
| `docs/contract.md` | Business rules, test data, skema Firestore, Security Rules, aturan tulis, teks bersama Android & web. **Menang bila berbeda dengan PRD/ARCHITECTURE** |
| `PRD.md` | Fitur, FR, skenario E2E, kontrak locator |
| `ARCHITECTURE.md` | Keputusan teknis, tech stack, antarmuka store (§6.5), prasyarat Firebase (§14.1) |
| `DESIGN.md` | Token warna/tipografi/spacing, spesifikasi komponen & layar |
| `docs/locators/*.md` | Kontrak locator per area (public API untuk learner) |

Urutan bila konflik: **contract → PRD → DESIGN → canvas desain**. Keputusan teknis mengikuti ARCHITECTURE. Mengubah `docs/contract.md` = naikkan versinya dan salin identik ke `../PracticeMart.web/docs/contract.md`.

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
- Data cloud dilihat di Firebase console (Firestore & Authentication). Test rules & transaksi memakai Firebase Emulator Suite (`firebase/`); HP fisik ke emulator di laptop lewat `adb reverse`.

## Struktur kode
```
app/src/main/java/com/automation/practice/
├── App.kt, MainActivity.kt      # Application + host bottom navigation
├── di/AppContainer.kt           # manual DI (tanpa Hilt)
├── core/ui/                     # komponen bersama, Compose (PracticePay), LocatorHints
├── data/firebase/               # FirestoreShopStore + mapper dokumen ⇄ domain
├── data/prefs/SettingsStore.kt  # DataStore: pengaturan & Developer Options
├── data/repo/                   # AuthRepository, CatalogRepository, ShopSession, OrderSimulator
├── domain/                      # aturan bisnis Kotlin murni (harga, voucher, kartu, wallet, hash)
└── feature/auth, feature/shop   # layar (Views + ViewBinding; PracticePay pakai Compose)
```
Data bundel: `app/src/main/assets/seed/catalog.json` (katalog + stok awal); data awal akun (contract §4.5) di `ShopSession.seedState`. Konstanta voucher, kartu, kontak, merchant di `domain/`.

## Aturan data & Firebase
- **Tanpa seeding.** Tidak ada script admin, akun uji bersama, atau template di Firestore. App menulis data awal akun saat login pertama dan `catalog/stock` saat pertama dibutuhkan (contract §8.3).
- **Tanpa realtime.** Jangan pakai `addSnapshotListener` / `snapshots()`. Baca dengan `get()` saat layar dibuka atau pull-to-refresh. State di memori (`StateFlow` di repository) adalah cache.
- **Aturan tulis wajib mengikuti contract §8.3:** field per akun dengan `update()` per field; pesanan, status, batal, transfer, Bayar QR lewat transaksi Firestore; Top Up lewat transaksi (kontrak v1.2). Jangan pernah menulis nilai `balance` hasil hitung di client.
- **Waktu:** semua timestamp yang ditulis ke Firestore memakai jam **asli** (`AppClock.realNow()`), juga saat mode deterministik ON. Tanggal tetap 2026-01-15 hanya untuk tampilan, DatePicker, dan segmen tanggal Order ID.
- Data per akun dikunci dengan `uid` Firebase Auth. Login selalu dengan email.
- **Password tidak pernah disimpan app** (dikelola Firebase Auth). **PIN tidak pernah plaintext:** pakai `Hashing.hash()` / `Hashing.verify()` (PBKDF2WithHmacSHA1 + salt; test vector di contract §8.6).
- Uang selalu `Long` Rupiah tanpa desimal.
- **Ubah bentuk dokumen = ikuti contract §8.5** (aditif, `schemaVersion`, default saat dibaca). Update APK tidak boleh menghapus data user.
- Unit test JVM memakai fake `ShopStore` di memori; jangan bergantung pada Firebase di `src/test`.

## Aturan UI & locator
- Layar mayoritas: Android Views + Material 3 + ViewBinding. Compose hanya PracticePay, root dengan `testTagsAsResourceId = true` (Riwayat sengaja tanpa `testTag`).
- resource-id, `testTag`, content-desc, dan teks assertion adalah **public API**. id baru/berubah wajib dicatat di `docs/locators/<area>.md`; id statis didaftarkan di `res/values/ids.xml`.
- Warna, ukuran teks, radius, spacing diambil dari token (`colors.xml`, `dimens.xml`, `themes.xml`, Compose `PmCompose.kt`). Tanpa hex atau dp langsung di layout.
- Teks UI default bahasa Indonesia (`values/`), terjemahan di `values-en/`. Teks yang ada di contract §6 wajib persis sama.

## Konvensi kode
- Ikuti gaya sekitar: KDoc singkat berbahasa Indonesia yang merujuk PRD/ARCHITECTURE/contract (mis. `(PRD FR-PAY-06)`).
- Jalan pintas yang disengaja diberi komentar `// shortcut: <batas>, <kapan upgrade>`.
- Dependency minimum: Firebase hanya `firebase-auth` + `firebase-firestore` (BoM, artefak non-`ktx`) + `kotlinx-coroutines-play-services`. Tanpa Hilt/Dagger, Retrofit/OkHttp langsung, Play Services lain, WorkManager.
- Aturan bisnis yang menjadi sumber assertion (harga, voucher, kartu, wallet, hash) wajib punya unit test di `app/src/test`.
- Commit: Conventional Commits, mis. `feat(firebase): …`, `fix(cart): …`.
