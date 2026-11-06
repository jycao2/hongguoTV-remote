package com.hongguo.remote

import android.app.Application
import com.hongguo.remote.data.AppDatabase
import com.hongguo.remote.data.RemoteRepository

/**
 * Application 入口，提供全局唯一的数据库与仓库实例。
 */
class HongguoRemoteApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val repository: RemoteRepository by lazy { RemoteRepository(database.remoteControlDao()) }
}
