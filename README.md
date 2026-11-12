# PracticeMart

Aplikasi marketplace Android untuk **latihan automation testing mobile**, dengan fitur dompet digital **PracticePay** di dalamnya.

- Package: `com.automation.practice`
- Full offline: tanpa backend dan tanpa permission `INTERNET`
- Data sesi (keranjang, stok, pesanan, saldo) kembali ke default setiap app di-kill, jadi setiap cold start bersih untuk test

> Status: kategori **Masuk & Onboarding** (AUTH-01…05) sudah jadi dan berjalan di perangkat. Kategori lain belum dibangun.

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

Deep link: `practiceapp://login?username=<x>`, `practiceapp://register`, `practiceapp://forgot-password`.
