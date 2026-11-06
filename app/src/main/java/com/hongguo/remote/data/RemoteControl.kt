package com.hongguo.remote.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 遥控器数据实体，保存在本地 Room 数据库。
 *
 * @param id        自增主键
 * @param name      用户命名的遥控器名称（如"客厅电视"）
 * @param host      局域网设备 IP（如 192.168.31.38）
 * @param port      设备端口（如 37579）
 * @param baseUrl   拼接后的基础地址 http://host:port
 * @param fullUrl   扫码得到的完整遥控页地址（含会话令牌路径），遥控界面直接加载它
 * @param createdAt 创建时间戳，用于排序
 */
@Entity(tableName = "remote_controls")
data class RemoteControl(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val host: String,
    val port: Int,
    val baseUrl: String,
    val fullUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
