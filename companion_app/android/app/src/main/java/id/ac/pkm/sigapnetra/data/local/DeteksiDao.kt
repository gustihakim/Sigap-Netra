package id.ac.pkm.sigapnetra.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.ac.pkm.sigapnetra.data.model.Deteksi
import kotlinx.coroutines.flow.Flow

@Dao
interface DeteksiDao {
    @Query("SELECT * FROM deteksi ORDER BY capturedAt DESC")
    fun getAllDeteksiFlow(): Flow<List<Deteksi>>

    @Query("SELECT * FROM deteksi WHERE jenis = :jenis ORDER BY capturedAt DESC")
    fun getDeteksiByJenisFlow(jenis: String): Flow<List<Deteksi>>

    @Query("SELECT * FROM deteksi WHERE id = :id LIMIT 1")
    suspend fun getDeteksiById(id: Long): Deteksi?

    /**
     * Idempotent insert: Jika dedupHash sudah ada, abaikan tanpa error.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(deteksiList: List<Deteksi>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(deteksi: Deteksi): Long

    @Query("SELECT COUNT(*) FROM deteksi")
    suspend fun countTotal(): Int
}
