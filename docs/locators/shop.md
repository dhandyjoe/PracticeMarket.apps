# Locator: Belanja (SHOP-01…06, SHOP-08)

Keranjang, Checkout, dan Pembayaran ada di [checkout.md](checkout.md).

Kontrak publik (PRD §12.3). Mengubah atau menghapus id di sini = breaking change.
Id memakai prefix `com.automation.practice:id/`. Id sama di beberapa baris daftar itu disengaja (latihan locator relatif).

## Bottom navigation (MainActivity)
| Id | Elemen | Catatan |
|---|---|---|
| `shop_nav_bottom` | Bar navigasi | hanya tampil di layar tab |
| `shop_tab_home` / `_category` / `_pay` / `_cart` / `_account` | Tab | Beranda, Kategori, PracticePay, Keranjang, Akun. Kategori masih "Segera hadir"; PracticePay lihat [pay.md](pay.md); Keranjang lihat [checkout.md](checkout.md) |
| badge di `shop_tab_cart` | Jumlah barang di keranjang | hilang saat 0 |

## SHOP-01 Beranda
| Id | Elemen | Catatan |
|---|---|---|
| `shop_txt_greeting` | Sapaan | `Halo, Standard User` |
| `shop_btn_search` | Kotak cari | membuka SHOP-03 |
| `shop_card_wallet` / `shop_txt_wallet_balance` | Kartu saldo / saldo | `Rp500.000` |
| `shop_pager_banner` | Banner voucher | geser otomatis tiap 4 detik, content-desc indikator `Banner 1 dari 3` |
| `shop_chip_category` | Chip kategori | id sama di tiap chip; 6 chip |
| `shop_banner_flash_sale` / `shop_txt_flash_countdown` | Flash sale / hitung mundur | `HH:MM:SS` |
| `shop_btn_view_grid` / `shop_btn_view_list` | Toggle tampilan | `selected` = aktif; content-desc `Tampilan grid` / `Tampilan list` |
| `shop_list_products` | Daftar rekomendasi | 5 produk, urutan acak tiap Beranda dibuka |
| `shop_btn_see_all` | Sel "Lihat semua" | hanya di tampilan grid |

## Kartu produk (Beranda, Hasil Pencarian)
| Id | Elemen | Catatan |
|---|---|---|
| `shop_item_product_card` | Kartu / baris | id sama di semua produk |
| `shop_item_product_open` | Area gambar + teks | membuka SHOP-05 |
| `shop_txt_product_name` / `_price` / `_rating` | Nama / harga / `★ 4.8` | |
| `shop_txt_product_old_price` / `_discount` | Harga coret / `−25%` | hanya produk diskon |
| `shop_btn_add_to_cart` | `+ Keranjang` | **teks kembar** di semua kartu. Tampilan list: tombol ikon, content-desc `Tambah <nama> ke keranjang`. Stok 0: disabled, `Stok Habis` |

## SHOP-03 Pencarian
| Id | Elemen | Catatan |
|---|---|---|
| `shop_btn_back` | Kembali | content-desc `Kembali` |
| `shop_input_search` | Kolom cari | IME action Search |
| `shop_btn_clear_search` | Hapus teks | content-desc `Hapus teks`, muncul bila ada teks |
| `shop_list_suggestions` | Saran | muncul mulai 2 karakter, maks 5 |
| `shop_item_suggestion_row` / `shop_txt_suggestion` / `shop_txt_suggestion_category` | Baris saran | |
| `shop_txt_no_suggestion` | Kosong | `Tidak ada saran untuk "<q>"` |
| `shop_list_history` | Riwayat | maks 5, terbaru di atas |
| `shop_item_history_row` / `shop_txt_history` | Baris riwayat | tekan lama → menu `Hapus` |
| `shop_btn_delete_history` | × per baris | content-desc `Hapus <kata>` |
| `shop_btn_clear_history` | `Hapus semua` | |
| `shop_txt_history_empty` | Kosong | `Belum ada riwayat pencarian` |
| `shop_group_popular` | Kategori populer | berisi `shop_chip_category` |

## SHOP-04 Hasil Pencarian
| Id | Elemen | Catatan |
|---|---|---|
| `shop_btn_search_box` / `shop_txt_search_query` | Kotak kata kunci | membuka SHOP-03 |
| `shop_btn_filter` | Tombol filter | content-desc `Filter` atau `Filter, N aktif` |
| `shop_badge_filter` | Badge jumlah filter aktif | hilang saat 0; urutan tidak dihitung |
| `shop_txt_result_count` | `12 produk · Harga Terendah` | |
| `shop_btn_view_grid` / `shop_btn_view_list` | Toggle tampilan | |
| `shop_list_results` | Daftar hasil | 20 per halaman, infinite scroll |
| `shop_progress_more` / `shop_txt_list_footer` | Penutup daftar | `Memuat produk lainnya…` lalu `Semua produk sudah ditampilkan` |
| `shop_empty_results` / `shop_btn_reset_filter` | Hasil kosong | `Produk tidak ditemukan` / `Reset filter` |

## SHOP-04 Filter & Urutkan (bottom sheet)
| Id | Elemen | Catatan |
|---|---|---|
| `shop_sheet_filter` | Sheet | |
| `shop_btn_close_filter` | Tutup | content-desc `Tutup` |
| `shop_group_sort` | Urutkan | chip tunggal: `Paling Relevan`, `Harga Terendah`, `Harga Tertinggi`, `Rating Tertinggi`, `Terbaru` |
| `shop_group_category` | Kategori | chip multi |
| `shop_slider_price` | RangeSlider harga | langkah Rp5.000, Rp0 – Rp2.000.000 |
| `shop_txt_price_range` | Rentang | `Rp50.000 – Rp200.000` |
| `shop_input_price_min` / `shop_input_price_max` | Isian harga | diterapkan saat fokus lepas atau Done |
| `shop_rdo_rating_all` / `_3` / `_4` / `_45` | Rating minimal | `Semua`, `≥ 3`, `≥ 4`, `≥ 4,5` |
| `shop_sw_stock_only` | `Hanya stok tersedia` | |
| `shop_btn_reset` | `Reset` | belum menerapkan |
| `shop_btn_apply` / `shop_badge_apply` | `Terapkan` / jumlah | content-desc `Terapkan, N filter aktif` |

## SHOP-05 Detail Produk
| Id | Elemen | Catatan |
|---|---|---|
| `shop_pager_gallery` | Galeri | 5 gambar, swipe |
| `shop_txt_gallery_counter` | `3/5` | |
| `shop_btn_prev_image` / `shop_btn_next_image` | Gambar sebelumnya / berikutnya | berputar 5 → 1 |
| `shop_btn_zoom` | Perbesar | content-desc `Perbesar gambar` |
| `shop_btn_wishlist` | Hati | content-desc `Tambah ke wishlist` / `Hapus dari wishlist` |
| `shop_btn_more` | Overflow | content-desc `Menu lainnya`; menu `Bagikan`, `Laporkan` |
| `shop_txt_product_meta` | `P001 · Fashion` | |
| `shop_txt_product_name` / `_price` / `_old_price` / `_discount` | Info produk | harga `glitch_user` memakai spasi non-breaking |
| `shop_txt_color_label` / `shop_group_color` | Warna | chip tunggal; chip dicoret & disabled bila stok 0 |
| `shop_txt_size_label` / `shop_group_size` | Ukuran | idem |
| `shop_txt_stock_note` | `Hitam · XL: stok 0` | |
| `shop_btn_qty_minus` / `shop_btn_qty_plus` | Stepper | content-desc `Kurangi` / `Tambah`; minus disabled di 1 |
| `shop_txt_qty` | Jumlah | |
| `shop_txt_qty_max` | Pesan batas | `Maksimal pembelian 10 pcs`, atau `Stok tersisa N pcs` |
| `shop_btn_buy_now` | `Beli Sekarang` | membuka Checkout hanya dengan produk ini (varian dan jumlah terpilih) |
| `shop_btn_add_to_cart` | `+ Keranjang` | Snackbar `Ditambahkan ke keranjang` + aksi `Lihat`; stok habis: `Stok Habis` disabled |

## SHOP-06 Image Viewer
| Id | Elemen | Catatan |
|---|---|---|
| `shop_btn_close_viewer` | Tutup | content-desc `Tutup` |
| `shop_txt_image_counter` | `3/5` | |
| `shop_zoom_image` | Area gambar | pinch 50–300%, double tap 2×, lempar horizontal untuk pindah |
| `shop_txt_zoom_level` | `2,0×` | tampil bila zoom ≠ 1× |
| `shop_btn_prev_image` / `shop_btn_next_image` | Panah | |
| `shop_item_viewer_thumb` | Thumbnail | content-desc `Gambar 1`…`Gambar 5`; `selected` = aktif |

## SHOP-08 Wishlist (dari tab Akun › Wishlist)
| Id | Elemen | Catatan |
|---|---|---|
| `shop_btn_edit_order` | `Atur urutan` / `Selesai` | |
| `shop_list_wishlist` | Daftar | awal: P001, P010, P002, P020 |
| `shop_item_wishlist_row` | Baris | id sama di semua baris |
| `shop_txt_wish_name` / `shop_txt_wish_price` | Nama / harga | |
| `shop_btn_wish_remove` | Hati | content-desc `Hapus <nama> dari wishlist` |
| `shop_img_drag_handle` | Handle drag | hanya saat `Atur urutan`; content-desc `Geser <nama>` |
| `shop_btn_wish_up` | Panah naik | hanya saat `Atur urutan`; content-desc `Pindahkan <nama> ke atas` |
| `shop_empty_wishlist` / `shop_btn_start_shopping` | Kosong | `Wishlist masih kosong` / `Mulai belanja` |

## Tab Akun (sementara)
| Id | Elemen | Catatan |
|---|---|---|
| `shop_item_wishlist` | Baris Wishlist | |
| `shop_btn_logout` | `Keluar` | |

## Deep link
`practiceapp://shop`, `…/shop/search?q=<kata>`, `…/shop/product/{id}`, `…/shop/category/{slug}`, `…/shop/wishlist`, `…/shop/cart`. Id atau slug tak dikenal: toast `Halaman tidak ditemukan`.
