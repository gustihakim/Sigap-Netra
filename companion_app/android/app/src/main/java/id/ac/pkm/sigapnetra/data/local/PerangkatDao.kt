package id.ac.pkm.sigapnetra.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import id.ac.pkm.sigapnetra.data.model.Perangkat
import kotlinx.coroutines.flow.Flow

@Dao
interface PerangkatDao {
    @Query("SELECT * FROM perangkat WHERE deviceId = :devId LIMIT 1")
    suspend fun getPerangkat(devId: String): Perangkat?

    @Query("SELECT * FROM perangkat LIMIT 1")
    fun getPerangkatAktifFlow(): Flow<Perangkat?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(perangkat: Perangkat)

    @Update
    suspend fun update(perangkat: Perangkat)
}
