# Locator: Pesanan (ORD-01…03)

Kontrak publik (PRD §12.3). Mengubah atau menghapus id di sini = breaking change.
Id memakai prefix `com.automation.practice:id/`. Id sama di beberapa baris daftar itu disengaja.
Deep link: `practiceapp://shop/orders`, `practiceapp://shop/order/{orderId}`.

## ORD-01 Pesanan Saya
| Id | Elemen | Catatan |
|---|---|---|
| `shop_btn_back` | Kembali | content-desc `Kembali` |
| `shop_txt_title` | Judul | `Pesanan Saya` |
| `shop_tab_orders` | Tab status | Semua / Menunggu / Diproses / Dikirim / Selesai / Dibatalkan; bisa diklik dan di-swipe |
| `shop_txt_orders_updated` | Waktu segar | `Tarik untuk memperbarui · HH:mm` |
| `shop_refresh_orders` | Tarik-segarkan | bisa gagal sesuai "Peluang error server": `Gagal memuat, coba lagi` + aksi `Coba lagi` |
| `shop_pager_orders` | Halaman per tab | |
| `shop_list_orders` | Daftar pesanan | satu per tab |
| `shop_item_order_row` | Baris pesanan | id sama di semua baris |
| `shop_txt_order_id` | Order ID | `PM-20260115-0001`, mono |
| `shop_txt_order_status` | Pil status | teks sama dengan tab; Menunggu Pembayaran, Dibayar, Dikemas, Dikirim, Selesai, Dibatalkan |
| `shop_txt_order_item` | Nama produk | `+N lainnya` bila lebih dari satu |
| `shop_txt_order_meta` | Varian | `Hitam · L · 2 pcs` |
| `shop_txt_order_total` | Total | `Rp158.000` |
| `shop_txt_order_note` | Catatan | `Bayar dalam MM:SS` (VA menunggu) atau alasan batal |
| `shop_empty_orders` | Empty state | `Belum ada pesanan` |

Ketuk pesanan VA yang menunggu membuka instruksi VA; pesanan lain membuka Detail.

## ORD-02 Detail & Tracking
| Id | Elemen | Catatan |
|---|---|---|
| `shop_txt_detail_order_id` | Order ID | bisa disalin dengan menekan lama |
| `shop_btn_copy_order_id` | Salin | content-desc `Salin Order ID`; snackbar `Order ID disalin` |
| `shop_txt_detail_method` / `shop_txt_detail_total` | Metode, total | |
| `shop_txt_detail_interval` | Mode update | `interval 10 dtk` atau `mode manual` |
| `shop_list_timeline` | Timeline | 5 langkah; teks langkah memakai nama status; langkah selesai bercentang |
| `shop_txt_step_label` / `shop_txt_step_sub` | Nama langkah / waktu | waktu `HH:mm:ss`, `Belum` bila belum tercapai |
| `shop_btn_next_status` | `Status berikutnya (mode manual)` | hanya tampil di mode Manual |
| `shop_btn_cancel_order` | `Batalkan Pesanan` | tampil bila status ≤ Dikemas |
| `shop_txt_cannot_cancel` | `Pesanan yang sudah dikirim tidak bisa dibatalkan` | status Dikirim |
| `shop_btn_review` | `Beri Ulasan` | status Selesai; ORD-04 belum dibangun, menampilkan snackbar `Ulasan segera hadir` |
| `shop_txt_detail_cancelled` | Teks batal | pesanan Dibatalkan; berisi alasan |

Status maju otomatis tiap interval Developer Options (3/10/30 dtk). Pesanan PracticePay yang Selesai memberi cashback 5% (maks Rp10.000) ke saldo.

## ORD-03 Batalkan Pesanan (bottom sheet)
| Id | Elemen | Catatan |
|---|---|---|
| `shop_sheet_cancel` | Sheet | |
| `shop_group_cancel_reason` | Grup radio | 4 alasan, teks persis PRD |
| `shop_input_cancel_note` / `shop_label_cancel_note` | Field "Tulis alasanmu" | muncul bila `Lainnya` dipilih; wajib terisi |
| `shop_btn_cancel_back` | `Kembali` | |
| `shop_btn_cancel_confirm` | `Batalkan` | disabled sampai alasan dipilih |
| `shop_panel_cancel_done` | Panel sukses | `Pesanan dibatalkan` |
| `shop_btn_cancel_done` | `Ke Pesanan Saya` | stok dikembalikan |
