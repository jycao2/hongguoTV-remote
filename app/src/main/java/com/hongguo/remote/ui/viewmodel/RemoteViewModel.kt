package com.hongguo.remote.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hongguo.remote.HongguoRemoteApp
import com.hongguo.remote.data.RemoteControl
import com.hongguo.remote.qr.ParsedRemote
import com.hongguo.remote.qr.toRemoteControl
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class RemoteViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as HongguoRemoteApp).repository

    /** 所有遥控器（按创建时间倒序），供"遥控"模块自动判断进入逻辑 */
    val remotes: StateFlow<List<RemoteControl>> = repository.observeRemotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 保存后新生成的遥控器 id，用于扫码后自动进入对应界面 */
    private val _savedId = MutableStateFlow<Long?>(null)
    val savedId: StateFlow<Long?> = _savedId.asStateFlow()

    fun addFromQr(parsed: ParsedRemote, name: String) {
        viewModelScope.launch {
            val id = repository.add(parsed.toRemoteControl(name))
            _savedId.value = id
        }
    }

    fun consumeSavedId() {
        _savedId.value = null
    }

    fun delete(remote: RemoteControl) {
        viewModelScope.launch { repository.remove(remote) }
    }
}
