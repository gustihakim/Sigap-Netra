package id.ac.pkm.sigapnetra.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import id.ac.pkm.sigapnetra.data.model.Deteksi
import id.ac.pkm.sigapnetra.data.model.Perangkat
import id.ac.pkm.sigapnetra.data.model.Rekaman
import id.ac.pkm.sigapnetra.data.model.Sesi
import id.ac.pkm.sigapnetra.data.model.StatusSesi
import id.ac.pkm.sigapnetra.data.model.Validasi

@Database(
    entities = [
        Perangkat::class,
        Sesi::class,
        Deteksi::class,
        Validasi::class,
        Rekaman::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SigapDatabase : RoomDatabase() {
    abstract fun perangkatDao(): PerangkatDao
    abstract fun sesiDao(): SesiDao
    abstract fun deteksiDao(): DeteksiDao
    abstract fun validasiDao(): ValidasiDao
    abstract fun rekamanDao(): RekamanDao

    /**
     * Pola Transaksi Atomik Komit (Bagian 6):
     * Simpan status sesi TERSIMPAN dan seluruh baris deteksi dalam SATU transaksi Room.
     */
    @Transaction
    suspend fun komitDeteksi(sesi: Sesi, baris: List<Deteksi>) {
        sesiDao().insert(sesi.copy(status = StatusSesi.TERSIMPAN))
        deteksiDao().insertAll(baris)
    }

    @Transaction
    suspend fun komitRekaman(sesi: Sesi, baris: List<Rekaman>) {
        sesiDao().insert(sesi.copy(status = StatusSesi.TERSIMPAN))
        rekamanDao().insertAll(baris)
    }

    companion object {
        @Volatile
        private var INSTANCE: SigapDatabase? = null

        fun getDatabase(context: Context): SigapDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SigapDatabase::class.java,
                    "sigap_netra.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
