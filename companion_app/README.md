# Sigap Netra — Assistive Companion Ecosystem

This directory houses the companion client applications designed to interface seamlessly with the Sigap Netra smart glasses over local Wi-Fi.

## 📱 Subsystems

### 1. `web/` (Progressive Web Application)
- Responsive dashboard (`sigap.html` / `index.html`) inspired by the SIPETA mobile design language.
- Features:
  - Real-time telemetry monitoring (battery voltage, percentage, LiDAR distance in cm).
  - Mode switching (`AUTO`, `UANG`, `TEKS`).
  - Remote capture trigger button.
  - Live transaction and reading history log.
  - Light and Dark mode UI.

### 2. `android/` (Android Studio Kotlin Project)
- Native Android wrapper built in Kotlin with WebView bridge architecture (`JembatanSigap.kt`).
- Hardware integration:
  - Native haptic feedback / vibration.
  - Android Speech Synthesis (TTS) fallback.
  - Wi-Fi socket communication.

## 🛠️ Opening the Android Project
Open the `android/` folder directly in **Android Studio** and build to any Android device running Android 8.0 (API 26) or higher.

## 📖 Handover & Architecture Guide
For the full concept, research methodology, and handover roadmap for junior researchers, see:
👉 [Konsep & Arsitektur Ekosistem Aplikasi Asistif](../../docs/konsep_aplikasi_asistif.md)
