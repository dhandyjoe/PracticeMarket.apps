# PracticeMart

Aplikasi marketplace Android untuk **latihan automation testing mobile**, dengan fitur dompet digital **PracticePay** di dalamnya.

- Package: `com.automation.practice`
- Full offline: tanpa backend dan tanpa permission `INTERNET`
- Data sesi (keranjang, stok, pesanan, saldo) kembali ke default setiap app di-kill, jadi setiap cold start bersih untuk test

> Status: **Masuk & Onboarding**, **Belanja & Checkout**, **Pesanan**, **PracticePay**, serta **Akun & Pengaturan** sudah dibangun. Belum: Kategori (SHOP-02), Ulasan (SHOP-07, ORD-04), Top Up (PAY-02), dan halaman tidak ditemukan (SYS-01).

## Unduh APK

- [**Unduh v0.1.0**](https://github.com/dhandyjoe/PracticeMarket.apps/releases/download/v0.1.0/automation-practice-v0.1.0.apk) (APK debug, Android 7.0+)
- [Semua rilis](https://github.com/dhandyjoe/PracticeMarket.apps/releases)

Pasang: buka berkas APK di HP lalu izinkan install dari sumber tidak dikenal, atau `adb install -r automation-practice-v0.1.0.apk`.
Akun uji: `standard_user` / `Practice123!`, kode OTP `123456`.

## Dokumentasi

| Dokumen | Isi |
|---|---|
| [PRD.md](PRD.md) | Product requirements: fitur, skenario E2E, test data, kontrak locator |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Arsitektur & tech stack |
| [DESIGN.md](DESIGN.md) | Design system & spesifikasi layar |
| [docs/locators/auth.md](docs/locators/auth.md) | Locator layar Auth |
| [docs/locators/shop.md](docs/locators/shop.md) | Locator layar Belanja |
| [docs/locators/checkout.md](docs/locators/checkout.md) | Locator Keranjang, Checkout & Pembayaran |
| [docs/locators/orders.md](docs/locators/orders.md) | Locator Pesanan |
| [docs/locators/pay.md](docs/locators/pay.md) | Locator PracticePay (Compose) |
| [docs/locators/account.md](docs/locators/account.md) | Locator Akun, Alamat, Pengaturan & Bantuan |

Urutan sumber kebenaran bila ada konflik: **PRD → DESIGN → canvas desain**.

## Build

Prasyarat: JDK 17 dan Android SDK (platform 35, build-tools 35.0.0). Gradle wrapper (8.9) sudah ada di repo.

```
export JAVA_HOME=<path JDK 17>
export ANDROID_HOME=<path Android SDK>
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew :app:testDebugUnitTest      # unit test AuthRules
./gradlew :app:assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
```

Deep link: `practiceapp://login?username=<x>`, `practiceapp://register`, `practiceapp://forgot-password`, `practiceapp://shop/orders`, `practiceapp://shop/order/{id}`, `practiceapp://pay[/transfer|/history|/qr?merchant=KOPIPAGI&amount=28000]`, `practiceapp://account[/address]`, `practiceapp://settings`, `practiceapp://help`.

Test hook tanpa UI: `practiceapp://settings/reset`, `practiceapp://settings?deterministic=true&latency=0&animations=false&flaky=0`.
