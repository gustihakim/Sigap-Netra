package com.example.sigapnetra.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "deteksi",
    indices = [Index(value = ["dedupHash"], unique = true)]
)
data class Deteksi(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dedupHash: String,
    val deviceId: String,
    val sessionId: String,
    val seq: Int,
    val capturedAt: Long,
    val jenis: String,
    val terbaca: String,
    val confidence: Double,
    val jarakCm: Int? = null,
    val modelVer: String? = null,
    val latencyMs: Int? = null,
    val thumbPath: String? = null
)
