package com.hongguo.remote.ui.remote

import androidx.compose.runtime.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.hongguo.remote.qr.ParsedRemote
import com.hongguo.remote.qr.QrParser
import com.hongguo.remote.ui.common.QrScanner
import com.hongguo.remote.ui.navigation.Screen
import com.hongguo.remote.ui.viewmodel.RemoteViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * 扫码导入流程：
 * 1) 相机扫码
 * 2) 解析局域网地址，解析失败提示
 * 3) 弹出命名弹窗
 * 4) 保存后自动进入刚导入的遥控界面
 *
 * "我的"页面右上角扫码与"遥控"模块空态扫码共用此路由。
 */
@Composable
fun ScanRoute(nav: NavHostController, viewModel: RemoteViewModel) {
    var parsed by remember { mutableStateOf<ParsedRemote?>(null) }
    var parseError by remember { mutableStateOf<String?>(null) }

    // 保存成功后自动进入对应遥控界面
    LaunchedEffect(Unit) {
        viewModel.savedId.collectLatest { id ->
            if (id != null) {
                viewModel.consumeSavedId()
                nav.navigate(Screen.Control.create(id)) {
                    popUpTo(Screen.Scan.route) { inclusive = true }
                }
            }
        }
    }

    if (parsed == null) {
        QrScanner(
            onQrDetected = { raw ->
                val result = QrParser.parse(raw)
                if (result != null) parsed = result
                else parseError = "无法识别的二维码内容：\n$raw"
            },
            onClose = { nav.popBackStack() }
        )
    } else {
        NameRemoteDialog(
            suggested = parsed!!.suggestedName,
            defaultName = "${parsed!!.host}:${parsed!!.port}",
            onConfirm = { name ->
                viewModel.addFromQr(parsed!!, name)
            },
            onDismiss = { parsed = null }
        )
    }

    // 解析失败提示
    if (parseError != null) {
        AlertDialog(
            onDismissRequest = { parseError = null },
            title = { Text("二维码无效") },
            text = { Text(parseError!!) },
            confirmButton = {
                TextButton(onClick = { parseError = null }) { Text("继续扫码") }
            }
        )
    }
}
