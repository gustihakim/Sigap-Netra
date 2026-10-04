# Sigap Netra — Edge AI Firmware & Application

This directory contains the embedded Python application deployed to the **Sipeed MaixCam** (Sophgo SG2002 SoC, RISC-V C906, 1 TOPS NPU).

## 📁 Directory Contents

- `main.py`: Primary application entrypoint (Bahasa Indonesia comments, fully optimized for device execution).
- `main_en.py`: English documented application runtime.
- `sigap_core.py`: Pure algorithmic core containing phonetic dictionaries, OCR text splitting, Levenshtein post-processing, bounding box deduplication, and button pattern state machine (verified by 284 unit tests).
- `sigap_api_server.py`: Multithreaded REST API server enabling wireless synchronization with the companion mobile app.
- `kata_utuh.py`: Indonesian culinary and currency vocabulary lexicon.
- `models/`:
  - `yolo11n_rupiah_delta_int8.cvimodel` (2.88 MB): Hardware-accelerated INT8 quantized YOLOv11 model.
  - `yolo11n_rupiah_delta_bf16.cvimodel` (5.76 MB): BF16 floating-point precision variant.
  - `.mud`: MaixPy unified descriptor files.

## 🚀 Running on Device

```bash
# Verify static code health before flashing
python3 periksa_main.py main.py

# Run on MaixCam
python3 main.py
```
