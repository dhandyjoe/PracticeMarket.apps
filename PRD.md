# PRD — PracticeMart (Android)

| Field | Isi |
|---|---|
| Dokumen | Product Requirements Document |
| Produk | **PracticeMart**: aplikasi marketplace Android untuk latihan automation mobile, dengan fitur dompet digital **PracticePay** di dalamnya. Nama tampilan `PracticeMart`, package `com.automation.practice` |
| Dokumen terkait | `docs/contract.md` v1.1 (kontrak bersama Android & web: business rules, test data, skema Firebase) · PRD Web v0.1 (`PracticeMart.web/PRD.md`) · `ARCHITECTURE.md` v0.6 (arsitektur & tech stack) · `DESIGN.md` v1.0 (design system & spesifikasi layar) · Canvas Claude Design "PracticeMart — UI Design" (judul artboard = ID layar) |
| Versi dokumen | 0.8 (Draft) |
| Tanggal | 19 November 2026 |
| Owner | Dhandy (Product & QA Automation) |
| Status | Draft, menunggu review |
| Target rilis | v1.0.0 |

**Riwayat perubahan**

| Versi | Tanggal | Perubahan | Oleh |
|---|---|---|---|
| 0.1 | 2026-11-12 | Draft awal: 17 modul latihan + capstone PracticeMart | Dhandy |
| 0.2 | 2026-11-12 | **Arah produk diubah.** Aplikasi tidak lagi berupa kumpulan modul latihan bergaya materi. Sekarang satu aplikasi simulasi yang realistis (PracticeMart + PracticePay), dan semua skill automation tersebar di fitur-fiturnya. Practice Hub, Info Sheet, dan result panel dihapus dari app. Materi, peta skill, dan locator pindah ke `docs/`. Test Controls menjadi **Developer Options**. Skill dari M01–M17 dipetakan ke fitur (§7). | Dhandy |
| 0.3 | 2026-11-12 | Keputusan arsitektur (lihat `architecture.md`): app full offline tanpa permission `INTERNET`; data sesi (keranjang, stok, pesanan, saldo) disimpan di database in-memory dan **kembali ke default setiap app di-kill**; pengaturan tetap disimpan. Disesuaikan: §6.3, §7, FR-SHOP-17, EDGE-01, EDGE-07, §10, NFR-04, NFR-14, §14, §15. | Dhandy |
| 0.4 | 2026-11-12 | Nama aplikasi ditetapkan **PracticeMart**, dengan **PracticePay** sebagai fitur di dalamnya (Q1 terjawab). Sinkronisasi dengan desain & `architecture.md`: teks persis untuk Register, Password Baru, Voucher, Bayar QR; kartu produk punya tombol "+ Keranjang"; overflow menu & Image Viewer; touch target; referensi `design.md`. | Dhandy |
| 0.5 | 2026-11-13 | **Database lokal persisten** (Room `practice.db`, lihat `architecture.md` §6): akun & password (hash), sesi login, keranjang, wishlist, stok, pesanan, alamat, profil, saldo & PIN PracticePay (hash), riwayat transaksi, riwayat pencarian **bertahan setelah app di-kill dan update APK**; hilang hanya lewat *Clear data*/uninstall/Reset. Data per akun. Toggle "Simpan data setelah app ditutup" diganti "Reset data setiap app dibuka". Disesuaikan: §6.3, §7, FR-AUTH-17, FR-SHOP-17, FR-PAY-06, EDGE-01, EDGE-07, EDGE-10, §10, NFR-14, NFR-15, §15, Lampiran A. | Dhandy |
| 0.6 | 2026-11-19 | **Akun & data pindah ke Firebase** (lihat `architecture.md` v0.4): akun & sesi login di **Firebase Authentication**, data aplikasi di **Cloud Firestore** (plan Spark); Room dihapus. Tujuannya: akun & data yang sama bisa dipakai web app PracticeMart (project terpisah). Logic, UI, dan function tetap, kecuali: Lupa Password memakai email reset Firebase (OTP & Password Baru dihapus); fitur reset data ("Reset semua data", `settings/reset`, "Reset data setiap app dibuka") dihapus; Order ID memakai nomor urut per akun; stok global untuk semua akun. Katalog & data awal akun diisi ke Firestore oleh script admin. Disesuaikan: §1, §3, §4, §5, §6, §7, FR-AUTH-01/13/14/16/17, FR-SHOP-17, FR-PAY-06, FR-PAYM-13, §8.7, §9, §10, §11, §13, §14, §15, §17, §18, §19, Lampiran A & B. | Dhandy |
| 0.7 | 2026-11-19 | **Kontrak bersama dengan web app.** Business rules (§8.7), bug `glitch_user` (§8.8), dan test data (§11) dipindah ke `docs/contract.md` agar Android & web memakai satu sumber. Sinkron antar platform saat layar dibuka/refresh (bukan realtime) dengan aturan tulis yang tidak saling menimpa. Teks Lupa Password ditetapkan di kontrak. Disesuaikan: §2, §6.3, FR-SHOP-03, FR-AUTH-13, §8.7, §8.8, §9 (EDGE-11, EDGE-12 baru), §10, §11. | Dhandy |
| 0.8 | 2026-11-19 | **Tanpa seeding & tanpa realtime** (kontrak v1.1, `ARCHITECTURE.md` v0.6). Akun uji bersama (`standard_user`, `locked_user`, `slow_user`, `glitch_user`, `new_user`) dan perilaku khusus akun dihapus; learner memakai akun hasil Register. Login memakai email. Katalog & data awal akun dibundel di APK; stok tetap global di Firestore. Lupa Password memakai teks netral (email enumeration protection aktif). Login sidik jari dihapus. Waktu yang disimpan di cloud memakai jam asli. Disesuaikan: §6.2, §6.3, §6.4, §7, FR-AUTH-01/03/05/06/09/10/12/13/15, FR-AUTH-08 & FR-AUTH-11 dihapus, FR-SHOP-03, FR-PAYM-16 baru, §8.8 dihapus, §9 (E2E-01, N-01, EDGE-05/06 dihapus, EDGE-07), §11, NFR-14, §14, §15, §17, §18, Lampiran A. Upgrade dari v0.2.x tidak memigrasi data Room. | Dhandy |
| 0.9 | 2026-11-20 | **Takeout Lupa Password; Top Up & tab Kategori dibangun.** Lupa Password di-takeout (alur OTP stub tidak bisa nyata tanpa backend, email reset tidak bisa dites dengan email palsu). Dihapus: AUTH-04, FR-AUTH-13, tautan "Lupa password?", deep link `forgot-password`, kode OTP stub, layar OTP & Password Baru. Top Up (PAY-02, FR-PAY-02) disederhanakan: input nominal → saldo langsung bertambah, tanpa PIN/bank/error flaky. Kategori (SHOP-02) menjadi grid 6 kategori. Kontrak naik ke v1.2. | Dhandy |

---

## Daftar Isi
1. Ringkasan Eksekutif
2. Latar Belakang & Problem Statement
3. Goals, Non-Goals & Success Metrics
4. Target User & Persona
5. User Stories
6. Ruang Lingkup, Arsitektur Informasi & Learning Path
7. Peta Skill → Fitur (pengganti modul latihan)
8. Functional Requirements per Area Fitur
9. Skenario E2E
10. Developer Options & Test Hooks
11. Test Data & Akun Uji
12. Strategi Locator & Kontrak Testability
13. Non-Functional Requirements
14. Aset Test & Dokumentasi Bawaan
15. Spesifikasi Teknis & Build
16. Rilis, Versioning & Distribusi
17. Roadmap & Milestone
18. Risiko & Mitigasi
19. Asumsi, Dependensi & Open Questions
20. Definition of Done v1.0
- Lampiran A: Traceability Matrix (Skill → Fitur → Appium → Maestro)
- Lampiran B: Glossary
- Lampiran C: Pemetaan dari PRD v0.1

---

## 1. Ringkasan Eksekutif
**PracticeMart** adalah aplikasi Android native yang menjadi **target latihan automation testing mobile**, seperti peran saucedemo.com atau the-internet.herokuapp.com untuk web. Bedanya dengan v0.1: aplikasi ini **bukan materi**. Learner membuka satu aplikasi yang terasa seperti app sungguhan, lalu menulis script Appium atau Maestro untuk mengotomasi fitur-fiturnya. PracticeMart terdiri dari:

- **Belanja (marketplace)**: onboarding, login, katalog, pencarian, filter, keranjang, voucher, checkout, pembayaran (kartu + 3DS WebView, virtual account, PracticePay, COD), dan pelacakan pesanan.
- **PracticePay**: fitur dompet digital di dalam PracticeMart (tab tersendiri di bottom navigation), dengan saldo, transfer ke kontak, PIN, scan QR (izin kamera), dan riwayat transaksi.
- **Akun & Pengaturan**: profil (date picker, foto), alamat (dropdown bertingkat, lokasi), pusat bantuan (WebView), dan **Developer Options** yang tersembunyi untuk kebutuhan test.

Seluruh skill dari rencana v0.1 (locator, input, gesture, list, wait, dialog, navigasi, WebView, fitur device, permission, Compose, test flaky) tetap tercakup, tetapi muncul **secara alami di fitur** (§7). Jenjang **Basic → Intermediate → Advanced** tetap ada, dalam bentuk kurikulum di `docs/` yang menunjuk ke fitur app, bukan menu di dalam app.

Akun dan data disimpan di Firebase (Authentication + Cloud Firestore) sehingga sama di aplikasi Android dan web app PracticeMart (project terpisah). Kondisi test tetap deterministik lewat Developer Options, locator konsisten dan terdokumentasi, serta kompatibel dengan **Appium (UiAutomator2 & Espresso driver)**, **Maestro**, dan tool berbasis Appium. APK dibangun lewat GitHub Actions dan dirilis gratis di GitHub Releases.

## 2. Latar Belakang & Problem Statement
**Kondisi saat ini**
- Untuk web sudah banyak situs latihan khusus (the-internet, saucedemo, automationexercise). Untuk mobile masih sangat sedikit.
- Belum ada target latihan yang **sama persis di mobile dan web** (akun, data, dan alur sama). PracticeMart juga dibangun sebagai web app (project terpisah, PRD Web) agar learner bisa membandingkan automation mobile dan web pada produk yang sama.
- APK yang biasa dipakai di tutorial (API Demos, Sauce Labs My Demo App, Maestro playground) dibuat untuk **demo atau menguji tool-nya**. Isinya terlalu sederhana atau tidak terdokumentasi.
- Kalau berlatih memakai aplikasi produksi (marketplace, e-wallet), datanya tidak stabil, ada login/OTP sungguhan, locator-nya terobfuscate, dan berisiko melanggar ToS.

**Temuan dari review v0.1**
- Desain v0.1 (Practice Hub berisi modul, "tujuan belajar" di setiap layar, Info Sheet, result panel) terasa seperti **materi**, bukan aplikasi yang diuji. Padahal keterampilan yang dicari di dunia kerja adalah mengotomasi **alur aplikasi nyata**: login, belanja, bayar, lalu memverifikasi hasilnya.

**Problem statement**
> Engineer yang belajar automation mobile tidak punya target latihan yang **terasa seperti aplikasi produksi**, tetapi tetap stabil, terdokumentasi locator-nya, bisa dimulai dari kondisi bersih, dan bisa dipakai lintas framework. Akibatnya kurva belajarnya curam, dan waktu habis untuk melawan aplikasinya, bukan mempelajari konsep automation.

**Peluang:** menyediakan "sandbox app" mobile open-source yang realistis untuk belajar, mengajar (bootcamp/kampus), dan demo framework.

## 3. Goals, Non-Goals & Success Metrics
### 3.1 Goals
| ID | Goal |
|---|---|
| G1 | Menyediakan APK siap install dan gratis untuk latihan automation mobile (butuh koneksi internet) |
| G2 | Aplikasi terasa seperti **app produksi** (marketplace + e-wallet), dan semua komponen UI Android serta interaksi sistem yang umum muncul secara alami di fiturnya |
| G3 | Kompatibel dengan Appium dan Maestro tanpa konfigurasi khusus |
| G4 | Setiap fitur punya peta skill, daftar locator, dan contoh kode di `docs/`. **Tidak ada konten materi di dalam app** |
| G5 | Menyediakan alur E2E realistis untuk latihan Page Object Model, test data, dan test isolation (lewat akun baru hasil Register) |
| G6 | Pemula bisa menjalankan test pertamanya (login) ≤ 30 menit dengan mengikuti README |
| G7 | Menyediakan kurikulum berjenjang **Basic → Intermediate → Advanced** di `docs/curriculum.md`, yang setiap tugasnya menunjuk ke fitur app |
| G8 | Semua kondisi non-deterministik (delay, acak, error server, waktu) bisa dikontrol lewat **Developer Options** atau deep link |
| G9 | Akun dan data yang sama bisa dipakai di aplikasi Android dan web app PracticeMart (dibangun di project terpisah) lewat Firebase |

### 3.2 Non-Goals (v1)
- **Tidak ada konten materi di dalam app**: tidak ada Practice Hub berisi modul, teks "tujuan belajar", tabel locator, atau result panel khusus assertion.
- Tidak ada server backend sendiri. Backend hanya Firebase (Authentication + Cloud Firestore, plan Spark) tanpa Cloud Functions; semua aturan bisnis berjalan di client.
- Web app PracticeMart tidak dibangun di repo ini (project terpisah yang memakai project Firebase yang sama).
- Tidak ada versi iOS (lihat Future).
- Bukan aplikasi produksi; tidak ada pembayaran sungguhan atau data pribadi.
- Tidak menyediakan kursus video. Dokumentasi berbentuk teks dan contoh kode.
- Tidak mengoptimalkan untuk tablet/foldable. Aplikasi tetap bisa jalan di sana, tapi tidak diuji khusus.

### 3.3 Success Metrics
| Kategori | Metrik | Target |
|---|---|---|
| Learning | Waktu pemula dari unduh APK sampai test login pertama lulus | ≤ 30 menit (diuji ke 3 orang tester) |
| Realisme | Tester menilai app "terasa seperti app sungguhan" (skala 1–5) | rata-rata ≥ 4 |
| Kualitas | Elemen interaktif yang punya locator terdokumentasi | 100% |
| Kualitas | Flow Maestro *smoke* hijau di CI | 10 run berturut-turut tanpa gagal (dengan mode deterministik) |
| Kualitas | Resource-id duplikat yang tidak disengaja dalam satu layar | 0 (dicek script lint di CI) |
| Cakupan | Skill di Lampiran A yang punya minimal 1 fitur tempat ia muncul | 100% |
| Adopsi | Unduhan APK di GitHub Releases | ≥ 500 dalam 6 bulan setelah v1.0 (target awal) |
| Adopsi | GitHub stars | ≥ 100 dalam 6 bulan (target awal) |

## 4. Target User & Persona
| Persona | Profil | Kebutuhan utama | Pain point |
|---|---|---|---|
| **P1 – Rina, QA Manual → Automation** | 2 tahun QA manual, baru belajar Appium | Target app yang mirip pekerjaan nyata; tugas latihan bertahap; contoh kode siap jalan | Bingung kenapa elemen "tidak ketemu"; setup rumit |
| **P2 – Bima, SDET** | Sudah mahir Selenium/web, pindah ke mobile | Kasus sulit: gesture, WebView, permission, flaky, alur E2E panjang | APK demo terlalu sederhana |
| **P3 – Sari, Mentor/Bootcamp** | Mengajar kelas QA automation | Materi konsisten untuk satu kelas; data deterministik; tiap siswa memakai akun sendiri | Tiap siswa punya kondisi app berbeda |
| **P4 – Tim/Tool Evaluator** | Membandingkan framework (Appium vs Maestro) | Target yang sama untuk benchmark | Tidak ada aplikasi netral |

## 5. User Stories
| ID | Sebagai | Saya ingin | Supaya | Prioritas |
|---|---|---|---|---|
| US-01 | P1 | mengotomasi alur nyata (login → cari → beli → bayar → lacak) | pengalaman latihan sama dengan pekerjaan sehari-hari | Must |
| US-02 | P1 | membaca kurikulum per level di `docs/` beserta fitur mana yang harus diotomasi | tahu urutan belajar dan kapan siap naik level | Must |
| US-03 | P1 | melihat daftar locator per layar di `docs/locators/` | tidak perlu menebak locator | Must |
| US-04 | P1 | melakukan assertion lewat UI yang wajar (pesan error, toast, snackbar, badge, status pesanan, total harga) | belajar assertion seperti di app produksi | Must |
| US-05 | P2 | berlatih gesture kompleks (swipe galeri, pinch zoom, swipe-to-delete, drag reorder) | siap menghadapi app nyata | Must |
| US-06 | P2 | berlatih context switching NATIVE ↔ WEBVIEW (3DS, Pusat Bantuan) | bisa menguji app hybrid | Must |
| US-07 | P2 | menghadapi elemen flaky, delay, dan konten dinamis yang muncul wajar (flash sale, error server, polling status) | belajar wait & retry yang benar | Must |
| US-08 | P2 | berinteraksi dengan UI sistem (dialog izin kamera/kontak/lokasi/notifikasi) | bisa menguji app yang memakai permission | Must |
| US-09 | P3 | membuat akun baru lewat Register untuk setiap siswa/test | setiap test/siswa mulai dari data default yang sama | Must |
| US-10 | P3 | mengaktifkan mode deterministik | hasil latihan konsisten di kelas & CI | Must |
| US-11 | P4 | memakai test bawaan Appium & Maestro sebagai referensi | membandingkan framework | Should |
| US-12 | P1 | menyalakan Locator Hint Overlay di Developer Options | melihat resource-id tanpa membuka Inspector | Should |
| US-13 | P2 | mengotomasi satu bagian app yang dibangun dengan Jetpack Compose (PracticePay) | paham perbedaan locator Compose vs View | Should |

## 6. Ruang Lingkup, Arsitektur Informasi & Learning Path
### 6.1 Arsitektur informasi
```
Launch
├── AUTH-01 Onboarding (first launch)
├── AUTH-02 Login ── AUTH-03 Register
└── Main (Bottom Navigation)
    ├── Beranda ──── SHOP-03 Pencarian → SHOP-04 Hasil + Filter & Urutkan
    │            └── SHOP-05 Detail Produk → SHOP-06 Image Viewer · SHOP-07 Ulasan
    ├── Kategori ─── SHOP-02 Kategori (nested list)
    ├── PracticePay ─ PAY-01 Beranda dompet
    │             ├── PAY-02 Top Up
    │             ├── PAY-03 Transfer: pilih kontak → PAY-04 Nominal & PIN
    │             ├── PAY-05 Scan QR (izin kamera)
    │             └── PAY-06 Riwayat Transaksi
    ├── Keranjang (badge) ─ SHOP-09 → SHOP-10 Voucher → SHOP-11 Checkout
    │             → SHOP-12 Metode Pembayaran → SHOP-13 3DS (WebView) / SHOP-14 VA / PIN PracticePay
    │             → SHOP-15 Hasil Pembayaran
    └── Akun ──── ACC-01 Akun
                  ├── ORD-01 Pesanan Saya → ORD-02 Detail & Tracking → ORD-03 Batalkan · ORD-04 Ulasan
                  ├── SHOP-08 Wishlist
                  ├── ACC-02 Edit Profil · ACC-03 Daftar & Form Alamat
                  ├── ACC-04 Pengaturan (umum + Developer Options, §10)
                  └── HELP-01 Pusat Bantuan (WebView)
SYS-01 Halaman tidak ditemukan (deep link invalid)
```

### 6.2 Daftar layar
| ID | Layar | Deep link | Prioritas |
|---|---|---|---|
| AUTH-01 | Onboarding (3 slide, Lewati / Lanjut / Mulai) | – | Must |
| AUTH-02 | Login (email + password) | `practiceapp://login?username=<email>` | Must |
| AUTH-03 | Register (S&K via WebView) | `practiceapp://register` | Must |
| ~~AUTH-04~~ | ~~Lupa Password~~ (dihapus v0.9) | – | – |
| ~~AUTH-05~~ | ~~Password Baru~~ (dihapus v0.6) | – | – |
| SHOP-01 | Beranda: saldo PracticePay, banner, chip kategori, flash sale, rekomendasi | `practiceapp://shop` | Must |
| SHOP-02 | Kategori (grid 6 kategori; tap membuka Hasil terfilter) | `practiceapp://shop/category/{slug}` | Should |
| SHOP-03 | Pencarian: suggestion, riwayat, kategori populer | `practiceapp://shop/search?q=<kata>` | Must |
| SHOP-04 | Hasil pencarian + Filter & Urutkan (bottom sheet) | – | Must |
| SHOP-05 | Detail Produk | `practiceapp://shop/product/{id}` | Must |
| SHOP-06 | Image Viewer (fullscreen, pinch, double tap zoom) | – | Must |
| SHOP-07 | Ulasan (list panjang, filter bintang) | – | Should |
| SHOP-08 | Wishlist (atur urutan dengan drag) | `practiceapp://shop/wishlist` | Must |
| SHOP-09 | Keranjang | `practiceapp://shop/cart` | Must |
| SHOP-10 | Voucher (bottom sheet + input kode) | – | Must |
| SHOP-11 | Checkout | – | Must |
| SHOP-12 | Metode Pembayaran + form kartu | – | Must |
| SHOP-13 | 3D Secure (WebView) | – | Must |
| SHOP-14 | Instruksi Virtual Account | – | Must |
| SHOP-15 | Hasil Pembayaran (Berhasil / Gagal / Pending) | – | Must |
| ORD-01 | Pesanan Saya (tab status) | `practiceapp://shop/orders` | Must |
| ORD-02 | Detail & Tracking Pesanan | `practiceapp://shop/order/{orderId}` | Must |
| ORD-03 | Batalkan Pesanan (bottom sheet) | – | Must |
| ORD-04 | Beri Ulasan (rating + teks + foto) | – | Should |
| PAY-01 | PracticePay (saldo, aksi cepat, transaksi terakhir) | `practiceapp://pay` | Must |
| PAY-02 | Top Up (input nominal, saldo langsung bertambah) | `practiceapp://pay/topup` | Should |
| PAY-03 | Transfer: pilih kontak | `practiceapp://pay/transfer` | Must |
| PAY-04 | Transfer: nominal & PIN | – | Must |
| PAY-05 | Scan QR | `practiceapp://pay/qr?merchant=<id>&amount=<n>` | Must |
| PAY-06 | Riwayat Transaksi | `practiceapp://pay/history` | Must |
| ACC-01 | Akun | `practiceapp://account` | Must |
| ACC-02 | Edit Profil | – | Must |
| ACC-03 | Daftar & Form Alamat | `practiceapp://account/address` | Must |
| ACC-04 | Pengaturan + Developer Options | `practiceapp://settings` | Must |
| HELP-01 | Pusat Bantuan (WebView) | `practiceapp://help` | Must |
| SYS-01 | Halaman tidak ditemukan | – | Should |

Navigasi utama: Bottom Navigation **Beranda · Kategori · PracticePay · Keranjang (badge) · Akun**. Wishlist dipindah ke Akun.

Setiap ID layar di atas sama dengan judul artboard di canvas desain. Status desain per layar (sudah/belum didesain) tercatat di `DESIGN.md` §9.

### 6.3 Prinsip "aplikasi nyata"
1. **Tidak ada teks materi di dalam app.** Semua tujuan belajar, tugas, dan kunci jawaban ada di `docs/`.
2. **Hasil aksi terlihat lewat UI yang wajar**: pesan error inline, banner, toast, snackbar, badge, label status, total harga, Order ID. Teks-teks ini menjadi sumber assertion dan wajib **persis sama** dengan PRD (case-sensitive).
3. **Kesulitan automation muncul secara alami**, bukan dibuat-buat: delay jaringan, countdown, konten acak, error server sesekali, WebView pembayaran, dialog izin sistem.
4. **Setiap kesulitan bisa dikendalikan** lewat Developer Options atau deep link (§10), sehingga kelas dan CI tetap deterministik.
5. **Fitur khusus test disembunyikan** seperti developer menu di app produksi: Akun › Pengaturan › Developer Options, serta deep link `practiceapp://settings…`.
6. **Data tersimpan seperti app sungguhan.** Akun dan sesi login dikelola Firebase Authentication. Keranjang, wishlist, riwayat pencarian, pesanan, alamat, profil, saldo, dan data lain disimpan per akun di Cloud Firestore; stok disimpan global untuk semua akun. **Data bertahan saat app di-kill, update APK, *Clear data*, uninstall, dan saat akun yang sama dibuka di device lain atau web app.** Data terbaru dibaca saat layar dibuka atau di-refresh (tidak realtime, tanpa listener); aturan sinkron & tulis di `docs/contract.md` §7–§8.3. Katalog dan data awal akun baru dibundel di APK; tidak ada seeding dari admin dan tidak ada akun uji bersama. Tidak ada fitur reset data; test yang butuh data bersih memakai akun baru hasil Register. Pengaturan (Developer Options, bahasa, tema, "Ingat saya") disimpan lokal di device dan hilang lewat *Clear data*/uninstall, begitu juga sesi login. Detail di `ARCHITECTURE.md` §6.

### 6.4 Learning Path per Level (di `docs/curriculum.md`)
Level tetap ada, tetapi berupa daftar tugas yang menunjuk ke fitur app. Setiap level diakhiri checkpoint E2E.

| | **Basic** | **Intermediate** | **Advanced** |
|---|---|---|---|
| Target learner | Baru mulai automation mobile (dari QA manual) | Sudah bisa menulis test sederhana | Siap menangani app nyata yang kompleks |
| Kompetensi | Setup Appium/Maestro, inspeksi elemen, strategi locator, input teks, tap, assertion teks & atribut | Gesture, scroll, explicit wait, dialog & bottom sheet, navigasi & deep link, Page Object Model, data-driven test | Context NATIVE ↔ WEBVIEW, permission & UI sistem, fitur device, test flaky, test isolation, E2E lintas layar, Compose, CI |
| Fitur yang dipakai | Onboarding, Login, Register, Pencarian, Detail Produk, Edit Profil | Filter & Urutkan, Keranjang, Voucher, Checkout, Pesanan Saya, Tracking, Batalkan, Alamat, Riwayat | 3DS, VA, Pusat Bantuan, Scan QR, Transfer (Compose), notifikasi, rotasi/background, Developer Options |
| Checkpoint | **CP-B:** Register akun baru, login, cari "kaos", buka detail, pilih varian, add to cart, assert badge & subtotal; negatif kredensial salah (N-01) | **CP-I:** E2E-05 (multi-item + swipe delete), voucher data-driven (N-04), filter & sort, deep link (E2E-08), dengan POM | **CP-A:** E2E-01 penuh (3DS + notifikasi + tracking), E2E-09 (transfer PracticePay), N-09 (countdown VA), EDGE-01…12 (latency 3× & peluang error 50% menggantikan `slow_user`) |
| Contoh output learner | Suite 10–20 test untuk fitur Basic | Suite POM PracticeMart dengan test data terpisah | Suite E2E stabil yang jalan di CI dengan emulator |
| Estimasi waktu belajar | ± 1–2 minggu | ± 2–3 minggu | ± 3–4 minggu |

**Aturan learning path**
- `docs/curriculum.md` berisi 3–5 tugas per fitur, dari mudah ke sulit, beserta kriteria lulus. Kunci jawabannya ada di `docs/spoilers/`.
- `docs/skill-map.md` memuat tabel §7 (skill → fitur) dan Lampiran A.
- Contoh kode menyesuaikan level. Contoh Basic memakai locator dan assertion sederhana, Intermediate memakai POM dan explicit wait, Advanced memakai helper, retry yang benar, dan setup/teardown lewat test hooks.

---

## 7. Peta Skill → Fitur (pengganti modul latihan)
Semua skill dari modul M01–M17 di v0.1 tetap wajib tercakup. Tabel ini menentukan **di fitur mana** skill itu muncul. Requirement detailnya ada di §8.

| Area skill (dulu) | Skill | Muncul di (fitur & perilaku) | Prioritas |
|---|---|---|---|
| Locator (M01) | resource-id | Semua elemen interaktif (§12) | Must |
| | accessibility id / content-desc saja | Tombol ikon: wishlist (hati), notifikasi, kembali, hapus riwayat pencarian, salin VA/Order ID | Must |
| | text saja | Link "Daftar", "Lihat semua" | Must |
| | teks kembar + locator relatif | Tombol "+ Keranjang" di setiap kartu produk; "Ubah" di setiap baris alamat; baris list memakai id yang sama | Must |
| | label–nilai (XPath following-sibling) | Ringkasan checkout, detail pesanan, About di Developer Options | Must |
| | teks dinamis (regex / textStartsWith) | Order ID `PM-YYYYMMDD-XXXX`, countdown VA/flash sale, "Diperbarui HH:mm:ss" | Must |
| | invisible / gone | Saldo disembunyikan (`Rp••••••`), password masked, banner promo yang ditutup (GONE), empty state | Must |
| | teks terpotong (ellipsize) | Nama produk panjang di grid | Should |
| Input & form (M02, M03) | input teks, validasi, error inline | Login, Register, Alamat, Edit Profil, Transfer | Must |
| | OTP kotak terpisah, kirim ulang | Dihapus (Lupa Password di-takeout v0.9) | – |
| | input terformat | Nomor kartu per 4 digit, nominal Rupiah, nomor HP `+62` | Must |
| | checkbox, radio, switch | Ingat saya, S&K, Pilih Semua keranjang, kurir, metode bayar, asuransi, alamat utama | Must |
| | disabled & read-only | Tombol Bayar/Lanjut sebelum valid, varian stok habis, kurir Same Day, COD > limit | Must |
| Pickers (M04) | Spinner / Exposed Dropdown bertingkat | Provinsi → Kota di Form Alamat | Must |
| | AutoComplete | Kota di Form Alamat (ketik untuk mencari) | Should |
| | chip filter / choice | Filter kategori, urutkan, label alamat, chip nominal transfer | Must |
| | RangeSlider | Rentang harga di Filter | Must |
| | RatingBar | Beri Ulasan (0,5 step) | Should |
| | DatePicker | Tanggal lahir (Edit Profil) | Must |
| | DateRangePicker | Filter tanggal di Riwayat Transaksi | Should |
| | TimePicker | Jadwal kirim Same Day di Checkout | Should |
| | stepper qty | Detail Produk & Keranjang (min 1, maks 10) | Must |
| State elemen (M05) | enabled/checked/selected/focused | Tab status pesanan, toggle grid/list, chip varian, tombol Bayar | Must |
| | loading button | Bayar, Simpan Alamat | Must |
| | badge | Keranjang, filter aktif, pesanan aktif | Must |
| Gestures (M06) | tap, double tap | Double tap zoom di Image Viewer | Must |
| | long press | Tahan item riwayat pencarian → menu Hapus; tooltip ikon | Must |
| | swipe / fling | Onboarding, banner, galeri produk, katalog | Must |
| | drag & drop / reorder | Atur urutan Wishlist dengan drag handle | Must |
| | pinch zoom | Image Viewer (50–300%) | Must |
| | swipe-to-dismiss + Undo | Hapus item keranjang | Must |
| | tap by coordinate | Pilih titik di peta Form Alamat | Should |
| Lists (M07) | list panjang, scroll until visible | Katalog 60 produk, kontak transfer, ulasan | Must |
| | infinite scroll | Hasil pencarian (20 per halaman), Riwayat Transaksi | Must |
| | carousel, grid, nested | Banner, rekomendasi, Kategori | Must |
| | sticky header | Kontak A–Z, tanggal di Riwayat | Must |
| | accordion | FAQ, cara bayar VA | Must |
| | pull-to-refresh | Pesanan Saya, Riwayat | Must |
| | collapsing toolbar | Detail Produk | Should |
| Waits (M08) | delay & loading overlay | Semua loading (diatur Network latency) | Must |
| | teks berganti bertahap | Status pesanan otomatis maju (polling) | Must |
| | countdown | VA, flash sale, login terkunci | Must |
| | toast & snackbar | "Ditambahkan ke keranjang", "Nomor VA disalin", "Menghubungkan ke CS…" | Must |
| | re-render (stale element) | Flash sale dan status tracking yang diperbarui berkala | Should |
| Dialogs (M09) | AlertDialog | Konfirmasi logout, reset pengaturan, hapus alamat | Must |
| | bottom sheet | Filter & Urutkan, Voucher, Batalkan Pesanan, PIN PracticePay | Must |
| | popup / overflow menu | Detail Produk (Bagikan, Laporkan) | Must |
| | dialog non-cancelable | Pembayaran sedang diproses | Should |
| Navigasi (M10) | bottom nav, tab + swipe | Navigasi utama, Pesanan Saya (ViewPager2) | Must |
| | back stack, Up vs Back | Detail Produk → produk terkait → … | Must |
| | deep link (+ parameter, invalid) | §6.2, SYS-01 | Must |
| | `appActivity` langsung | Activity utama per area `exported=true` (§15) | Must |
| WebView (M11) | context NATIVE ↔ WEBVIEW | 3DS (SHOP-13), Pusat Bantuan (HELP-01), S&K Register | Must |
| | JS dialog | `confirm()` saat membatalkan 3DS | Must |
| | JS bridge | "Hubungi kami" di Pusat Bantuan memicu toast native | Must |
| | iframe | Form kontak di Pusat Bantuan | Should |
| Device (M12) | rotasi | Checkout tetap utuh setelah rotasi | Must |
| | keyboard & IME | Search (IME Search), OTP 3DS, field yang tertutup keyboard di Form Alamat | Must |
| | clipboard | Salin nomor VA & Order ID; tempel kode voucher | Must |
| | notifikasi | Pesanan berhasil, status pesanan, transfer masuk | Must |
| | background / kill / persistence | Background: checkout & keranjang tetap utuh. Kill lalu buka lagi: masih login, keranjang, saldo, pesanan utuh (persistence). Test isolation: akun baru hasil Register; `clearState` hanya mengeluarkan sesi dan mengembalikan pengaturan | Must |
| | status jaringan | Banner "Tidak ada koneksi" & tombol Bayar disabled | Must |
| | lokasi | "Gunakan lokasi saat ini" di Form Alamat | Must |
| | biometrik | Konfirmasi transfer (login sidik jari dihapus v0.8) | Should |
| | tema | Terang/Gelap/Sistem di Pengaturan | Should |
| Permission (M13) | runtime permission & UI sistem | Kamera (Scan QR, foto profil), Kontak (Transfer), Lokasi (Alamat), Notifikasi (first launch, Android 13+) | Must |
| | ditolak / ditolak permanen | Banner + "Buka Pengaturan" | Must |
| | Photo Picker / SAF | Foto profil, QR dari galeri, foto ulasan | Must |
| | share sheet, buka app lain | Bagikan produk; telepon CS (dialer) | Should |
| Media (M14) | lazy load gambar | Placeholder gambar produk | Should |
| | visual baseline | Onboarding slide 1 (statis) | Should |
| | canvas | Tanda tangan penerimaan COD di Detail Pesanan | Could |
| A11y & i18n (M15) | ganti bahasa ID ↔ EN | Pengaturan (test berbasis text gagal, berbasis id tetap lulus) | Should |
| | content-desc bermakna | Semua tombol ikon | Must |
| Compose (M16) | `testTag` + `testTagsAsResourceId` | Seluruh alur Transfer PracticePay (PAY-03, PAY-04) | Should |
| | tanpa testTag (semantics saja) | Riwayat Transaksi (PAY-06), sebagai pembanding | Should |
| Challenge (M17) | urutan acak | Rekomendasi di Beranda diacak tiap dibuka (mode deterministik OFF) | Must |
| | elemen bergerak | Banner auto-slide | Must |
| | overlay memblokir klik | Overlay "Memproses pembayaran" ±2 dtk | Must |
| | error flaky | "Gagal memuat, coba lagi" pada Cek Status VA (sesuai "Peluang error server") | Must |
| | jendela waktu | Tombol "Beli" flash sale aktif hanya selama countdown | Must |
| | NBSP / spasi trailing | Dihapus v0.8 bersama `glitch_user` | – |
| | tombol tertutup keyboard | "Simpan Alamat" | Must |
| | scroll bersarang | Daftar voucher di dalam bottom sheet | Should |
| | toast super singkat | "Nomor VA disalin" (1 dtk bila Animasi OFF) | Should |

---

## 8. Functional Requirements per Area Fitur
Format: setiap requirement punya ID `FR-<AREA>-nn`. Teks yang disebut di requirement wajib **persis sama** (case-sensitive), karena teks itulah yang akan di-assert learner.

### 8.1 Auth & Onboarding (AUTH)
| ID | Requirement | Prioritas |
|---|---|---|
| FR-AUTH-01 | Onboarding 3 slide hanya tampil di first launch, termasuk setelah *Clear data*/uninstall; bisa swipe, tombol Lewati/Lanjut, slide terakhir tombol "Mulai" | Must |
| FR-AUTH-02 | Saat first launch di Android 13+, app meminta izin notifikasi setelah onboarding | Must |
| FR-AUTH-03 | Login dengan **email** + password: `shop_input_username` (field email; id dipertahankan sebagai public API), `shop_input_password`, `shop_btn_login`. Firebase Authentication `signInWithEmailAndPassword` | Must |
| FR-AUTH-04 | Toggle show/hide password, content-desc `"Tampilkan password"` / `"Sembunyikan password"` | Must |
| FR-AUTH-05 | Validasi kosong inline: `"Email wajib diisi"`, `"Password wajib diisi"` | Must |
| FR-AUTH-06 | Kredensial salah (email tidak terdaftar atau password salah): banner `shop_txt_login_error` = `"Email atau password salah"`. Gagal jaringan: `"Tidak ada koneksi. Periksa internet lalu coba lagi."`; Firebase `too-many-requests`: `"Terlalu banyak percobaan. Coba lagi nanti."` | Must |
| FR-AUTH-07 | 3× gagal berturut-turut → terkunci 30 detik, tombol disabled dengan label `"Coba lagi dalam 30 detik"` (berkurang tiap detik); hitungan di memori per device | Must |
| ~~FR-AUTH-08~~ | Dihapus v0.8 (`locked_user`; tidak ada akun uji bersama) | – |
| FR-AUTH-09 | Login sukses → Beranda dengan sapaan `"Halo, <Nama>"` (nama yang diisi saat Register, mis. `"Halo, Rina"`) | Must |
| FR-AUTH-10 | "Ingat saya" menyimpan email setelah app di-restart | Should |
| ~~FR-AUTH-11~~ | Dihapus v0.8 (login sidik jari tidak bisa dilakukan tanpa password di Firebase Authentication). Sidik jari tetap ada untuk konfirmasi transfer (FR-PAY-08) | – |
| FR-AUTH-12 | Register: nama, email (`"Format email tidak valid"`; sudah dipakai → `"Email sudah terdaftar"`), password (min 8, huruf+angka, indikator `Lemah`/`Sedang`/`Kuat`, helper `"Minimal 8 karakter, berisi huruf dan angka"`), konfirmasi (`"Konfirmasi password tidak sama"`), checkbox S&K wajib; link "Syarat & Ketentuan" membuka WebView; tombol "Daftar" disabled sampai semua valid. Sukses: akun dibuat di Firebase Authentication, `users/{uid}` ditulis, sesi otomatis dari Firebase di-`signOut`, lalu layar Login. Data awal akun (`docs/contract.md` §4.5) ditulis saat login pertama | Must |
| ~~FR-AUTH-13~~ | Dihapus v0.9 (fitur Lupa Password di-takeout; tidak ada cara mengganti password lewat app) | – |
| ~~FR-AUTH-14~~ | Dihapus v0.6 ("Kirim ulang" OTP Lupa Password; diganti email reset) | – |
| ~~FR-AUTH-16~~ | Dihapus v0.6 (layar Password Baru; password diganti lewat link di email reset) | – |
| FR-AUTH-15 | Deep link `practiceapp://login?username=<email>` mengisi field email (nama parameter dipertahankan) | Should |
| FR-AUTH-17 | Sesi login disimpan: setelah app di-kill atau di-update, app langsung membuka Beranda dengan akun yang sama sampai user Keluar. Sesi, akun hasil Register, dan password dikelola Firebase Authentication; app tidak menyimpan password sendiri | Must |

### 8.2 Belanja (SHOP-01 … SHOP-10)
| ID | Requirement | Prioritas |
|---|---|---|
| FR-SHOP-01 | Beranda menampilkan saldo PracticePay (link ke PAY-01), banner carousel auto-slide 4 dtk dengan indikator, chip 6 kategori, flash sale dengan countdown, dan grid rekomendasi | Must |
| FR-SHOP-02 | Urutan rekomendasi diacak setiap Beranda dibuka; tetap bila mode deterministik ON | Must |
| FR-SHOP-03 | Katalog 60 produk, 6 kategori, dibundel di APK (`assets/seed/catalog.json`); stok berjalan global dari Firestore `catalog/stock` (`docs/contract.md` §4.2) | Must |
| FR-SHOP-04 | Search: suggestion muncul setelah ≥ 2 karakter; riwayat 5 pencarian terakhir, bisa dihapus per item (ikon ×, long press → "Hapus") atau "Hapus semua"; empty state `"Tidak ada saran untuk \"<q>\""` | Must |
| FR-SHOP-05 | Filter (bottom sheet): kategori (multi chip), rentang harga (RangeSlider + input min/maks), rating minimal (radio), "Hanya stok tersedia" (switch); badge jumlah filter aktif di tombol Terapkan dan di ikon filter | Must |
| FR-SHOP-06 | Urutkan: Paling Relevan, Harga Terendah, Harga Tertinggi, Rating Tertinggi, Terbaru | Must |
| FR-SHOP-07 | Hasil pencarian: jumlah produk + urutan aktif (`"12 produk · Harga Terendah"`), toggle tampilan grid/list, infinite scroll 20 produk per halaman dengan footer `"Memuat produk lainnya…"`; akhir daftar `"Semua produk sudah ditampilkan"` | Must |
| FR-SHOP-07a | Setiap kartu produk (Beranda, hasil pencarian) punya tombol "+ Keranjang" (teks kembar di setiap kartu; di tampilan list berupa tombol ikon dengan content-desc `"Tambah <nama produk> ke keranjang"`). Produk stok 0 → tombol disabled `"Stok Habis"` | Must |
| FR-SHOP-08 | Detail produk: galeri (swipe + indikator `"2/5"`), harga coret + persen diskon, varian Warna × Ukuran; kombinasi stok 0 → chip disabled & tombol `"Stok Habis"` | Must |
| FR-SHOP-09 | Stepper qty: min 1, maks = stok varian (maks 10); melebihi batas → `"Maksimal pembelian 10 pcs"` | Must |
| FR-SHOP-10 | Add to Cart → snackbar `"Ditambahkan ke keranjang"` + aksi `"Lihat"`; badge keranjang bertambah | Must |
| FR-SHOP-11 | "Beli Sekarang" langsung ke Checkout hanya dengan produk tersebut | Should |
| FR-SHOP-12 | Wishlist toggle (content-desc `"Tambah ke wishlist"` / `"Hapus dari wishlist"`), tersinkron antar layar; Wishlist bisa diatur urutannya dengan drag handle | Must |
| FR-SHOP-13 | Image Viewer (dibuka dari tombol "Perbesar gambar" di galeri): pinch zoom 50–300%, double tap zoom 2×, indikator zoom `"2,0×"` saat diperbesar, swipe/tombol antar gambar, indikator `"3/5"`, thumbnail | Must |
| FR-SHOP-14 | Overflow menu (content-desc `"Menu lainnya"`) di Detail Produk: Bagikan (share sheet sistem), Laporkan | Should |
| FR-SHOP-15 | Flash sale: tombol "Beli" aktif hanya selama countdown; harga flash berlaku hanya selama countdown | Should |
| FR-SHOP-16 | Keranjang: item dari produk+varian sama digabung; checkbox per item & Pilih Semua (tri-state); ubah qty; swipe-to-delete + Undo 4 dtk; "Simpan untuk nanti" | Must |
| FR-SHOP-17 | Keranjang disimpan per akun: bertahan setelah logout lalu login ulang dengan user yang sama, setelah app di-kill, dan setelah update APK. Akun lain punya keranjang, wishlist, riwayat pencarian, pesanan, alamat, dan saldo sendiri. Data yang sama tampil saat akun dibuka di device lain atau web app | Must |
| FR-SHOP-18 | Voucher (bottom sheet dari Keranjang): input kode + tombol "Tempel" (clipboard) + "Pakai"; daftar voucher dengan tombol "Pakai"/"Lepas" (voucher kedaluwarsa disabled); hanya 1 voucher aktif; pesan error spesifik (§8.7); tombol bawah `"Selesai · <KODE> terpasang"` | Must |

### 8.3 Checkout & Pembayaran (SHOP-11 … SHOP-15)
| ID | Requirement | Prioritas |
|---|---|---|
| FR-PAYM-01 | Ringkasan harga dihitung ulang real-time (subtotal, diskon, ongkir, asuransi, biaya layanan, total) | Must |
| FR-PAYM-02 | Checkout: alamat default terpilih otomatis; bisa ganti/tambah alamat (ACC-03) | Must |
| FR-PAYM-03 | Kurir Reguler/Express/Same Day; Same Day disabled dengan label `"Tidak tersedia untuk kota ini"` di luar DKI Jakarta; bila Same Day dipilih, wajib pilih jam kirim (TimePicker, 09:00–17:00) | Must |
| FR-PAYM-04 | State checkout tetap terjaga saat rotasi dan saat app kembali dari background | Must |
| FR-PAYM-05 | Metode bayar: Kartu Kredit/Debit, Virtual Account (BCA/BNI/Mandiri), PracticePay, COD | Must |
| FR-PAYM-06 | Kartu: format nomor otomatis per 4 digit, validasi Luhn (`"Nomor kartu tidak valid"`), MM/YY tidak boleh lewat (`"Kartu sudah kedaluwarsa"`), CVV 3 digit; tombol Bayar disabled sampai valid | Must |
| FR-PAYM-07 | 3DS di WebView (`secure.practicebank.test`, HTML lokal): input OTP (`id="otp"`), Submit, "Batal" memicu JS `confirm("Batalkan transaksi?")` | Must |
| FR-PAYM-08 | VA: nomor VA + tombol "Salin" (snackbar `"Nomor VA disalin"`) + countdown (default 15 menit, diatur di Developer Options) + accordion cara bayar; "Saya sudah bayar" → sukses; kedaluwarsa → order `Dibatalkan` | Must |
| FR-PAYM-09 | "Cek Status" pada VA bisa gagal sesuai "Peluang error server" dengan pesan `"Gagal memuat, coba lagi"` | Must |
| FR-PAYM-10 | PracticePay: bottom sheet PIN 6 digit (sama dengan FR-PAY-06); saldo kurang → opsi disabled dengan label `"Saldo tidak cukup"` | Must |
| FR-PAYM-11 | COD ditolak bila total > Rp1.000.000 (opsi disabled + alasan `"Untuk total maksimal Rp1.000.000"`) | Must |
| FR-PAYM-12 | Tombol "Bayar" mencegah double submit (disabled setelah tap pertama + loading) dan menampilkan overlay non-cancelable `"Memproses pembayaran…"` ±2 dtk × latency | Must |
| FR-PAYM-13 | Hasil: `"Pembayaran Berhasil"` / `"Pembayaran Gagal"` / `"Menunggu Pembayaran"`; Order ID format `PM-YYYYMMDD-XXXX` dengan nomor urut per akun (akun baru + mode deterministik: `PM-20260115-0001`, dst.) dengan tombol salin | Must |
| FR-PAYM-14 | Order sukses → notifikasi sistem `"Pesanan PM-… berhasil dibuat"`; tap notifikasi membuka Detail Pesanan | Must |
| FR-PAYM-15 | Offline saat checkout → banner `"Tidak ada koneksi"` dan tombol Bayar disabled | Must |
| FR-PAYM-16 | Stok global bisa habis karena akun lain. Bila saat Bayar stok varian tidak cukup, pesanan tidak dibuat, keranjang tetap utuh, dan tampil `"Stok tidak cukup untuk <nama produk>"` | Must |

### 8.4 Pesanan (ORD)
| ID | Requirement | Prioritas |
|---|---|---|
| FR-ORD-01 | Pesanan Saya: tab Semua / Menunggu / Diproses / Dikirim / Selesai / Dibatalkan (bisa diklik dan di-swipe), pull-to-refresh, empty state `"Belum ada pesanan"` | Must |
| FR-ORD-02 | Tracking: status maju otomatis **Menunggu Pembayaran → Dibayar → Dikemas → Dikirim → Selesai** (interval default 10 dtk, diatur di Developer Options; mode Manual menampilkan tombol "Status berikutnya") | Must |
| FR-ORD-03 | Batalkan hanya bila status ≤ Dikemas, lewat bottom sheet; tombol "Batalkan" disabled sampai alasan dipilih; "Lainnya" memunculkan field teks | Must |
| FR-ORD-04 | Status Dikirim → tombol batal hilang, tampil `"Pesanan yang sudah dikirim tidak bisa dibatalkan"` | Must |
| FR-ORD-05 | Stok produk berkurang setelah order sukses dan kembali bila dibatalkan | Must |
| FR-ORD-06 | Pesanan Selesai → tombol "Beri Ulasan" (RatingBar 0,5, teks, foto via Photo Picker) yang muncul di halaman ulasan produk | Should |
| FR-ORD-07 | COD Selesai meminta tanda tangan penerima (canvas) → `"Tanda tangan tersimpan"` | Could |

### 8.5 PracticePay (PAY)
Seluruh alur Transfer (PAY-03, PAY-04) dibangun dengan **Jetpack Compose** (`testTag` + `testTagsAsResourceId = true`). Riwayat (PAY-06) dibangun dengan Compose **tanpa** testTag sebagai pembanding.

| ID | Requirement | Prioritas |
|---|---|---|
| FR-PAY-01 | Beranda PracticePay: saldo dengan toggle sembunyikan (`"Rp••••••"`, content-desc `"Sembunyikan saldo"` / `"Tampilkan saldo"`), aksi Top Up / Transfer / Scan QR / Riwayat, 4 transaksi terakhir | Must |
| FR-PAY-02 | Top Up: input nominal (digit saja, format Rupiah; min Rp10.000, maks Rp10.000.000) → tombol `"Top Up Rp…"` langsung menambah saldo dan mencatat transaksi masuk `"Top Up Saldo"`; tanpa PIN, tanpa pilih bank. Galat: `"Minimal top up Rp10.000"`, `"Maksimal top up Rp10.000.000"` | Should |
| FR-PAY-03 | Transfer – pilih kontak: banner izin kontak (`READ_CONTACTS`) + tombol "Izinkan"; daftar kontak sticky header A–Z, search nama/nomor, deretan "Terakhir"; empty state `"Kontak tidak ditemukan"` | Must |
| FR-PAY-04 | Bila izin kontak ditolak, daftar tetap berisi kontak uji bawaan (`docs/contract.md` §4.4) | Must |
| FR-PAY-05 | Transfer – nominal: input auto-format Rupiah, chip Rp50.000/Rp100.000/Rp200.000, catatan opsional (maks 50 karakter); `"Minimal transfer Rp10.000"`; `"Saldo tidak cukup"`; tombol Lanjut disabled bila tidak valid | Must |
| FR-PAY-06 | PIN: bottom sheet dengan PIN pad (tombol angka bertext `0`–`9`, content-desc tombol hapus `"Hapus"`), 6 indikator; PIN valid `123456`; salah → `"PIN salah. Sisa N percobaan."`; 3× salah → PracticePay terkunci 30 dtk. PIN disimpan per akun dalam bentuk hash di Firestore; hitungan salah dan kunci 30 dtk tetap berlaku walau app di-kill | Must |
| FR-PAY-07 | Transfer berhasil → `"Transfer berhasil"`, saldo berkurang, transaksi muncul di Riwayat, notifikasi sistem untuk penerima simulasi | Must |
| FR-PAY-08 | Opsi konfirmasi dengan sidik jari sebagai pengganti PIN | Should |
| FR-PAY-09 | Scan QR: meminta izin kamera (dialog sistem); ditolak → banner `"Izin kamera ditolak"` + "Buka Pengaturan"; QR juga bisa dipilih dari galeri (Photo Picker) memakai gambar QR contoh di `docs/assets/qr/` | Must |
| FR-PAY-10 | QR valid → layar konfirmasi merchant + nominal (bisa diubah untuk merchant nominal bebas) → PIN → `"Pembayaran berhasil"`. Deep link `practiceapp://pay/qr?merchant=KOPIPAGI&amount=28000` mensimulasikan hasil scan untuk emulator | Must |
| FR-PAY-11 | Riwayat: chip Semua/Masuk/Keluar, DateRangePicker, sticky header tanggal, infinite scroll (20 per halaman; hanya data Firestore, akun baru 8 transaksi sehingga halaman ke-2 baru muncul setelah >20 transaksi), pull-to-refresh `"Diperbarui HH:mm:ss"` | Must |

### 8.6 Akun, Alamat & Bantuan (ACC, HELP)
| ID | Requirement | Prioritas |
|---|---|---|
| FR-ACC-01 | Akun: profil (inisial, nama, email), menu Pesanan Saya (badge jumlah aktif), Wishlist, Daftar Alamat, PracticePay, Pengaturan, Pusat Bantuan, Developer Options; Keluar dengan dialog konfirmasi | Must |
| FR-ACC-02 | Edit Profil: nama, email, tanggal lahir (Material DatePicker, format `YYYY-MM-DD`), jenis kelamin (radio); Simpan → snackbar `"Profil tersimpan"` | Must |
| FR-ACC-03 | Form Alamat: "Gunakan lokasi saat ini" (izin lokasi, mengisi provinsi/kota dari mock location), label Rumah/Kantor/Lainnya, nama penerima, No. HP, Provinsi → Kota bertingkat, alamat lengkap, kode pos 5 digit (`"Kode pos harus 5 digit"`), switch alamat utama | Must |
| FR-ACC-04 | Tombol "Simpan Alamat" berada di bawah dan tertutup keyboard saat mengetik | Must |
| FR-ACC-05 | Pilih titik di peta (gambar peta statis, tap koordinat menampilkan pin dan teks `"Titik dipilih"`) | Should |
| FR-ACC-06 | Pengaturan umum: Bahasa (Indonesia/English, per-app locale), Tema (Sistem/Terang/Gelap), Notifikasi pesanan (switch) | Must |
| FR-HELP-01 | Pusat Bantuan: WebView HTML lokal dengan search (`data-testid="faq-search"`), accordion FAQ, tombol "Hubungi kami" (`data-testid="btn-contact"`) yang memanggil JS bridge → toast native `"Menghubungkan ke CS PracticeMart…"` | Must |
| FR-HELP-02 | `WebView.setWebContentsDebuggingEnabled(true)` untuk semua WebView sehingga context `WEBVIEW_com.automation.practice` terlihat | Must |
| FR-HELP-03 | Form kontak di dalam iframe; tombol native "Chat dengan CS" di footer | Should |
| FR-HELP-04 | "Telepon CS" membuka dialer (app eksternal) | Should |

### 8.7 Business rules (sumber kebenaran untuk assertion)
Dipindah ke **`docs/contract.md` §2** agar sama di Android & web: ongkir, asuransi, biaya layanan, voucher `HEMAT10`/`ONGKIR`/`MIN100K`/`EXPIRED` beserta pesan error, urutan kalkulasi, maks qty, limit COD, countdown VA, interval status, saldo awal, transfer, cashback, dan format Order ID.

**Contoh kalkulasi (untuk AC):** 2 × Kaos Polos Basic (Rp75.000), `HEMAT10`, Express, asuransi → **Total Rp158.000** (rincian di kontrak §2).

### 8.8 ~~Intentional bugs untuk akun `glitch_user`~~
Dihapus v0.8 bersama semua akun uji bersama.

---

## 9. Skenario E2E
Skenario ini menjadi acuan suite test bawaan (§14) dan latihan untuk learner. Tag: `smoke`, `regression`, `negative`, `edge`.

### 9.1 Happy path utama — E2E-01 (smoke)
| Step | Aksi | Expected result |
|---|---|---|
| 1 | Bersihkan state lokal (Maestro `clearState` / Appium `fullReset`), buka app | Onboarding tampil |
| 2 | Swipe 2 slide → tap "Mulai"; izinkan notifikasi | Layar login |
| 3 | Login dengan akun baru hasil Register (mis. nama `Rina`) | Beranda tampil, sapaan `"Halo, Rina"` |
| 4 | Buka pencarian, ketik `"kaos"` → pilih suggestion `"Kaos Polos Basic"` | Detail produk P001 |
| 5 | Kembali ke hasil, buka Filter: kategori Fashion, harga Rp50.000–Rp200.000, rating ≥ 4, "Hanya stok tersedia" → Terapkan | Badge filter `"4"`; semua hasil sesuai filter |
| 6 | Urutkan "Harga Terendah" | Harga hasil terurut naik |
| 7 | Buka P001 Kaos Polos Basic | Detail tampil; galeri `"1/5"` |
| 8 | Swipe galeri ke gambar 3 | Indikator `"3/5"` |
| 9 | Pilih Warna Hitam, Ukuran L; qty +1 → 2 | XL disabled; harga per item Rp75.000 |
| 10 | Add to Cart | Snackbar tampil; badge `"2"` |
| 11 | Tambah ke wishlist | content-desc berubah; muncul di Wishlist |
| 12 | Buka Keranjang | 1 item, qty 2, subtotal Rp150.000 |
| 13 | Pasang voucher `HEMAT10` | Diskon −Rp15.000 |
| 14 | Checkout → alamat default (Bandung) → kurir Express → asuransi ON | Same Day disabled; total **Rp158.000** |
| 15 | Rotasi ke landscape lalu kembali ke portrait | Semua pilihan tetap |
| 16 | Pilih Kartu `4111 1111 1111 1111`, 12/30, CVV 123 → Bayar | Overlay memproses, lalu 3DS |
| 17 | Switch ke WEBVIEW, isi OTP `123456`, Submit, kembali ke NATIVE | `"Pembayaran Berhasil"` |
| 18 | Simpan Order ID | Format `PM-YYYYMMDD-XXXX` |
| 19 | Buka notifikasi → tap notifikasi order | Detail pesanan dengan ID yang sama |
| 20 | Tunggu status sampai `"Selesai"` | Timeline lengkap 5 status |
| 21 | Beri ulasan 5 bintang | Ulasan tampil di produk P001 |
| 22 | Logout | Kembali ke login; badge keranjang 0 |

> Akun baru hasil Register mulai dengan data awal yang sama (`docs/contract.md` §4.5): alamat default Bandung, saldo Rp500.000, keranjang kosong. Stok global dipakai bersama semua learner, jadi jangan meng-assert angka stok absolut.

### 9.2 Katalog skenario
| ID | Skenario | Tag | Skill utama |
|---|---|---|---|
| E2E-01 | Happy path kartu kredit + tracking + ulasan (§9.1) | smoke | POM, WebView, wait |
| E2E-02 | Happy path Virtual Account: salin nomor VA (clipboard) → "Saya sudah bayar" | smoke | clipboard |
| E2E-03 | Bayar dengan PracticePay (PIN) → saldo berkurang sesuai total | regression | Compose, assertion saldo |
| E2E-04 | COD dengan total < Rp1.000.000 | regression | – |
| E2E-05 | Multi-item: 3 produk berbeda, hapus 1 dengan swipe, Undo, lalu checkout hanya 2 item terpilih | regression | gesture, checkbox |
| E2E-06 | "Beli Sekarang" langsung dari detail | regression | – |
| E2E-07 | Tambah alamat baru Jakarta (pakai lokasi) → Same Day tersedia → pilih jam kirim | regression | lokasi, TimePicker |
| E2E-08 | Deep link `practiceapp://shop/product/P010` → add to cart → checkout | regression | deep link |
| E2E-09 | Transfer Rp150.000 ke Budi Santoso (izin kontak, PIN) → Riwayat menampilkan transaksi | smoke | permission, Compose, sticky list |
| E2E-10 | Scan QR via deep link/galeri → bayar merchant → Riwayat | regression | permission, Photo Picker |
| E2E-11 | Edit profil: ubah tanggal lahir lewat DatePicker | regression | picker |
| E2E-12 | Pusat Bantuan: cari FAQ di WebView, tap "Hubungi kami" → toast native | regression | WebView, JS bridge |
| N-01 | Login dengan email tidak terdaftar → `"Email atau password salah"` (jangan memakai password salah berulang pada akun sungguhan; Firebase bisa membalas `too-many-requests`) | negative | assertion error |
| N-02 | Varian stok habis tidak bisa dibeli | negative | state disabled |
| N-03 | Qty melebihi batas | negative | – |
| N-04 | Voucher `EXPIRED`, `MIN100K` di bawah minimum, kode acak | negative | data-driven test |
| N-05 | Kartu ditolak `4000 0000 0000 0002` → `"Pembayaran Gagal"`, keranjang utuh | negative | – |
| N-06 | Kartu pending `4000 0000 0000 0119` → status `Menunggu Pembayaran` | negative | – |
| N-07 | COD dengan total > Rp1.000.000 tidak bisa dipilih | negative | – |
| N-08 | Nomor kartu gagal Luhn, kartu kedaluwarsa | negative | validasi |
| N-09 | Countdown VA habis (30 dtk) → order `Dibatalkan`, stok kembali | negative | wait panjang |
| N-10 | Batalkan pesanan berstatus Dikirim → tombol tidak tersedia | negative | – |
| N-11 | PIN salah 3× → PracticePay terkunci 30 dtk | negative | – |
| N-12 | Transfer melebihi saldo / di bawah Rp10.000 | negative | validasi |
| N-13 | Tolak izin kamera di Scan QR → banner + Buka Pengaturan | negative | UI sistem |
| EDGE-01 | Kill app saat di keranjang → buka lagi → masih login, keranjang, badge, dan stok sama seperti sebelum kill. Variasi: *Clear data* → harus login ulang; setelah login, keranjang tetap sama (data di cloud) | edge | lifecycle & persistence |
| EDGE-02 | App ke background 10 dtk saat checkout → kembali, state utuh | edge | lifecycle |
| EDGE-03 | Double tap tombol Bayar → hanya 1 order terbuat | edge | idempotensi |
| EDGE-04 | Offline (airplane mode) saat checkout → banner `"Tidak ada koneksi"` & tombol Bayar disabled | edge | network |
| ~~EDGE-05~~ | Dihapus v0.8 (`slow_user`). Latihan wait memakai Network latency 3× | – | – |
| ~~EDGE-06~~ | Dihapus v0.8 (`glitch_user`) | – | – |
| EDGE-07 | Stok berkurang setelah order dan tetap berkurang setelah kill, *Clear data*, dan di device lain (stok global semua akun, tidak pernah di-reset otomatis); hanya pembatalan pesanan atau VA kedaluwarsa yang mengembalikannya. Assertion memakai selisih stok sebelum/sesudah, bukan angka absolut | edge | persistence |
| EDGE-08 | Peluang error server 50%: Cek Status VA tetap lulus dengan retry yang benar | edge | retry |
| EDGE-09 | Ganti bahasa ke English → test berbasis resource-id tetap lulus | edge | i18n |
| EDGE-10 | Buat pesanan & ubah saldo → pasang APK versi baru dengan `adb install -r` → buka app → masih login, pesanan & saldo sama | edge | persistence saat update |
| EDGE-11 | Login akun yang sama di device lain atau web app → setelah layar dibuka/refresh, keranjang, wishlist, pesanan, saldo, dan alamat sama | edge | sinkronisasi cloud |
| EDGE-12 | Akun sama dibuka di HP dan web bersamaan, buat pesanan PracticePay di keduanya → dua Order ID berurutan tanpa duplikat, saldo berkurang dua kali dengan benar | edge | konkurensi |

---

## 10. Developer Options & Test Hooks
Fitur khusus test tidak tampil di alur utama. Letaknya di **Akun › Pengaturan › Developer Options** (bagian bertanda DEV), seperti developer menu di app produksi.

| ID | Fitur | Deskripsi | Prioritas |
|---|---|---|---|
| DEV-01 | Developer Options | Pengaturan test (tabel di bawah) | Must |
| DEV-02 | Test hooks via deep link | `practiceapp://settings?deterministic=true&latency=0&animations=false&flaky=0` | Must |
| DEV-03 | Locator Hint Overlay | Bila aktif, setiap elemen interaktif menampilkan label kecil berisi resource-id | Should |
| DEV-04 | About | versionName, versionCode, commit SHA, tanggal build, versi kontrak locator | Must |

**Pengaturan Developer Options**
| Pengaturan | Opsi | Default |
|---|---|---|
| Reset pengaturan (semua pengaturan di tabel ini ke default) | tombol + dialog konfirmasi | – |
| Mode deterministik (seed tetap, tanggal app 2026-01-15) | on/off | off |
| Network latency multiplier | 0× / 1× / 2× / 3× | 1× |
| Animasi | on/off | on |
| Peluang error server | 0% / 30% / 50% | 0% |
| Batas waktu VA | 30 dtk / 2 mnt / 15 mnt | 15 mnt |
| Update status pesanan | 3 / 10 / 30 dtk / Manual | 10 dtk |
| Locator Hint Overlay | on/off | off |

Bahasa dan Tema ada di Pengaturan umum (FR-ACC-06). Semantik tiap pengaturan sama dengan web app (`docs/contract.md` §5).

**Penyimpanan:** semua pengaturan di atas, bahasa, tema, status onboarding, dan email "Ingat saya" disimpan permanen di DataStore; hilang hanya lewat "Reset pengaturan", *Clear data*, atau uninstall. Akun & sesi login dikelola Firebase Authentication, data aplikasi (keranjang, pesanan, saldo, dst.) disimpan di Cloud Firestore dan bertahan saat kill, update APK, dan *Clear data* (prinsip §6.3 no. 6). Tidak ada fitur reset data.

**AC**
- *Given* mode deterministik ON, *When* Beranda dibuka dua kali, *Then* urutan rekomendasi identik.
- *Given* mode deterministik ON, *When* "Peluang error server" 30%, *Then* request pertama gagal dan request kedua berhasil.
- *Given* mode deterministik ON, *When* DatePicker dibuka tanpa nilai sebelumnya, *Then* tanggal awal adalah `2026-01-15`.

---

## 11. Test Data & Akun Uji
Dipindah ke **`docs/contract.md` §4** (sama untuk Android & web):
- §4.1 akun: **tidak ada akun uji bersama**; learner membuat akun sendiri lewat Register dan login dengan email
- §4.2 katalog 60 produk & stok awal (dibundel di APK; stok berjalan global di Firestore)
- §4.3 voucher, kartu uji, OTP 3DS, nomor VA, alamat
- §4.4 PracticePay: saldo awal, PIN, kontak uji, merchant QR
- §4.5 data awal akun baru (alamat, saldo, 8 transaksi awal, wishlist, riwayat pencarian), dibundel di APK dan ditulis saat login pertama

**Khusus Android**
| Jenis | Data |
|---|---|
| Mock location emulator | -6.2088, 106.8228 → DKI Jakarta, Jakarta Pusat |
| Gambar QR merchant (Photo Picker) | `docs/assets/qr/` |

---

## 12. Strategi Locator & Kontrak Testability
### 12.1 Konvensi penamaan resource-id
Format: `<prefix area>_<tipe>_<nama>` dalam snake_case.

| Prefix | Area | Prefix | Area |
|---|---|---|---|
| `shop_` | AUTH, SHOP, ORD (alur PracticeMart) | `addr_` | Form & Daftar Alamat |
| `pay_` | PracticePay (View & Compose `testTag`) | `help_` | Pusat Bantuan (native) |
| `acc_` | Akun, Edit Profil, Pengaturan umum | `dev_` | Developer Options |

Tipe: `btn`, `input`, `txt`, `img`, `chk`, `rdo`, `sw`, `chip`, `sld`, `spn`, `list`, `item`, `tab`, `card`, `badge`, `progress`.
Contoh: `shop_btn_add_to_cart`, `shop_txt_cart_total`, `shop_item_cart_row`, `pay_input_amount`, `addr_input_postal`.

### 12.2 Aturan
1. Setiap elemen interaktif & teks yang perlu di-assert punya resource-id **unik dalam satu layar**. Pengecualian yang disengaja (baris list, kartu produk) didokumentasikan di `docs/locators/`.
2. Baris list memakai id yang sama (`*_item_*_row`). Elemen anak punya id sendiri, jadi baris dibedakan dari teks/nilai anaknya.
3. **Content-desc hanya untuk label aksesibilitas yang bermakna bagi manusia**, wajib untuk tombol ikon. Content-desc tidak diisi dengan "test id" yang merusak pengalaman TalkBack.
4. WebView (3DS, Pusat Bantuan, S&K) memakai atribut `id` dan `data-testid`.
5. Compose (PracticePay Transfer) memakai `testTag` dengan konvensi yang sama; Riwayat sengaja tanpa testTag.
6. Urutan prioritas locator yang diajarkan di dokumentasi: **resource-id → accessibility id → UiSelector/text → XPath** (XPath sebagai jalan terakhir).
7. Locator **tidak ditampilkan di UI** kecuali Locator Hint Overlay dinyalakan.

### 12.3 Kontrak stabilitas locator
- Locator diperlakukan sebagai **public API**. Mengubah atau menghapus resource-id yang terdokumentasi = **breaking change** dan menaikkan versi MAJOR (§16).
- `docs/locators/<layar>.md` dihasilkan atau divalidasi otomatis di CI. Build gagal bila ada id di layout yang tidak terdokumentasi, atau ada id duplikat yang tidak disengaja.

---

## 13. Non-Functional Requirements
| ID | Kategori | Requirement |
|---|---|---|
| NFR-01 | Kompatibilitas OS | Android 7.0 (API 24) s.d. Android 15 (API 35); diuji di emulator API 29, 33, 35 |
| NFR-02 | Kompatibilitas tool | Appium 2.x/3.x + UiAutomator2 driver (Must), Espresso driver (Should), Maestro versi stabil terbaru (Must) |
| NFR-03 | Arsitektur CPU | Tanpa native lib; satu APK universal (x86_64 & arm64) |
| NFR-04 | Konektivitas & cloud | Butuh koneksi internet. Backend hanya Firebase Authentication + Cloud Firestore (plan Spark); tanpa analytics, Crashlytics, FCM, atau SDK cloud lain; tidak bergantung pada Google Play Services (tetap jalan di emulator AOSP). Permission `INTERNET`. Data yang sudah dimuat tetap terbaca dari cache Firestore saat offline. Hanya share sheet, dialer, dan Pengaturan sistem yang membuka app eksternal |
| NFR-05 | Determinisme | Dengan mode deterministik, dua run berurutan menghasilkan UI & data identik |
| NFR-06 | Performa | Cold start ≤ 2 dtk di emulator API 33 (host CI standar); setiap layar tampil ≤ 1 dtk (tanpa delay yang disengaja); diukur dengan sesi login & cache Firestore sudah ada |
| NFR-07 | Ukuran | APK ≤ 25 MB (naik dari 20 MB karena aset produk & QR) |
| NFR-08 | Testability | Semua delay, keacakan, error server, dan waktu bisa dikontrol dari Developer Options / deep link |
| NFR-09 | Aksesibilitas | Semua layar lolos Accessibility Scanner tanpa error kritis; touch target ≥ 48dp (ukuran 44px di artboard dibaca sebagai minimum 48dp saat implementasi, lihat `DESIGN.md` §3) |
| NFR-10 | Privasi & keamanan | Tanpa analytics; data hanya dikirim ke project Firebase PracticeMart dan dilindungi Firestore Security Rules (akun hanya bisa membaca & menulis datanya sendiri); tanpa data pribadi sungguhan selain email untuk login; permission hanya yang dibutuhkan fitur (kamera, kontak, lokasi, notifikasi, biometrik) |
| NFR-11 | Lokalisasi | Semua string di `strings.xml` (ID default, EN) |
| NFR-12 | Maintainability | Kotlin, lint & detekt bersih, struktur package per area fitur |
| NFR-13 | Realisme | Tidak ada teks materi, istilah automation, atau locator yang tampil di UI pada konfigurasi default |
| NFR-14 | Siklus data | Akun baru (hasil Register) mulai dengan data awal yang identik dengan `docs/contract.md` §4.5 (dibundel di APK, tanpa seeding) |
| NFR-15 | Persistensi | Data aplikasi di Firestore bertahan setelah kill, update APK, dan *Clear data*; perubahan struktur dokumen wajib backward compatible (tanpa menghapus data user); password dikelola Firebase Authentication, PIN hanya disimpan sebagai hash |

## 14. Aset Test & Dokumentasi Bawaan
```
tests/
├── maestro/
│   ├── features/       # 1+ flow per fitur, tag smoke
│   ├── e2e/            # E2E-01…12, N-01…13, EDGE-01…12 kecuali EDGE-05/06 (sebisa mungkin dengan Maestro)
│   └── config.yaml
├── appium-python/
│   ├── conftest.py     # capabilities, fixture fullReset + login akun hasil Register
│   ├── pages/          # Page Object per layar
│   ├── tests/features/ # contoh per fitur
│   └── tests/e2e/      # suite E2E
└── appium-java/        # (Could, v1.x) TestNG + Page Object
ARCHITECTURE.md         # arsitektur & tech stack (root repo)
DESIGN.md               # design system, komponen, spesifikasi layar (root repo)
docs/
├── contract.md         # kontrak bersama Android & web: business rules, test data, skema Firebase
├── getting-started.md  # install APK, emulator, Appium, Maestro, capabilities, koneksi internet, Register akun, data cloud & isolasi lewat akun baru
├── curriculum.md       # level Basic/Intermediate/Advanced + tugas per fitur
├── skill-map.md        # §7 + Lampiran A
├── locators/<layar>.md # tabel locator per layar
├── deep-links.md
├── assets/qr/          # gambar QR merchant untuk Photo Picker
└── spoilers/           # kunci jawaban tugas
```
- Skenario yang tidak didukung Maestro (pinch, drag & drop, notifikasi, biometrik; lihat Lampiran A) hanya disediakan versi Appium-nya, dan ditandai di dokumentasi.
- Capability minimal Appium yang didokumentasikan: `platformName=Android`, `appium:automationName=UiAutomator2`, `appium:app=<path apk>`, `appium:appPackage=com.automation.practice`, `appium:appActivity=.MainActivity`.

## 15. Spesifikasi Teknis & Build
| Aspek | Keputusan | Alasan |
|---|---|---|
| Bahasa | Kotlin | Native, hierarchy UiAutomator paling bersih |
| UI | Android Views (XML) + Material Components 3 untuk sebagian besar app; **Jetpack Compose untuk PracticePay Transfer & Riwayat** | resource-id muncul alami; Compose dipelajari di konteks nyata |
| Struktur | `MainActivity` (bottom nav + Navigation Component) + Activity terpisah untuk `AuthActivity`, `CheckoutActivity`, `PracticePayActivity`, `SettingsActivity`, `HelpActivity`, semua `exported=true` | `appActivity` jelas; deep link mudah |
| Data aplikasi | **Cloud Firestore** (plan Spark) dengan offline persistence, tanpa realtime listener: profil, keranjang, wishlist, riwayat pencarian, pesanan, alamat, saldo, PIN (hash), transaksi per akun; stok global (skema `docs/contract.md` §8.2) | sama di mobile & web; bertahan saat kill, update APK, *Clear data* |
| Autentikasi | **Firebase Authentication** email/password | sesi & password dikelola Firebase; dipakai juga oleh web app |
| Pengaturan | DataStore Preferences | Developer Options & preferensi bertahan saat kill |
| Data default | **Tanpa seeding.** Katalog, stok awal, dan data awal akun dibundel di `assets/seed/`; voucher, kartu uji, kontak, merchant konstanta di `domain/`; `assets/web/*.html`, `assets/images/` | tanpa script admin; katalog tidak memakan kuota Firestore |
| Detail | Lihat `ARCHITECTURE.md` | tech stack lengkap, skema, FakeBackend |
| Build | Gradle Kotlin DSL, version catalog, AGP 8.x, minSdk 24, target/compile 35 | standar terkini |
| Varian | `debug` (debuggable, WebView debugging aktif) sebagai artefak distribusi utama | Espresso driver & WebView context butuh debuggable |
| CI | GitHub Actions: lint + unit test + `assembleDebug` → artifact; tag `v*` → GitHub Release; workflow e2e: emulator (android-emulator-runner) + Maestro smoke | APK tanpa setup lokal; regresi app & locator terdeteksi |

## 16. Rilis, Versioning & Distribusi
- **SemVer:** MAJOR = perubahan locator/flow yang breaking; MINOR = fitur baru; PATCH = perbaikan bug.
- Distribusi: GitHub Releases (`automation-practice-vX.Y.Z.apk`) + `CHANGELOG.md` dengan bagian "Locator changes".
- Tidak dirilis ke Play Store di v1 (bisa dipertimbangkan nanti).
- Lisensi: MIT (perlu dikonfirmasi; lihat §19).

## 17. Roadmap & Milestone
| Milestone | Isi | Exit criteria |
|---|---|---|
| M0 – PRD | Dokumen ini disetujui | Open questions prioritas tinggi terjawab |
| M1 – Fondasi | Project Gradle, CI build, AUTH-01…05, Beranda, Developer Options, reset & deep link | APK terunduh dari CI; flow Maestro login hijau |
| M2 – Belanja | SHOP-02…10 (pencarian, filter, detail, image viewer, wishlist, keranjang, voucher) | Checkpoint CP-B bisa dikerjakan |
| M3 – Checkout & Pesanan | SHOP-11…15 (termasuk 3DS WebView & VA), ORD-01…04, notifikasi | E2E-01 hijau di Maestro & Appium |
| M4 – PracticePay & Akun | PAY-01…06 (Compose), ACC-01…04, HELP-01, permission | E2E-09/10/12 hijau |
| M5 – Rilis v1.0.0 | Dokumentasi lengkap (curriculum, skill-map, locators), uji pemula (3 orang), release | DoD §20 terpenuhi |
| M6 – Migrasi Firebase (v0.3.0) | Firebase Auth + Firestore menggantikan Room; tanpa seeding (data bundel di APK); tanpa realtime; Security Rules; reset data, akun uji, login sidik jari dihapus. Data Room v0.2.x tidak dimigrasi; CHANGELOG mencatat "Locator changes" (id OTP, Password Baru, `shop_btn_biometric` dihapus) | Semua Must lulus AC; data akun sama di 2 device; unit test, test rules & transaksi di Firebase Emulator, dan Maestro smoke hijau; app jalan di emulator AOSP tanpa Play Services |
| v1.x (Future) | Appium Java, fitur Could, Play Store | – |
| v2 (Future) | Versi iOS (XCUITest), "twin app" Flutter/React Native untuk belajar perbedaan locator lintas framework | – |

## 18. Risiko & Mitigasi
| Risiko | Dampak | Kemungkinan | Mitigasi |
|---|---|---|---|
| Dialog permission & notifikasi berbeda antar versi Android | Test bawaan gagal di versi tertentu | Tinggi | Matriks uji API 29/33/35; dokumentasi locator UI sistem per versi |
| Scan QR sulit di emulator (kamera virtual) | Learner terhambat | Tinggi | Deep link simulasi hasil scan + QR dari galeri (FR-PAY-09/10) |
| Emulator di CI lambat/flaky | Pipeline merah palsu | Sedang | Mode deterministik + latency 0× + error server 0% di CI; retry level job; cache AVD |
| Perubahan versi Appium/Maestro | Contoh kode usang | Sedang | Pin versi di docs & CI; review per kuartal |
| Scope creep (app realistis mudah melebar) | Rilis molor | Tinggi | Prioritas MoSCoW; v1.0 hanya Must; setiap fitur baru harus memetakan skill di §7 |
| Skill tertentu "hilang" karena tidak ada modul khusus | Cakupan belajar turun | Sedang | Metrik cakupan §3.3 dan tabel §7 dicek setiap milestone |
| Learner menyalin pola buruk (sleep, XPath absolut) | Tujuan edukasi gagal | Sedang | Contoh kode menunjukkan best practice + bagian "anti-pattern" di curriculum |
| Ukuran APK membengkak (gambar produk, Compose) | Unduhan lambat | Rendah | Batas NFR-07; gambar WebP kecil |
| Stok global dipakai bersama semua learner, tanpa reset & tanpa seeding | Stok berkurang permanen; siapa pun yang punya akun bisa mengubahnya (mis. ke 0) | Tinggi | Rules menjaga bentuk data; owner menghapus `catalog/stock` di console bila rusak, lalu app membuatnya ulang dari stok awal; test tidak meng-assert angka stok absolut (FR-PAYM-16) |
| Kuota Firebase Spark habis (50.000 read & 20.000 write per hari per project, Android + web) | App gagal memuat/menyimpan sampai kuota reset (tengah malam waktu Pasifik) | Sedang | Katalog dibundel, stok 1 dokumen, tanpa realtime, write hanya saat data berubah; pantau Usage di Firebase console |
| Limit Firebase Auth Spark: 100 akun baru per jam per IP | Satu kelas di satu jaringan gagal Register | Sedang | Siswa Register sekali di awal lalu memakai akunnya; suite hanya Register saat butuh data bersih |
| Rate limit Firebase Auth (`too-many-requests`) saat suite login berulang atau salah password berulang | Test login flaky | Sedang | Login sekali per suite lalu pakai sesi; test negatif memakai email tidak terdaftar (N-01) |
| Koneksi learner lambat atau terputus | Login & muat data pertama gagal | Sedang | Katalog dari APK; cache offline Firestore; teks error kontrak §6 |
| Upgrade dari v0.2.x (Room) | Akun & data lokal hilang | Pasti, sekali | Dicatat di CHANGELOG; learner Register ulang |
| Security Rules salah konfigurasi (config Firebase ikut di APK publik) | Data akun lain terbaca atau terubah | Rendah | Rules di repo, diuji dengan Firebase Emulator di CI |

## 19. Asumsi, Dependensi & Open Questions
**Asumsi**
- Learner memakai emulator Android (Android Studio) atau device fisik dengan USB debugging.
- Repository di GitHub publik sehingga GitHub Actions gratis.
- Learner punya koneksi internet saat memakai app.

**Dependensi:** project Firebase (plan Spark: Authentication + Cloud Firestore), GitHub Actions, reactivecircus/android-emulator-runner, Maestro CLI, Appium server + UiAutomator2 driver, Chromedriver yang cocok untuk WebView (didokumentasikan).

**Open questions**
| # | Pertanyaan | Owner | Target |
|---|---|---|---|
| Q1 | ~~Nama final aplikasi & package~~ **Terjawab (v0.4):** nama aplikasi **PracticeMart**, PracticePay adalah fitur di dalamnya; package tetap `com.automation.practice` | Dhandy | Selesai |
| Q2 | Bahasa default UI: Indonesia atau English (untuk audiens global)? | Dhandy | Sebelum M1 |
| Q3 | Lisensi: MIT? | Dhandy | Sebelum rilis |
| Q4 | Apakah PracticePay seluruhnya masuk v1.0, atau Top Up & Scan QR ditunda ke v1.1? | Dhandy | M2 |
| Q5 | Perlu Appium Java di v1.0, atau cukup Python? | Dhandy | M3 |
| Q6 | ~~Apakah halaman Kategori (SHOP-02) cukup Should?~~ Dijawab v0.9: tab Kategori dibangun sebagai grid sederhana | Dhandy | M2 |

## 20. Definition of Done v1.0
- [ ] Semua requirement **Must** terimplementasi dan lulus AC.
- [ ] Semua skill di §7 berprioritas Must punya minimal 1 fitur yang terimplementasi dan 1 tugas di `docs/curriculum.md`.
- [ ] Tidak ada teks materi/locator yang tampil di UI pada konfigurasi default (NFR-13).
- [ ] Setiap layar punya `docs/locators/<layar>.md`.
- [ ] Lint locator di CI: 0 id tak terdokumentasi, 0 duplikat tak disengaja.
- [ ] Flow Maestro smoke + E2E-01/02/09 hijau 10× berturut-turut di CI (API 33).
- [ ] Suite Appium Python E2E-01 hijau di emulator API 33.
- [ ] Tes manual di API 29 & 35 tanpa crash.
- [ ] README/getting-started diuji 3 pemula, semuanya menjalankan test pertama ≤ 30 menit.
- [ ] APK ≤ 25 MB, dirilis di GitHub Releases dengan CHANGELOG.

---

## Lampiran A — Traceability Matrix (Skill → Fitur → Appium → Maestro)
Dukungan Maestro dicek ulang terhadap versi terbaru saat implementasi.

| Skill | Fitur | Appium (UiAutomator2) | Maestro |
|---|---|---|---|
| Locator id / accessibility id / text | Semua; tombol ikon (wishlist, salin) | `AppiumBy.ID`, `ACCESSIBILITY_ID`, `ANDROID_UIAUTOMATOR` | `tapOn: id:` / `text:` |
| XPath / locator relatif | Kartu produk, baris keranjang, ringkasan checkout | `AppiumBy.XPATH` | `tapOn` + `below/above/childOf` |
| Input teks | Login, Register, Alamat, Transfer | `send_keys`, `clear` | `inputText`, `eraseText` |
| Assertion teks/atribut | Semua | `.text`, `get_attribute("enabled")` | `assertVisible`, `assertNotVisible` |
| Tap / double / long press | Image Viewer, riwayat pencarian | `mobile: clickGesture` / `doubleClickGesture` / `longClickGesture` | `tapOn`, `doubleTapOn`, `longPressOn` |
| Swipe / fling | Onboarding, banner, galeri, katalog | `mobile: swipeGesture`, `flingGesture` | `swipe` |
| Drag & drop | Atur urutan Wishlist | `mobile: dragGesture` | tidak didukung langsung |
| Pinch zoom | Image Viewer | `mobile: pinchOpenGesture` / `pinchCloseGesture` | tidak didukung |
| Swipe-to-delete | Keranjang | `mobile: swipeGesture` pada baris | `swipe` dari elemen |
| Scroll sampai terlihat | Katalog, kontak, ulasan | `mobile: scrollGesture`, `UiScrollable` | `scrollUntilVisible` |
| Explicit wait | Loading, tracking, Network latency 3× | `WebDriverWait` + expected conditions | `extendedWaitUntil`, `waitForAnimationToEnd` |
| Toast | "Nomor VA disalin", Pusat Bantuan | XPath `//android.widget.Toast` | `assertVisible` (terbatas waktu) |
| Dialog & bottom sheet | Filter, Voucher, Batalkan, PIN | locator biasa | `tapOn` |
| Date/Time picker | Edit Profil, jadwal Same Day | locator tombol tanggal / mode input keyboard | `tapOn` + `inputText` |
| Back | Semua | `driver.back()` | `back` |
| Deep link | §6.2 | `mobile: deepLink` | `openLink` |
| Start activity langsung | §15 | `mobile: startActivity` | `launchApp` (hanya activity utama) |
| WebView context | 3DS, Pusat Bantuan, S&K | `switch_to.context("WEBVIEW_…")` | interaksi via accessibility tree WebView |
| JS dialog | Batal 3DS | `switch_to.alert` di context WEBVIEW | `tapOn` tombol dialog |
| Rotasi | Checkout | `driver.orientation` | `setOrientation` (cek versi) |
| Keyboard | Search, OTP 3DS, Form Alamat | `hide_keyboard`, `is_keyboard_shown`, `press_keycode` | `hideKeyboard`, `pressKey` |
| Clipboard | VA, Order ID, voucher | `get_clipboard_text` / `set_clipboard_text` | `copyTextFrom`, `pasteText` |
| Notifikasi | Pesanan, transfer | `open_notifications` | tidak didukung langsung (swipe dari atas) |
| Background / kill | Checkout, keranjang | `background_app`, `terminate_app`, `activate_app` | `stopApp`, `killApp`, `launchApp` |
| Network | Checkout offline | `mobile: setConnectivity` | `setAirplaneMode` / `toggleAirplaneMode` |
| Lokasi | Form Alamat | `set_location` | `setLocation` |
| Biometrik | Transfer | `finger_print` (emulator) | tidak didukung |
| Permission | Scan QR, Kontak, Alamat, Notifikasi | `autoGrantPermissions`, `mobile: changePermissions`, tap dialog sistem | `launchApp: permissions:` |
| Photo Picker | Foto profil, QR galeri | interaksi UI sistem / `push_file` | `tapOn` UI sistem |
| Pindah app | Share, dialer, Pengaturan sistem | `activate_app`, `current_package` | `launchApp: appId:` |
| Compose | Transfer PracticePay | `AppiumBy.ID` (testTagsAsResourceId) / text | `tapOn: id:` |
| Reset state | Sesi & pengaturan lokal (data aplikasi ada di cloud) | `fullReset` atau `appium:noReset=false`; data bersih = akun baru via Register | `launchApp: clearState: true` |
| Retry & flaky | Cek Status VA | pola retry di framework test | `retry` |
| Menyimpan nilai dinamis | Order ID, nomor VA | simpan `.text` ke variabel | `copyTextFrom` + `${maestro.copiedText}` |

## Lampiran B — Glossary
| Istilah | Arti |
|---|---|
| resource-id | ID elemen Android (`package:id/nama`), locator paling stabil |
| content-desc / accessibility id | Label aksesibilitas elemen; dipakai TalkBack dan locator `ACCESSIBILITY_ID` |
| UiAutomator2 | Driver Appium untuk Android berbasis framework UiAutomator |
| Context | Mode Appium: `NATIVE_APP` atau `WEBVIEW_<package>` |
| Deep link | URL (`practiceapp://…`) yang membuka layar tertentu secara langsung |
| POM | Page Object Model: pola memisahkan locator/aksi layar dari logika test |
| Deterministic mode | Mode yang mematikan keacakan supaya hasil selalu sama |
| Developer Options | Menu tersembunyi di Pengaturan untuk mengontrol kondisi test |
| Flaky test | Test yang kadang lulus, kadang gagal tanpa perubahan kode |
| Stale element | Referensi elemen yang tidak valid lagi karena UI di-render ulang |
| Firebase Authentication | Layanan login Firebase; menyimpan akun, password, dan sesi login |
| Cloud Firestore | Database dokumen Firebase tempat data aplikasi disimpan; dipakai bersama oleh app Android dan web |
| Security Rules | Aturan akses Firestore yang menentukan siapa boleh membaca/menulis dokumen mana |

## Lampiran C — Pemetaan dari PRD v0.1
| v0.1 | v0.2 |
|---|---|
| Practice Hub (daftar modul per level) | Dihapus dari app. Level & tugas di `docs/curriculum.md`; app dibuka ke Onboarding/Beranda |
| Info Sheet ⓘ di setiap layar | Dihapus dari app. Pindah ke `docs/locators/` dan contoh kode di `tests/` |
| Result panel `<prefix>_txt_result` | Dihapus. Assertion memakai UI alami (error, toast, snackbar, badge, status, total) |
| Test Controls | Developer Options (§10) |
| Locator Hint Overlay | Tetap, di Developer Options |
| M01–M17 (modul) | Skill dipetakan ke fitur (§7); FR baru per area (§8) |
| M18 PracticeMart (capstone) | Menjadi inti app (§8.2–8.4) |
| SHOP-17 E-Wallet WebView | Diganti PracticePay native + PIN (FR-PAYM-10) |
| Prefix `loc_`, `login_`, `form_`, … `chl_` | Prefix per area: `shop_`, `pay_`, `acc_`, `addr_`, `help_`, `dev_` (§12.1) |
| Contoh kalkulasi Rp158.000 | Tetap (§8.7) |
