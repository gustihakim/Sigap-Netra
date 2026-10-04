package id.ac.pkm.sigapnetra.util

import kotlin.math.pow

data class HasilUjiStatistik(
    val menang: Int,
    val total: Int,
    val pValue: Double?,
    val label: String,
    val medianLama: Double,
    val medianBaru: Double
)

object UjiTanda {
    private fun kombinasi(n: Int, k: Int): Double {
        if (k < 0 || k > n) return 0.0
        var r = 1.0
        for (i in 1..k) {
            r = r * (n - k + i) / i
        }
        return r
    }

    /** Menghitung nilai p uji tanda dua arah (two-sided sign test). */
    fun p(menang: Int, total: Int): Double {
        if (total == 0) return 1.0
        val k = maxOf(menang, total - menang)
        var ekor = 0.0
        for (i in k..total) {
            ekor += kombinasi(total, i)
        }
        return minOf(1.0, 2.0 * ekor / 2.0.pow(total.toDouble()))
    }

    /** Menghitung median dari sekumpulan nilai desimal. */
    fun median(list: List<Double>): Double {
        if (list.isEmpty()) return 0.0
        val sorted = list.sorted()
        val size = sorted.size
        return if (size % 2 == 0) {
            (sorted[size / 2 - 1] + sorted[size / 2]) / 2.0
        } else {
            sorted[size / 2]
        }
    }

    /** Evaluasi pengujian statistik uji tanda dua arah berdasarkan signifikansi. */
    fun evaluasi(menang: Int, total: Int, dataLama: List<Double>, dataBaru: List<Double>): HasilUjiStatistik {
        val medLama = median(dataLama)
        val medBaru = median(dataBaru)

        if (total < 6) {
            return HasilUjiStatistik(
                menang = menang,
                total = total,
                pValue = null,
                label = "Jumlah data belum cukup (n < 6)",
                medianLama = medLama,
                medianBaru = medBaru
            )
        }

        val pVal = p(menang, total)
        val labelHasil = if (pVal < 0.05) "Terbukti" else "Belum cukup"

        return HasilUjiStatistik(
            menang = menang,
            total = total,
            pValue = pVal,
            label = labelHasil,
            medianLama = medLama,
            medianBaru = medBaru
        )
    }
}
