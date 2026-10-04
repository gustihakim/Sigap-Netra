package id.ac.pkm.sigapnetra.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Entitas rekaman pengujian kamera jalur lama dan baru. */
@Entity(
    tableName = "rekaman",
    indices = [Index(value = ["dedupHash"], unique = true)]
)
data class Rekaman(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dedupHash: String,                 // "$deviceId|$no|$jalur"
    val deviceId: String,
    val sessionId: String,
    val no: Int,
    val detik: Double,
    val objek: String? = null,             // teks | uang | lain | null
    val jalur: String,                     // lama | baru
    val msBaca: Int,
    val ketajaman: Double,
    val berkas: String,
    val lokalPath: String? = null
)
