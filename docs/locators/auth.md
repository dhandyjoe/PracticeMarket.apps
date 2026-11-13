# Locator: Masuk & Onboarding (AUTH-01…05)

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
| `shop_input_username` | Field username | boleh diisi username atau email |
| `shop_input_password` | Field password | |
| `shop_btn_toggle_password` | Mata | content-desc `Tampilkan password` / `Sembunyikan password` |
| `shop_txt_username_error` / `shop_txt_password_error` | Error inline kosong | `Username wajib diisi` / `Password wajib diisi` |
| `shop_banner_login_error` | Kontainer banner | |
| `shop_txt_login_error` | Teks banner | `Username atau password salah`, `Akun Anda terkunci. Hubungi admin.`, `Autentikasi gagal` |
| `shop_chk_remember` | Ingat saya | default centang |
| `shop_btn_forgot_password` | "Lupa password?" | |
| `shop_btn_login` | "Masuk" | setelah 3× gagal: disabled, label `Coba lagi dalam N detik` |
| `shop_progress_login` | Spinner login | tampil saat loading (`slow_user` 4 dtk) |
| `shop_btn_biometric` | "Masuk dengan sidik jari" | disabled sebelum login pertama atau tanpa sidik jari terdaftar |
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

## AUTH-04 Lupa Password · OTP
| Id | Elemen | Catatan |
|---|---|---|
| `shop_input_forgot_email` | Field email (fase 1) | |
| `shop_txt_forgot_email_error` | Error email | `Format email tidak valid`, `Email tidak terdaftar` |
| `shop_btn_send_code` | "Kirim kode" | |
| `shop_txt_otp_subtitle` | Email tersamar | mis. `Kode 6 digit sudah dikirim ke s***@contoh.id.` |
| `shop_input_otp_1` … `shop_input_otp_6` | Kotak OTP | content-desc `Digit N`; kode valid `123456` |
| `shop_txt_otp_error` | Error | `Kode OTP salah` |
| `shop_btn_verify_otp` | "Verifikasi" | disabled sampai 6 digit terisi |
| `shop_btn_resend_otp` | Kirim ulang | label `Kirim ulang dalam 00:SS`, disabled 30 dtk |

## AUTH-05 Password Baru
| Id | Elemen | Catatan |
|---|---|---|
| `shop_input_new_password` / `shop_input_new_password_confirm` | Field | |
| `shop_txt_rule_min` / `_letter` / `_digit` / `_match` | Checklist syarat | warna berubah saat terpenuhi |
| `shop_btn_save_password` | "Simpan Password" | disabled sampai semua syarat terpenuhi |
| `shop_txt_password_changed` | Judul sukses | `Password berhasil diubah` |
| `shop_btn_goto_login` | "Ke halaman Masuk" | |

## Setelah login
`shop_txt_greeting` kini ada di Beranda dan `shop_btn_logout` di tab Akun. Lihat [shop.md](shop.md).

## WebView S&K (`help.practicemart.test/terms.html`)
`#terms` (`data-testid="terms"`), `#akun`, `#belanja`, `#practicepay`, `#privasi` (masing-masing punya `data-testid` `terms-*`).
