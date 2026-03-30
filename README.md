# 🔋 Charging Meter

Aplikasi Android untuk memantau kondisi baterai secara **akurat dan real-time** dengan tema **Material You (Material Design 3)**.

## ✨ Fitur

| Fitur | Keterangan |
|-------|-----------|
| 🔋 Level Baterai | Gauge animasi dengan warna dinamis |
| ⚡ Arus Real-time | Data langsung dari `BATTERY_PROPERTY_CURRENT_NOW` (µA) |
| 🔌 Voltase | Dari `ACTION_BATTERY_CHANGED` intent (mV) |
| ⚡ Daya | Dihitung: P = V × I (mW / W) |
| 🌡️ Suhu | Celsius & Fahrenheit |
| 📊 Grafik Arus | History 60 detik terakhir |
| ⏱️ Estimasi Waktu | Hitung sendiri dari kapasitas + arus (akurat) |
| 🏥 Health | Status kesehatan baterai |
| 🎨 Material You | Dynamic color Android 12+ |

## 🏗️ Build

### Via GitHub Actions
Push ke branch `main` atau `master` → otomatis build Debug APK.

Untuk Release: jalankan workflow manual → pilih `release`.

### Lokal
```bash
./gradlew assembleDebug
```
APK ada di: `app/build/outputs/apk/debug/`

## 📋 Requirement
- Android 8.0+ (API 26)
- Target SDK 35

## 🎨 Tech Stack
- Kotlin + Jetpack Compose
- Material Design 3 / Material You
- Dynamic Color (Android 12+)
- `BatteryManager` API untuk data akurat
