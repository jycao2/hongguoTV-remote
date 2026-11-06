package com.hongguo.remote.ui.remote

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.hongguo.remote.ui.navigation.Screen
import com.hongguo.remote.ui.viewmodel.RemoteViewModel

/**
 * "遥控"模块入口，根据本地遥控器数量分流：
 * - 0 个：展示扫一扫导入空态
 * - ≥1 个：展示遥控器列表
 *
 * 单条数据时首次自动进入遥控界面（Control 路由，导航栏隐藏）；
 * 按返回（侧滑/返回键）回到本页出现列表，之后由用户手动进入。
 * autoEntered 以 rememberSaveable 记在该导航页上：本次进入已自动跳过则不再跳，
 * 冷启动新页面会重新自动进入。
 */
@Composable
fun RemoteModuleScreen(nav: NavHostController, viewModel: RemoteViewModel) {
    val remotes by viewModel.remotes.collectAsState()
    var autoEntered by rememberSaveable { mutableStateOf(false) }

    // 仅有一条数据且尚未自动进入过：自动进入遥控界面
    LaunchedEffect(remotes.size) {
        if (!autoEntered && remotes.size == 1) {
            autoEntered = true
            nav.navigate(Screen.Control.create(remotes.first().id))
        }
    }

    when {
        remotes.isEmpty() -> EmptyRemoteScreen(
            onScan = { nav.navigate(Screen.Scan.route) }
        )
        else -> {
            // 单条（返回后）或多条：展示列表，点击进入
            RemoteListScreen(nav = nav, remotes = remotes, viewModel = viewModel)
        }
    }
}

/**
 * 遥控控制界面路由（列表点击或单条自动进入，带返回）。
 * 返回手势（侧滑或返回键）先回退网页历史，无历史则回到列表/空态。
 */
@Composable
fun RemoteControlRoute(
    nav: NavHostController,
    remoteId: Long,
    viewModel: RemoteViewModel
) {
    val remotes by viewModel.remotes.collectAsState()
    val remote = remotes.firstOrNull { it.id == remoteId }

    if (remote == null) {
        // 遥控器已被删除，回退
        androidx.compose.runtime.LaunchedEffect(Unit) { nav.popBackStack() }
        Box(Modifier.fillMaxSize())
        return
    }

    WebRemoteScreen(remote = remote, onBack = { nav.popBackStack() })
}
