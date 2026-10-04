# Sigap Netra - Device Application

Code that runs on the **Sipeed MaixCam** (SG2002, RISC-V C906, 1 TOPS NPU).

## Contents

| File | Purpose |
| :--- | :--- |
| `main.py` | Main application: camera, YOLO, PP-OCR, audio, button, LiDAR, battery, HTTP server |
| `main_en.py` | Same application with English comments (reference only) |
| `sigap_core.py` | Pure logic without `maix`, testable on a PC (284 unit tests) |
| `sigap_api_server.py` | Standalone HTTP server (reference; `main.py` embeds its own) |
| `kata_utuh.py` | Word bank for the dictionary |
| `app.yaml` | MaixPy app manifest, app id `aplikasi_utama` |
| `models/` | YOLOv11n Delta INT8 (2.88 MB) and BF16 (5.76 MB) |

## Quick check before deploying

```bash
python sigap_core.py                         # expected: 284 lulus, 0 gagal
python ../tools/periksa_main.py main.py      # expected: no MASALAH
```

## Install and usage

See the full step-by-step guide in the root README:
[Sipeed MaixCam Usage Guide](../README.md#-sipeed-maixcam-usage-guide).
