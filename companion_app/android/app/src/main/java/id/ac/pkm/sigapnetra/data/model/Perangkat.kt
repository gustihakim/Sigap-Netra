package id.ac.pkm.sigapnetra.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Entitas perangkat terdaftar dengan identitas berbasis ID perangkat. */
@Entity(tableName = "perangkat")
data class Perangkat(
    @PrimaryKey val deviceId: String,
    val alias: String? = null,
    val lastHost: String? = null,
    val lastPort: Int? = null,
    val firmware: String? = null,
    val lastSyncAt: Long? = null
)
