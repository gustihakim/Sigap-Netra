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

/** Entitas sesi sinkronisasi data antar perangkat dan aplikasi. */
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
