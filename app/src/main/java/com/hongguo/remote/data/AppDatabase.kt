package com.hongguo.remote.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// v2: RemoteControl 增加 fullUrl（扫码得到的完整遥控页地址）；测试期直接重建
@Database(entities = [RemoteControl::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun remoteControlDao(): RemoteControlDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hongguo_remote_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
