<div align="center">

# 👓 Sigap Netra

### Kacamata Asistif Berbasis Edge-AI Luring untuk Penyandang Tunanetra

*Mengenali pecahan uang kertas Rupiah dan membaca teks cetak secara mandiri, sepenuhnya di perangkat.*

[![Platform](https://img.shields.io/badge/Platform-Sipeed%20MaixCam%20(SG2002)-0F9B8E?style=for-the-badge&logo=linux)](https://wiki.sipeed.com/maixpy/)
[![YOLOv11n](https://img.shields.io/badge/YOLOv11n%20Delta-98.57%25%20%40%20conf%200.80-success?style=for-the-badge)](#-hasil-pengujian-empiris)
[![Kuantisasi](https://img.shields.io/badge/TPU--MLIR-INT8%202.88%20MB-orange?style=for-the-badge)](device/models/)
[![OCR](https://img.shields.io/badge/OCR-PP--OCR%20%2B%20Kamus-blue?style=for-the-badge)](device/sigap_core.py)
[![Pengujian](https://img.shields.io/badge/Unit%20Tests-284%20lulus-brightgreen?style=for-the-badge)](device/sigap_core.py)
[![Lisensi](https://img.shields.io/badge/Lisensi-MIT-green?style=for-the-badge)](LICENSE)

**Gusti Hakim Thoriq Wicaksono** · [@gustihakim](https://github.com/gustihakim)  
Teknik Mekatronika · Politeknik Negeri Batam  
PKM-KC (Program Kreativitas Mahasiswa - Karsa Cipta) & Tugas Akhir

<br/>

**Bahasa:** [English](README.md) | **Bahasa Indonesia**

</div>

---

## 📑 Daftar Isi

1. [Ringkasan Proyek](#-ringkasan-proyek)
2. [Arsitektur Sistem](#-arsitektur-sistem)
3. [Fitur Unggulan](#-fitur-unggulan)
4. [Hasil Pengujian Empiris](#-hasil-pengujian-empiris)
5. [Pinout & Pengkabelan Perangkat Keras](#-pinout--pengkabelan-perangkat-keras)
6. [Struktur Repositori](#-struktur-repositori)
7. [Panduan Penggunaan Sipeed MaixCam](#-panduan-penggunaan-sipeed-maixcam)
8. [Aplikasi Pendamping (Companion App)](#-aplikasi-pendamping-companion-app)
9. [Referensi REST API](#-referensi-rest-api)
10. [Penulis](#-penulis)
11. [Lisensi](#-lisensi)

---

## 📌 Ringkasan Proyek

**Sigap Netra** adalah perangkat kacamata asistif yang dirancang untuk membantu penyandang tunanetra menghitung uang dan membaca teks cetak (menu restoran, label kemasan, dokumen) secara mandiri. Seluruh komputasi berjalan **100% luring (offline)** pada papan pengembang **Sipeed MaixCam** (SoC Sophgo SG2002, prosesor RISC-V C906, NPU terintegrasi 1 TOPS). Sistem tidak memerlukan koneksi internet, cloud server, maupun smartphone untuk penggunaan harian.

Keluaran informasi berupa suara berbahasa Indonesia alami yang dihasilkan melalui perangkaian berkas potongan rekaman audio `.wav`, sehingga responsivitasnya instan tanpa ketergantungan mesin TTS berat di perangkat.

> **Prinsip Utama: Diam Lebih Baik daripada Salah Sebut.**  
> Pengguna tunanetra tidak dapat memverifikasi visual apa yang diucapkan oleh pengeras suara. Sistem dirancang agar jika tingkat keyakinan model berada di bawah ambang batas aman, alat memilih untuk diam agar pengguna dapat mengulang pemindaian, alih-alih memberikan informasi nominal yang keliru.

---

## 🧭 Arsitektur Sistem

```text
 ┌───────────────────────────────┐
 │  SENSOR & PERIFERAL           │
 │  • Kamera MaixCam (ISP)       │
 │  • TF-Luna LiDAR   UART1      │
 │  • ADS1115 ADC     I2C5       │
 │  • Tombol Fisik    GPIO A23   │
 │  • Audio DAC  ->  Earphone    │
 └───────────────┬───────────────┘
                 ▼
 ┌───────────────────────────────┐
 │  PERANGKAT TEPI (MaixCam)     │
 │  • YOLOv11n "Delta" INT8      │
 │  • PP-OCR + Pemotongan 2x2    │
 │  • Mesin Koreksi Kamus (NLP)  │
 │  • Perangkai Potongan Audio   │
 │  • HTTP REST Server :8080     │
 └───────────────┬───────────────┘
                 │ Wi-Fi Lokal (Opsional)
                 ▼
 ┌───────────────────────────────┐
 │  APLIKASI PENDAMPING          │
 │  • Android (Kotlin + WebView) │
 │  • Sinkronisasi Log (SHA-1)   │
 │  • Validasi Manusia & SignTest│
 └───────────────────────────────┘
```

---

## 🌟 Fitur Unggulan

### 1. Pengenalan Uang Rupiah (YOLOv11n "Delta")
- Dilatih pada 7 pecahan uang kertas Rupiah emisi terkini (Rp1.000 hingga Rp100.000), dikuantisasi penuh menggunakan **TPU-MLIR** ke format INT8 (**2.88 MB**) dan cadangan BF16 (**5.76 MB**).
- **Konsensus Temporal Burst**: Mengambil 6 frame berurutan (`FRAME_CAPTURE = 6`); nominal hanya diumumkan jika didukung minimal 2 frame (`SETUJU_MIN = 2`).
- **Deduplikasi Spasial Sensitif Nominal (*Class-Aware*)**:
  - `IOU_DEDUP = 0.45` untuk pecahan nominal sama (mencegah uang terlipat terhitung ganda).
  - `IOU_DEDUP_BEDA = 0.70` untuk pecahan nominal berbeda (memungkinkan penjumlahan uang bertumpuk).
- **Penyaring Geometri**: Menolak kotak deteksi dengan rasio aspek di luar batas wajar (`1.40` hingga `3.40`) atau memenuhi lebih dari 72% luas frame.

### 2. Pembacaan Teks & Menu Restoran (PP-OCR + Mesin Kamus)
- **Pemotongan Petak Tumpang Tindih 2x2** (resolusi per petak `704x493` piksel) untuk membaca font kecil pada menu tanpa membebani CPU.
- **Pemilihan Frame Tertajam Otomatis** berbasis varians Laplacian 2D sebelum inferensi OCR.
- **Penajaman Citra Adaptif Jarak**: Parameter penajaman kontras disesuaikan dinamis berdasarkan jarak baca sensor LiDAR.
- **Mesin Kamus Deterministik (`sigap_core.py`)**:
  - Pemisahan kata menempel (*de-concatenation*).
  - Koreksi fonetik kata serapan (misal salah baca `speclal` dikoreksi menjadi `spesial`).
  - Pembacaan menu 2 kolom dan pemasangan nama hidangan ke nominal harga.
  - Penolakan baris sampah: harga **tidak akan pernah diucapkan** jika nama makanannya tidak teridentifikasi.
  - Terverifikasi dengan **284 unit test otomatis** (0 kegagalan).

### 3. Fusi Sensor Perangkat Keras
- **LiDAR TF-Luna**: Berfungsi sebagai gerbang jarak pada mode teks (rentang aman **10 hingga 40 cm**). Jika di luar rentang, alat memberi instruksi *"terlalu dekat"* atau *"terlalu jauh"*.
- **ADC ADS1115**: Memantau kurva pelepasan baterai LiPo 1S dengan logika histeresis: peringatan baterai *lemah* (~3.55 V) dan *kritis* (~3.40 V) tanpa spam audio.
- **Tombol Fisik Tunggal (A23)**: Mesin status penekanan mendukung tekan sekali (proses), tekan dua kali (ganti mode), dan tahan 1 detik (interupsi/batal).

---

## 📊 Hasil Pengujian Empiris

Hasil komparasi resmi pada **210 sampel citra uji baku** (7 pecahan x 30 sampel, 5 variasi kondisi pencahayaan dan pelipatan, tidak pernah dipakai saat pelatihan), diukur pada **ambang batas operasional alat `CONF_MIN = 0.80`**:

| Generasi Model | Benar/Total | Akurasi | Rentang Wilson 95% | Salah Sebut | Diam | Status |
| :--- | :---: | :---: | :---: | :---: | :---: | :--- |
| Beta (v7) | 182/210 | 86.67% | 81.40% - 90.64% | 7 | 21 | Tidak Dipakai |
| Charlie (v8) | 195/210 | 92.86% | 88.55% - 95.62% | 0 | 15 | Cadangan |
| **Delta (v9)** | **207/210** | **98.57%** | **95.88% - 99.51%** | **0** | **3** | **Terpasang Aktif** |
| Epsilon (v10) | 204/210 | 97.14% | 93.91% - 98.68% | 0 | 6 | Tahap Evaluasi |

- **Nol Kesalahan Sebut**: Sejak model v8 dan v9, seluruh sisa galat adalah alat memilih mode diam (*false negative*), yang aman bagi pengguna karena dapat langsung diulang.
- Sumber data: `Laporan/hasil_komparasi_semua_model.txt`.

> [!NOTE]
> Seluruh 210 sampel pengujian berisi uang kertas riil. Pengujian scene tanpa uang (*false positive background*) belum diukur secara kuantitatif. Jangan menurunkan nilai `CONF_MIN` di bawah 0.80.

---

## 🔌 Pinout & Pengkabelan Perangkat Keras

| Komponen | Antarmuka | Pin Fisik MaixCam | Keterangan |
| :--- | :--- | :--- | :--- |
| TF-Luna LiDAR | UART1 RX | **A18** | Sambungkan ke kabel TX LiDAR (`/dev/ttyS1`, 115200 8N1) |
| TF-Luna LiDAR | UART1 TX | **A19** | Sambungkan ke kabel RX LiDAR |
| ADS1115 ADC | I2C5 SCL | **A15** | Alamat I2C `0x48`, pembagi tegangan 100k / 51k |
| ADS1115 ADC | I2C5 SDA | **A27** | Jalur data I2C5 |
| Tombol Tekan | GPIO | **A23** | Pull-up internal aktif, kaki lainnya ke **GND** |
| Audio Output | ALSA | - | DAC I2S/PCM5102 atau USB DAC, terdeteksi via `aplay -l` |

> [!WARNING]
> **Hindari Pin A16 dan A17.** Kedua pin tersebut dialokasikan untuk protokol komunikasi sistem launcher bawaan MaixPy. Memakai pin yang keliru akan gagal **tanpa pesan galat apapun**. Skema detail: [docs/pinout.md](docs/pinout.md).

---

## 📂 Struktur Repositori

```text
Sigap-Netra/
├── device/                    # Berkas yang berjalan di MaixCam
│   ├── app.yaml               # Manifes aplikasi MaixPy (id: aplikasi_utama)
│   ├── icon.png               # Ikon menu launcher
│   ├── main.py                # Aplikasi utama aktif (komentar Bahasa Indonesia)
│   ├── main_en.py             # Rujukan aplikasi beranotasi Bahasa Inggris
│   ├── sigap_core.py          # Logika murni tanpa dependensi maix (284 uji lulus)
│   ├── sigap_api_server.py    # Peladen HTTP REST mandiri (rujukan)
│   ├── kata_utuh.py           # Bank kosakata kamus kuliner & nominal
│   ├── models/                # Bobot model YOLO11 Delta INT8 & BF16
│   └── README.md
├── companion_app/
│   ├── android/               # Proyek Android Studio (Kotlin + WebView bridge)
│   ├── web/                   # Halaman dashboard HTML mandiri
│   └── README.md
├── tools/
│   └── periksa_main.py        # Pemeriksa statis main.py sebelum flashing
├── docs/
│   ├── konsep_aplikasi_asistif.md   # Konsep arsitektur aplikasi pendamping
│   └── pinout.md              # Diagram visual pengkabelan pin
├── LICENSE
├── README.md                  # Dokumentasi bahasa Inggris
└── README.id.md               # Dokumentasi bahasa Indonesia
```

---

## 🛠️ Panduan Penggunaan Sipeed MaixCam

### Langkah 0: Alat & Bahan yang Diperlukan

| Komponen / Berkas | Fungsi |
| :--- | :--- |
| Sipeed MaixCam + MicroSD (min. 8 GB) | Unit komputasi utama |
| Kabel USB Type-C | Catu daya & koneksi serial/SSH pertama kali |
| Komputer dengan software [MaixVision](https://wiki.sipeed.com/maixvision) | Flashing, instalasi, dan monitoring log |
| Python 3.8+ di Komputer | Pengujian unit test sebelum pemuatan berkas |
| Sensor TF-Luna, ADS1115, tombol, DAC Audio, earphone | Periferal pendukung kacamata |
| Baterai LiPo 1S (3.7V - 4.2V) | Sumber daya portabel perangkat |

Aset eksternal berukuran besar yang **tidak disertakan** di repositori git:

| Aset | Direktori Tujuan di Alat | Keterangan |
| :--- | :--- | :--- |
| Bank Audio Suara Pria | `/root/audio_sigap/` | ~3.200 file `.wav`, ~175 MB (**Wajib**) |
| Bank Audio Suara Wanita | `/root/audio_sigap_perempuan/` | Opsional |
| Model PP-OCR Sipeed | `/root/models/pp_ocr.mud` + `.cvimodel` | Model resmi bawaan MaixHub |

*Hubungi pengelola repositori untuk arsip bank audio lengkap.*

### Langkah 1: Persiapan Perangkat MaixCam

1. Flash citra sistem operasi MaixPy terbaru ke MicroSD mengikuti [panduan resmi Sipeed](https://wiki.sipeed.com/maixpy/doc/en/basic/os.html).
2. Pasang kartu memori ke MaixCam, nyalakan via kabel USB-C hingga layar launcher menyala.
3. Melalui layar sentuh, buka **Settings -> WiFi** dan sambungkan ke Wi-Fi yang sama dengan PC (hotspot smartphone sangat disarankan).
4. Catat alamat IP perangkat (tertera di layar atau terdeteksi oleh MaixVision).
5. Uji akses remote SSH dari terminal PC (kredensial default: `root` / `root`):
   ```bash
   ssh root@<IP_MAIXCAM>
   ```

### Langkah 2: Pemasangan Pengkabelan Sensor

Pasang seluruh kabel mengacu pada [Tabel Pinout](#-pinout--pengkabelan-perangkat-keras):
- Kabel **TX LiDAR ke Pin A18** dan **RX LiDAR ke Pin A19** (silang).
- Tombol tekan dipasang di antara **Pin A23** dan pin **GND**.
- Pasang earphone ke DAC Audio sebelum perangkat dinyalakan agar dikenali oleh ALSA.

### Langkah 3: Verifikasi Kode di Komputer Sebelum Flashing

Karena `sigap_core.py` tidak memiliki dependensi perangkat keras `maix`, jalankan pengujian lokal di PC terlebih dahulu:

```bash
cd device
python sigap_core.py                          # harus menghasilkan: 284 lulus, 0 gagal
cd ..
python tools/periksa_main.py device/main.py   # harus menghasilkan: bersih (0 masalah)
```

Jangan memuat kode ke perangkat jika salah satu uji di atas menghasilkan galat.

### Langkah 4: Menyalin Aset Besar ke Perangkat

Dari komputer, salin bank audio dan model PP-OCR sekali saja melalui SCP:

```bash
scp -r audio_sigap root@<IP_MAIXCAM>:/root/
ssh root@<IP_MAIXCAM> "mkdir -p /root/models"
scp pp_ocr.mud *.cvimodel ppocr_keys_v1.txt root@<IP_MAIXCAM>:/root/models/
```

### Langkah 5: Instalasi Aplikasi ke MaixCam

**Metode A — Melalui MaixVision (Paling Direkomendasikan)**
1. Buka MaixVision di komputer dan sambungkan ke perangkat.
2. Buka folder `device/` sebagai folder proyek.
3. Klik tombol **Install App**. MaixVision akan membaca `app.yaml` dan mendaftarkan aplikasi di menu launcher dengan nama **Sigap Netra**.

**Metode B — Pembaruan Manual via SSH**

Karena ID aplikasi adalah `aplikasi_utama`, direktori target adalah `/maixapp/apps/aplikasi_utama/`:

```bash
scp -r device/* root@<IP_MAIXCAM>:/maixapp/apps/aplikasi_utama/
ssh root@<IP_MAIXCAM> "rm -rf /maixapp/apps/aplikasi_utama/__pycache__ /root/__pycache__"
```

> [!IMPORTANT]
> **Waspadai Duplikasi Berkas `sigap_core.py`.** Python di MaixCam akan memprioritaskan berkas di `/root/` sebelum folder aplikasi. Jika terdapat file `sigap_core.py` versi lama di `/root/`, hapus atau perbarui berkas tersebut dan selalu hapus folder `__pycache__` setelah penyalinan.

### Langkah 6: Menjalankan Aplikasi

| Cara Eksekusi | Kapan Digunakan | Catatan |
| :--- | :--- | :--- |
| Ikon **Sigap Netra** di Launcher | Penggunaan harian mandiri | Menjalankan salinan di `/maixapp/apps/` |
| Tombol **Run** di MaixVision | Pengembangan & monitoring log | Menyalin skrip sementara ke `/tmp/maixpy_run/` |
| Fitur **Auto Start** di Pengaturan | Mode produksi siap pakai | Aplikasi langsung berjalan saat boot |

Saat aplikasi aktif, sistem akan melakukan pemanasan model, memindai kartu suara, lalu mengucapkan **"kamera siap"**. Mode awal adalah **AUTO**.

Untuk menghentikan: tekan tombol Stop di MaixVision atau kirim sinyal terminasi via SSH (`kill <PID>`). Tombol fisik internal `KEY_OK` sengaja dinonaktifkan di dalam kode agar tidak tertekan secara tak sengaja oleh casing bingkai kacamata.

### Langkah 7: Pengoperasian Kacamata

#### Mode Operasi
Tekan tombol fisik dua kali dengan cepat untuk berganti mode secara berurutan (**AUTO -> UANG -> TEKS**):

| Mode | Cara Kerja |
| :--- | :--- |
| **AUTO** | Memeriksa uang terlebih dahulu. Jika tidak ada uang, beralih membaca teks menu. |
| **UANG** | Hanya mendeteksi uang kertas. Mengumumkan nominal tiap lembar dan totalnya. |
| **TEKS** | Khusus membaca dokumen/menu dengan pengawasan batas jarak LiDAR (10–40 cm). |

#### Pola Tombol Fisik (A23)
Ambang waktu: ketukan biasa adalah penekanan singkat; **ketuk dua kali (ganda)** berjarak maksimal **0.18 detik**; **tahan lama** ditekan minimal **1 detik**.

| Status Alat | Tekan 1x (Tunggal) | Tekan 2x (Ganda) | Tahan (1 detik) |
| :--- | :--- | :--- | :--- |
| **Siaga (IDLE)** | Bidik & Proses (Ambil 6 frame) | Ganti Mode Operasi | Hentikan audio, kembali siaga |
| **Membaca Baris Teks** | Lompat ke 3 baris berikutnya | Ulangi pembacaan 3 baris ini | Hentikan bacaan, kembali ke kamera |
| **Navigasi Kategori Menu** | Masuk ke daftar hidangan kategori ini | Pindah ke kategori menu berikutnya | Keluar ke tampilan kamera |

#### Panduan Indikator Suara

| Ucapan Audio | Arti | Kondisi Pemicu |
| :--- | :--- | :--- |
| *"kamera siap"* | Kamera siap membidik | Selesai inisialisasi atau kembali ke kondisi siaga |
| *"mohon tunggu"* | Sedang memproses | Pipeline inferensi OCR sedang berjalan di NPU |
| *"terlalu dekat"* | Dokumen terlalu dekat | Jarak ukur LiDAR < 10 cm |
| *"terlalu jauh"* | Dokumen terlalu jauh | Jarak ukur LiDAR > 40 cm |
| *"deteksi gagal"* | Objek tidak terbaca | Tidak ada uang atau teks valid yang terdeteksi |
| *(Diam)* | Tidak ada deteksi valid | Tidak ada objek uang yang melampaui ambang `CONF_MIN = 0.80` |

### Langkah 8: Parameter Konfigurasi (`device/main.py`)

| Variabel | Nilai Default | Penjelasan |
| :--- | :--- | :--- |
| `CONF_MIN` | `0.80` | Ambang keyakinan minimum deteksi uang |
| `FRAME_CAPTURE` | `6` | Jumlah frame yang diambil dalam 1 sesi burst |
| `SETUJU_MIN` | `2` | Jumlah frame minimum yang harus sepakat |
| `MODE_URUT` | `AUTO, UANG, TEKS` | Urutan siklus pergantian mode |
| `BATAS_JARAK_TEKS_CM` | `40.0` | Jarak maksimum pemindaian teks |
| `JARAK_MIN_CM` | `10.0` | Jarak minimum pemindaian teks |
| `VOICE_PROFILE` | `"MALE"` | Profil suara (`"FEMALE"` memakai audio perempuan) |
| `VOLUME_DEFAULT` | `75` | Persentase volume keluaran pengeras suara |
| `ENABLE_LIDAR` | `True` | Nonaktifkan jika perangkat tidak dipasangi sensor LiDAR |
| `ENABLE_TOUCHSCREEN` | `False` | Layar sentuh (dinonaktifkan agar tidak tertekan tak sengaja) |
| `PIN_TOMBOL` | `"A23"` | Pin GPIO tombol utama |

### Langkah 9: Log & Penyimpanan Berkas

| Berkas / Direktori | Isi Data |
| :--- | :--- |
| Terminal MaixVision | Log operasional langsung (`TOMBOL`, `LIDAR`, `YOLO`, `AUDIO`, `HASIL`) |
| `/maixapp/apps/aplikasi_utama/galat.log` | Jejak stacktrace jika terjadi crash |
| `/root/uji_rekam.csv` | Log aktif sesi transaksi pemindaian |
| `/root/uji_rekam/` | Berkas foto hasil tangkapan kamera |
| `/root/segel_<timestamp>.csv` | Arsip sesi yang siap disinkronkan ke aplikasi Android |

### Langkah 10: Solusi Masalah Umum (Troubleshooting)

| Gejala | Hal yang Perlu Diperiksa |
| :--- | :--- |
| Tidak ada suara | Periksa `aplay -l`. Pastikan folder `/root/audio_sigap/` berisi berkas `.wav`. |
| Tombol tidak merespons | Log terminal harus menampilkan `[AKTIF] Pin A23`. Pastikan kabel tombol terhubung ke pin GND. |
| Tidak ada peringatan jarak | Jika log menampilkan `LiDAR tidak aktif`: periksa kabel TX/RX tertukar atau kabel daya 5V lepas. |
| Model gagal dimuat | Pastikan berkas `.cvimodel` dan `.mud` ada di folder `models/` atau `/root/models/`. |
| OCR tidak berjalan | Pastikan model PP-OCR terpasang pada salah satu direktori pencarian (Langkah 4). |
| Terjadi `AttributeError` di `sigap_core` | Terdapat file salinan lama di `/root/sigap_core.py`. Perbarui dan bersihkan folder `__pycache__`. |

---

## 📱 Aplikasi Pendamping (Companion App)

Aplikasi Android untuk pendamping, keluarga, atau peneliti. Berfungsi mengunduh log transaksi, melakukan validasi mata manusia, dan menghitung signifikansi statistik uji kamera A/B.

1. Buka folder `companion_app/android/` di Android Studio.
2. Build dan pasang pada ponsel dengan **Android 7.0 (API 24)** atau lebih tinggi.
3. Sambungkan ponsel ke jaringan Wi-Fi yang sama dengan kacamata.
4. Masukkan alamat IP alat pada aplikasi, lalu tekan **"Sinkronkan"** (Sync).

Detail konsep dan struktur data: [companion_app/README.md](companion_app/README.md) serta dokumen [docs/konsep_aplikasi_asistif.md](docs/konsep_aplikasi_asistif.md).

---

## 📡 Referensi REST API

Peladen HTTP internal aktif di port **8080** saat aplikasi kacamata berjalan:

| Metode | Endpoint | Deskripsi |
| :--- | :--- | :--- |
| `GET` | `/health`, `/api/status`, `/api/ping` | Status alat, firmware, persentase baterai, sisa antrean |
| `GET` | `/sessions` | Daftar sesi tersegel beserta jumlah baris dan nilai SHA-1 |
| `GET` | `/session/<id>/csv` | Mengunduh file data CSV sesi tersegel |
| `GET` | `/session/<id>/img/<file>` | Mengunduh file foto hasil tangkapan kamera |
| `GET` | `/foto/<file>` | Jalur alternatif pengunduhan foto |
| `GET` | `/api/transaksi` | Riwayat transaksi aktif dalam format JSON |
| `GET` | `/api/download_csv` | Mengunduh berkas log CSV aktif |
| `POST` | `/session/seal` | Menyegel log aktif menjadi berkas sesi baru |
| `POST` | `/api/capture`, `/api/proses` | Memicu pemindaian foto dari jarak jauh |
| `POST` | `/api/hapus_semua` | Menghapus seluruh riwayat log dan foto di alat |
| `DELETE` | `/session/<id>` | Menghapus sesi tersegel setelah diverifikasi aplikasi |
| `DELETE` | `/api/transaksi` | Mengosongkan data log transaksi aktif |

---

## 👤 Penulis

**Gusti Hakim Thoriq Wicaksono**

| | |
| :--- | :--- |
| **NIM** | 4212301085 |
| **Program Studi** | Teknik Mekatronika, Semester 7 |
| **Perguruan Tinggi** | Politeknik Negeri Batam, Indonesia |
| **GitHub** | [@gustihakim](https://github.com/gustihakim) |

Ucapan terima kasih kepada dosen pembimbing PKM-KC, Laboratorium Robotika Politeknik Negeri Batam, serta komunitas open-source pengembang MaixPy, Ultralytics YOLO, dan PaddleOCR.

---

## 📄 Lisensi

Proyek ini dilisensikan di bawah [Lisensi MIT](LICENSE).
