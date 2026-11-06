package com.hongguo.remote.ui.mine

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.navigation.NavHostController
import com.hongguo.remote.ui.navigation.Screen
import com.hongguo.remote.ui.viewmodel.MineViewModel
import com.hongguo.remote.ui.viewmodel.UpdateState

/**
 * "我的"页面：
 * - 右上角扫一扫添加遥控器
 * - 版本信息展示，点击版本号检查更新
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MineScreen(nav: NavHostController, viewModel: MineViewModel) {
    val updateState by viewModel.updateState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的") },
                actions = {
                    IconButton(onClick = { nav.navigate(Screen.Scan.route) }) {
                        Icon(Icons.Default.CenterFocusStrong, contentDescription = "添加遥控器")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("版本信息", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.checkUpdate() },
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text("当前版本：", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            viewModel.versionName,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "点击检查更新",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    when (val s = updateState) {
                        is UpdateState.Checking -> Text("正在检查更新…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        is UpdateState.Latest -> Text("已是最新版本", color = Color(0xFF188038))
                        is UpdateState.HasUpdate -> Text("发现新版本 ${s.newVersion}", color = MaterialTheme.colorScheme.error)
                        is UpdateState.Error -> Text(s.message, color = MaterialTheme.colorScheme.error)
                        is UpdateState.Idle -> Text("版本号 $viewModel.versionName", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
