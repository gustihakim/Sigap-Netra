# Sigap Netra - Companion App

Optional app for caregivers and researchers. It is **not** required for daily
use of the glasses; the device works fully offline on its own.

## What it does
- **Sync** device logs over local Wi-Fi with SHA-1 integrity check.
  Data is deleted from the device only after the checksum matches.
- **Validation queue**: a human marks each detection as correct or wrong.
- **Camera A/B test** with median and two-sided sign test.
- **Export** results to CSV.

## Folders

| Folder | Content |
| :--- | :--- |
| `android/` | Android Studio project (Kotlin, WebView bridge `JembatanSigap.kt`) |
| `web/` | The same UI (`sigap.html`) as a standalone page |

## Build the Android app
1. Open `android/` in Android Studio and let Gradle sync.
2. Build and install on **Android 7.0 (API 24)** or newer.
3. Connect the phone to the same Wi-Fi as the MaixCam.
4. Set the device IP in the app, then tap **Sinkronkan**.

## Further reading
- [Concept & handover guide](../docs/konsep_aplikasi_asistif.md)
- [REST API reference](../README.md#-rest-api-reference)
