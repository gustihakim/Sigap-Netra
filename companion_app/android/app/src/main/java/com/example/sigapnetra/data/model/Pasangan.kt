package com.example.sigapnetra.data.model

data class Pasangan(
    val no: Int,
    val detik: Double,
    val objek: String?,
    val msLama: Int,
    val msBaru: Int,
    val tajamLama: Double,
    val tajamBaru: Double,
    val berkasLama: String,
    val berkasBaru: String
) {
    val menangLatensi: Boolean
        get() = msBaru < msLama

    val menangKetajaman: Boolean
        get() = tajamBaru > tajamLama
}
