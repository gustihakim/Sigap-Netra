package com.example.sigapnetra.util

import com.example.sigapnetra.data.model.Deteksi
import com.example.sigapnetra.data.model.Rekaman

data class HasilParseRekaman(
    val barisValid: List<Rekaman>,
    val barisRusak: Int
)

data class HasilParseDeteksi(
    val barisValid: List<Deteksi>,
    val barisRusak: Int
)

object CsvParser {

    fun parseUjiKameraCsv(csvText: String, deviceId: String, sessionId: String): HasilParseRekaman {
        val lines = csvText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return HasilParseRekaman(emptyList(), 0)

        val headers = lines[0].split(",").map { it.trim().lowercase() }
        val idxNo = headers.indexOf("no").takeIf { it != -1 } ?: 0
        val idxDetik = headers.indexOf("detik").takeIf { it != -1 } ?: 1
        val idxObjek = headers.indexOf("objek").takeIf { it != -1 }
        val idxJalur = headers.indexOf("jalur").takeIf { it != -1 } ?: 2
        val idxLebar = headers.indexOf("lebar").takeIf { it != -1 } ?: 3
        val idxTinggi = headers.indexOf("tinggi").takeIf { it != -1 } ?: 4
        val idxFormat = headers.indexOf("format").takeIf { it != -1 } ?: 5
        val idxMs = headers.indexOf("ms_baca").takeIf { it != -1 } ?: 6
        val idxTajam = headers.indexOf("ketajaman").takeIf { it != -1 } ?: 7
        val idxBerkas = headers.indexOf("berkas").takeIf { it != -1 } ?: 8

        val barisValid = mutableListOf<Rekaman>()
        var barisRusak = 0

        for (i in 1 until lines.size) {
            val parts = lines[i].split(",").map { it.trim() }
            if (parts.size < 8) {
                barisRusak++
                continue
            }

            try {
                val no = parts[idxNo].toInt()
                val detik = parts[idxDetik].toDouble()
                val objek = if (idxObjek != null && idxObjek < parts.size && parts[idxObjek].isNotEmpty()) {
                    parts[idxObjek]
                } else null
                val jalur = parts[idxJalur]
                val msBaca = parts[idxMs].toInt()
                val ketajaman = parts[idxTajam].toDouble()
                val berkas = if (idxBerkas < parts.size) parts[idxBerkas] else ""

                if (ketajaman < 0 || berkas == "-") {
                    barisRusak++
                    continue
                }

                if (jalur != "lama" && jalur != "baru") {
                    barisRusak++
                    continue
                }

                val dedupHash = "$deviceId|$no|$jalur"
                val record = Rekaman(
                    dedupHash = dedupHash,
                    deviceId = deviceId,
                    sessionId = sessionId,
                    no = no,
                    detik = detik,
                    objek = objek,
                    jalur = jalur,
                    msBaca = msBaca,
                    ketajaman = ketajaman,
                    berkas = berkas
                )
                barisValid.add(record)
            } catch (e: Exception) {
                barisRusak++
            }
        }

        return HasilParseRekaman(barisValid, barisRusak)
    }

    fun parseDeteksiCsv(csvText: String, deviceId: String, sessionId: String): HasilParseDeteksi {
        val lines = csvText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return HasilParseDeteksi(emptyList(), 0)

        val barisValid = mutableListOf<Deteksi>()
        var barisRusak = 0

        val startIdx = if (lines[0].startsWith("seq")) 1 else 0
        for (i in startIdx until lines.size) {
            val parts = lines[i].split(",").map { it.trim() }
            if (parts.size < 5) {
                barisRusak++
                continue
            }

            try {
                val seq = parts[0].toInt()
                val tsIso = parts[1]
                val type = parts[2].uppercase()
                val rawValue = parts[3].trim('"', '\'')
                val confidence = parts[4].toDouble()
                val distanceCm = parts.getOrNull(5)?.toIntOrNull()
                val modelVer = parts.getOrNull(6)
                val latencyMs = parts.getOrNull(7)?.toIntOrNull()
                val thumb = parts.getOrNull(8)?.takeIf { it.isNotBlank() }

                val dedupHash = "$deviceId|$seq|$tsIso"
                val deteksi = Deteksi(
                    dedupHash = dedupHash,
                    deviceId = deviceId,
                    sessionId = sessionId,
                    seq = seq,
                    capturedAt = System.currentTimeMillis(),
                    jenis = type,
                    terbaca = rawValue,
                    confidence = confidence,
                    jarakCm = distanceCm,
                    modelVer = modelVer,
                    latencyMs = latencyMs,
                    thumbPath = thumb
                )
                barisValid.add(deteksi)
            } catch (e: Exception) {
                barisRusak++
            }
        }

        return HasilParseDeteksi(barisValid, barisRusak)
    }
}
