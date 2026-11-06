package com.hongguo.remote.ui.remote

import android.util.Log
import android.net.http.SslError
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.ConsoleMessage
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.hongguo.remote.data.RemoteControl

/**
 * 遥控控制界面：直接以 WebView 加载扫码得到的遥控网页地址。
 *
 * 红果TV 二维码对应的是一个局域网网页（含会话令牌路径），
 * 页面内已包含官方的完整遥控 UI（方向键/返回/选集/菜单/语音搜索/继续观看等），
 * 直接呈现即可获得与官方一致的体验，按键协议由网页自身处理。
 * 页面语音搜索走输入法语音输入，无需 RECORD_AUDIO 权限。
 */
@Composable
fun WebRemoteScreen(remote: RemoteControl, onBack: (() -> Unit)?) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var swipeRefresh by remember { mutableStateOf<SwipeRefreshLayout?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }

    val targetUrl = remote.fullUrl.ifBlank { remote.baseUrl }

    BackHandler(enabled = onBack != null) {
        val wv = webView
        if (wv != null && wv.canGoBack()) wv.goBack() else onBack?.invoke()
    }

    if (loadError != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("无法连接到电视", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "请确认手机与电视在同一局域网，且电视端遥控服务已开启。\n$loadError",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                loadError = null
                webView?.reload()
            }) { Text("重试") }
            if (onBack != null) {
                Spacer(Modifier.height(8.dp))
                Button(onClick = onBack) { Text("返回") }
            }
        }
        return
    }

    // 下拉刷新：SwipeRefreshLayout 原生感知 WebView 滚动位置（仅在页面顶部可下拉），
    // Compose 的 PullToRefreshBox 对 AndroidView 内的 WebView 手势不生效，故不用
    val refreshColor = MaterialTheme.colorScheme.primary.toArgb()
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            // 临时开启远程调试：可用 chrome://inspect 查看页面控制台定位问题
            WebView.setWebContentsDebuggingEnabled(true)
            val wv = WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.loadsImagesAutomatically = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.javaScriptCanOpenWindowsAutomatically = true
                // 去掉 WebView 特有的 "; wv)" 标记，避免页面按 WebView 降级渲染
                settings.userAgentString =
                    WebSettings.getDefaultUserAgent(ctx).replace("; wv)", "")
                // 页面为 http，海报等资源可能来自 https CDN（或反过来），放行混合内容
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                // 接受第三方 Cookie，保证会话与图片鉴权正常
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String?) {
                        swipeRefresh?.isRefreshing = false
                        // 兼容老内核（如 Chrome 99）：html/body 的 height:100% 会被
                        // 解析为 0，导致整页 flex 布局塌陷、"继续观看"等 overflow:auto
                        // 区域被裁剪为不可见。改用像素高度并在视口变化时重设。
                        view.evaluateJavascript(LEGACY_KERNEL_FIX_JS, null)
                    }

                    // 仅主文档加载失败才显示错误页；子资源失败只记日志，
                    // 避免单个图片 404 导致整页被错误页替换
                    override fun onReceivedError(
                        view: WebView,
                        request: WebResourceRequest,
                        error: WebResourceError
                    ) {
                        if (request.isForMainFrame) {
                            loadError = error.description?.toString()
                        } else {
                            Log.w(
                                "HgWebView",
                                "resource failed: ${request.url} ${error.description}"
                            )
                        }
                    }

                    // 局域网工具场景：证书异常时放行并记录，避免海报 CDN 证书链问题
                    override fun onReceivedSslError(
                        view: WebView,
                        handler: SslErrorHandler,
                        error: SslError?
                    ) {
                        Log.w("HgWebView", "ssl error: $error, proceed")
                        handler.proceed()
                    }
                }

                    webChromeClient = object : WebChromeClient() {
                        // 页面 JS 日志输出到 logcat（tag: HgWebView），便于定位渲染问题
                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            Log.d(
                                "HgWebView",
                                "[${consoleMessage?.messageLevel()}] ${consoleMessage?.message()} " +
                                    "(${consoleMessage?.sourceId()}:${consoleMessage?.lineNumber()})"
                            )
                            return true
                        }
                    }
                loadUrl(targetUrl)
            }
            webView = wv

            SwipeRefreshLayout(ctx).apply {
                addView(
                    wv,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                setColorSchemeColors(refreshColor)
                setOnRefreshListener {
                    loadError = null
                    wv.reload()
                }
            }.also { swipeRefresh = it }
        },
        update = { /* URL 固定，无需更新 */ }
    )
}

/**
 * 老内核（如 Chrome 99）修复，两类塌陷：
 * 1. html/body 的 height:100% 被解析为 0，整页 flex 布局塌陷，
 *    overflow:auto 区域（如"继续观看"）被裁剪为不可见 → 用像素高度替代；
 * 2. vh 单位被解析为 0，如 .sheet{max-height:82vh} 导致弹层抽屉高度为 0
 *    内容不可见 → 扫描样式表把 vh 值按当前视口换算成像素后覆盖。
 * 两项均监听 resize/orientationchange 重算。
 */
private const val LEGACY_KERNEL_FIX_JS =
    "(function(){" +
        "function fixH(){var h=window.innerHeight+'px';" +
        "document.documentElement.style.height=h;" +
        "document.body.style.height=h;}" +
        "function fixVh(){" +
        "var out='';" +
        "for(var s=0;s<document.styleSheets.length;s++){" +
        "var rules;try{rules=document.styleSheets[s].cssRules}catch(e){continue}" +
        "for(var i=0;i<rules.length;i++){" +
        "var st=rules[i].style;if(!st)continue;" +
        "var add='';" +
        "for(var p=0;p<st.length;p++){" +
        "var name=st[p],val=st.getPropertyValue(name);" +
        "if(/[0-9.]+vh/.test(val)){" +
        "var px=val.replace(/([0-9.]+)vh/g,function(m,n){" +
        "return (parseFloat(n)*window.innerHeight/100)+'px'});" +
        "add+=name+':'+px+' !important;'}}" +
        "if(add&&rules[i].selectorText)out+=rules[i].selectorText+'{'+add+'}'}}" +
        "var el=document.getElementById('hg-vh-fix');" +
        "if(!el){el=document.createElement('style');el.id='hg-vh-fix';" +
        "document.head.appendChild(el);}" +
        "el.textContent=out;}" +
        "function fixAll(){fixH();fixVh();}" +
        "fixAll();" +
        "window.addEventListener('resize',fixAll);" +
        "window.addEventListener('orientationchange',fixAll);" +
    "})();"
