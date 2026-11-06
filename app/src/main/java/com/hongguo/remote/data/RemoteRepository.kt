package com.hongguo.remote.data

import kotlinx.coroutines.flow.Flow

/**
 * 遥控器数据仓库：封装数据源访问，供 ViewModel 使用。
 */
class RemoteRepository(private val dao: RemoteControlDao) {

    fun observeRemotes(): Flow<List<RemoteControl>> = dao.observeAll()

    suspend fun getById(id: Long): RemoteControl? = dao.getById(id)

    suspend fun add(remote: RemoteControl): Long = dao.insert(remote)

    suspend fun rename(remote: RemoteControl) = dao.update(remote)

    suspend fun remove(remote: RemoteControl) = dao.delete(remote)
}
