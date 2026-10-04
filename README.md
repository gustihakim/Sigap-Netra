<div align="center">

# 👓 Sigap Netra
### **Edge AI Assistive Smart Glasses & Companion Ecosystem for the Visually Impaired**

[![Platform](https://img.shields.io/badge/Platform-Sipeed%20MaixCam%20(SG2002)-0F9B8E?style=for-the-badge&logo=linux)](https://wiki.sipeed.com/maixcam)
[![YOLOv11](https://img.shields.io/badge/YOLOv11n-Delta%2098.57%25%20Accuracy-success?style=for-the-badge)](device/models/)
[![Quantization](https://img.shields.io/badge/TPU--MLIR-INT8%20(2.88MB)%20%7C%20BF16-orange?style=for-the-badge)](device/models/)
[![OCR](https://img.shields.io/badge/OCR-PP--OCRv4%20%2B%20NLP%20Lexicon-blue?style=for-the-badge)](device/sigap_core.py)
[![Companion App](https://img.shields.io/badge/Companion%20App-Android%20Kotlin%20%7C%20Web-purple?style=for-the-badge&logo=android)](companion_app/)
[![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)](LICENSE)

<br/>

**Lead Developer:** [Gusti Hakim Thoriq Wicaksono](https://github.com/gustihakim)  
**Affiliation:** Mechatronics Engineering, Politeknik Negeri Batam  
**Project Track:** PKM-KC (Program Kreativitas Mahasiswa - Karsa Cipta) & Undergraduate Final Project

---

</div>

## 📌 Executive Overview

**Sigap Netra** is an all-in-one assistive wearable solution engineered to empower visually impaired individuals with complete financial and textual autonomy. Unlike conventional assistive apps that depend on cloud APIs with high latency and unstable internet connections, Sigap Netra operates **100% offline at the edge** on the **Sipeed MaixCam** (Sophgo SG2002 SoC, RISC-V C906 @ 1 GHz, 1 TOPS integrated TPU NPU).

The core philosophy of Sigap Netra is **zero-hallucination assistive safety**: *Visually impaired users cannot visually verify what comes out of their speaker. Silence is far better than misidentifying a banknote or a menu item.*

```
                              ┌──────────────────────────────────┐
                              │  SENSORS & PERIPHERALS           │
                              │  • GC4653 HD Camera (ISP 1080p)  │
                              │  • TF-Luna LiDAR (UART1 A18/A19) │
                              │  • ADS1115 ADC (I2C5 A15/A27)    │
                              │  • Tactile Button (GPIO A23)     │
                              │  • USB / I2S DAC Audio Out       │
                              └────────────────┬─────────────────┘
                                               │
                                               ▼
                              ┌──────────────────────────────────┐
                              │  EDGE AI DEVICE (MaixCam SG2002) │
                              │  • YOLOv11n Delta (INT8 2.88MB)  │
                              │  • PP-OCRv4 + Dynamic Tiling     │
                              │  • Offline NLP Lexicon Engine    │
                              │  • Local HTTP REST API Server    │
                              └────────────────┬─────────────────┘
                                               │ Wi-Fi AP / LAN
                                               ▼
                              ┌──────────────────────────────────┐
                              │  ASSISTIVE COMPANION APP         │
                              │  • Android Studio (Kotlin Native)│
                              │  • Responsive Web Interface (PWA)│
                              │  • Real-Time Telemetry & Sync    │
                              └──────────────────────────────────┘
```

---

## 🌟 Key Innovations & Subsystems

### 1. Banknote Recognition Pipeline (YOLOv11n Delta)
- **Extreme TPU Quantization**: Trained on custom Indonesian Rupiah dataset across multiple lighting, folding, and background conditions. Quantized via `TPU-MLIR` into INT8 (`yolo11n_rupiah_delta_int8.cvimodel`, only **2.88 MB**) and BF16 (**5.76 MB**).
- **Temporal Consensus Voting**: Runs a 6-frame burst capture (`FRAME_CAPTURE = 6`, `SETUJU_MIN = 2`) to eliminate motion jitter from handheld usage.
- **Dual-Threshold Spatial Deduplication**: Employs class-aware NMS and IoU filtering:
  - `IOU_DEDUP = 0.45`: Strict threshold for same denomination (prevents duplicate counting).
  - `IOU_DEDUP_BEDA = 0.70`: Permissive threshold for stacked banknotes of different denominations to allow accurate multi-bill summing.
- **Empirical Accuracy**: **98.57% test accuracy** on official evaluation sets with **zero misclassifications** (fails safely to silence rather than stating an incorrect amount).

### 2. Printed Text & Restaurant Menu OCR (PP-OCRv4 + NLP Engine)
- **Spatial Dual-Tiling**: Splits full-resolution camera frames into overlapping patches (`704x493` with 128px overlap) to detect small menu fonts without CPU overhead.
- **Laplacian Anti-Blur Selection**: Computes 2D Laplacian variance across consecutive frames to feed only the sharpest frame into the NPU.
- **LiDAR-Conditioned Unsharp Masking**: Dynamically adjusts image sharpening and contrast parameters based on live millimeter distance from the TF-Luna sensor.
- **Deterministic Lexicon Engine (`sigap_core.py`)**: 
  - Dynamic programming word de-concatenation (`_pisah_kata_menempel`).
  - Phonetic loanword correction (e.g. OCR noise `speclal` $\rightarrow$ `spesial`).
  - Dual-column reading order and menu item-to-price association.
  - Verified by **284 automated unit tests**.

### 3. Edge Sensor Fusion & Hardware Integration
- **TF-Luna LiDAR (UART1 @ Pin A18/A19)**: Acts as a spatial distance gate. In text mode, it verbally guides the user if a document is held too close (< 20 cm) or too far (> 45 cm).
- **ADS1115 16-bit ADC (I2C5 @ Pin A15/A27)**: Tracks 1S LiPo battery discharge curve with multi-stage audio warnings (Normal $\rightarrow$ Lemah $\rightarrow$ Kritis).
- **Single Tactile Button (GPIO A23)**: Multi-tap state machine supporting single-click (capture/process), double-click (switch mode), and long-press (instant interrupt/cancel).
- **ALSA Audio Engine**: Scans hardware mixer controls, prioritizes external USB DAC / earphones, and concatenates pre-recorded natural audio snippets for offline zero-latency voice output.

### 4. Assistive Companion Ecosystem (Android & Web)
- Located in `companion_app/`, providing a companion interface for caregivers, relatives, or users with residual vision:
  - **Android Native Bridge**: Kotlin WebView wrapper (`MainActivity.kt` & `JembatanSigap.kt`) providing hardware vibration and speech synthesis integration.
  - **Modern Web Dashboard**: PWA-ready responsive interface (`sigap.html`) with dark/light mode, real-time telemetry badge, transaction logs, and remote triggers.

---

## 🛠️ Hardware Pinout & Wiring Specifications

| Component | Interface | MaixCam Physical Pin | Operating Voltage | Function |
| :--- | :--- | :--- | :--- | :--- |
| **TF-Luna LiDAR** | UART1 RX | **Pin A18** | 5V / 3.3V Logic | Laser Time-of-Flight distance sensor |
| **TF-Luna LiDAR** | UART1 TX | **Pin A19** | 5V / 3.3V Logic | Configuration & command transmission |
| **ADS1115 ADC** | I2C5 SCL | **Pin A15** | 3.3V | 16-Bit ADC Clock Line |
| **ADS1115 ADC** | I2C5 SDA | **Pin A27** | 3.3V | 16-Bit ADC Data Line |
| **Push Button** | GPIO | **Pin A23** | 3.3V (Pull-Up) | Primary user input / navigation |
| **Audio Output** | USB Type-C | Direct OTG | 5V | USB DAC / 3.5mm Earphone connector |

---

## 📂 Repository Architecture

```text
Sigap-Netra/
├── .gitignore                      # Git ignore rules for Python, MaixCam, & Android
├── LICENSE                         # MIT License
├── README.md                       # Comprehensive documentation
├── device/                         # Edge AI Firmware & Application (MaixCam)
│   ├── app.yaml                    # MaixPy application manifest
│   ├── icon.png                    # Application launcher icon
│   ├── main.py                     # Main application runtime (Indonesian version)
│   ├── main_en.py                  # English documented runtime
│   ├── sigap_core.py               # Pure algorithmic NLP & text processing engine
│   ├── sigap_api_server.py         # Embedded HTTP REST API server
│   ├── kata_utuh.py                # Food & currency vocabulary dictionary
│   ├── models/                     # Compiled Cvitek/Sophgo cvimodels
│   │   ├── yolo11n_rupiah_delta_int8.cvimodel  (2.88 MB)
│   │   ├── yolo11n_rupiah_delta_int8.mud
│   │   ├── yolo11n_rupiah_delta_bf16.cvimodel  (5.76 MB)
│   │   └── yolo11n_rupiah_delta_bf16.mud
│   └── README.md                   # Device installation and flashing instructions
├── companion_app/                  # Assistive Companion Ecosystem
│   ├── web/                        # Responsive Web / PWA UI
│   │   ├── index.html              # Mobile companion dashboard
│   │   └── sigap.html              # Standalone web app
│   └── android/                    # Android Studio Project
│       ├── README.md               # Android build guide
│       └── app/src/main/
│           ├── assets/             # Bundled offline Web interface
│           ├── java/               # Kotlin source (MainActivity, JembatanSigap)
│           └── res/                # Android layout and drawable resources
└── docs/                           # Documentation, diagrams & schematics
    ├── architecture.md             # Detailed engineering architecture
    └── pinout.md                   # Hardware wiring diagrams
```

---

## 🚀 Getting Started

### 1. Edge Device Setup (Sipeed MaixCam)
1. **Connect to MaixCam**: Plug the MaixCam via USB-C to your PC and open [MaixVision](https://wiki.sipeed.com/maixcam).
2. **Transfer Application**:
   ```bash
   scp -r device/* root@192.168.0.1:/maixapp/apps/sigap_netra/
   ```
3. **Verify Peripheral Communication**:
   ```bash
   python3 -c "import sigap_core; print('Core OK')"
   python3 periksa_main.py /maixapp/apps/sigap_netra/main.py
   ```
4. **Auto-Run on Boot**: Enable the application in MaixPy app launcher settings or invoke:
   ```bash
   python3 /maixapp/apps/sigap_netra/main.py
   ```

### 2. Companion Mobile App Setup
1. Open `companion_app/android` in **Android Studio Hedgehog or newer**.
2. Sync Gradle dependencies and compile to an Android device (Android 8.0 Oreo or higher).
3. Connect the smartphone to the MaixCam Wi-Fi hotspot (`SIGAP-NETRA-XXXX`).
4. Launch the app to view real-time distance telemetry, battery levels, and speech history.

---

## 📡 REST API Reference

The onboard HTTP server (`sigap_api_server.py`) listens on port `8080`:

| Endpoint | Method | Description | Sample Response |
| :--- | :--- | :--- | :--- |
| `/api/status` | `GET` | Returns real-time device health, mode, and battery | `{"status":"OK","mode":"AUTO","jarak_cm":28.5,"baterai_v":3.92,"persen":82}` |
| `/api/capture` | `POST` | Triggers immediate remote camera snapshot & AI pipeline | `{"sukses":true,"mode":"UANG","hasil":"50000"}` |
| `/api/history` | `GET` | Retrieves last 50 detection & reading transactions | `[{"waktu":"14:22:05","tipe":"UANG","nilai":"Rp50.000"}]` |
| `/api/mode` | `POST` | Switches operating mode (`AUTO`, `UANG`, `TEKS`) | `{"mode":"TEKS"}` |

---

## 📊 Experimental Results & Benchmarks

| Metric | Beta Model (v7) | Charlie Model (v8) | **Delta Model (v9 Current)** |
| :--- | :---: | :---: | :---: |
| **Overall Accuracy** | 86.67% | 92.86% | **98.57%** |
| **False Positive Nominal** | 7 errors | 0 errors | **0 errors** |
| **Model Size (INT8)** | 2.89 MB | 2.89 MB | **2.88 MB** |
| **TPU Inference Latency** | ~32 ms | ~32 ms | **~31 ms** |
| **Rp100.000 Recall** | 36.7% | 93.3% | **96.7%** |
| **Wilson 95% Lower Bound** | 81.40% | 88.55% | **94.82%** |

*All evaluations conducted on standardized 210-image physical test benches under operational confidence threshold $\text{CONF\_MIN} = 0.80$.*

---

## 👤 Author & Acknowledgments

- **Lead Developer**: **Gusti Hakim Thoriq Wicaksono**
  - **NIM**: 4212301065
  - **Program**: Mechatronics Engineering, Semester 6 / 7
  - **Institution**: Politeknik Negeri Batam, Indonesia
  - **GitHub**: [@gustihakim](https://github.com/gustihakim)

Special thanks to the PKM-KC advisory committee, Politeknik Negeri Batam robotics laboratory, and the open-source communities behind MaixPy, Ultralytics YOLOv11, and PaddlePaddle PP-OCR.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE) - see the [LICENSE](LICENSE) file for details.
