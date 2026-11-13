# Locator: Akun, Alamat, Pengaturan & Bantuan (ACC-01…04, HELP-01)

Kontrak publik (PRD §12.3). Mengubah atau menghapus id di sini = breaking change.
Id memakai prefix `com.automation.practice:id/`.
Deep link: `practiceapp://account`, `account/address`, `settings`, `help`. Test hook tanpa UI: `practiceapp://settings/reset`, `practiceapp://settings?deterministic=true&latency=0&animations=false&flaky=0`.

## ACC-01 Akun
| Id | Elemen | Catatan |
|---|---|---|
| `acc_txt_initials`, `acc_img_avatar` | Avatar | inisial, atau foto bila sudah diubah |
| `acc_txt_name`, `acc_txt_username` | Nama, username | |
| `acc_btn_edit_profile` | `Edit` | |
| `shop_item_orders` | `Pesanan Saya` | tag `N aktif` (`acc_txt_menu_tag`) |
| `shop_item_wishlist` | `Wishlist` | |
| `acc_item_address` / `acc_item_pay` | Daftar Alamat / PracticePay | |
| `acc_item_settings` / `acc_item_help` / `acc_item_dev` | Pengaturan / Pusat Bantuan / Developer Options | Developer Options membuka Pengaturan di bagian DEV |
| `shop_btn_logout` | `Keluar` | membuka dialog `Keluar dari akun?` (Batal / Keluar) |
| `acc_txt_version` | `PracticeMart v0.1.0 (1)` | |

## ACC-02 Edit Profil
| Id | Elemen | Catatan |
|---|---|---|
| `acc_btn_change_photo` | `Ubah foto` | pilihan Kamera / Galeri (Photo Picker) |
| `acc_img_photo`, `acc_txt_photo_initials` | Foto | content-desc `Foto profil` |
| `acc_input_name` / `acc_error_name` | Nama | `Nama wajib diisi` |
| `acc_input_email` / `acc_error_email` | Email | `Format email tidak valid` |
| `acc_btn_birthdate` | Tanggal lahir | Material DatePicker, tampil `YYYY-MM-DD`; awal 2026-01-15 di mode deterministik |
| `acc_group_gender`, `acc_rdo_male`, `acc_rdo_female` | Jenis kelamin | |
| `acc_btn_save` | `Simpan` | disabled sampai valid; snackbar `Profil tersimpan` |

## ACC-03 Daftar & Form Alamat (prefix `addr_`)
| Id | Elemen | Catatan |
|---|---|---|
| `addr_list`, `addr_item_row` | Daftar | id baris sama |
| `addr_txt_label`, `addr_badge_primary`, `addr_txt_recipient`, `addr_txt_full` | Isi baris | |
| `addr_btn_edit` / `addr_btn_delete` | Ubah / Hapus | hapus memakai dialog |
| `addr_btn_add` | `Tambah Alamat` | |
| `addr_btn_use_location`, `addr_txt_location` | `Gunakan lokasi saat ini` | izin lokasi; mengisi provinsi/kota terdekat dari lokasi (mock location → DKI Jakarta, Jakarta Pusat) |
| `addr_group_label`, `addr_chip_home` / `_office` / `_other` | Label | Rumah / Kantor / Lainnya |
| `addr_input_name`, `addr_input_phone` | Penerima, No. HP | |
| `addr_spn_province` → `addr_spn_city` | Dropdown bertingkat | kota mengikuti provinsi |
| `addr_input_street` | Alamat lengkap | |
| `addr_input_postal` / `addr_error_postal` | Kode pos | `Kode pos harus 5 digit` |
| `addr_sw_primary` | `Jadikan alamat utama` | |
| `addr_btn_save` | `Simpan Alamat` | di bawah layar dan **tertutup keyboard** saat mengetik (FR-ACC-04); disabled sampai valid |

## ACC-04 Pengaturan & Developer Options
| Id | Elemen | Catatan |
|---|---|---|
| `acc_seg_language` | Bahasa | Indonesia / English |
| `acc_seg_theme` | Tema | Sistem / Terang / Gelap |
| `acc_sw_notifications` | Notifikasi pesanan | |
| `dev_sw_deterministic` | Mode deterministik | |
| `dev_seg_latency` | Network latency | 0× / 1× / 2× / 3× |
| `dev_sw_animations` | Animasi | disimpan, belum memengaruhi animasi |
| `dev_seg_flaky` | Peluang error server | 0% / 30% / 50% |
| `dev_sw_hints` | Locator Hint Overlay | menampilkan id View di atas elemen; Compose tidak ikut |
| `dev_sw_persist` | Simpan data setelah app ditutup | disimpan, belum diterapkan |
| `dev_seg_va` | Batas waktu VA | 30 dtk / 2 mnt / 15 mnt |
| `dev_seg_interval` | Update status pesanan | 3 / 10 / 30 dtk / Manual |
| `dev_btn_reset_data` / `dev_btn_reset_prefs` | Reset data… / Reset pengaturan… | dialog konfirmasi; reset data kembali ke Onboarding |
| `dev_txt_version`, `dev_txt_commit`, `dev_txt_build_date`, `dev_txt_contract` | About | |

## HELP-01 Pusat Bantuan
Native: `shop_btn_back`, `help_web`, `help_btn_chat` (`Chat dengan CS`).
WebView (konteks `WEBVIEW_com.automation.practice`): `#faq-search` / `data-testid="faq-search"`, `data-testid="faq-item"`, `data-testid="faq-empty"`, `#btn-contact` / `data-testid="btn-contact"` (JS bridge → toast native `Menghubungkan ke CS PracticeMart…`).
