package id.ac.pkm.sigapnetra.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.ac.pkm.sigapnetra.data.model.Pasangan
import id.ac.pkm.sigapnetra.data.model.Rekaman
import kotlinx.coroutines.flow.Flow

@Dao
interface RekamanDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(rekamanList: List<Rekaman>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(rekaman: Rekaman): Long

    @Query("SELECT * FROM rekaman ORDER BY no ASC, jalur DESC")
    fun getAllRekamanFlow(): Flow<List<Rekaman>>

    /** Query pasangan rekaman uji kamera jalur A dan B berdasarkan ID perangkat. */
    @Query("""
        SELECT l.no, l.detik, COALESCE(l.objek, b.objek) AS objek,
               l.msBaca AS msLama, b.msBaca AS msBaru,
               l.ketajaman AS tajamLama, b.ketajaman AS tajamBaru,
               l.berkas AS berkasLama, b.berkas AS berkasBaru
        FROM rekaman l
        JOIN rekaman b ON l.no = b.no AND l.deviceId = b.deviceId
        WHERE l.jalur = 'lama' AND b.jalur = 'baru' AND l.deviceId = :dev
          AND l.ketajaman >= 0 AND b.ketajaman >= 0
        ORDER BY l.no ASC
    """)
    suspend fun pasangan(dev: String): List<Pasangan>

    @Query("""
        SELECT l.no, l.detik, COALESCE(l.objek, b.objek) AS objek,
               l.msBaca AS msLama, b.msBaca AS msBaru,
               l.ketajaman AS tajamLama, b.ketajaman AS tajamBaru,
               l.berkas AS berkasLama, b.berkas AS berkasBaru
        FROM rekaman l
        JOIN rekaman b ON l.no = b.no AND l.deviceId = b.deviceId
        WHERE l.jalur = 'lama' AND b.jalur = 'baru' AND l.deviceId = :dev
          AND l.objek = :objek
          AND l.ketajaman >= 0 AND b.ketajaman >= 0
        ORDER BY l.no ASC
    """)
    suspend fun pasanganByObjek(dev: String, objek: String): List<Pasangan>
}
