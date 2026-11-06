package com.hongguo.remote.ui.remote

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.hongguo.remote.data.RemoteControl
import com.hongguo.remote.ui.navigation.Screen
import com.hongguo.remote.ui.viewmodel.RemoteViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 多遥控器列表：点击进入对应遥控界面；卡片右滑露出删除滑块，删除需二次确认。
 */
@Composable
fun RemoteListScreen(
    nav: NavHostController,
    remotes: List<RemoteControl>,
    viewModel: RemoteViewModel
) {
    var pendingDelete by remember { mutableStateOf<RemoteControl?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(remotes, key = { it.id }) { remote ->
            SwipeDismissRemoteItem(
                remote = remote,
                onClick = { nav.navigate(Screen.Control.create(remote.id)) },
                onRequestDelete = { pendingDelete = remote }
            )
        }
    }

    // 删除二次确认
    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除遥控器") },
            text = { Text("确定要删除「${pendingDelete!!.name}」吗？删除后需重新扫码导入。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(pendingDelete!!)
                    pendingDelete = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            }
        )
    }
}

/**
 * 单项：卡片向右拖动露出左侧删除滑块，滑过一半松手保持展开，点击删除触发二次确认。
 * 未展开时点击整行进入遥控界面；已展开时点击整行收起。
 */
@Composable
private fun SwipeDismissRemoteItem(
    remote: RemoteControl,
    onClick: () -> Unit,
    onRequestDelete: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val maxSwipePx = with(density) { 120.dp.toPx() }
    var open by remember { mutableStateOf(false) }
    var offsetX by remember { mutableFloatStateOf(0f) }

    // 从当前位置平滑收到目标位置（0 或 maxSwipePx）
    fun settle(target: Float) {
        scope.launch {
            androidx.compose.animation.core.animate(
                initialValue = offsetX,
                targetValue = target,
                animationSpec = tween(200)
            ) { value, _ -> offsetX = value }
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        // 底层：删除滑块（卡片右滑后从左侧露出）
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color(0x22D32F2F)),
            contentAlignment = Alignment.CenterStart
        ) {
            TextButton(
                onClick = {
                    open = false
                    scope.launch { settle(0f) }
                    onRequestDelete()
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Delete, contentDescription = "删除")
                Spacer(Modifier.width(4.dp))
                Text("删除")
            }
        }

        // 上层：卡片内容，可向右拖动
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        offsetX = (offsetX + delta).coerceIn(0f, maxSwipePx)
                    },
                    onDragStopped = {
                        val target =
                            if (offsetX > maxSwipePx / 2) maxSwipePx else 0f
                        open = target != 0f
                        settle(target)
                    }
                )
                .clickable {
                    if (open) {
                        open = false
                        scope.launch { settle(0f) }
                    } else {
                        onClick()
                    }
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Tv,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(remote.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        remote.baseUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text("进入 ›", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
