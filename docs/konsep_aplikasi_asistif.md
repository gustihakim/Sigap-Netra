# 📱 Konsep & Arsitektur Ekosistem Aplikasi Asistif Sigap Netra
> **Dokumen Panduan Riset & Serah Terima Proyek (Handover Guide)**  
> **Penulis**: Gusti Hakim Thoriq Wicaksono (Teknik Mekatronika, Politeknik Negeri Batam)

---

## 🎯 1. Mengapa Kacamata Cerdas Memerlukan Aplikasi Pendamping?

Bagi masyarakat umum, kacamata pintar sering kali hanya dianggap sebagai kamera dan speaker. Namun, dalam konteks **alat asistif tunanetra**, ada paradoks mendasar:

> **"Pengguna tunanetra tidak memiliki kemampuan untuk memverifikasi apa pun yang terjadi pada perangkat secara visual."**
> - Berapa sisa baterai kacamata saat ini?
> - Apakah lensa kamera sedang kotor atau terhalang?
> - Apakah uang kembalian yang dibacakan kacamata di kasir tadi benar Rp50.000 atau salah?
> - Bagaimana keluarga/pendamping bisa membantu memeriksa riwayat transaksi tanpa harus merebut kacamata dari kepala pengguna?

Aplikasi Asistif Sigap Netra (`companion_app/`) dirancang bukan sebagai *gimmick*, melainkan sebagai **infrastruktur transparansi, pendampingan, dan validasi ilmiah (Human-in-the-Loop Validation)**.

---

## 🏗️ 2. Arsitektur Tiga Lapis (Three-Tier Architecture)

```
┌────────────────────────────────────────────────────────────────────────┐
│                   LAPIS 1: EDGE WEARABLE (HARDWARE)                    │
│  Sipeed MaixCam (SoC SG2002 RISC-V C906, NPU 1 TOPS, 64MB DDR)         │
│  • Inferensi 100% Luring: YOLOv11n Delta (Uang) + PP-OCR (Teks)        │
│  • Sensor: TF-Luna LiDAR (UART1), ADS1115 (I2C5), Tombol (GPIO A23)    │
│  • Output: ALSA Audio PCM5102 / USB DAC                                │
│  • Server Internal: sigap_api_server.py (Port 8080)                    │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Wi-Fi AP / LAN (Protokol Bersegel SHA-1)
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                LAPIS 2: JEMBATAN DATA & SINKRONISASI                   │
│  JembatanSigap.kt & SigapNetworkManager.kt (OkHttp3 & Coroutines)      │
│  • Handshake: GET /health (Memeriksa ID, FW, Voltase Baterai)          │
│  • Segel Sesi: POST /session/seal (Verifikasi Hash Integritas SHA-1)   │
│  • Pengambilan Berkas: GET /session/{sid}/csv & GET /foto/{nama}       │
│  • Database Lokal: Room Database (SQLite) / Local Preferences Cache    │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ WebView JavascriptInterface Bridge
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│              LAPIS 3: APLIKASI ASISTIF & VALIDASI ILMIAH               │
│  Frontend: sigap.html (PWA Responsive, Standar Desain SIPETA)         │
│  Backend Mobile: Android Kotlin Native (MainActivity.kt)               │
│  • Dashboard Telemetri Real-Time (Baterai, Jarak LiDAR, Mode)          │
│  • Human-in-the-Loop Validation (Cocok / Tidak Cocok Ground Truth)     │
│  • Mesin Uji Statistik Non-Parametrik (Uji Tanda / Sign Test p-value)  │
│  • Pemicu Pindai Jarak Jauh (Remote Trigger Capture)                  │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 💎 3. Fitur Utama yang Menjadikan Aplikasi Ini Istimewa

### A. Sinkronisasi Bersegel Kriptografis (*Cryptographically Sealed Sync*)
- Data pengujian yang diambil kacamata di lapangan (foto frame kamera + log CSV) tidak boleh tercecer atau rusak.
- Sistem menggunakan mekanisme **Sealing**:
  1. Kacamata membundel kumpulan rekaman menjadi sesi tertutup.
  2. Kacamata menghitung *checksum* SHA-1 dari berkas CSV.
  3. Aplikasi Android mengunduh CSV, menghitung ulang SHA-1 secara lokal, dan hanya menandai sesi "BERHASIL" jika hash cocok 100%.

### B. Validasi Ground Truth (*Human-in-the-Loop Validation*)
- Di aplikasi terdapat menu **Antrean Validasi**:
  - Foto yang dibidik kacamata ditampilkan di layar HP pendamping.
  - Pendamping/peneliti melihat teks/nominal hasil prediksi AI di samping foto asli.
  - Peneliti menekan tombol **"Cocok"** atau **"Tidak Cocok"**.
  - Aplikasi secara otomatis menghitung metrik akurasi empiris riil berdasarkan validasi mata manusia.

### C. Mesin Uji Statistik Otomatis (*Sign Test / Uji Tanda*)
- Terintegrasi langsung di dalam `com.example.sigapnetra.util.UjiTanda`:
  - Menghitung formula kombinatorial $P$-value secara mandiri di Android.
  - Membandingkan model lama vs model baru secara ilmiah ($n \ge 6$).
  - Membuktikan signifikansi peningkatan akurasi atau latensi secara kuantitatif untuk kebutuhan laporan PKM, jurnal, atau skripsi.

### D. Pemicu Jarak Jauh (*Remote Trigger Capture*)
- Pendamping dapat menginstruksikan kacamata untuk mengambil foto dan memproses bacaan dari jarak jauh melalui tombol di HP (via `POST /api/capture`).

---

## 🗺️ 4. Panduan Riset Lanjutan untuk Adik Tingkat (Handover Roadmap)

Bagi rekan-rekan mahasiswa atau adik tingkat yang melanjutkan riset ini, berikut adalah modul yang sudah matang dan peluang inovasi berikutnya:

| Modul | Status Saat Ini | Peluang Pengembangan Lanjutan (Ide Tugas Akhir / PKM) |
| :--- | :--- | :--- |
| **Model Deteksi Uang** | YOLOv11n Delta INT8 (Akurasi 98.57%) | Eksplorasi deteksi uang koin atau deteksi uang Rupiah TE 2022 emisi terbaru yang lusuh ekstrem. |
| **Model Pembaca Teks** | PP-OCRv4 + Dynamic Dual-Tiling | Tambahkan deteksi tulisan tangan (*handwritten OCR*) untuk nota belanja tradisional. |
| **Konektivitas** | Wi-Fi Socket & REST API HTTP | Integrasikan **Bluetooth Low Energy (BLE)** untuk koneksi pairing instan tanpa Wi-Fi router. |
| **Telemetri Cloud** | Penyimpanan Lokal di HP (Room DB) | Sinkronisasi opsional ke Firebase / Supabase agar keluarga di luar kota bisa memantau lansia/tunanetra. |
| **Audio Asistif** | Potongan WAV ALSA luring | Tambahkan fitur pengaturan kecepatan bicara (*speech rate control*) dari aplikasi HP. |

---

## 👨‍💻 Hak Cipta & Informasi Peneliti
- **Peneliti Utama**: Gusti Hakim Thoriq Wicaksono
- **Program Studi**: D4 Teknik Mekatronika, Jurusan Teknik Elektro
- **Kampus**: Politeknik Negeri Batam
