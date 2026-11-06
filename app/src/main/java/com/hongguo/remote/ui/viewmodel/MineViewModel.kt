package com.hongguo.remote.ui.viewmodel

import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** "我的"页面检查更新状态。 */
sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    object Latest : UpdateState()
    data class HasUpdate(val newVersion: String, val url: String) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

class MineViewModel(application: Application) : AndroidViewModel(application) {

    val versionName: String
    val versionCode: Long

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState

    init {
        val pm = application.packageManager
        val info = pm.getPackageInfo(application.packageName, 0)
        versionName = info.versionName ?: "1.0.0"
        versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
    }

    /** 点击版本号检查更新（占位实现，需接入真实更新接口）。 */
    fun checkUpdate() {
        if (_updateState.value is UpdateState.Checking) return
        _updateState.value = UpdateState.Checking
        viewModelScope.launch {
            // TODO: 替换为真实版本检查接口（如 GET https://api.example.com/version）
            delay(1200)
            _updateState.value = UpdateState.Latest
        }
    }

    fun resetUpdateState() {
        _updateState.value = UpdateState.Idle
    }
}
