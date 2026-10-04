package id.ac.pkm.sigapnetra.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Entitas anotasi validasi pengguna yang terpisah dari data mentah asli. */
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
