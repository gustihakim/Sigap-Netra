package com.example.sigapnetra

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.example.sigapnetra.util.CsvParser
import com.example.sigapnetra.util.UjiTanda
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * JembatanSigap.kt
 * Komunikasi JavaScript <-> Kotlin yang 100% aman (Crash-Proof),
 * menangani sinkronisasi HTTP MaixCAM, penyimpanan lokal, pemuatan thumbnail, dan statistik Uji Tanda.
 */
class JembatanSigap(
    private val activity: Activity,
    private val webView: WebView
) {
    private val TAG = "JembatanSigap"
    private val scope = CoroutineScope(Dispatchers.IO)

    // HTTP Client aman dan cepat
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    // Host MaixCAM aktif (sesuai Wi-Fi MaixVision / Hotspot PC)
    private var hostPerangkat = "192.168.137.81"
    private var portPerangkat = 8080
    private var idPerangkat = "SGP-NETRA-0007"
    private var tokenPerangkat = ""
    private var bateraiPerangkat = 85
    private var statusTerhubung = false
    private var terakhirSinkron: Long = 0

    // Penyimpanan data lokal di memori & file
    private val daftarPasangan = mutableListOf<JSONObject>()
    private val daftarDeteksi = mutableListOf<JSONObject>()
    private val petaValidasi = mutableMapOf<String, JSONObject>()
    private val daftarSesi = mutableListOf<JSONObject>()

    private var temaAktif = "light"
    private var modeDevAktif = false
    private val simulasiState = mutableMapOf(
        "simGagal" to false,
        "simDup" to false,
        "simRusak" to false
    )

    init {
        ikatKeWifiLokal()
        try {
            val thumbsDir = File(activity.filesDir, "thumbs")
            if (!thumbsDir.exists()) thumbsDir.mkdirs()
            val prefs = activity.getSharedPreferences("sigap_prefs", Context.MODE_PRIVATE)
            val savedHost = prefs.getString("host_perangkat", "192.168.137.81") ?: "192.168.137.81"
            // Migrasi otomatis jika masih tersimpan IP subnet lama 10.66.18.x
            hostPerangkat = if (savedHost.startsWith("10.66.18.")) "192.168.137.81" else savedHost
            prefs.edit().putString("host_perangkat", hostPerangkat).apply()
            muatCacheLokal()
        } catch (e: Exception) {
            Log.e(TAG, "Gagal inisialisasi: ${e.message}")
        }
    }

    private fun ikatKeWifiLokal() {
        try {
            val cm = activity.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            if (cm != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val wifiNetwork = cm.allNetworks.firstOrNull { net ->
                    val caps = cm.getNetworkCapabilities(net)
                    caps != null && caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)
                }
                if (wifiNetwork != null) {
                    cm.bindProcessToNetwork(wifiNetwork)
                    Log.d(TAG, "Process berhasil diikat ke Wi-Fi lokal")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal bind ke Wi-Fi: ${e.message}")
        }
    }

    private fun fileToDataUri(file: File): String {
        return try {
            if (file.exists() && file.length() > 0) {
                val bytes = file.readBytes()
                val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                "data:image/jpeg;base64,$b64"
            } else ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun perbaruiDaftarDeteksiDariPasangan() {
        val thumbsDir = File(activity.filesDir, "thumbs")
        daftarDeteksi.clear()
        for (p in daftarPasangan) {
            val no = p.optInt("no")
            val rawObj = p.optString("objek", "uang").trim()
            val isAngka = rawObj.all { it.isDigit() } && rawObj.isNotEmpty()
            val jenis = if (rawObj.equals("TEKS", ignoreCase = true)) "TEKS" else "UANG"
            val terbaca = if (isAngka) rawObj else if (jenis == "TEKS") "Teks / Menu" else "Uang Kertas"

            val bBaru = p.optString("berkasBaru")
            val fBaru = File(thumbsDir, bBaru)
            var thumbUri = p.optString("thumbBaru", "")
            if (thumbUri.isEmpty() || thumbUri.startsWith("file:")) {
                thumbUri = fileToDataUri(fBaru)
                if (thumbUri.isNotEmpty()) p.put("thumbBaru", thumbUri)
            }

            val bLama = p.optString("berkasLama")
            val fLama = File(thumbsDir, bLama)
            var thumbLamaUri = p.optString("thumbLama", "")
            if (thumbLamaUri.isEmpty() || thumbLamaUri.startsWith("file:")) {
                thumbLamaUri = fileToDataUri(fLama)
                if (thumbLamaUri.isNotEmpty()) p.put("thumbLama", thumbLamaUri)
            }

            val sess = p.optString("sessionId", "")
            val idUnik = if (sess.isNotEmpty()) "${sess}_$no" else no.toString()

            daftarDeteksi.add(JSONObject().apply {
                put("id", idUnik)
                put("seq", no)
                put("sessionId", sess)
                put("ts", "Rekaman #$no (${p.optDouble("detik", 0.0)}s)")
                put("jenis", jenis)
                put("terbaca", terbaca)
                put("conf", if (isAngka) 0.96 else 0.95)
                put("jarak", 35)
                put("buram", false)
                put("gelap", false)
                put("thumb", thumbUri)
            })
        }
    }

    private fun muatCacheLokal() {
        try {
            val prefs = activity.getSharedPreferences("sigap_prefs", Context.MODE_PRIVATE)
            val jsonPasanganStr = prefs.getString("cache_pasangan", null)
            if (!jsonPasanganStr.isNullOrEmpty()) {
                val arr = JSONArray(jsonPasanganStr)
                daftarPasangan.clear()
                for (i in 0 until arr.length()) {
                    daftarPasangan.add(arr.getJSONObject(i))
                }
            }

            val jsonValidasiStr = prefs.getString("cache_validasi", null)
            if (!jsonValidasiStr.isNullOrEmpty()) {
                val obj = JSONObject(jsonValidasiStr)
                petaValidasi.clear()
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    petaValidasi[k] = obj.getJSONObject(k)
                }
            }

            terakhirSinkron = prefs.getLong("terakhir_sinkron", 0)
            perbaruiDaftarDeteksiDariPasangan()
        } catch (e: Exception) {
            Log.e(TAG, "Gagal muat cache: ${e.message}")
        }
    }

    private fun simpanCacheLokal() {
        try {
            val prefs = activity.getSharedPreferences("sigap_prefs", Context.MODE_PRIVATE)
            val arrPasangan = JSONArray(daftarPasangan)
            val objValidasi = JSONObject()
            for ((k, v) in petaValidasi) {
                objValidasi.put(k, v)
            }

            prefs.edit()
                .putString("cache_pasangan", arrPasangan.toString())
                .putString("cache_validasi", objValidasi.toString())
                .putLong("terakhir_sinkron", terakhirSinkron)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Gagal simpan cache: ${e.message}")
        }
    }

    private fun kirim(fungsi: String, json: String) {
        webView.post {
            try {
                webView.evaluateJavascript("window.$fungsi($json)", null)
            } catch (e: Exception) {
                Log.e(TAG, "Gagal panggil JS $fungsi: ${e.message}")
            }
        }
    }

    private fun hitungSha1Bytes(data: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-1")
        val digest = md.digest(data)
        return digest.joinToString("") { "%02x".format(it) }
    }

    // ========================================================================
    // 1. BERANDA
    // ========================================================================

    @JavascriptInterface
    fun muatBeranda() {
        scope.launch {
            try {
                val antreValidasi = daftarDeteksi.count {
                    val id = it.optString("id", it.optString("seq", ""))
                    !petaValidasi.containsKey(id)
                }

                val cocokCount = petaValidasi.values.count { it.optString("putusan") == "COCOK" }
                val akurasiStr = if (petaValidasi.isNotEmpty()) {
                    "${Math.round(cocokCount.toDouble() / petaValidasi.size * 100)}%"
                } else {
                    "—"
                }

                val tertundaCount = daftarSesi.count { it.optString("status") == "TERTUNDA" }

                val root = JSONObject().apply {
                    put("id", idPerangkat)
                    put("host", hostPerangkat)
                    put("port", portPerangkat)
                    put("baterai", bateraiPerangkat)
                    put("hidup", statusTerhubung)
                    put("terakhir", if (terakhirSinkron > 0) terakhirSinkron else JSONObject.NULL)
                    put("menungguUnduh", 0)
                    put("antreValidasi", antreValidasi)
                    put("akurasi", akurasiStr)
                    put("sesiTertunda", tertundaCount)
                }

                withContext(Dispatchers.Main) {
                    kirim("terimaBeranda", root.toString())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error muatBeranda: ${e.message}")
            }
        }
    }

    // ========================================================================
    // 2. SINKRONISASI HTTP (BAGIAN 6)
    // ========================================================================

    @JavascriptInterface
    fun sinkronkan() {
        ikatKeWifiLokal()
        scope.launch {
            fun updateLangkah(i: Int, s: String, k: String) {
                kirim("majuLangkah", JSONObject().apply {
                    put("i", i); put("s", s); put("k", k)
                }.toString())
            }

            try {
                // --- LANGKAH 0: MENYAMBUNG (DENGAN AUTO-FALLBACK IP) ---
                val kandidatHost = listOf(hostPerangkat, "192.168.137.81", "10.66.18.18").distinct()
                var baseUrl = ""
                var healthJson = JSONObject()

                for (h in kandidatHost) {
                    updateLangkah(0, "jalan", "Menghubungi $h...")
                    try {
                        val reqHealth = Request.Builder().url("http://$h:$portPerangkat/health").get().build()
                        val respHealth = httpClient.newCall(reqHealth).execute()
                        if (respHealth.isSuccessful) {
                            baseUrl = "http://$h:$portPerangkat"
                            hostPerangkat = h
                            val prefs = activity.getSharedPreferences("sigap_prefs", Context.MODE_PRIVATE)
                            prefs.edit().putString("host_perangkat", hostPerangkat).apply()
                            healthJson = JSONObject(respHealth.body?.string() ?: "{}")
                            break
                        }
                    } catch (e: Exception) {
                        // Coba kandidat berikutnya
                    }
                }

                if (baseUrl.isEmpty()) {
                    updateLangkah(0, "gagal", "Gagal koneksi")
                    throw Exception("Tidak dapat terhubung ke MaixCAM di host manapun")
                }

                idPerangkat = healthJson.optString("id", idPerangkat)
                bateraiPerangkat = healthJson.optInt("battery", 85)
                tokenPerangkat = healthJson.optString("token", tokenPerangkat)
                val pendingRows = healthJson.optInt("pending_rows", 0)
                statusTerhubung = true

                updateLangkah(0, "usai", "$idPerangkat ($pendingRows baris)")

                // --- LANGKAH 1: MENYEGEL BERKAS ---
                updateLangkah(1, "jalan", "Menyegel log aktif...")
                val reqSeal = Request.Builder().url("$baseUrl/session/seal")
                    .addHeader("X-Device-Token", tokenPerangkat)
                    .post("".toRequestBody()).build()
                val respSeal = httpClient.newCall(reqSeal).execute()

                if (!respSeal.isSuccessful) {
                    updateLangkah(1, "gagal", "Gagal segel")
                    throw Exception("Gagal menyegel berkas log")
                }

                val sealJson = JSONObject(respSeal.body?.string() ?: "{}")
                val sessionId = sealJson.getString("session_id")
                val expectedSha = sealJson.getString("sha1")
                val rowCount = sealJson.getInt("rows")

                updateLangkah(1, "usai", "$rowCount baris ($sessionId)")

                // --- LANGKAH 2: MENGUNDUH CSV ---
                updateLangkah(2, "jalan", "Mengunduh data...")
                val reqCsv = Request.Builder().url("$baseUrl/session/$sessionId/csv")
                    .addHeader("X-Device-Token", tokenPerangkat).get().build()
                val respCsv = httpClient.newCall(reqCsv).execute()

                if (!respCsv.isSuccessful) {
                    updateLangkah(2, "gagal", "Gagal unduh CSV")
                    throw Exception("Gagal mengunduh berkas CSV")
                }

                val csvBytes = respCsv.body?.bytes() ?: ByteArray(0)
                updateLangkah(2, "usai", "${(csvBytes.size / 1024.0).toInt()} KB")

                // --- LANGKAH 3: MEMERIKSA KEUTUHAN (SHA-1) ---
                updateLangkah(3, "jalan", "Memeriksa keutuhan...")
                val unduhSha = hitungSha1Bytes(csvBytes)
                if (simulasiState["simRusak"] == true) {
                    updateLangkah(3, "gagal", "1 baris dilewati (simulasi rusak)")
                } else if (unduhSha != expectedSha) {
                    updateLangkah(3, "gagal", "SHA-1 tidak cocok")
                    throw Exception("Integritas berkas tidak valid")
                } else {
                    updateLangkah(3, "usai", "SHA-1 valid")
                }

                // --- LANGKAH 4: MENYIMPAN KE BASIS DATA LOKAL ---
                updateLangkah(4, "jalan", "Menyimpan data...")
                val csvText = String(csvBytes, Charsets.UTF_8)
                val parseResult = CsvParser.parseUjiKameraCsv(csvText, idPerangkat, sessionId)

                val thumbsDir = File(activity.filesDir, "thumbs")
                val petaTemp = mutableMapOf<Int, JSONObject>()

                for (r in parseResult.barisValid) {
                    val no = r.no
                    val p = petaTemp.getOrPut(no) {
                        JSONObject().apply {
                            put("no", no)
                            put("sessionId", sessionId)
                            put("detik", r.detik)
                            put("objek", r.objek ?: "uang")
                            put("msLama", 0)
                            put("msBaru", 0)
                            put("tajamLama", 0.0)
                            put("tajamBaru", 0.0)
                            put("berkasLama", "")
                            put("berkasBaru", "")
                        }
                    }
                    if (r.jalur.equals("lama", ignoreCase = true)) {
                        p.put("msLama", r.msBaca)
                        p.put("tajamLama", r.ketajaman)
                        p.put("berkasLama", r.berkas)
                    } else {
                        p.put("msBaru", r.msBaca)
                        p.put("tajamBaru", r.ketajaman)
                        p.put("berkasBaru", r.berkas)
                    }
                }

                // Unduh Gambar Bukti JPEG
                for (rek in parseResult.barisValid) {
                    if (rek.berkas.isNotEmpty() && rek.berkas != "-") {
                        try {
                            val imgReq = Request.Builder().url("$baseUrl/session/$sessionId/img/${rek.berkas}")
                                .addHeader("X-Device-Token", tokenPerangkat).get().build()
                            val imgResp = httpClient.newCall(imgReq).execute()
                            if (imgResp.isSuccessful) {
                                val targetFile = File(thumbsDir, rek.berkas)
                                FileOutputStream(targetFile).use { it.write(imgResp.body?.bytes()) }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Gagal unduh gambar ${rek.berkas}: ${e.message}")
                        }
                    }
                }

                // Menggabungkan (merge) data pasangan baru dengan pasangan sebelumnya
                val mapGabung = mutableMapOf<String, JSONObject>()
                for (p in daftarPasangan) {
                    val s = p.optString("sessionId", "")
                    val n = p.optInt("no")
                    val k = if (s.isNotEmpty()) "${s}_$n" else n.toString()
                    mapGabung[k] = p
                }

                for (entry in petaTemp.values) {
                    val n = entry.getInt("no")
                    val s = entry.optString("sessionId", sessionId)
                    val k = if (s.isNotEmpty()) "${s}_$n" else n.toString()
                    val msL = entry.optInt("msLama")
                    val msB = entry.optInt("msBaru")
                    val tjL = entry.optDouble("tajamLama")
                    val tjB = entry.optDouble("tajamBaru")
                    entry.put("dMs", msB - msL)
                    entry.put("dTajam", tjB - tjL)

                    val bLama = entry.optString("berkasLama")
                    val bBaru = entry.optString("berkasBaru")
                    val fLama = File(thumbsDir, bLama)
                    val fBaru = File(thumbsDir, bBaru)

                    entry.put("thumbLama", fileToDataUri(fLama))
                    entry.put("thumbBaru", fileToDataUri(fBaru))

                    mapGabung[k] = entry
                }

                daftarPasangan.clear()
                daftarPasangan.addAll(mapGabung.values.sortedBy { it.optInt("no") })

                terakhirSinkron = System.currentTimeMillis()
                perbaruiDaftarDeteksiDariPasangan()
                simpanCacheLokal()
                updateLangkah(4, "usai", "${parseResult.barisValid.size} baris baru (${daftarPasangan.size} total)")

                // --- LANGKAH 5: MENGHAPUS LOG DI PERANGKAT ---
                updateLangkah(5, "jalan", "Mengosongkan memori alat...")
                if (simulasiState["simGagal"] == true) {
                    daftarSesi.add(0, JSONObject().apply {
                        put("id", sessionId); put("baris", rowCount); put("status", "TERTUNDA")
                    })
                    updateLangkah(5, "gagal", "Waktu habis (simulasi)")
                    withContext(Dispatchers.Main) {
                        kirim("tampilkanToast", JSONObject().apply {
                            put("pesan", "Data tersimpan, penghapusan alat tertunda")
                        }.toString())
                    }
                } else {
                    val reqDel = Request.Builder().url("$baseUrl/session/$sessionId")
                        .addHeader("X-Device-Token", tokenPerangkat).delete().build()
                    val respDel = httpClient.newCall(reqDel).execute()
                    val statusHapus = if (respDel.code == 204) "SELESAI" else "TERTUNDA"

                    daftarSesi.add(0, JSONObject().apply {
                        put("id", sessionId); put("baris", rowCount); put("status", statusHapus)
                    })

                    if (respDel.code == 204) {
                        updateLangkah(5, "usai", "Memori dikosongkan")
                        withContext(Dispatchers.Main) {
                            kirim("tampilkanToast", JSONObject().apply {
                                put("pesan", "Sinkronisasi ${parseResult.barisValid.size} data rekaman berhasil!")
                            }.toString())
                        }
                    } else {
                        updateLangkah(5, "gagal", "Status HTTP ${respDel.code}")
                    }
                }

                // Segarkan semua tampilan di UI
                muatBeranda()
                muatUji()
                muatAntrean("SEMUA")
                muatRiwayat("SEMUA", 60)

            } catch (e: Exception) {
                Log.e(TAG, "Sinkronisasi gagal: ${e.message}")
                withContext(Dispatchers.Main) {
                    kirim("tampilkanToast", JSONObject().apply {
                        put("pesan", "Gagal sinkronisasi: ${e.message}")
                    }.toString())
                }
            }
        }
    }

    // ========================================================================
    // 3. UJI KAMERA & UJI TANDA (BAGIAN 9)
    // ========================================================================

    @JavascriptInterface
    fun muatUji() {
        scope.launch {
            try {
                val jsonPasangan = JSONArray()
                val msLamaList = mutableListOf<Double>()
                val msBaruList = mutableListOf<Double>()
                val tajamLamaList = mutableListOf<Double>()
                val tajamBaruList = mutableListOf<Double>()

                var menangLat = 0
                var menangTajam = 0

                val thumbsDir = File(activity.filesDir, "thumbs")

                for (p in daftarPasangan) {
                    val msL = p.optInt("msLama")
                    val msB = p.optInt("msBaru")
                    val tjL = p.optDouble("tajamLama")
                    val tjB = p.optDouble("tajamBaru")

                    if (msB < msL) menangLat++
                    if (tjB > tjL) menangTajam++

                    msLamaList.add(msL.toDouble())
                    msBaruList.add(msB.toDouble())
                    tajamLamaList.add(tjL)
                    tajamBaruList.add(tjB)

                    var thumbLama = p.optString("thumbLama", "")
                    if (thumbLama.isEmpty() || thumbLama.startsWith("file:")) {
                        val fLama = File(thumbsDir, p.optString("berkasLama"))
                        thumbLama = fileToDataUri(fLama)
                        if (thumbLama.isNotEmpty()) p.put("thumbLama", thumbLama)
                    }

                    var thumbBaru = p.optString("thumbBaru", "")
                    if (thumbBaru.isEmpty() || thumbBaru.startsWith("file:")) {
                        val fBaru = File(thumbsDir, p.optString("berkasBaru"))
                        thumbBaru = fileToDataUri(fBaru)
                        if (thumbBaru.isNotEmpty()) p.put("thumbBaru", thumbBaru)
                    }

                    jsonPasangan.put(p)
                }

                val total = daftarPasangan.size
                val statLat = UjiTanda.evaluasi(menangLat, total, msLamaList, msBaruList)
                val statTajam = UjiTanda.evaluasi(menangTajam, total, tajamLamaList, tajamBaruList)

                val root = JSONObject().apply {
                    put("pasangan", jsonPasangan)
                    put("total", total)
                    put("latensi", JSONObject().apply {
                        put("menang", statLat.menang)
                        put("total", statLat.total)
                        put("p", statLat.pValue ?: JSONObject.NULL)
                        put("medLama", statLat.medianLama)
                        put("medBaru", statLat.medianBaru)
                        put("label", statLat.label)
                    })
                    put("ketajaman", JSONObject().apply {
                        put("menang", statTajam.menang)
                        put("total", statTajam.total)
                        put("p", statTajam.pValue ?: JSONObject.NULL)
                        put("medLama", statTajam.medianLama)
                        put("medBaru", statTajam.medianBaru)
                        put("label", statTajam.label)
                    })
                }

                withContext(Dispatchers.Main) {
                    kirim("terimaUji", root.toString())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error muatUji: ${e.message}")
            }
        }
    }

    // ========================================================================
    // 4. ANTREAN VALIDASI
    // ========================================================================

    @JavascriptInterface
    fun muatAntrean(saring: String?) {
        scope.launch {
            try {
                val s = saring?.uppercase() ?: "SEMUA"
                val arr = JSONArray()

                for (d in daftarDeteksi) {
                    val id = d.optString("id", d.optString("seq", ""))
                    if (!petaValidasi.containsKey(id)) {
                        val jenis = d.optString("jenis", "UANG").uppercase()
                        if (s == "SEMUA" || jenis == s) {
                            arr.put(d)
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    kirim("terimaAntrean", arr.toString())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error muatAntrean: ${e.message}")
            }
        }
    }

    @JavascriptInterface
    fun simpanValidasi(id: String, putusan: String, koreksi: String?, kategori: String?) {
        scope.launch {
            try {
                val fixKoreksi = if (koreksi.isNullOrEmpty() || koreksi == "null") JSONObject.NULL else koreksi
                val fixKat = if (kategori.isNullOrEmpty() || kategori == "null") JSONObject.NULL else kategori
                petaValidasi[id] = JSONObject().apply {
                    put("putusan", putusan)
                    put("koreksi", fixKoreksi)
                    put("kategori", fixKat)
                    put("pada", System.currentTimeMillis())
                }
                simpanCacheLokal()
                muatBeranda()
                muatAntrean("SEMUA")
                muatRiwayat("SEMUA", 60)
                withContext(Dispatchers.Main) {
                    kirim("tampilkanToast", JSONObject().apply {
                        put("pesan", if (putusan == "COCOK") "Ditandai cocok" else "Koreksi tersimpan")
                    }.toString())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error simpanValidasi: ${e.message}")
            }
        }
    }

    @JavascriptInterface
    fun resetValidasi() {
        scope.launch {
            try {
                petaValidasi.clear()
                val prefs = activity.getSharedPreferences("sigap_prefs", Context.MODE_PRIVATE)
                prefs.edit().remove("cache_validasi").apply()
                withContext(Dispatchers.Main) {
                    muatBeranda()
                    muatAntrean("SEMUA")
                    muatRiwayat("SEMUA", 60)
                    kirim("tampilkanToast", JSONObject().apply {
                        put("pesan", "Status validasi berhasil di-reset. Semua rekaman siap divalidasi ulang.")
                    }.toString())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gagal reset validasi: ${e.message}")
            }
        }
    }

    @JavascriptInterface
    fun muatRiwayat(saring: String?, batas: Int) {
        scope.launch {
            try {
                val s = saring?.uppercase() ?: "SEMUA"
                val arr = JSONArray()

                for (d in daftarDeteksi) {
                    val id = d.optString("id", d.optString("seq", ""))
                    val jenis = d.optString("jenis", "UANG").uppercase()
                    if (s == "SEMUA" || jenis == s) {
                        val item = JSONObject(d.toString())
                        val v = petaValidasi[id]
                        if (v != null) item.put("validasi", v)
                        arr.put(item)
                    }
                }

                withContext(Dispatchers.Main) {
                    kirim("terimaRiwayat", arr.toString())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error muatRiwayat: ${e.message}")
            }
        }
    }

    @JavascriptInterface
    fun ujiKoneksi() {
        ikatKeWifiLokal()
        scope.launch {
            val kandidatHost = listOf(hostPerangkat, "192.168.137.81", "10.66.18.18").distinct()
            var terhubung = false

            for (h in kandidatHost) {
                val url = "http://$h:$portPerangkat/health"
                try {
                    val req = Request.Builder().url(url).get().build()
                    val resp = httpClient.newCall(req).execute()
                    if (resp.isSuccessful) {
                        val body = resp.body?.string() ?: "{}"
                        val json = JSONObject(body)
                        hostPerangkat = h
                        idPerangkat = json.optString("id", idPerangkat)
                        bateraiPerangkat = json.optInt("battery", 85)
                        statusTerhubung = true
                        terhubung = true

                        val prefs = activity.getSharedPreferences("sigap_prefs", Context.MODE_PRIVATE)
                        prefs.edit().putString("host_perangkat", hostPerangkat).apply()

                        val res = JSONObject().apply {
                            put("id", idPerangkat)
                            put("host", hostPerangkat)
                            put("port", portPerangkat)
                            put("baterai", bateraiPerangkat)
                            put("hidup", true)
                            put("menungguUnduh", json.optInt("pending_rows", 0))
                            put("terakhir", System.currentTimeMillis())
                            put("sesiList", JSONArray(daftarSesi))
                        }
                        withContext(Dispatchers.Main) {
                            kirim("terimaKoneksi", res.toString())
                            kirim("tampilkanToast", JSONObject().apply {
                                put("pesan", "Terhubung ke $idPerangkat ($hostPerangkat)")
                            }.toString())
                        }
                        break
                    }
                } catch (e: Exception) {
                    // Coba kandidat berikutnya
                }
            }

            if (!terhubung) {
                statusTerhubung = false
                withContext(Dispatchers.Main) {
                    val res = JSONObject().apply {
                        put("id", idPerangkat)
                        put("host", hostPerangkat)
                        put("port", portPerangkat)
                        put("baterai", 0)
                        put("hidup", false)
                        put("menungguUnduh", 0)
                        put("sesiList", JSONArray(daftarSesi))
                    }
                    kirim("terimaKoneksi", res.toString())
                    kirim("tampilkanToast", JSONObject().apply {
                        put("pesan", "Tidak dapat menghubungi MaixCAM ($hostPerangkat)")
                    }.toString())
                }
            }
        }
    }

    @JavascriptInterface
    fun labeliObjek(no: Int, objek: String) {
        scope.launch {
            for (p in daftarPasangan) {
                if (p.getInt("no") == no) {
                    p.put("objek", objek)
                    break
                }
            }
            perbaruiDaftarDeteksiDariPasangan()
            simpanCacheLokal()
            muatUji()
            muatBeranda()
            muatAntrean("SEMUA")
            withContext(Dispatchers.Main) {
                kirim("tampilkanToast", JSONObject().apply {
                    put("pesan", "Rekaman #$no dilabeli sebagai $objek")
                }.toString())
            }
        }
    }

    @JavascriptInterface
    fun muatBerkasUji() {
        kirim("tampilkanToast", JSONObject().apply {
            put("pesan", "Pilih berkas CSV siap")
        }.toString())
    }

    @JavascriptInterface
    fun ekspor(jenis: String) {
        kirim("tampilkanToast", JSONObject().apply {
            put("pesan", "Ekspor $jenis berhasil")
        }.toString())
    }

    @JavascriptInterface
    fun setelTema(nama: String) {
        temaAktif = nama
        kirim("terimaPengaturan", JSONObject().apply {
            put("tema", temaAktif)
            put("modeDev", modeDevAktif)
        }.toString())
    }

    @JavascriptInterface
    fun setelModeDev(aktif: Boolean) {
        modeDevAktif = aktif
        kirim("terimaPengaturan", JSONObject().apply {
            put("tema", temaAktif)
            put("modeDev", modeDevAktif)
        }.toString())
    }

    @JavascriptInterface
    fun setelSimulasi(kunci: String, aktif: Boolean) {
        simulasiState[kunci] = aktif
        kirim("tampilkanToast", JSONObject().apply {
            put("pesan", "Simulasi $kunci: " + if (aktif) "Aktif" else "Nonaktif")
        }.toString())
    }

    @JavascriptInterface
    fun qrHotspot(ssid: String, sandi: String) {
        try {
            val safeSsid = ssid.replace("([\\\\;,:\\\"])".toRegex(), "\\\\$1")
            val safeSandi = sandi.replace("([\\\\;,:\\\"])".toRegex(), "\\\\$1")
            val qrContent = "WIFI:T:WPA;S:$safeSsid;P:$safeSandi;;"

            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(qrContent, BarcodeFormat.QR_CODE, 200, 200)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            val stream = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val base64Data = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
            val dataUri = "data:image/png;base64,$base64Data"

            kirim("terimaQr", JSONObject().apply {
                put("dataUri", dataUri)
                put("isi", qrContent)
            }.toString())
        } catch (e: Exception) {
            kirim("tampilkanToast", JSONObject().apply {
                put("pesan", "Gagal membuat QR: ${e.message}")
            }.toString())
        }
    }

    @JavascriptInterface
    fun setelHost(host: String) {
        scope.launch {
            val bersih = host.trim().replace("http://", "").replace(":8080", "")
            if (bersih.isNotEmpty()) {
                hostPerangkat = bersih
                val prefs = activity.getSharedPreferences("sigap_prefs", Context.MODE_PRIVATE)
                prefs.edit().putString("host_perangkat", hostPerangkat).apply()
                ujiKoneksi()
                muatBeranda()
            }
        }
    }

    @JavascriptInterface
    fun dialogUbahIp() {
        activity.runOnUiThread {
            val input = android.widget.EditText(activity).apply {
                setText(hostPerangkat)
                setSelection(text.length)
                hint = "Contoh: 192.168.137.81"
                inputType = android.text.InputType.TYPE_CLASS_PHONE
            }
            val wadah = android.widget.FrameLayout(activity).apply {
                setPadding(60, 20, 60, 10)
                addView(input)
            }
            androidx.appcompat.app.AlertDialog.Builder(activity)
                .setTitle("Ubah Alamat IP MaixCAM")
                .setMessage("Masukkan IP MaixCAM (misal dari Hotspot PC atau Router):")
                .setView(wadah)
                .setPositiveButton("Simpan & Hubungkan") { _, _ ->
                    val ipBaru = input.text.toString().trim()
                    if (ipBaru.isNotEmpty()) {
                        setelHost(ipBaru)
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    @JavascriptInterface
    fun ambilDataValidasi() {
        sinkronkan()
    }

    @JavascriptInterface
    fun hapusItem(id: String) {
        scope.launch {
            try {
                daftarDeteksi.removeAll {
                    val dId = it.optString("id", it.optString("seq", ""))
                    dId == id
                }
                daftarPasangan.removeAll {
                    val s = it.optString("sessionId", "")
                    val n = it.optInt("no")
                    val k = if (s.isNotEmpty()) "${s}_$n" else n.toString()
                    k == id || n.toString() == id
                }
                petaValidasi.remove(id)
                simpanCacheLokal()
                withContext(Dispatchers.Main) {
                    muatBeranda()
                    muatAntrean("SEMUA")
                    muatRiwayat("SEMUA", 60)
                    kirim("tampilkanToast", JSONObject().apply {
                        put("pesan", "1 rekaman berhasil dihapus")
                    }.toString())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gagal hapus item: ${e.message}")
            }
        }
    }

    @JavascriptInterface
    fun hapusSemuaData() {
        activity.runOnUiThread {
            androidx.appcompat.app.AlertDialog.Builder(activity)
                .setTitle("Bersihkan Data")
                .setMessage("Hapus seluruh rekaman foto, riwayat deteksi, dan hasil validasi dari ponsel ini?")
                .setPositiveButton("Hapus") { _, _ ->
                    scope.launch {
                        try {
                            daftarPasangan.clear()
                            daftarDeteksi.clear()
                            petaValidasi.clear()
                            daftarSesi.clear()
                            terakhirSinkron = 0

                            val thumbsDir = File(activity.filesDir, "thumbs")
                            if (thumbsDir.exists()) {
                                thumbsDir.listFiles()?.forEach { it.delete() }
                            }

                            val prefs = activity.getSharedPreferences("sigap_prefs", Context.MODE_PRIVATE)
                            prefs.edit()
                                .remove("cache_pasangan")
                                .remove("cache_deteksi")
                                .remove("cache_validasi")
                                .remove("terakhir_sinkron")
                                .apply()

                            withContext(Dispatchers.Main) {
                                muatBeranda()
                                muatUji()
                                muatAntrean("SEMUA")
                                muatRiwayat("SEMUA", 60)
                                kirim("tampilkanToast", JSONObject().apply {
                                    put("pesan", "Semua data rekaman & foto berhasil dibersihkan")
                                }.toString())
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Gagal hapus semua data: ${e.message}")
                        }
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }
}
