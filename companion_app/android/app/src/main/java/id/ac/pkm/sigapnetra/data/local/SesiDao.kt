package id.ac.pkm.sigapnetra.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import id.ac.pkm.sigapnetra.data.model.Sesi
import id.ac.pkm.sigapnetra.data.model.StatusSesi
import kotlinx.coroutines.flow.Flow

@Dao
interface SesiDao {
    @Query("SELECT * FROM sesi ORDER BY startedAt DESC")
    fun getAllSesi(): Flow<List<Sesi>>

    @Query("SELECT * FROM sesi WHERE sessionId = :sid LIMIT 1")
    suspend fun getSesiById(sid: String): Sesi?

    @Query("SELECT * FROM sesi WHERE status = :status")
    suspend fun getSesiByStatus(status: StatusSesi): List<Sesi>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sesi: Sesi)

    @Update
    suspend fun update(sesi: Sesi)

    @Query("UPDATE sesi SET status = :status WHERE sessionId = :sid")
    suspend fun updateStatus(sid: String, status: StatusSesi)
}
