# Kontrak Bersama — PracticeMart (Android & Web)

| Field | Isi |
|---|---|
| Dokumen | Kontrak data, aturan bisnis, dan test data yang **wajib sama** di app Android dan web app |
| Versi kontrak | 1.4 |
| Tanggal | 23 November 2026 |
| Sumber (master) | `PracticeMart.apps/docs/contract.md` |
| Salinan | `PracticeMart.web/docs/contract.md` (harus identik) |
| Dipakai oleh | PRD Android v0.9 · PRD Web v0.1 · ARCHITECTURE kedua project |
| Owner | Dhandy |

---

## 1. Aturan kontrak
1. Kedua platform memakai **project Firebase yang sama**. Akun, data per akun, dan stok dibagi; katalog dibundel di masing-masing platform; pengaturan tidak dibagi.
2. Isi dokumen ini adalah sumber kebenaran untuk assertion lintas platform. Bila PRD atau ARCHITECTURE salah satu platform berbeda dengan dokumen ini, **dokumen ini yang berlaku**.
3. Perubahan apa pun: ubah master, naikkan versi kontrak, salin ke project lain di commit yang sama waktunya, dan catat di §10.
4. Perubahan skema Firestore wajib mengikuti §8.5 (tanpa menghapus data user).
5. **Tanpa seeding.** Tidak ada script admin, akun uji bersama, atau template di Firestore. Katalog, stok awal, dan data awal akun dibundel di app (§4); app sendiri yang menulisnya ke Firestore saat pertama dibutuhkan (§8.3).
6. **Tanpa realtime.** Tidak ada listener Firestore (`addSnapshotListener` / `onSnapshot`); data dibaca dengan `get` (§7).

## 2. Business rules (sumber kebenaran untuk assertion)
| Rule | Nilai |
|---|---|
| Mata uang | Rupiah, integer (`Long` / `number` bulat), format `Rp125.000` |
| Ongkir | Reguler Rp10.000 · Express Rp20.000 · Same Day Rp35.000 (hanya DKI Jakarta) |
| Asuransi pengiriman (opsional) | Rp2.000 flat |
| Default checkout | Kurir Express, asuransi aktif (Rp98.000 untuk P001 x1 ke alamat Bandung) |
| Same Day | Hanya alamat provinsi DKI Jakarta (selain itu `"Tidak tersedia untuk kota ini"` dan kurir kembali ke Express); wajib jam kirim 09:00–17:00 |
| Biaya layanan | Rp1.000 per order |
| `HEMAT10` | Diskon 10% subtotal, maks Rp50.000, dibulatkan ke bawah |
| `ONGKIR` | Potongan ongkir maks Rp20.000 (Same Day tetap bayar Rp15.000) |
| `MIN100K` | Potongan Rp15.000, hanya jika subtotal ≥ Rp100.000; selain itu error `"Minimal belanja Rp100.000"` |
| `EXPIRED` | Error `"Voucher sudah kedaluwarsa"` |
| Kode lain | Error `"Voucher tidak ditemukan"` |
| Urutan kalkulasi | Total = Subtotal − Diskon voucher + (Ongkir − Potongan ongkir) + Asuransi + Biaya layanan |
| Maks qty per item | min(stok varian, 10) |
| Limit COD | Total ≤ Rp1.000.000 |
| Countdown VA | 15 menit (Developer Options: 30 dtk / 2 mnt / 15 mnt) |
| Interval status order | 10 dtk (Developer Options: 3 / 10 / 30 dtk / Manual) |
| Urutan status order | Menunggu Pembayaran → Dibayar → Dikemas → Dikirim → Selesai; Dibatalkan hanya dari status ≤ Dikemas |
| Saldo awal PracticePay | Rp500.000 saat akun pertama kali login |
| Transfer | Min Rp10.000, maks = saldo |
| PIN PracticePay | `123456`; 3× salah → terkunci 30 dtk |
| Login | 3× gagal berturut-turut → terkunci 30 dtk (dihitung per client di memori, tidak disimpan di cloud) |
| Cashback PracticePay | Hanya pesanan yang dibayar dengan PracticePay: 5% dari total, maks Rp10.000, dibulatkan ke bawah, masuk ke saldo saat pesanan Selesai (tidak mengubah total pesanan) |
| Order ID | `PM-YYYYMMDD-XXXX`; `XXXX` = nomor urut per akun 4 digit (§8.3); `YYYYMMDD` dari tanggal app (§5) |
| Pencarian | Saran muncul mulai 2 karakter, maks 5 saran; riwayat maks 5 |
| Flash sale | Durasi awal 02:14:37 |

**Contoh kalkulasi (untuk AC):** 2 × Kaos Polos Basic (Rp75.000) = Subtotal Rp150.000, voucher `HEMAT10` −Rp15.000, Express Rp20.000, asuransi Rp2.000, biaya layanan Rp1.000 → **Total Rp158.000**. Bila dibayar dengan PracticePay, saldo menjadi Rp342.000, lalu cashback Rp7.900 masuk saat pesanan Selesai.

## 3. ~~Intentional bugs `glitch_user`~~
Dihapus di v1.1 bersama semua akun uji bersama (§4.1).

## 4. Akun & test data
### 4.1 Akun
- **Tidak ada akun uji bersama.** Learner membuat akun sendiri lewat Register (email + password). Perilaku khusus akun (`LOCKED`, `SLOW`, `GLITCH`, `NEW`) dihapus.
- Login selalu memakai **email + password**.
- Akun baru menerima data awal §4.5 saat pertama kali login.

### 4.2 Katalog & stok awal
- 60 produk, ID `P001`–`P060`, 6 kategori: Fashion, Elektronik, Rumah Tangga, Olahraga, Buku, Kecantikan.
- Katalog (nama, harga, rating, varian, kategori, urutan) **dibundel** di app: Android `app/src/main/assets/seed/catalog.json`, web salinan identik di bundle. Katalog tidak ada di Firestore.
- Stok awal per varian dihitung dari field `stock` + `zero` di `catalog.json`. Stok berjalan disimpan global di Firestore `catalog/stock` (§8.2) dan dipakai bersama semua akun, learner, dan platform.
- Rekomendasi mode deterministik: `P001`, `P002`, `P010`, `P020`, `P060` (urutan tetap). Mode acak: 5 produk acak.

| ID | Nama | Harga | Varian | Catatan |
|---|---|---|---|---|
| P001 | Kaos Polos Basic | Rp75.000 | Hitam/Putih/Navy × S/M/L/XL | Hitam-XL stok awal 0 |
| P002 | Sepatu Lari Ringan | Rp450.000 (coret Rp600.000, −25%) | Ukuran 39–44 | stok awal 3 per ukuran |
| P010 | Headphone Nirkabel | Rp850.000 | Hitam/Putih | dipakai untuk N-07 bersama P020 |
| P020 | Smartwatch Sport | Rp1.250.000 | – | > limit COD |
| P060 | Buku Catatan A5 | Rp15.000 | – | stok awal 0 total |

### 4.3 Voucher, kartu, alamat
| Jenis | Data |
|---|---|
| Voucher | `HEMAT10`, `ONGKIR`, `MIN100K`, `EXPIRED` (aturan di §2) |
| Kartu sukses | `4111 1111 1111 1111` |
| Kartu ditolak | `4000 0000 0000 0002` |
| Kartu pending | `4000 0000 0000 0119` |
| OTP 3DS | `123456` (lainnya → `"Kode OTP salah"`) |
| Nomor VA BCA (deterministik) | `8077 0000 1234 5678` |
| Nomor VA bank lain, dan BCA di luar mode deterministik | Awalan bank (BCA `8077`, BNI `8808`, Mandiri `8900`) + digit Order ID, dikelompokkan 4 digit. Contoh BNI `PM-20260115-0003` → `8808 2026 0115 0003` |
| Alamat default | Rumah — Jl. Merdeka No. 10, Bandung, Jawa Barat 40111 (penerima = nama akun) |
| Alamat kedua | Kantor — Jl. Sudirman No. 1, Jakarta Pusat, DKI Jakarta 10220 (Same Day tersedia) |
| Lokasi uji | -6.2088, 106.8228 → DKI Jakarta, Jakarta Pusat |

### 4.4 PracticePay
| Jenis | Data |
|---|---|
| Saldo awal | Rp500.000 |
| PIN | `123456` |
| Kontak uji | Andi Pratama, Anisa Rahma, Budi Santoso (0812-3456-7890), Bunga Lestari, Citra Dewi, Dewi Anggraini, Dimas Saputra, Eka Putri, Fajar Nugroho, Gilang Ramadhan |
| Merchant QR | `KOPIPAGI` (Kopi Pagi, nominal bebas), `TOKOBUKU` (nominal tetap Rp45.000) |

### 4.5 Data awal akun (dibundel di app)
Ditulis app ke `states/{uid}` dan `states/{uid}/transactions` saat akun pertama kali login (§8.3). Nilai sama di Android & web.

| Field | Nilai awal |
|---|---|
| Alamat | 2 alamat §4.3 (Rumah = utama) |
| Saldo | Rp500.000 |
| Transaksi | 8 transaksi di bawah |
| Wishlist | `P001`, `P010`, `P002`, `P020` |
| Riwayat pencarian | `sepatu lari`, `headphone`, `buku catatan`, `smartwatch` |
| Keranjang, voucher, pesanan | kosong |
| PIN | hash dari `123456` dengan salt acak per akun (§8.6) |
| `orderSeq` | 0 |

8 transaksi awal (waktu Asia/Jakarta, terbaru di atas; ini data tetap, bukan waktu login):

| ID | Judul | Nominal | Waktu |
|---|---|---|---|
| 8 | Top Up dari BCA | +Rp300.000 | 2026-01-15 09:12 |
| 7 | Belanja PracticeMart | −Rp86.000 | 2026-01-15 08:47 |
| 6 | Transfer ke Budi Santoso | −Rp150.000 | 2026-01-14 19:30 |
| 5 | Cashback belanja | +Rp4.300 | 2026-01-14 12:05 |
| 4 | Bayar QR · Kopi Pagi | −Rp28.000 | 2026-01-14 07:58 |
| 3 | Terima dari Citra Dewi | +Rp75.000 | 2026-01-13 20:14 |
| 2 | Belanja PracticeMart | −Rp158.000 | 2026-01-13 10:02 |
| 1 | Top Up dari Mandiri | +Rp500.000 | 2026-01-12 18:40 |

## 5. Developer Options (semantik sama, disimpan per device/browser)
| Pengaturan | Opsi | Default | Semantik |
|---|---|---|---|
| Mode deterministik | on/off | off | Tanggal app tetap `2026-01-15` dengan jam berjalan sungguhan (Asia/Jakarta); urutan acak memakai urutan tetap |
| Network latency multiplier | 0× / 1× / 2× / 3× | 1× | Mengalikan delay buatan; tidak memengaruhi latency jaringan sungguhan |
| Animasi | on/off | on | – |
| Peluang error server | 0% / 30% / 50% | 0% | Deterministik ON: permintaan ke-1 gagal, ke-2 berhasil, dst. Pesan `"Gagal memuat, coba lagi"` |
| Batas waktu VA | 30 dtk / 2 mnt / 15 mnt | 15 mnt | – |
| Update status pesanan | 3 / 10 / 30 dtk / Manual | 10 dtk | Manual = tombol "Status berikutnya" |
| Locator Hint Overlay | on/off | off | – |
| Reset pengaturan | tombol + dialog | – | Hanya pengaturan; tidak menyentuh data cloud |

Tidak ada fitur reset data di platform mana pun.

**Waktu di cloud vs tanggal app.** Semua waktu yang **ditulis ke Firestore** (`statusAt`, `stamps`, `vaDeadline`, `createdAt`, `at`) memakai jam device **asli**, juga saat mode deterministik ON. Tanggal tetap `2026-01-15` hanya dipakai untuk: segmen `YYYYMMDD` Order ID, nilai awal DatePicker, dan tampilan tanggal. Alasannya: client lain (web, atau device dengan mode OFF) menghitung countdown VA dan status dari waktu yang sama; tanggal palsu di cloud akan membuat VA langsung kedaluwarsa dan status melompat.

## 6. Teks bersama yang ditetapkan kontrak
Teks lain mengikuti FR di PRD masing-masing (ID FR sama di kedua PRD, teks wajib persis sama).

| Konteks | Teks |
|---|---|
| Login — field kosong | `"Email wajib diisi"`, `"Password wajib diisi"` |
| Login — kredensial salah (`INVALID_LOGIN_CREDENTIALS`) | `"Email atau password salah"` |
| Register — email sudah dipakai (`email-already-in-use`) | `"Email sudah terdaftar"` |
| Terlalu banyak percobaan (`too-many-requests`) | `"Terlalu banyak percobaan. Coba lagi nanti."` |
| Login / Register gagal karena jaringan (`network-request-failed`) | `"Tidak ada koneksi. Periksa internet lalu coba lagi."` |
| Checkout gagal karena stok varian tidak cukup (transaksi §8.3) | `"Stok tidak cukup untuk <nama produk>"`; keranjang tetap utuh |
| Data akun gagal dimuat (cold start tanpa cache & tanpa jaringan, atau pembacaan gagal) | `"Gagal memuat, coba lagi"` + tombol `"Coba lagi"` |


## 7. Sinkron antar platform
- **Tidak realtime.** Data dibaca dengan `get` **saat layar/halaman dibuka atau di-refresh** (pull-to-refresh, reload). Tidak ada listener Firestore.
- Write dari dua client tidak boleh menghilangkan data (aturan §8.3).
- Pengaturan, sesi login, "Ingat saya", dan status onboarding **per device/browser**.
- Simulasi status pesanan berjalan di client yang sedang terbuka; status dihitung dari timestamp asli (§5) dan ditulis dengan transaksi kondisional (§8.3), sehingga dua client tidak membuat lompatan dobel.

## 8. Firebase
### 8.1 Project & autentikasi
- Plan **Spark**: Authentication (email/password) + Cloud Firestore. Tanpa Cloud Functions, Cloud Storage, FCM, Analytics, App Check.
- Pengaturan project: provider Email/Password aktif; **email enumeration protection aktif**; satu database Firestore `(default)` mode Native. Region ditetapkan sekali saat database dibuat dan tidak bisa diubah (dicatat di ARCHITECTURE masing-masing project).
- Login: `signInWithEmailAndPassword(email, password)`.
- Register: `createUserWithEmailAndPassword` → tulis `users/{uid}` → `signOut` → layar Masuk. Data awal akun belum ditulis di tahap ini.
- Login pertama: bila `states/{uid}` belum ada, tulis data awal §4.5 (§8.3). Bila `users/{uid}` belum ada (Register terputus setelah akun Auth dibuat), buat sekalian.

### 8.2 Skema Firestore
| Path | Field | Catatan |
|---|---|---|
| `users/{uid}` | `email`, `displayName`, `createdAt`, `schemaVersion` | `uid` dari Firebase Auth |
| `states/{uid}` | `profile` {`name`, `email`, `birth`, `gender`, `photoUri` (tidak dipakai lagi, tetap dipertahankan; §8.5)}, `balance`, `pinHash`, `pinFailures`, `pinLockedUntil`, `voucher`, `cart` [{`productId`, `color`, `size`, `qty`, `selected`, `saved`}], `wishlist` [productId], `history` [query], `addresses` [{`id` (string: `"home"`, `"office"`, `"addr-<epochMs>"`; angka lama dibaca sebagai string), `label`, `recipient`, `phone`, `province`, `city`, `street`, `postal`, `isPrimary`}], `orderSeq`, `schemaVersion` | Satu dokumen per akun; dibuat dari data awal §4.5 saat pertama login |
| `states/{uid}/orders/{orderId}` | `id`, `total`, `method`, `methodLabel`, `status`, `statusAt`, `stamps` {status: epochMs}, `cancelReason`, `vaDeadline`, `lines` [{`productId`, `color`, `size`, `qty`}], `createdAt` | Satu dokumen per pesanan. `methodLabel` VA = `"Virtual Account <Bank>"`; bank dibaca dengan `contains` (label lama `"<Bank> Virtual Account"` tetap terbaca). `cancelReason` selalu Bahasa Indonesia: alasan pilihan `"Ingin ubah alamat pengiriman"`, `"Ingin ubah metode pembayaran"`, `"Menemukan harga lebih murah"`, atau teks bebas maks 100 karakter untuk `"Lainnya"`; VA kedaluwarsa `"Waktu pembayaran habis"` |
| `states/{uid}/transactions/{txId}` | `id`, `title`, `amount` (+ masuk / − keluar), `at` | Satu dokumen per transaksi; urut `at` turun. `title` selalu Bahasa Indonesia, tidak ikut bahasa device: `"Top Up Saldo"`, `"Transfer ke <nama>"`, `"Bayar QR · <merchant>"`, `"Belanja PracticeMart"`, `"Cashback belanja"` |
| `catalog/stock` | `stock` {`<productId>`: {`"<warna>\|<ukuran>"`: n}}, `schemaVersion` | **Satu dokumen** untuk stok global semua produk (1 read saat cold start). Dibuat oleh client pertama yang membutuhkannya dari stok awal §4.2. Varian tanpa warna/ukuran memakai key `"\|"` |

Uang selalu integer Rupiah. Waktu selalu epoch ms dari jam asli (§5). Status order disimpan sebagai `WAITING`/`PAID`/`PACKED`/`SHIPPED`/`DONE`/`CANCELLED`.

### 8.3 Aturan tulis (agar dua client tidak saling menimpa)
| Data | Cara tulis |
|---|---|
| Data awal akun (login pertama) | Transaksi: baca `states/{uid}`; bila belum ada, buat dari §4.5. Bila transaksi ini yang membuat dokumen, tulis 8 dokumen `transactions/` dengan ID tetap `1`–`8` (penulisan ulang ID yang sama tidak menggandakan) |
| Stok awal | Bila `catalog/stock` belum ada saat dibaca, client membuatnya dari stok awal §4.2 di dalam transaksi (create hanya bila belum ada). Saat sudah ada, client tidak pernah menulis ulang seluruh dokumen |
| Pesanan baru | Satu transaksi: baca `states/{uid}` & `catalog/stock`; gagal dengan teks §6 bila stok varian kurang; `orderSeq + 1`; buat `orders/{id}`; kurangi stok tiap varian. Bila PracticePay: gagal bila `balance` < total, kurangi `balance`, buat dokumen `transactions/` "Belanja PracticeMart" |
| Status pesanan | Transaksi: ubah hanya bila `status` saat ini sama dengan status yang diharapkan. Transisi ke `DONE` untuk pesanan PracticePay sekaligus menambah cashback ke `balance` dan membuat dokumen `transactions/` "Cashback belanja" di transaksi yang sama |
| Batal / VA kedaluwarsa | Transaksi: status → `CANCELLED` hanya bila belum `CANCELLED` dan status ≤ `PACKED`; stok dikembalikan di transaksi yang sama. Saldo PracticePay **tidak** dikembalikan (diterima, sama di kedua client; teks batal tidak menjanjikan refund) |
| Transfer, Bayar QR | Transaksi: gagal bila `balance` < nominal; kurangi `balance`; buat dokumen `transactions/` |
| Top Up | Transaksi Firestore: `balance + n` + dokumen `transactions/` berjudul `"Top Up Saldo"`; nominal Rp10.000–Rp10.000.000 (dicek di app, bukan di Rules) |
| `cart`, `wishlist`, `history`, `addresses`, `profile`, `voucher`, `pinHash`, `pinFailures`, `pinLockedUntil` | `update()` per field. Tabrakan di field yang sama = last-write-wins (diterima) |

Client tidak pernah menulis nilai `balance` hasil hitung sendiri; selalu lewat transaksi atau `increment`. Transaksi butuh online; offline → aksi diblokir (banner `"Tidak ada koneksi"`). `update()` per field boleh offline (diantre SDK).

### 8.4 Security Rules
| Path | Read | Write |
|---|---|---|
| `users/{uid}` | pemilik | pemilik (create/update, tanpa delete); `email` harus sama dengan `request.auth.token.email` |
| `states/{uid}` | pemilik | pemilik; bentuk dijaga `validState`: `cart` list ≤ 100, `wishlist` list ≤ 60, `history` list ≤ 5, `addresses` list ≤ 20, `profile` map, `voucher` null/string, `balance`/`orderSeq`/`pinFailures`/`pinLockedUntil` int. Client menjaga batas yang sama. Tidak bisa dihapus |
| `states/{uid}/orders/{id}`, `states/{uid}/transactions/{id}` (satu tingkat) | pemilik | pemilik |
| `catalog/stock` | user login | user login. Create hanya bila belum ada. Update hanya field `stock`; key produk & varian tidak boleh bertambah atau hilang; setiap nilai int ≥ 0 |
| lainnya | ditolak | ditolak |

File rules ada di `PracticeMart.apps/firebase/firestore.rules` dan di-deploy dari sana. Salinan web hanya untuk test emulator dan identik dengan master; web tidak men-deploy rules.

**Batas rules yang diterima:** rules hanya bisa menjaga bentuk data, bukan niat. Siapa pun yang punya akun bisa mengubah stok global (mis. ke 0) atau saldo akunnya sendiri. Tidak ada reseed; bila stok rusak, owner menghapus dokumen `catalog/stock` di Firebase console dan client berikutnya membuatnya ulang dari stok awal.

### 8.5 Evolusi skema
- Setiap dokumen punya `schemaVersion`.
- Perubahan aditif: field baru opsional dan diberi default saat dibaca bila belum ada.
- Rename/hapus field: client membaca bentuk lama, menulis bentuk baru, menaikkan `schemaVersion`. Field lama baru dihapus setelah kedua platform membaca bentuk baru.
- Update app tidak boleh menghapus atau menimpa data user.

### 8.6 Hash PIN
- Format `pbkdf2$<iterasi>$<saltHex>$<hashHex>`, hex huruf kecil.
- PBKDF2 dengan HMAC-SHA1, 10.000 iterasi, salt acak 16 byte, panjang kunci 256 bit (32 byte), input PIN di-encode UTF-8.
- Android: `PBKDF2WithHmacSHA1` (`javax.crypto`). Web: `crypto.subtle.deriveBits({name: "PBKDF2", hash: "SHA-1", ...}, ..., 256)`.
- Verifikasi wajib menerima iterasi dari string tersimpan, bukan konstanta.
- **Test vector** (wajib lulus di unit test kedua platform): PIN `123456`, salt `000102030405060708090a0b0c0d0e0f`, 10.000 iterasi →
  `pbkdf2$10000$000102030405060708090a0b0c0d0e0f$6f4f2d621842bcdf52c44ef73d28d6017045e5d543b5547d56ddda1524d03ce1`

## 9. Batas & risiko bersama
- Kuota Cloud Firestore gratis: 50.000 read, 20.000 write, 20.000 delete per hari untuk seluruh project (Android + web), reset sekitar tengah malam waktu Pasifik. Read dari cache lokal tidak dihitung; `clearState`/`fullReset`/browser baru menghapus cache.
- Firebase Authentication (Spark): 100 akun baru per jam per alamat IP. Satu kelas di belakang satu jaringan bisa kena batas Register.
- Rate limit Firebase Auth berlaku per IP & per akun; suite yang login berulang atau sengaja salah password bisa kena `too-many-requests`.
- Stok global dipakai bersama dan tidak pernah di-reset otomatis; stok berkurang permanen sampai owner menghapus `catalog/stock` (§8.4).

## 10. Riwayat kontrak
| Versi | Tanggal | Perubahan |
|---|---|---|
| 1.0 | 2026-11-19 | Versi awal: dipindah dari PRD Android v0.6 (§8.7, §8.8, §11) dan ARCHITECTURE Android v0.4 (§6.4–6.8); tambah aturan tulis §8.3, teks Lupa Password §6, sinkron saat reload §7 |
| 1.1 | 2026-11-19 | **Tanpa seeding & tanpa realtime.** Akun uji bersama, perilaku `LOCKED`/`SLOW`/`GLITCH`/`NEW`, bug `glitch_user` (§3), `homeBalance`, `usernames/`, `products/`, `catalog/meta`, `templates/*`, dan script admin dihapus. Login hanya email. Katalog & data awal akun dibundel di app (§4.2, §4.5); stok global satu dokumen `catalog/stock` dibuat client pertama. Waktu cloud memakai jam asli (§5). Teks Lupa Password netral + teks error baru (§6). Aturan tulis dilengkapi: data awal, stok awal, cashback, transfer/QR, Top Up (§8.3). Rules baru (§8.4). Test vector PIN (§8.6). Limit Auth Spark (§9). Path `PracticeMarket.apps` → `PracticeMart.apps` |
| 1.2 | 2026-11-20 | **Lupa Password dihapus** (§6, §8.2, §9): tidak ada layar, teks, maupun `sendPasswordResetEmail`. **Top Up** jadi transaksi (bukan batch), nominal Rp10.000–Rp10.000.000, judul `"Top Up Saldo"` (§8.3). Web app perlu menyesuaikan salinan kontrak ini. |
| 1.3 | 2026-11-23 | **Parity app–web.** Default checkout Express + asuransi aktif, Same Day hanya DKI Jakarta dengan jam kirim 09:00–17:00 (§2). Format nomor VA per bank dan `methodLabel` VA `"Virtual Account <Bank>"` (§4.3, §8.2). Peluang error server bawaan 0% (§5). Judul transaksi dan `cancelReason` selalu Bahasa Indonesia, batal order PracticePay tanpa refund saldo (§8.2, §8.3). Rules gabungan dengan `validState` dan batas list (§8.4). Catatan Transfer belum diatur. Web perlu menyalin kontrak ini dan rules master. |
| 1.4 | 2026-11-23 | **Id alamat string.** `addresses[].id` selalu string (`"home"`, `"office"`, `"addr-<epochMs>"`); app tidak lagi menulis angka dan membaca id angka lama sebagai string (§8.2). Memperbaiki pilih alamat di checkout app (G-12). Web cukup menyalin kontrak ini. |
