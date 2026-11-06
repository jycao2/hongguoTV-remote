package com.hongguo.remote.ui.remote

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 扫码成功后命名遥控器的弹窗。
 *
 * @param suggested  二维码中携带的推荐名称（可为空）
 * @param defaultName 默认名称（如 host:port）
 * @param onConfirm  确认并保存
 * @param onDismiss  取消
 */
@Composable
fun NameRemoteDialog(
    suggested: String?,
    defaultName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(suggested ?: defaultName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("命名遥控器") },
        text = {
            Column {
                Text("为该红果TV设备起一个名字，便于识别。")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("遥控器名称") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.ifBlank { defaultName }) },
                enabled = name.isNotBlank()
            ) {
                Text("完成")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
