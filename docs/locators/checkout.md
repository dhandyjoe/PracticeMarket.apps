# Locator: Keranjang, Checkout & Pembayaran (SHOP-09…15)

Kontrak publik (PRD §12.3). Mengubah atau menghapus id di sini = breaking change.
Id memakai prefix `com.automation.practice:id/`. Id sama di beberapa baris daftar itu disengaja.

## Data uji
| Hal | Nilai |
|---|---|
| Contoh kalkulasi | 2 × Kaos Polos Basic (Rp75.000) + `HEMAT10` + Express + asuransi = **Rp158.000** |
| Voucher | `HEMAT10`, `ONGKIR`, `MIN100K`, `EXPIRED` (aturan PRD §8.7) |
| Kartu sukses | `4111 1111 1111 1111`, exp `12/30`, CVV `123` |
| Kartu ditolak (hasil **Gagal**) | `4000 0000 0000 0002` |
| Kartu pending (hasil **Menunggu Pembayaran**) | `4000 0000 0000 0119` |
| OTP 3DS | `123456` |
| PIN PracticePay | `123456`; 3× salah mengunci 30 dtk |
| Saldo awal PracticePay | Rp500.000 |
| Order ID | `PM-20260115-0001`, `-0002`, … |

Kartu ditolak dan pending adalah keputusan implementasi; PRD tidak menetapkan pemicunya.

## SHOP-09 Keranjang (tab Keranjang)
| Id | Elemen | Catatan |
|---|---|---|
| `shop_chk_select_all` | Pilih Semua | tri-state: centang, tidak, campuran (`indeterminate`) |
| `shop_txt_selected_count` | `1 dari 2 dipilih` | |
| `shop_list_cart` | Daftar | geser kiri untuk hapus, Snackbar `<nama> dihapus` + `Urungkan` (4 dtk) |
| `shop_item_cart_row` | Baris | id sama di semua baris |
| `shop_chk_cart_item` | Centang item | content-desc `Pilih <nama>` |
| `shop_txt_cart_name` / `_variant` / `_price` | Nama / `Hitam · L` atau `Ukuran 42` / harga baris | |
| `shop_btn_cart_minus` / `shop_btn_cart_plus` / `shop_txt_cart_qty` | Stepper | content-desc `Kurangi` / `Tambah`; minus disabled di 1; maks min(stok, 10) |
| `shop_btn_cart_save` | `Simpan untuk nanti` / `Pindahkan ke keranjang` | baris disimpan: pudar, tidak bisa dipilih, tidak dihitung |
| `shop_btn_voucher` | Kartu voucher | judul `Voucher HEMAT10 terpasang` atau `Pakai voucher` |
| `shop_txt_voucher_title` / `_note` / `_off` | Isi kartu | `_off` = `−Rp15.000`, atau `Potongan di checkout` untuk `ONGKIR` |
| `shop_txt_cart_subtotal_label` / `shop_txt_cart_subtotal` | `Subtotal (2 pcs)` | |
| `shop_row_cart_discount` / `shop_txt_cart_discount` | Diskon voucher | tampil bila diskon > 0 |
| `shop_txt_cart_total` | Total belanja | |
| `shop_btn_checkout` | `Checkout (N)` | N = jumlah baris dipilih; disabled bila 0 |
| `shop_empty_cart` / `shop_btn_start_shopping` | Kosong | `Keranjang masih kosong` / `Mulai belanja` |

Badge tab (`shop_tab_cart`) = jumlah pcs semua baris yang tidak disimpan.

## SHOP-10 Voucher (bottom sheet)
| Id | Elemen | Catatan |
|---|---|---|
| `shop_sheet_voucher` | Sheet | |
| `shop_btn_close_voucher` | Tutup | content-desc `Tutup` |
| `shop_input_voucher_code` | Kode | huruf besar otomatis |
| `shop_btn_paste_voucher` | `Tempel` | isi dari clipboard |
| `shop_btn_apply_voucher` | `Pakai` | |
| `shop_txt_voucher_error` | Galat | `Voucher tidak ditemukan`, `Voucher sudah kedaluwarsa`, `Minimal belanja Rp100.000` |
| `shop_item_voucher_row` | Baris voucher | id sama; berisi `shop_txt_voucher_code`, `shop_txt_voucher_row_title`, `shop_txt_voucher_row_note` |
| `shop_btn_voucher_apply` | `Pakai` / `Lepas` per baris | `EXPIRED` disabled; hanya satu voucher aktif |
| `shop_btn_voucher_done` | `Selesai` / `Selesai · HEMAT10 terpasang` | |

## SHOP-11 Checkout
| Id | Elemen | Catatan |
|---|---|---|
| `shop_btn_back` | Kembali | |
| `shop_btn_change_address` | `Ubah` | belum ada ACC-03: hanya menampilkan `Ubah alamat segera hadir` |
| `shop_txt_address_label` / `shop_txt_address` | `Rumah` / alamat | `Jl. Merdeka No. 10, Bandung, Jawa Barat 40111` |
| `shop_list_checkout_items` | Barang | berisi `shop_item_checkout_row`, `shop_txt_checkout_name` / `_detail` / `_line_total` |
| `shop_rdo_courier_reguler` / `_express` / `_sameday` | Kurir | default Express. `Same Day` selalu disabled (`Tidak tersedia untuk kota ini`) |
| `shop_row_courier_*` | Baris kurir | dapat diketuk; `shop_txt_courier_*_price` = ongkir |
| `shop_sw_insurance` | Asuransi | default ON, `Rp2.000 flat` |
| `shop_input_note` | Catatan | opsional |
| `shop_list_summary` | Ringkasan | baris `Subtotal`, `Diskon voucher <KODE>`, `Ongkir`, `Asuransi`, `Biaya layanan`; id `shop_txt_summary_key` / `_value` |
| `shop_txt_checkout_total` | Total | |
| `shop_btn_pick_payment` | `Pilih Pembayaran · Rp158.000` | |

## SHOP-12 Metode Pembayaran
| Id | Elemen | Catatan |
|---|---|---|
| `shop_rdo_pay_card` / `_va` / `_wallet` / `_cod` | Metode | default Kartu. Wallet disabled bila saldo kurang (`Saldo tidak cukup`); COD disabled bila total > Rp1.000.000 |
| `shop_row_pay_*`, `shop_box_pay_*` | Baris / kartu metode | |
| `shop_input_card_number` | Nomor kartu | format per 4 digit |
| `shop_txt_card_number_error` | `Nomor kartu tidak valid` | muncul dari 13 digit bila Luhn salah |
| `shop_input_card_exp` | MM/YY | `/` otomatis |
| `shop_txt_card_exp_error` | `Kartu sudah kedaluwarsa` / `Format MM/YY tidak valid` | |
| `shop_input_card_cvv` | CVV | 3 digit, tersamar |
| `shop_group_bank` | Bank VA | `shop_chip_bank_bca` / `_bni` / `_mandiri` |
| `shop_txt_pay_wallet_note` / `shop_txt_pay_cod_note` | Keterangan | `Saldo Rp500.000` / `Untuk total maksimal Rp1.000.000` |
| `shop_txt_pay_total` | Total tagihan | |
| `shop_btn_pay` | `Bayar Rp158.000` | disabled sampai valid dan saat memproses |
| `shop_overlay_processing` | Overlay | `Memproses pembayaran…` ±2 dtk, tidak bisa dibatalkan |

PIN PracticePay (bottom sheet `shop_sheet_pin`): tombol angka dicari lewat teks `0`–`9`, hapus lewat content-desc `Hapus`, `shop_indicator_pin` (content-desc `N dari 6 digit terisi`), `shop_txt_pin_error` (`PIN salah. Sisa N percobaan.` / `PracticePay terkunci. Coba lagi dalam N detik.`).

## SHOP-13 3D Secure (WebView `https://secure.practicebank.test/3ds.html`)
Konteks `WEBVIEW_com.automation.practice`. Native: `shop_btn_close_threeds` (content-desc `Tutup`, membatalkan).
| Selector web | Elemen |
|---|---|
| `#otp` (`data-testid="otp"`) | Kolom OTP, 6 digit |
| `#error` (`data-testid="otp-error"`) | `Kode OTP salah` |
| `#submit` (`data-testid="submit"`) | Submit |
| `#cancel` (`data-testid="cancel"`) | `Batal`: memicu `confirm("Batalkan transaksi?")` yang tampil sebagai dialog native (OK / Cancel) |
| `#amount`, `#card` | `IDR 158.000`, `**** **** **** 1111` |

## SHOP-14 Virtual Account
| Id | Elemen | Catatan |
|---|---|---|
| `shop_txt_va_timer` | Hitung mundur | `MM:SS`, mulai 15:00 |
| `shop_txt_va_bank` / `shop_txt_va_number` | Bank / nomor | BCA `8077 0000 1234 5678`, BNI `8848 …`, Mandiri `8908 …` |
| `shop_btn_copy_va` | `Salin` | Snackbar `Nomor VA disalin` |
| `shop_txt_va_total` | Total tagihan | |
| `shop_btn_va_guide` | Judul accordion | selected = terbuka; berisi `shop_txt_va_guide_title`, `shop_txt_va_guide_steps` |
| `shop_btn_check_status` | `Cek Status` | Snackbar `Pembayaran belum diterima` (belum bisa gagal, lihat catatan) |
| `shop_btn_paid` | `Saya sudah bayar` | hasil Berhasil |

Waktu habis: pesanan `Dibatalkan`, stok kembali, hasil Gagal dengan `Waktu pembayaran habis. Pesananmu dibatalkan.`

## SHOP-15 Hasil Pembayaran
| Id | Elemen | Catatan |
|---|---|---|
| `shop_txt_result_title` | Judul | `Pembayaran Berhasil` / `Pembayaran Gagal` / `Menunggu Pembayaran` |
| `shop_txt_result_body` | Isi | |
| `shop_txt_order_id` | Order ID | `PM-20260115-0001`; tidak ada pada hasil Gagal |
| `shop_btn_copy_order` | Salin | content-desc `Salin Order ID` |
| `shop_txt_result_total` / `_method` / `_status` | Total / metode / status | status `Dibayar` atau `Menunggu Pembayaran` |
| `shop_btn_result_primary` | `Lihat Pesanan` / `Coba Lagi` | `Lihat Pesanan` belum membuka ORD-02: toast `Daftar pesanan segera hadir` |
| `shop_btn_result_home` | `Kembali ke Beranda` | |

Sukses: stok varian berkurang, saldo berkurang (PracticePay), barang terbeli keluar dari keranjang, voucher dilepas, dan notifikasi sistem `Pesanan PM-… berhasil dibuat` (butuh izin notifikasi di Android 13+; ketuk membuka app di Beranda).

## Belum diimplementasi
Developer Options (latensi, peluang error server untuk `Cek Status`, VA 30 dtk / 2 mnt), banner `Tidak ada koneksi`, TimePicker Same Day, ubah alamat, Detail Pesanan dari notifikasi. Bug `glitch_user`: subtotal salah bila qty ≥ 3, tap pertama `Bayar` diabaikan, badge keranjang tidak turun setelah hapus.
