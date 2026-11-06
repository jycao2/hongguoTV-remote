package com.hongguo.remote.ui.remote

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner

/**
 * "遥控"模块空态：展示扫一扫按钮与提示文案。
 */
@Composable
fun EmptyRemoteScreen(onScan: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FilledTonalIconButton(
            onClick = onScan,
            modifier = Modifier.size(120.dp)
        ) {
            Icon(
                Icons.Default.QrCodeScanner,
                contentDescription = "扫一扫",
                modifier = Modifier.size(72.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = "扫描红果TV手机遥控二维码导入遥控界面",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}
