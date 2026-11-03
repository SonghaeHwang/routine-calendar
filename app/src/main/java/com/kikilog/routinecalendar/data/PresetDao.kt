package com.kikilog.routinecalendar.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PresetDao {

    @Query("SELECT * FROM preset ORDER BY position, id")
    abstract fun observeAll(): Flow<List<PresetEntity>>

    @Query("SELECT * FROM preset ORDER BY position, id")
    abstract suspend fun all(): List<PresetEntity>

    @Query("SELECT * FROM preset WHERE id = :id")
    abstract suspend fun byId(id: Long): PresetEntity?

    @Query("SELECT COALESCE(MAX(position), -1) FROM preset")
    abstract suspend fun maxPosition(): Int

    @Insert
    abstract suspend fun insert(preset: PresetEntity): Long

    @Insert
    abstract suspend fun insertAll(presets: List<PresetEntity>)

    @Update
    abstract suspend fun update(preset: PresetEntity): Int

    @Query("DELETE FROM preset WHERE id = :id")
    abstract suspend fun delete(id: Long): Int

    @Query("DELETE FROM preset")
    abstract suspend fun deleteAll()

    /** 기본값으로 되돌리기: 전부 지우고 다시 채운다(한 트랜잭션) */
    @Transaction
    open suspend fun replaceAll(presets: List<PresetEntity>) {
        deleteAll()
        insertAll(presets)
    }
}
