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

@Entity(tableName = "saved_devices")
data class SavedDeviceEntity(
    @PrimaryKey val endpoint: String, // "host:port"
    val host: String,
    val port: Int,
    val label: String,
    val lastConnectedAt: Long
)

@Entity(tableName = "command_history")
data class CommandHistoryEntity(
    @PrimaryKey val command: String,
    val lastUsedAt: Long,
    val isFavorite: Boolean = false
)

@Dao
interface AdbDao {
    @Query("SELECT * FROM saved_devices ORDER BY lastConnectedAt DESC")
    fun observeSavedDevices(): Flow<List<SavedDeviceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSavedDevice(device: SavedDeviceEntity)

    @Query("DELETE FROM saved_devices WHERE endpoint = :endpoint")
    suspend fun deleteSavedDevice(endpoint: String)

    @Query("SELECT * FROM command_history ORDER BY isFavorite DESC, lastUsedAt DESC LIMIT 50")
    fun observeCommandHistory(): Flow<List<CommandHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCommand(entry: CommandHistoryEntity)

    @Query("UPDATE command_history SET isFavorite = :favorite WHERE command = :command")
    suspend fun setCommandFavorite(command: String, favorite: Boolean)

    @Query("DELETE FROM command_history WHERE command = :command")
    suspend fun deleteCommand(command: String)
}

@Database(
    entities = [SavedDeviceEntity::class, CommandHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AdbDatabase : RoomDatabase() {
    abstract fun adbDao(): AdbDao

    companion object {
        @Volatile
        private var instance: AdbDatabase? = null

        fun getInstance(context: Context): AdbDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AdbDatabase::class.java,
                    "adb_deck.db"
                ).build().also { instance = it }
            }
        }
    }
}
