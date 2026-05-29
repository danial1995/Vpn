package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "clean_ips")
data class CleanIp(
    @PrimaryKey val ip: String,
    val port: Int = 2408,
    val latency: Int, // in milliseconds
    val scannedAt: Long = System.currentTimeMillis(),
    val isCloudSaved: Boolean = false,
    val packetLoss: Double = 0.0,
    val subnet: String = ""
)

@Dao
interface CleanIpDao {
    @Query("SELECT * FROM clean_ips ORDER BY latency ASC")
    fun getCleanIps(): Flow<List<CleanIp>>

    @Query("SELECT * FROM clean_ips ORDER BY latency ASC LIMIT 10")
    fun getTopCleanIps(): Flow<List<CleanIp>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIp(ip: CleanIp)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIps(ips: List<CleanIp>)

    @Query("DELETE FROM clean_ips WHERE ip = :ip")
    suspend fun deleteIp(ip: String)

    @Query("DELETE FROM clean_ips")
    suspend fun clearAll()

    @Query("UPDATE clean_ips SET isCloudSaved = 1")
    suspend fun markAllAsCloudSaved()
}

@Database(entities = [CleanIp::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cleanIpDao(): CleanIpDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "clean_vpn_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class CleanIpRepository(private val cleanIpDao: CleanIpDao) {
    val allCleanIps: Flow<List<CleanIp>> = cleanIpDao.getCleanIps()
    val topCleanIps: Flow<List<CleanIp>> = cleanIpDao.getTopCleanIps()

    suspend fun insert(cleanIp: CleanIp) {
        cleanIpDao.insertIp(cleanIp)
    }

    suspend fun insertAll(ips: List<CleanIp>) {
        cleanIpDao.insertIps(ips)
    }

    suspend fun delete(ip: String) {
        cleanIpDao.deleteIp(ip)
    }

    suspend fun clear() {
        cleanIpDao.clearAll()
    }

    suspend fun markAllAsCloudSaved() {
        cleanIpDao.markAllAsCloudSaved()
    }
}
