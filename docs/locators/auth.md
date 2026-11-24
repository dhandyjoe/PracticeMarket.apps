# Locator: Masuk & Onboarding (AUTH-01…04)

Kontrak publik (PRD §12.3). Mengubah atau menghapus id di sini = breaking change.
Id memakai prefix `com.automation.practice:id/`.

## AUTH-01 Onboarding
| Id | Elemen | Catatan |
|---|---|---|
| `shop_btn_skip` | Tombol "Lewati" | |
| `shop_pager_onboarding` | ViewPager2 slide | swipe horizontal |
| `shop_txt_onboarding_title` / `shop_txt_onboarding_body` | Judul / isi slide | id sama di tiap slide |
| `shop_btn_next` | "Lanjut" | hilang di slide terakhir |
| `shop_btn_start` | "Mulai" | hanya di slide terakhir |

## AUTH-02 Login
| Id | Elemen | Catatan |
|---|---|---|
| `shop_input_username` | Field email | id dipertahankan; login hanya dengan email (PRD FR-AUTH-03) |
| `shop_input_password` | Field password | |
| `shop_btn_toggle_password` | Mata | content-desc `Tampilkan password` / `Sembunyikan password` |
| `shop_txt_username_error` / `shop_txt_password_error` | Error inline kosong | `Email wajib diisi` / `Password wajib diisi` |
| `shop_banner_login_error` | Kontainer banner | |
| `shop_txt_login_error` | Teks banner | `Email atau password salah`, `Tidak ada koneksi. Periksa internet lalu coba lagi.`, `Terlalu banyak percobaan. Coba lagi nanti.` |
| `shop_chk_remember` | Ingat saya | default centang |
| `shop_btn_login` | "Masuk" | setelah 3× gagal: disabled, label `Coba lagi dalam N detik` |
| `shop_progress_login` | Spinner login | tampil saat loading (diatur Network latency) |
| `shop_btn_register_link` | "Daftar" | |

## AUTH-03 Register
| Id | Elemen | Catatan |
|---|---|---|
| `shop_btn_back` | Kembali | content-desc `Kembali` |
| `shop_input_reg_name` / `shop_input_reg_email` / `shop_input_reg_password` / `shop_input_reg_confirm` | Field | |
| `shop_txt_reg_email_error` | Error email | `Format email tidak valid`, `Email sudah terdaftar` |
| `shop_txt_password_strength` | Kekuatan password | `Lemah` / `Sedang` / `Kuat` |
| `shop_txt_reg_confirm_error` | Error konfirmasi | `Konfirmasi password tidak sama` |
| `shop_chk_terms` | Checkbox S&K | |
| `shop_txt_terms` | Teks + link "Syarat & Ketentuan" | link adalah span, bukan view terpisah |
| `shop_btn_register` | "Daftar" | disabled sampai semua valid |
| `shop_btn_login_link` | "Masuk" | |

## Dihapus (breaking)
Dihapus v0.9: `shop_btn_forgot_password`, layar Lupa Password (`shop_input_forgot_email`, `shop_txt_forgot_email_error`, `shop_btn_send_code`, `shop_txt_forgot_result`, `shop_btn_goto_login`), dan deep link `practiceapp://forgot-password`.

Dihapus v0.3.0: `shop_btn_biometric` (login sidik jari), layar OTP (`shop_txt_otp_subtitle`, `shop_input_otp_1` … `shop_input_otp_6`, `shop_txt_otp_error`, `shop_btn_verify_otp`, `shop_btn_resend_otp`), dan layar Password Baru (`shop_input_new_password`, `shop_input_new_password_confirm`, `shop_txt_rule_min` / `_letter` / `_digit` / `_match`, `shop_btn_save_password`, `shop_txt_password_changed`). Teks `Email tidak terdaftar` tidak lagi tampil (contract §6).

## Setelah login
`shop_txt_greeting` kini ada di Beranda dan `shop_btn_logout` di tab Akun. Lihat [shop.md](shop.md).

## WebView S&K (`help.practicemart.test/terms.html`)
`#terms` (`data-testid="terms"`), `#akun`, `#belanja`, `#practicepay`, `#privasi` (masing-masing punya `data-testid` `terms-*`).
