package com.hongguo.remote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hongguo.remote.ui.mine.MineScreen
import com.hongguo.remote.ui.navigation.Screen
import com.hongguo.remote.ui.remote.RemoteControlRoute
import com.hongguo.remote.ui.remote.RemoteModuleScreen
import com.hongguo.remote.ui.remote.ScanRoute
import com.hongguo.remote.ui.theme.HongguoRemoteTheme
import com.hongguo.remote.ui.viewmodel.MineViewModel
import com.hongguo.remote.ui.viewmodel.RemoteViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HongguoRemoteTheme {
                AppRoot()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppRoot() {
    val nav = rememberNavController()
    val remoteVm: RemoteViewModel = viewModel()
    val mineVm: MineViewModel = viewModel()

    val navBackStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // 扫码 / 控制界面为全屏子页面，不显示底部导航
    val showBottomBar = currentRoute in listOf(Screen.Remote.route, Screen.Mine.route)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    val items = listOf(
                        BottomItem(Screen.Remote.route, "遥控", Icons.Default.Tv),
                        BottomItem(Screen.Mine.route, "我的", Icons.Default.Person)
                    )
                    val currentDestination = navBackStackEntry?.destination
                    items.forEach { item ->
                        NavigationBarItem(
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                nav.navigate(item.route) {
                                    popUpTo(nav.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Screen.Remote.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            composable(Screen.Remote.route) {
                RemoteModuleScreen(nav, remoteVm)
            }
            composable(Screen.Mine.route) {
                MineScreen(nav, mineVm)
            }
            composable(Screen.Scan.route) {
                ScanRoute(nav, remoteVm)
            }
            composable(Screen.Control.route) { backStack ->
                val id = backStack.arguments?.getString("remoteId")?.toLongOrNull() ?: 0L
                RemoteControlRoute(nav, id, remoteVm)
            }
        }
    }
}

private data class BottomItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)
