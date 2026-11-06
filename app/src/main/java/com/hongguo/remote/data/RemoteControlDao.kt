package com.hongguo.remote.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RemoteControlDao {

    /** 按创建时间倒序观察所有遥控器，供 UI 实时响应变化 */
    @Query("SELECT * FROM remote_controls ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<RemoteControl>>

    @Query("SELECT * FROM remote_controls WHERE id = :id")
    suspend fun getById(id: Long): RemoteControl?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(remote: RemoteControl): Long

    @Update
    suspend fun update(remote: RemoteControl)

    @Delete
    suspend fun delete(remote: RemoteControl)
}
