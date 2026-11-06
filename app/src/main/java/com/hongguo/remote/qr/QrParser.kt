package com.hongguo.remote.qr

import android.net.Uri
import com.hongguo.remote.data.RemoteControl

/**
 * 二维码解析结果。
 *
 * @param host           设备局域网 IP，如 192.168.31.38
 * @param port           设备端口，如 37579
 * @param fullUrl        扫码得到的完整地址（可能含会话令牌路径），
 *                       如 http://192.168.31.38:37579/2ac602bc7075bc4...
 * @param suggestedName  二维码中可选携带的推荐名称（可能为 null）
 */
data class ParsedRemote(
    val host: String,
    val port: Int,
    val fullUrl: String,
    val suggestedName: String? = null
)

/**
 * 红果TV 手机遥控二维码解析器。
 *
 * 兼容以下二维码内容格式：
 * 1) 标准 URL：      http://192.168.31.38:37579/2ac602bc7075bc4...（红果TV 实际格式，含会话路径）
 * 2) 自定义 scheme： hongguo://remote?host=192.168.1.50&port=8080&name=客厅电视
 * 3) 纯文本：        192.168.1.50:8080
 *
 * 解析失败返回 null。
 */
object QrParser {

    fun parse(raw: String): ParsedRemote? {
        val text = raw.trim()
        if (text.isEmpty()) return null
        return when {
            text.startsWith("hongguo://") -> parseScheme(text)
            text.startsWith("http://") || text.startsWith("https://") -> parseUrl(text)
            else -> parsePlain(text)
        }
    }

    private fun parseUrl(url: String): ParsedRemote? {
        val uri = Uri.parse(url)
        val host = uri.host ?: return null
        if (host.isBlank()) return null
        val port = if (uri.port != -1) uri.port else 80
        val name = uri.getQueryParameter("name")
        return ParsedRemote(host, port, url, name)
    }

    private fun parseScheme(s: String): ParsedRemote? {
        val uri = Uri.parse(s)
        val host = uri.getQueryParameter("host") ?: return null
        if (host.isBlank()) return null
        val port = uri.getQueryParameter("port")?.toIntOrNull() ?: 8080
        val name = uri.getQueryParameter("name")
        return ParsedRemote(host, port, "http://$host:$port/", name)
    }

    private fun parsePlain(s: String): ParsedRemote? {
        // 形如 192.168.1.50:8080
        val parts = s.split(":")
        if (parts.size == 2) {
            val host = parts[0].trim()
            val port = parts[1].trim().toIntOrNull() ?: return null
            if (host.isNotBlank()) {
                return ParsedRemote(host, port, "http://$host:$port/")
            }
        }
        return null
    }
}

/** 将解析结果转换为可保存的 [RemoteControl] 实体。 */
fun ParsedRemote.toRemoteControl(name: String): RemoteControl {
    return RemoteControl(
        name = name,
        host = host,
        port = port,
        baseUrl = "http://$host:$port",
        fullUrl = fullUrl
    )
}
