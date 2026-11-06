package com.hongguo.remote.ui.navigation

/** 应用内导航路由定义。 */
sealed class Screen(val route: String) {
    object Remote : Screen("remote")
    object Mine : Screen("mine")
    object Scan : Screen("scan")
    object Control : Screen("control/{remoteId}") {
        fun create(remoteId: Long) = "control/$remoteId"
    }
}
