package id.ac.pkm.sigapnetra.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Anotasi Manusia (Bagian 5.3) - TERPISAH dari data mentah.
 * Memungkinkan data mentah tetap utuh sebagai bukti dan mendukung validasi ulang.
 */
@Entity(tableName = "validasi")
data class Validasi(
    @PrimaryKey val deteksiId: Long,
    val putusan: String,                   // COCOK | TIDAK | RAGU
    val nilaiSebenarnya: String? = null,
    val kategoriGalat: String? = null,     // SALAH_KELAS | TERPOTONG | BURAM | CAHAYA | LAIN
    val validator: String? = null,
    val padaMs: Long = System.currentTimeMillis(),
    val catatan: String? = null
)
