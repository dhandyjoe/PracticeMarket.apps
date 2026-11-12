# Design — PracticeMart (dengan fitur PracticePay)

| Field | Isi |
|---|---|
| Dokumen | Design system & spesifikasi layar |
| Versi | 1.0 |
| Tanggal | 12 November 2026 |
| Owner | Dhandy |
| Terkait | PRD v0.4 · `architecture.md` v0.2 |
| Canvas | Claude Design "PracticeMart — UI Design", halaman **PracticeMart (app)**: https://claude.ai/artifact/5gVU8MGLFscacTeVfxP2Uc |

---

## 0. Cara memakai dokumen ini (untuk AI agent & developer)
1. **Urutan sumber kebenaran:**
   - **PRD** untuk perilaku, aturan bisnis, dan teks yang di-assert.
   - **design.md** untuk tampilan, token, komponen, dan tata letak.
   - **Artboard di canvas** sebagai ilustrasi visual.

   Bila ada konflik, ikuti urutan itu dan catat perbedaannya di PR.
2. **Judul artboard = ID layar di PRD** (mis. `SHOP-05 Detail Produk`). File artboard tercantum di §9.
3. **Jangan menulis nilai hex, sp, atau dp langsung di layout/composable.** Selalu pakai token di §2–§3 (lihat mapping implementasi di §11).
4. **Jangan menampilkan teks materi, resource-id, deep link, atau istilah automation di UI** (PRD NFR-13). Satu-satunya pengecualian adalah Locator Hint Overlay saat dinyalakan dari Developer Options.
5. **Teks UI yang tercantum di PRD harus sama persis** (huruf besar/kecil, tanda baca, spasi). Untuk teks yang tidak ada di PRD, ikuti aturan bahasa di §6.
6. Ukuran di artboard memakai px pada lebar 390. Dalam implementasi Android, **1 px artboard = 1 dp** (teks: 1 px = 1 sp). Pengecualian: semua target sentuh minimal **48 dp** walaupun di artboard digambar 44 px.
7. Artboard memakai ikon garis buatan sendiri dan gambar produk berupa placeholder berwarna. Implementasi memakai set ikon dan gambar di §4.17 dan §4.18.

## 1. Identitas & prinsip visual
- **Nama aplikasi:** PracticeMart. **PracticePay** adalah fitur dompet di dalam PracticeMart (tab di bottom navigation dan kartu saldo di Beranda), bukan aplikasi terpisah.
- **Karakter:** marketplace modern yang tenang dan terpercaya. Warna utama hijau teal untuk aksi dan merek. Netral abu-kebiruan untuk teks dan garis. Gelap pekat untuk area "uang" (header PracticePay, nomor Order ID).
- **Prinsip:**
  1. *Seperti app sungguhan.* Tidak ada elemen "latihan" yang terlihat oleh user.
  2. *Satu aksi utama per layar*, berupa tombol teal penuh di bawah atau di akhir form.
  3. *Status selalu terlihat*: error inline, badge, status pill, snackbar. Semuanya juga menjadi sumber assertion.
  4. *Angka penting menonjol*: harga, total, saldo, dan countdown memakai Space Grotesk atau JetBrains Mono.
  5. *Aksesibel*: kontras ≥ 4,5:1 untuk teks, target sentuh ≥ 48 dp, label untuk semua tombol ikon.

## 2. Design tokens
### 2.1 Warna (tema terang)
| Token | Hex | Pemakaian |
|---|---|---|
| `color.primary` | `#0E7C66` | Tombol utama, link, tab aktif, switch ON, radio terpilih, header Beranda |
| `color.primary.pressed` | `#0A5E4D` | Pressed/hover; **teks di atas `primary.container`** |
| `color.primary.container` | `#DDF0EA` | Chip terpilih, ikon sukses, placeholder produk Fashion, tombol mini di kartu saldo |
| `color.primary.surface` | `#EEF7F4` | Opsi terpilih (kurir, voucher), tombol "Gunakan lokasi", kartu "Lihat semua" |
| `color.primary.outline` | `#9CC9BD` | Garis putus-putus kartu voucher/"Lihat semua" |
| `color.on.primary` | `#FFFFFF` | Teks/ikon di atas primary |
| `color.ink` | `#12161C` | Teks utama, tombol gelap (toggle aktif), latar PracticePay & kartu Order ID |
| `color.ink.secondary` | `#3A4350` | Teks sekunder, label field |
| `color.ink.muted` | `#5B6573` | Caption, placeholder, ikon chevron, teks disabled di atas `surface.disabled` |
| `color.ink.disabled` | `#9AA4B2` | Teks pada elemen nonaktif bergaris putus-putus; teks muted di atas latar gelap |
| `color.background` | `#F4F6F5` | Latar layar daftar (Beranda, Keranjang, Checkout, Akun) |
| `color.surface` | `#FFFFFF` | Kartu, app bar, bottom bar, sheet, latar layar form |
| `color.surface.subtle` | `#F1F3F6` | Field pencarian, stepper, segmented control, chip netral |
| `color.surface.disabled` | `#E3E6EB` | Latar tombol disabled |
| `color.outline` | `#E3E6EB` | Garis kartu & pemisah |
| `color.outline.strong` | `#D5DAE1` | Border field & tombol sekunder |
| `color.track` | `#C9CFD8` | Switch OFF, handle bottom sheet, radio tidak terpilih, titik indikator |
| `color.error` | `#B42318` | Teks & border error, badge, tombol danger, harga coret diskon (%) |
| `color.error.strong` | `#8A1C12` | Teks di atas `error.container` |
| `color.error.container` | `#FDECEA` | Banner error |
| `color.warning` | `#B4400A` | Ikon flash sale, kotak countdown flash sale |
| `color.warning.strong` | `#8A3108` | Teks di atas `warning.container` |
| `color.warning.container` | `#FFF1E8` | Banner flash sale, countdown VA, status "Menunggu" |
| `color.warning.outline` | `#F6C9A9` | Border banner warning |
| `color.info` | `#1D4ED8` | Tombol di banner izin kontak |
| `color.info.strong` | `#1D3A9E` | Teks di atas `info.container`, status "Diproses/Dikirim" |
| `color.info.container` | `#E0E8FD` | Banner izin, status "Diproses/Dikirim", placeholder Elektronik/Olahraga |
| `color.accent.purple` / `.container` | `#5B21B6` / `#EEE6FC` | Placeholder kategori lain |
| `color.accent.orange` / `.container` | `#7A3606` / `#FCEBD9` | Placeholder kategori, avatar |
| `color.dark.surface` | `#12161C` | Header PracticePay, kartu Order ID, kartu voucher di Beranda |
| `color.dark.surface.raised` | `#262C35` | Tombol di atas latar gelap, **snackbar** |
| `color.dark.accent` | `#7CE3A4` | Angka/aksen di atas latar gelap (Order ID, ikon aksi PracticePay), aksi snackbar |
| `color.dark.muted` | `#9AA4B2` / `#D5DAE1` | Label / teks sekunder di atas latar gelap |
| `color.toast` | `#3A4350` | Latar toast |
| `color.scrim` | `rgba(18,22,28,0.55)` | Di belakang bottom sheet & dialog |
| `color.hint.bg` / `.fg` | `#FDE68A` / `#4A3800` | **Hanya** Locator Hint Overlay (Developer Options) |

**Warna halaman WebView** (sengaja beda dari app agar terasa "bank/situs lain"):
- 3DS PracticeBank: header `#1A3D6D`, tombol `#1A5FB4`, latar `#F2F4F7`, font Arial/Helvetica.
- Pusat Bantuan: header `#0E7C66`, tombol `#1A5FB4`, latar `#FAFBFC`, font Arial/Helvetica.

**Pasangan kontras yang sudah diverifikasi (WCAG):** ink/surface 18,2 · ink.secondary/surface 10,0 · ink.muted/surface 5,9 · ink.muted/background 5,4 · primary/surface 5,1 · on.primary/primary 5,1 · primary.pressed/primary.container 6,5 · error/surface 6,6 · error.strong/error.container 8,1 · warning.strong/warning.container 7,5 · info.strong/info.container 8,0 · dark.accent/dark.surface 11,6 · dark.muted(#9AA4B2)/dark.surface 7,2 · ink.muted/surface.disabled 4,7.
**Dilarang:** teks `primary` di atas `primary.container` (4,3:1). Untuk itu pakai `primary.pressed`.

### 2.2 Tema gelap
Belum didesain. PRD meminta pilihan Tema (Sistem/Terang/Gelap). Usulan awal untuk implementasi: `background #0B0E12`, `surface #12161C`, `surface.subtle #1B2027`, `outline #262C35`, `ink #F1F3F6`, `ink.secondary #D5DAE1`, `ink.muted #9AA4B2`, `primary #3FB59A`, `on.primary #04241D`, `primary.container #12392F`. Semua pasangan wajib diverifikasi ulang ≥ 4,5:1 sebelum dipakai.

### 2.3 Tipografi
Font dibundel di APK (lihat `architecture.md` D12):
- **Space Grotesk**: judul layar besar, harga/total, saldo, nama brand.
- **Inter**: semua teks UI.
- **JetBrains Mono**: nilai yang "dibaca mesin" (Order ID, nomor VA, nomor kartu, OTP, countdown, kode voucher, nomor HP kontak, tanggal `YYYY-MM-DD`).

| Token | Font | Size / line height | Weight | Contoh |
|---|---|---|---|---|
| `type.display` | Space Grotesk | 32 / 38 | 700 | Saldo PracticePay |
| `type.headline` | Space Grotesk | 28 / 34 | 700 | "Masuk", "Buat akun", judul onboarding |
| `type.title.lg` | Space Grotesk | 22–26 / 30 | 700 | Total, harga detail produk, judul hasil |
| `type.title.md` | Inter | 18 / 24 | 600 | Judul app bar, judul sheet |
| `type.title.sm` | Inter | 15–16 / 22 | 600–700 | Judul kartu, nama produk detail, judul section |
| `type.body.lg` | Inter | 15 / 22 | 400–600 | Isi field, teks baris list |
| `type.body` | Inter | 14 / 20 | 400 | Deskripsi, ringkasan harga |
| `type.label` | Inter | 13 / 18 | 600 | Label field, chip, tombol kecil |
| `type.caption` | Inter | 12 / 16 | 400–600 | Helper, error inline, meta |
| `type.micro` | Inter | 11 / 14 | 500–700 | Label bottom nav, status pill, badge |
| `type.mono.lg` | JetBrains Mono | 20–32 / 1.2 | 600 | Nomor VA, Order ID, countdown VA |
| `type.mono` | JetBrains Mono | 12–15 / 1.4 | 400–600 | Nomor HP kontak, kode voucher, tanggal |

Aturan:
- Maksimal dua weight per layar untuk teks isi.
- Angka uang **tanpa** spasi setelah `Rp` (`Rp158.000`), kecuali bug `glitch_user` (PRD §8.8).

### 2.4 Spacing, radius, elevasi, motion
| Kategori | Token → nilai |
|---|---|
| Spacing (grid 4) | `space.1` 4 · `space.1.5` 6 · `space.2` 8 · `space.2.5` 10 · `space.3` 12 · `space.3.5` 14 · `space.4` 16 · `space.5` 20 · `space.6` 24 · `space.8` 32 · `space.12` 48 |
| Padding layar | 16 (layar daftar/kartu) · 20 (form, sheet) · 24 (auth & layar hasil) |
| Jarak antar field | 14–16 · antar kartu 10–12 · antar section 18–22 |
| Radius | `radius.xs` 4 (badge kecil) · `radius.sm` 6 (status pill, label) · `radius.md` 10 (tombol kecil, thumbnail, chip kotak) · `radius.lg` 12 (field, tombol utama) · `radius.xl` 14 (kartu) · `radius.2xl` 16 (kartu besar, grup list) · `radius.sheet` 24 (sudut atas bottom sheet, header PracticePay) · `radius.dialog` 28 (dialog/date picker) · `radius.full` 999 (chip pill, toast, avatar) |
| Border | 1 dp (`outline`) untuk kartu · 1,5 dp untuk field, tombol sekunder, opsi terpilih · 1,5 dp putus-putus untuk "slot" (voucher, lihat semua, opsi nonaktif) |
| Elevasi | Kartu: datar dengan border (tanpa bayangan) · Menu/popup: `0 8 24 rgba(18,22,28,.16–.18)` · Switch thumb: `0 1 2 rgba(0,0,0,.25)` · Segmented aktif: `0 1 2 rgba(18,22,28,.12)` |
| Motion | Durasi standar 200 ms, sheet 250 ms, snackbar tampil 4 dtk, toast ~2 dtk; banner auto-slide 4 dtk. Bila Developer Options "Animasi" OFF → semua durasi 0 |

## 3. Layout & ukuran
| Elemen | Ukuran |
|---|---|
| Lebar referensi | 390 dp (tinggi 844 untuk layar standar; artboard lebih tinggi = layar scroll) |
| Top app bar | 64 dp; tombol back/aksi 48 dp; judul `type.title.md` |
| Bottom navigation | 68 dp, 5 item, ikon 22–24 dp + label `type.micro` |
| Bottom action bar | padding 12/16–24, tombol 52 dp, garis atas `outline` |
| Tombol utama | tinggi 52 dp, radius 12, lebar penuh |
| Tombol sekunder/kecil | 40–46 dp (target sentuh tetap 48 dp via padding) |
| Field | 50–52 dp, radius 12, padding horizontal 14 |
| Baris list | 54–64 dp |
| Ikon | 18–24 dp, stroke 2 dp |
| Target sentuh minimum | **48 dp** |

## 4. Komponen
Setiap komponen di bawah ini wajib dibuat sebagai komponen ulang-pakai: custom view/style di Views, composable di Compose. Kolom "Locator" mengikuti PRD §12.

### 4.1 Top app bar
- Kiri: tombol back (ikon panah, content-desc `"Kembali"`), atau `"Tutup"` (ikon ×) untuk layar modal (3DS, Scan QR, Image Viewer).
- Judul `type.title.md`, rata kiri. Aksi kanan opsional (mis. "Atur urutan" sebagai text button).
- Latar `surface` dengan garis bawah `outline`. Pengecualian: Scan QR & Image Viewer berlatar gelap, Beranda memakai header teal.

### 4.2 Tombol
| Varian | Spesifikasi | Contoh |
|---|---|---|
| Primary | `primary` + teks `on.primary` `type.title.sm` 700, 52 dp, radius 12 | Masuk, Bayar, Simpan |
| Secondary (outline) | latar `surface`, border 1,5 `outline.strong`, teks `ink` | Reset, Cek Status, Kembali |
| Outline primary | border 1,5 `primary`, teks `primary` | "+ Keranjang", Beli Sekarang, Salin |
| Danger outline | border 1,5 `error`, teks `error` | Batalkan Pesanan, Keluar, Reset data |
| Danger filled | `error` + putih | Konfirmasi "Batalkan" di sheet |
| Text button | tanpa latar, teks `primary` 700 | Lupa password?, Lihat semua, Kirim ulang |
| Disabled | latar `surface.disabled`, teks `ink.muted`, tanpa border | semua tombol yang menunggu input valid |
| Loading | label diganti spinner 20 dp + tetap disabled | Bayar, Simpan Alamat, Verifikasi |

Aturan:
- **Disabled-until-valid** dipakai di semua form (PRD).
- Teks tombol memakai kata kerja ("Bayar Rp158.000", "Simpan Alamat").

### 4.3 Field input
- Label di atas field (`type.label`, `ink.secondary`). **Tidak memakai floating label**, supaya label tetap terlihat di hierarchy.
- Border 1,5 `outline.strong`, fokus `primary`, error `error` dengan pesan `type.caption` 600 `error` di bawah field.
- Varian:
  - **Password**: tombol mata di kanan (content-desc `"Tampilkan password"` / `"Sembunyikan password"`).
  - **Prefix**: `+62` dengan latar `surface.subtle`.
  - **Clear**: ikon ×, content-desc `"Kosongkan …"`.
  - **Textarea**: dengan counter `27/200`.
  - **Nominal**: Space Grotesk 30, tinggi 64, auto-format Rupiah.
  - **Kartu**: JetBrains Mono, grup 4 digit.
  - **OTP**: 6 kotak 56 dp, JetBrains Mono 22, border `primary` saat terisi.
  - **Pencarian**: latar `surface.subtle` atau border, ikon kaca pembesar, IME Search.
- Field read-only: border putus-putus. Field disabled: latar `#EEF0F3`, teks `ink.muted`.

### 4.4 Checkbox, radio, switch, segmented
- Checkbox/radio 20 dp, warna terpilih `primary`. Pilih Semua mendukung tri-state (kotak teal dengan garis putih).
- **Opsi radio besar** (kurir, metode bayar): kartu 60 dp, border 1,5. Terpilih = border `primary` + latar `primary.surface`. Nonaktif = border putus-putus + teks `ink.disabled` + alasan di baris kedua.
- Switch: track 52×32 (ON `primary`, OFF `track`), thumb putih 24 dp.
- Segmented control (Developer Options): latar `surface.subtle`, item aktif putih + bayangan kecil, tinggi 36.

### 4.5 Chip
- Pill tinggi 38–40, padding 14–16, `type.label`.
- Default: border 1,5 `outline.strong`. Terpilih: border `primary`, latar `primary.container`, teks `primary.pressed`.
- Chip filter "Semua/Masuk/Keluar" di Riwayat memakai varian gelap: terpilih `ink` + putih.

### 4.6 Kartu produk
- **Grid (2 kolom, gap 10):** area gambar 130–140 dp, nama `type.body` 600 (1 baris + ellipsis di hasil pencarian), harga `type.title.sm` 700, harga coret + persen diskon (`error`), rating `★ 4.8` (caption), tombol "+ Keranjang" (outline primary, 40 dp) di bawah. Stok habis → tombol disabled "Stok Habis".
- **List:** thumbnail 64 dp, teks di tengah, tombol ikon + (content-desc `"Tambah <nama> ke keranjang"`).
- Area gambar dan teks adalah satu target (buka detail). Tombol keranjang target terpisah.

### 4.7 Grup list (menu, transaksi, kontak)
Kartu `surface` radius 14–16 dengan border, isi baris dipisah garis 1 dp. Baris: ikon kiri 20 dp, teks, meta/tag kanan, chevron. Sticky header (Kontak A–Z, tanggal Riwayat): latar `background`, `type.label` 700.

### 4.8 Badge & status pill
- Badge angka: lingkaran 18 dp `error` + putih `type.micro` 700 (keranjang, filter aktif).
- Status pesanan:

| Status | Latar / teks |
|---|---|
| Menunggu Pembayaran | `warning.container` / `warning.strong` |
| Dibayar, Dikemas (Diproses), Dikirim | `info.container` / `info.strong` |
| Selesai | `primary.container` / `primary.pressed` |
| Dibatalkan | `surface.subtle` / `ink.muted` |

### 4.9 Banner
Kotak radius 12–14, ikon 18–22 dp + teks `type.body` 600.
- **Error**: `error.container`, `role=alert`.
- **Info/izin**: `info.container` + tombol "Izinkan".
- **Warning/flash sale**: `warning.container` + border.
- **Sukses**: `primary.container`.

### 4.10 Bottom sheet
Sudut atas radius 24, handle 40×4 `track`, judul `type.title.md` + tombol tutup. Isi bisa scroll sendiri (scroll bersarang di Voucher). Tombol aksi di footer dengan garis atas. Scrim `color.scrim`. Dipakai untuk: Filter & Urutkan, Voucher, Batalkan Pesanan, PIN PracticePay.

### 4.11 Dialog
- Material 3 dialog radius 28.
- Konfirmasi (Keluar, Reset data): judul + isi + dua text button kanan.
- Date picker: header "Pilih tanggal" + tanggal besar Space Grotesk, grid 7 kolom, hari terpilih lingkaran `primary`, aksi "Batal"/"OK".
- Dialog izin sistem (kamera, kontak, lokasi, notifikasi) **bukan** komponen app. Itu UI Android, dan artboard Scan QR hanya meniru tampilannya.

### 4.12 Snackbar & toast
- **Snackbar**: latar `dark.surface.raised`, teks `#F1F3F6` 14, aksi `dark.accent` atau `#A9C1FF` 700, radius 10, di atas bottom bar. Contoh: "Ditambahkan ke keranjang · Lihat", "Nomor VA disalin", "Item dihapus · Undo".
- **Toast**: pill `color.toast`, teks putih 14, di tengah bawah. Contoh: "Menghubungkan ke CS PracticeMart…".

### 4.13 Stepper qty
Border 1,5 `outline.strong` radius 10–12; tombol −/+ 44 dp (target 48 dp); angka JetBrains Mono 600. Tombol − disabled di qty 1; pesan `"Maksimal pembelian 10 pcs"` di sebelahnya.

### 4.14 PIN pad (PracticePay)
Bottom sheet: judul "Masukkan PIN PracticePay", 6 titik indikator (14 dp; terisi `primary`, kosong outline `track`), grid 3×4 tombol 56 dp radius 14 latar `background`.
- Teks tombol angka `0`–`9` (`type.headline` 22). Tombol hapus content-desc `"Hapus"`. Slot kosong tidak terlihat.
- Error `"PIN salah. Sisa N percobaan."` di atas pad.

### 4.15 Timeline status (Tracking)
Lingkaran 24 dp: selesai = isi `primary` + centang putih; saat ini = ring tebal 6 dp `primary`; belum = outline `track`. Garis penghubung 2 dp (`primary` untuk yang sudah lewat). Label 15 dp (saat ini 700), waktu/keterangan caption.

### 4.16 Countdown & nilai dinamis
- **Countdown VA**: JetBrains Mono 32 dalam banner warning (`role=timer`).
- **Flash sale**: 3 kotak `warning` putih (jam, menit, detik).
- **OTP/kirim ulang**: inline text button `"Kirim ulang dalam 00:24"`.
- **Order ID**: JetBrains Mono, di kartu gelap memakai `dark.accent`, selalu dengan tombol salin.

### 4.17 Ikon
- Gaya garis (outline), grid 24, stroke 2 dp, ujung & sambungan membulat.
- Implementasi: **Material Symbols Outlined/Rounded** sebagai vector drawable yang dibundel (offline). Ikon yang dipakai: arrow_back, close, search, tune (filter), grid_view, view_list, favorite (outline/terisi), more_vert, open_in_full, shopping_cart, home, category, account_balance_wallet, person, add, remove, content_copy, qr_code_scanner, upload (galeri), flashlight, fingerprint, location_on, my_location, calendar_month, visibility, check, expand_more, chevron_right, delete, sync, schedule, local_offer, bolt, notifications.
- Tombol yang hanya berisi ikon **wajib** punya content-desc (PRD §12.2).

### 4.18 Gambar produk & placeholder
- Implementasi memakai foto/ilustrasi produk WebP dari `assets/images/products/` (5 gambar per produk, rasio 1:1).
- Selama gambar dimuat, atau bila gambar tidak ada, tampilkan placeholder latar warna kategori + ikon garis kategori seperti di artboard:

| Kategori | Latar placeholder |
|---|---|
| Fashion | `primary.container` |
| Olahraga/Elektronik | `info.container` / `accent.orange.container` |
| Lainnya | `accent.purple.container` |

### 4.19 Header PracticePay
Latar `dark.surface`, sudut bawah radius 24, label "Saldo kamu", saldo `type.display` putih + tombol mata (content-desc `"Sembunyikan saldo"` / `"Tampilkan saldo"`, saldo tersembunyi `Rp••••••`), 4 aksi cepat (kotak 48 dp radius 16 `dark.surface.raised`, ikon `dark.accent`, label putih `type.micro`).

### 4.20 Locator Hint Overlay (Developer Options)
Label kecil JetBrains Mono 10 sp, latar `hint.bg`, teks `hint.fg`, radius 4, menempel di pojok kanan atas elemen ber-id. Tidak pernah tampil bila opsi OFF (default).

## 5. Pola interaksi
| Pola | Aturan |
|---|---|
| Validasi form | Validasi saat submit dan saat field kehilangan fokus. Error inline di bawah field. Tombol utama disabled sampai valid (kecuali Login, yang menampilkan error setelah ditekan) |
| Loading | Layar: skeleton/placeholder kartu. Aksi: spinner di tombol. Proses pembayaran: overlay non-cancelable "Memproses pembayaran…" |
| Empty state | Judul 16–17 700 + satu kalimat penjelas + (opsional) tombol aksi. Contoh: "Belum ada pesanan", "Wishlist masih kosong", "Kontak tidak ditemukan", "Tidak ada saran untuk "q"" |
| Error jaringan simulasi | Banner/inline `"Gagal memuat, coba lagi"` + tombol "Coba lagi" |
| Konfirmasi destruktif | Bottom sheet atau dialog; tombol konfirmasi danger, disabled sampai syarat terpenuhi (mis. alasan batal) |
| Umpan balik aksi | Tambah ke keranjang → snackbar + badge; salin → snackbar; transfer/bayar → layar/sheet sukses dengan ikon centang di lingkaran `primary.container` |
| Hasil pembayaran | Satu layar dengan tiga varian (Berhasil, Gagal, Menunggu), ikon dan warna sesuai status |
| Pull-to-refresh | Teks `"Diperbarui HH:mm:ss"` / `"Tarik untuk memperbarui"` di atas list |
| Infinite scroll | Footer spinner + `"Memuat … lainnya…"`, akhir daftar dengan teks penutup |
| Navigasi | Bottom nav untuk 5 tujuan utama. Detail & alur transaksi dibuka di atasnya dengan back. Modal (3DS, Scan QR, Image Viewer) memakai ikon × |

## 6. Bahasa & copy
- Bahasa default **Indonesia**, sapaan santai **"kamu"/-mu** ("Saldo kamu", "Pesananmu…"). Teks yang sudah ditetapkan PRD tetap dipakai apa adanya, termasuk yang memakai "Anda" (`"Akun Anda terkunci. Hubungi admin."`).
- Kalimat pendek, tanpa jargon teknis/automation.
- Format: uang `Rp158.000`; tanggal tampilan `15 Januari 2026` / `15 Jan 2026 · 09:12`; tanggal input `YYYY-MM-DD`; jam `HH:mm` (24 jam); nomor HP tampilan `0812-3456-7890`.
- Elipsis memakai karakter `…`.
- Semua string di `strings.xml` (ID) dan `values-en` (EN). Contoh teks EN mengikuti makna, bukan terjemahan literal.
- **Daftar teks wajib** ada di PRD §8 dan §11. Contoh yang paling sering di-assert: `"Username atau password salah"`, `"Halo, Standard User"`, `"Ditambahkan ke keranjang"`, `"Voucher tidak ditemukan"`, `"Pembayaran Berhasil"`, `"Nomor VA disalin"`, `"PIN salah. Sisa N percobaan."`, `"Transfer berhasil"`, `"Kode pos harus 5 digit"`.

## 7. Testability dalam desain
- Setiap elemen interaktif dan setiap teks yang di-assert punya resource-id sesuai PRD §12 (prefix `shop_`, `pay_`, `acc_`, `addr_`, `help_`, `dev_`). Contoh id yang sudah muncul di artboard: `shop_input_username`, `shop_input_password`, `shop_txt_login_error`, `shop_input_otp_1…6`, `shop_input_card_number`, `shop_input_voucher_code`, `pay_input_amount`, `pay_input_contact_search`, `pay_input_qr_amount`, `acc_input_name`, `acc_btn_birthdate`, `addr_input_postal`, `addr_spn_province`, `addr_spn_city`. Daftar lengkap ditulis di `docs/locators/` saat implementasi.
- Label field memakai elemen teks terpisah (bukan hint di dalam field), supaya label dan nilai bisa dibedakan.
- Teks kembar yang disengaja: "+ Keranjang" di setiap kartu produk, "Pakai" di setiap voucher, "Ubah" di setiap alamat.
- Konten WebView memakai `id`/`data-testid` (contoh: `#otp`, `faq-search`, `btn-contact`).
- PracticePay (Compose) memakai `testTag` dengan nama yang sama seperti resource-id. Riwayat sengaja tanpa testTag.

## 8. Aksesibilitas
- Kontras: §2.1 (semua pasangan teks ≥ 4,5:1).
- Target sentuh ≥ 48 dp.
- Content-desc bermakna untuk tombol ikon. Ikon dekoratif tidak dibaca (`importantForAccessibility=no`).
- Grup radio/segmented memakai semantik radio. Switch memakai semantik switch.
- Status penting (`role=alert` untuk error, `role=status` untuk snackbar/toast, `role=timer` untuk countdown) diumumkan TalkBack.
- Mendukung font scale 200%: teks boleh membungkus, tombol menyesuaikan tinggi.

## 9. Inventaris layar
Status: **Didesain** = ada artboard. **Belum** = belum ada artboard; ikuti komponen & pola di dokumen ini.

| ID | Layar | Artboard | Status | Elemen kunci | Ke |
|---|---|---|---|---|---|
| AUTH-01 | Onboarding | `Onboarding.dc.html` | Didesain | 3 slide, indikator titik, Lewati, Lanjut/Mulai | AUTH-02 |
| AUTH-02 | Login | `Login.dc.html` | Didesain | field username/password, mata, banner error, terkunci 30 dtk, Ingat saya, sidik jari | SHOP-01, AUTH-03, AUTH-04 |
| AUTH-03 | Register | `Register.dc.html` | Didesain | indikator kekuatan password, konfirmasi, S&K, Daftar disabled-until-valid | AUTH-02 |
| AUTH-04 | Lupa Password · OTP | `OTP.dc.html` | Didesain | 6 kotak OTP, Kirim ulang dalam 00:SS | AUTH-05 |
| AUTH-05 | Password Baru | `NewPassword.dc.html` | Didesain | checklist syarat live, layar sukses | AUTH-02 |
| SHOP-01 | Beranda | `ShopHome.dc.html` | Didesain | sapaan, pencarian, kartu saldo PracticePay, banner voucher, chip kategori, flash sale, grid + "+ Keranjang", bottom nav | SHOP-03, SHOP-05, PAY-01 |
| SHOP-02 | Kategori | – | Belum | baris horizontal per kategori (pola kartu produk §4.6) | SHOP-04 |
| SHOP-03 | Pencarian | `Search.dc.html` | Didesain | suggestion ≥ 2 huruf, riwayat (hapus/hapus semua), kategori populer | SHOP-04, SHOP-05 |
| SHOP-04 | Hasil Pencarian | `SearchResults.dc.html` | Didesain | tombol filter + badge, jumlah & urutan, toggle grid/list, infinite scroll | SHOP-05, Filter |
| SHOP-04 | Filter & Urutkan | `Filter.dc.html` | Didesain | sheet: urutkan, kategori, RangeSlider, rating, switch stok, Terapkan (badge) | SHOP-04 |
| SHOP-05 | Detail Produk | `ShopProduct.dc.html` | Didesain | galeri 3/5, wishlist, overflow menu, varian (XL disabled), stepper, snackbar, Beli Sekarang / + Keranjang | SHOP-06, SHOP-09, SHOP-11 |
| SHOP-06 | Image Viewer | `ImageViewer.dc.html` | Didesain | latar hitam, double tap 2×, indikator zoom, thumbnail | SHOP-05 |
| SHOP-07 | Ulasan | – | Belum | list panjang + chip filter bintang | – |
| SHOP-08 | Wishlist | `Wishlist.dc.html` | Didesain | hapus (hati), Atur urutan (handle drag), empty state | SHOP-05 |
| SHOP-09 | Keranjang | `ShopCart.dc.html` | Didesain | Pilih Semua tri-state, item, swipe-to-delete, voucher, ringkasan, Checkout (n) | SHOP-10, SHOP-11 |
| SHOP-10 | Voucher | `Voucher.dc.html` | Didesain | sheet: input + Tempel + Pakai, daftar Pakai/Lepas, error, Selesai | SHOP-09 |
| SHOP-11 | Checkout | `ShopCheckout.dc.html` | Didesain | alamat, item, kurir (Same Day nonaktif), asuransi, catatan, ringkasan Rp158.000 | SHOP-12 |
| SHOP-12 | Metode Pembayaran | `PayMethod.dc.html` | Didesain | kartu (Luhn), VA (bank), PracticePay (saldo), COD (limit) | SHOP-13/14/15 |
| SHOP-13 | 3D Secure | `ThreeDS.dc.html` | Didesain | WebView PracticeBank, OTP, Submit, Batal | SHOP-15 |
| SHOP-14 | Virtual Account | `VA.dc.html` | Didesain | countdown, nomor VA + Salin, accordion cara bayar, Cek Status / Saya sudah bayar | SHOP-15 |
| SHOP-15 | Hasil Pembayaran | `OrderResult.dc.html` | Didesain | varian Berhasil/Gagal/Pending (tweak), Order ID + salin | ORD-02, SHOP-01 |
| ORD-01 | Pesanan Saya | `Orders.dc.html` | Didesain | tab status, kartu pesanan, pull-to-refresh, empty state | ORD-02 |
| ORD-02 | Detail & Tracking | `ShopTracking.dc.html` | Didesain | kartu Order ID gelap, timeline 5 status, Batalkan | ORD-03 |
| ORD-03 | Batalkan Pesanan | `CancelSheet.dc.html` | Didesain | sheet: radio alasan, "Lainnya" + teks, tombol disabled-until-valid | ORD-01 |
| ORD-04 | Beri Ulasan | – | Belum | RatingBar 0,5, textarea, tambah foto | – |
| PAY-01 | PracticePay | `WalletHome.dc.html` | Didesain | header gelap, sembunyikan saldo, 4 aksi, promo cashback, transaksi terakhir | PAY-03/05/06 |
| PAY-02 | Top Up | – | Belum | pilih bank → instruksi (pola VA) | – |
| PAY-03 | Transfer · Kontak | `Contacts.dc.html` | Didesain | cari, banner izin kontak, "Terakhir", sticky A–Z | PAY-04 |
| PAY-04 | Transfer · Nominal & PIN | `Amount.dc.html` | Didesain | nominal auto-format, chip nominal, catatan, PIN pad sheet, sukses | PAY-06 |
| PAY-05 | Scan QR | `ScanQR.dc.html` | Didesain | viewfinder, senter, dialog izin sistem, ditolak → banner, Upload dari galeri | PAY-05 konfirmasi |
| PAY-05 | Konfirmasi Bayar QR | `QRConfirm.dc.html` | Didesain | merchant, nominal, PIN pad, sukses | PAY-06 |
| PAY-06 | Riwayat | `History.dc.html` | Didesain | chip Semua/Masuk/Keluar, sticky tanggal, infinite scroll | – |
| ACC-01 | Akun | `Account.dc.html` | Didesain | profil, menu (Pesanan, Wishlist, Alamat, PracticePay, Pengaturan, Bantuan, Developer Options), Keluar | – |
| ACC-02 | Edit Profil | `EditProfile.dc.html` | Didesain | ubah foto, field, date picker dialog, radio | ACC-01 |
| ACC-03 | Form Alamat | `AddressForm.dc.html` | Didesain | lokasi saat ini, chip label, provinsi → kota, kode pos, alamat utama | ACC-01 |
| ACC-04 | Pengaturan & Developer Options | `DevOptions.dc.html` | Didesain | Umum (bahasa, tema, notifikasi), DEV (deterministik, latency, animasi, error server, locator hint, simpan data), simulasi pembayaran, reset, About | ACC-01 |
| HELP-01 | Pusat Bantuan | `HelpCenter.dc.html` | Didesain | WebView: cari FAQ, accordion, Hubungi kami (JS bridge → toast); tombol native Chat dengan CS | ACC-01 |
| SYS-01 | Halaman tidak ditemukan | – | Belum | ilustrasi sederhana + "Kembali ke Beranda" (pola empty state) | SHOP-01 |
| – | Dialog Keluar / Reset data | – | Belum | §4.11 | – |
| – | Overlay "Memproses pembayaran…" | – | Belum | scrim + spinner + teks, non-cancelable | – |

## 10. Alur yang bisa dicoba di canvas (mode Play)
1. **Belanja → bayar kartu → lacak:** Onboarding → Login (`standard_user` / `Practice123!`) → Beranda → kartu Kaos Polos Basic → + Keranjang → Lihat → Keranjang → Checkout → Pilih Pembayaran → Kartu (`4111 1111 1111 1111`) → Bayar → 3DS OTP `123456` → Submit → Hasil → Lihat Pesanan → Tracking (Status berikutnya) → Batalkan.
2. **Lupa password:** Login → Lupa password? → OTP `123456` → Password Baru → Masuk.
3. **Cari & filter:** Beranda → Pencarian → ketik "ka" → Hasil → Filter → Terapkan.
4. **PracticePay:** Beranda (kartu saldo) → PracticePay → Transfer → Budi → nominal → Lanjut → PIN `123456` → Riwayat. Atau Scan QR → izinkan → Upload dari galeri → Bayar → PIN.
5. **Akun:** Akun → Edit Profil (date picker) / Alamat / Pengaturan & Developer Options / Pusat Bantuan.

## 11. Mapping implementasi Android
### 11.1 Views (XML)
`res/values/colors.xml` memakai nama token, misalnya:
```xml
<color name="pm_primary">#0E7C66</color>
<color name="pm_primary_pressed">#0A5E4D</color>
<color name="pm_primary_container">#DDF0EA</color>
<color name="pm_ink">#12161C</color>
<color name="pm_ink_secondary">#3A4350</color>
<color name="pm_ink_muted">#5B6573</color>
<color name="pm_background">#F4F6F5</color>
<color name="pm_surface">#FFFFFF</color>
<color name="pm_outline">#E3E6EB</color>
<color name="pm_outline_strong">#D5DAE1</color>
<color name="pm_error">#B42318</color>
<!-- lengkap sesuai §2.1 -->
```
`themes.xml`: `Theme.PracticeMart` (parent `Theme.Material3.Light.NoActionBar`) memetakan:
- `colorPrimary` → `pm_primary`
- `colorOnPrimary` → putih
- `colorPrimaryContainer` → `pm_primary_container`
- `colorSurface` → `pm_surface`
- `colorOnSurface` → `pm_ink`
- `colorOnSurfaceVariant` → `pm_ink_secondary`
- `colorOutline` → `pm_outline_strong`
- `colorError` → `pm_error`
- `android:colorBackground` → `pm_background`

Komponen memakai style turunan: `Widget.PracticeMart.Button.Primary`, `.Button.Outline`, `.Button.Danger`, `.TextField`, `.Chip.Filter`, `.Card`. Tipografi memakai `TextAppearance.PracticeMart.*` sesuai §2.3. Dimensi ada di `dimens.xml` (`pm_space_*`, `pm_radius_*`, `pm_button_height` = 52dp).

### 11.2 Compose (PracticePay)
`ui/theme/Tokens.kt` berisi `object PmColors`, `PmType`, `PmSpace`, `PmRadius` dengan nilai yang sama. `PracticeMartTheme` membungkus `MaterialTheme(colorScheme = lightColorScheme(primary = PmColors.Primary, …))`. Root layar PracticePay memakai `Modifier.semantics { testTagsAsResourceId = true }` (kecuali Riwayat).

### 11.3 Aturan untuk agent saat mengimplementasi layar
1. Cari ID layar di §9, buka artboard terkait untuk tata letak, lalu baca FR di PRD untuk perilaku & teks.
2. Susun dari komponen §4; jangan membuat varian baru tanpa menambahkannya ke dokumen ini.
3. Beri resource-id ke setiap elemen interaktif & teks assertion sesuai PRD §12. Tambahkan ke `docs/locators/<layar>.md`.
4. Pastikan semua state ada: default, loading, error, empty, disabled, sukses.
5. Cek target sentuh 48 dp, content-desc, dan kontras.
6. Jangan menampilkan id/deep link/teks latihan di UI.

## 12. Keputusan terbuka
| # | Hal | Usulan |
|---|---|---|
| DS-1 | Tema gelap | Usulan §2.2, perlu desain & verifikasi kontras |
| DS-2 | Foto produk | Ilustrasi flat bergaya sama per kategori (lebih ringan dan konsisten) atau foto stok berlisensi bebas |
| DS-3 | Layar Should yang belum didesain (§9) | Didesain saat milestone terkait (PRD §17) |
| DS-4 | Ikon aplikasi (launcher) | Tas belanja putih di kotak teal `#0E7C66` sudut membulat, seperti logo di layar Login |

## 13. Riwayat perubahan
| Versi | Tanggal | Perubahan |
|---|---|---|
| 1.0 | 2026-11-12 | Versi awal: token, komponen, pola, inventaris 33 artboard, mapping implementasi. Sinkron dengan PRD v0.4 & architecture v0.2. Perbaikan desain saat sinkronisasi: id/deep link yang tampil di UI dihapus, tombol "+ Keranjang" di kartu produk, overflow menu & Image Viewer, Login sukses langsung ke Beranda, salin Order ID, opsi "Simpan data setelah app ditutup" & "Reset pengaturan", kontras sapaan Beranda diperbaiki, tambah 7 layar (Register, Password Baru, Hasil Pencarian, Image Viewer, Wishlist, Voucher, Konfirmasi QR) |
