package id.ac.pkm.sigapnetra.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class StatusSesi {
    UNDUH,
    TERSIMPAN,
    TERTUNDA,
    SELESAI,
    GAGAL
}

/**
 * Entitas Sesi Sinkronisasi (Bagian 5.3)
 */
@Entity(tableName = "sesi")
data class Sesi(
    @PrimaryKey val sessionId: String,
    val deviceId: String,
    val rowCount: Int,
    val sha1: String?,
    val startedAt: Long,
    val finishedAt: Long? = null,
    val status: StatusSesi
)
