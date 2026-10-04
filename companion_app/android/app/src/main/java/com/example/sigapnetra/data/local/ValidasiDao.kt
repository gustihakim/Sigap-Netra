package com.example.sigapnetra.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.sigapnetra.data.model.Validasi
import kotlinx.coroutines.flow.Flow

@Dao
interface ValidasiDao {
    @Query("SELECT * FROM validasi WHERE deteksiId = :deteksiId LIMIT 1")
    suspend fun getValidasiByDeteksiId(deteksiId: Long): Validasi?

    @Query("SELECT * FROM validasi")
    fun getAllValidasiFlow(): Flow<List<Validasi>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(validasi: Validasi)

    @Update
    suspend fun update(validasi: Validasi)

    @Query("DELETE FROM validasi WHERE deteksiId = :deteksiId")
    suspend fun delete(deteksiId: Long)
}
