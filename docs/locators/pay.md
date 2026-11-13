# Locator: PracticePay (PAY-01, 03…06)

Kontrak publik (PRD §12.3). Mengubah atau menghapus id di sini = breaking change.
Id memakai prefix `com.automation.practice:id/`.
**Transfer (PAY-03, 04, QR konfirmasi) dibangun dengan Jetpack Compose**: id berasal dari `testTag` + `testTagsAsResourceId`.
**Riwayat (PAY-06) dibangun dengan Compose tanpa testTag** sebagai pembanding: tidak ada resource-id, cari lewat teks, content-desc, atau posisi.
Deep link: `practiceapp://pay`, `pay/transfer`, `pay/history`, `pay/qr?merchant=KOPIPAGI&amount=28000`.

## PAY-01 PracticePay (View)
| Id | Elemen | Catatan |
|---|---|---|
| `pay_txt_balance` | Saldo | `Rp500.000`, tersembunyi `Rp••••••` |
| `pay_btn_toggle_balance` | Mata | content-desc `Sembunyikan saldo` / `Tampilkan saldo` |
| `pay_btn_topup` | Top Up | belum aktif: snackbar `Fitur ini segera hadir` |
| `pay_btn_transfer` / `pay_btn_scan` / `pay_btn_history` | Aksi cepat | Transfer, Scan QR, Riwayat |
| `pay_list_recent` | 4 transaksi terakhir | |
| `pay_item_tx_row` | Baris transaksi | id sama di semua baris |
| `pay_txt_tx_title` / `pay_txt_tx_time` / `pay_txt_tx_amount` | Isi baris | jumlah `+Rp300.000` / `−Rp86.000` |
| `pay_link_see_all` | `Lihat semua` | membuka Riwayat |

## PAY-03 Transfer · Kontak (Compose, testTag)
| Id | Elemen | Catatan |
|---|---|---|
| `pay_btn_back`, `pay_txt_title` | Top bar | |
| `pay_input_contact_search` | Cari | nama atau nomor HP |
| `pay_txt_contacts_banner`, `pay_btn_allow_contacts` | Banner izin `READ_CONTACTS` | hilang bila izin diberikan; saat ditolak kontak uji tetap tampil |
| `pay_item_recent` | Chip "Terakhir" | |
| `pay_list_contacts` | Daftar | |
| `pay_txt_contact_header` | Header huruf A–Z | sticky |
| `pay_item_contact_row`, `pay_txt_contact_name`, `pay_txt_contact_phone` | Baris kontak | |
| `pay_txt_empty_contacts` | `Kontak tidak ditemukan` | |

## PAY-04 Transfer · Nominal & PIN (Compose, testTag)
| Id | Elemen | Catatan |
|---|---|---|
| `pay_txt_payee_name` | Penerima | |
| `pay_input_amount` | Nominal | auto-format `Rp150.000` |
| `pay_txt_amount_error` | Galat | `Minimal transfer Rp10.000`, `Saldo tidak cukup` |
| `pay_txt_balance` | Saldo | `Saldo Rp500.000` |
| `pay_chip_50k` / `pay_chip_100k` / `pay_chip_200k` | Chip nominal | |
| `pay_input_note` | Catatan | maks 50 karakter |
| `pay_btn_next` | `Lanjut` | disabled sampai nominal valid |
| PIN | Bottom sheet PIN bersama checkout | id `shop_*` di [checkout.md](checkout.md); PIN `123456`, 3× salah mengunci 30 dtk |
| `pay_txt_success`, `pay_txt_success_body` | `Transfer berhasil` | |
| `pay_btn_history` / `pay_btn_done` | `Lihat Riwayat` / `Selesai` | |

## PAY-05 Scan QR (View) dan Konfirmasi Bayar (Compose, testTag)
| Id | Elemen | Catatan |
|---|---|---|
| `pay_btn_close` | Tutup | content-desc `Tutup` |
| `pay_frame_viewfinder`, `pay_txt_scan_hint` | Bingkai dan petunjuk | tanpa pratinjau kamera |
| `pay_banner_denied` | Banner `Izin kamera ditolak` | `role=alert` |
| `pay_btn_open_settings` | `Buka Pengaturan` | |
| `pay_btn_gallery` | `Upload dari galeri` | Photo Picker; nama berkas memuat `kopipagi` / `tokobuku` |
| `pay_input_qr_amount` | Nominal bayar | tidak bisa diubah untuk `TOKOBUKU` (Rp45.000) |
| `pay_btn_next` | `Bayar Rp28.000` | |
| `pay_txt_success` | `Pembayaran berhasil` | |

## PAY-06 Riwayat (Compose, **tanpa testTag**)
Tidak ada resource-id. Elemen yang bisa dicari: judul `Riwayat Transaksi`, chip `Semua` / `Masuk` / `Keluar`, ikon kalender (content-desc `Filter tanggal`), `Diperbarui HH:mm:ss`, header tanggal (`15 Januari 2026`), baris transaksi (judul, jam, jumlah), `Memuat transaksi lainnya…`. Halaman 20 transaksi, tarik untuk menyegarkan.
