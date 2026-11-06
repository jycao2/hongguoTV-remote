# 红果遥控（Hongguo Remote）

扫描红果TV 电视上「手机遥控」的二维码，直接在本机打开官方遥控页，并把多台电视的遥控入口统一管理起来。

项目包含两条产品线：

| 产品线 | 状态 | 入口 |
| --- | --- | --- |
| **H5 / PWA**（`web/`） | 主线，持续迭代 | https://hongguo-remote-h5.app.workbuddy.host/?sharecode=G3AWH3OYSU |
| **Android 原生**（`app/`） | v1.0.9 已交付，转为离线备份 | `红果遥控器-v1.0.9-release.apk` |

两条线的交互模型一致：扫码 → 命名 → 保存 → 点条目进遥控页。核心体验都是 **WebView / 浏览器直接加载扫码得到的那条局域网网址**——红果TV 的遥控 UI、按键协议、语音搜索全部由该网页自身实现，本项目管理的是「入口地址」而不是按键协议。

## 功能

- **扫码导入**：扫描电视上 `http://<电视IP>:<端口>/<会话令牌>` 形式的二维码，解析出完整地址后保存；支持 `hongguo://remote?host=&port=&name=` 与纯 `host:port` 两种兼容格式
- **多遥控器管理**：列表展示名称与地址，支持命名、重名去重（同地址更新名称）、删除（二次确认）
- **单条自动进入**：只有一台遥控器时直接进入遥控页并隐藏底部导航，返回后回到列表
- **遥控页**：加载官方网页，支持下拉刷新、物理/侧滑返回时先走网页历史再退出、加载失败显示「无法连接到电视」错误页并可重试
- **添加到桌面（H5）**：底部引导条 + 分步教程 + 悬浮按钮三层引导；按 UA 识别夸克 / UC / QQ / 小米 / OPPO / vivo / 华为 / Edge / 微信内置 / iOS Safari，各给真实菜单路径；Android Chrome 拦截 `beforeinstallprompt` 直接拉起系统安装框
- **拍照识别兜底（H5）**：摄像头不可用的浏览器（典型是夸克）可用 `<label for>` 调起系统相机拍一张，再用 jsQR 从照片解码
- **WiFi 前置提示**：空态页与扫码页都提示「手机需与电视连接同一 WiFi」

## 快速开始

### H5

`web/template.html` 是唯一需要手改的源文件，`web/index.html` 是构建产物（单文件全内联，扫码库、图标、PWA manifest 都在里面）。改完模板后重新构建：

```bash
# 在项目根目录执行：模板 + jsqr.js → 单文件 index.html
python web/build.py
```

本地预览（摄像头在 localhost 属于安全上下文，可直接调起）：

```bash
python -m http.server 8000 -d web
```

构建后校验内联脚本语法（模板改动容易漏括号）：

```bash
python -c "import re;h=open('web/index.html',encoding='utf-8').read();open('_app_check.js','w',encoding='utf-8').write(re.findall(r'<script>(.*?)</script>',h,re.S)[-1])" && node --check _app_check.js && rm -f _app_check.js
```

发布：把 `web/` 目录作为静态站点发布到内置托管服务（domainPrefix 固定为 `hongguo-remote-h5`，域名不变）。纯静态发布不消耗资源点，链接长期有效，只有手动下线/删除才会失效。

### Android

工作区没有 `gradlew`，用本机缓存的 Gradle 8.14.3 直接构建（首次会自动下载 AGP 与依赖）：

```bash
"$USERPROFILE/.gradle/wrapper/dists/gradle-8.14.3-bin/cv11ve7ro1n3o1j4so8xd9n66/gradle-8.14.3/bin/gradle" assembleRelease --no-daemon
```

产物按 ABI 分包，输出在 `app/build/outputs/apk/release/`：

| 文件 | 适用 | 体积 |
| --- | --- | --- |
| `app-arm64-v8a-release.apk` | 主流手机（交付版） | 7.5 MB |
| `app-armeabi-v7a-release.apk` | 老 32 位设备 | 6.0 MB |
| `app-x86_64-release.apk` | 模拟器 | 8.7 MB |

```bash
adb install -r app/build/outputs/apk/release/app-arm64-v8a-release.apk
```

## 目录结构

```
hongguo_remote/
├── app/                                  Android 模块
│   ├── build.gradle.kts                  版本号 / 签名 / 混淆 / ABI 分包
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml           仅 CAMERA / INTERNET / ACCESS_NETWORK_STATE
│       ├── res/                          图标、主题、字符串
│       └── java/com/hongguo/remote/
│           ├── MainActivity.kt           Scaffold + 底部导航 + NavHost 四个路由
│           ├── data/                     Room：实体 / DAO / 数据库 / 仓库
│           ├── qr/QrParser.kt            二维码三种格式解析
│           ├── ui/common/QrScanner.kt    CameraX + ML Kit 扫码界面
│           ├── ui/remote/                空态 / 列表 / 命名弹窗 / 扫码路由 / WebView 遥控页
│           ├── ui/mine/MineScreen.kt     「我的」：扫一扫、版本号
│           ├── ui/viewmodel/             RemoteViewModel / MineViewModel
│           └── ui/theme/                 深色主题
├── web/                                  H5 / PWA
│   ├── template.html                     源文件（含 __JSQR__ / __ICON180__ / __MANIFEST__ 占位符）
│   ├── jsqr.js                           纯 JS 二维码解码库（构建时内联）
│   ├── build.py                          构建脚本
│   └── index.html                        产物：单文件 315 KB，可直接丢到任意静态托管
├── keystore/hongguo_remote.jks           签名密钥（已在 .gitignore 中排除）
├── keystore.properties                   密钥口令与别名（同样不入库）
└── 红果遥控器-v1.0.9-release.apk         交付包（arm64）
```

## 技术栈

| 层 | 选型 |
| --- | --- |
| 语言 / 构建 | Kotlin 2.0.21、AGP 8.5.2、KSP 2.0.21-1.0.28、JDK 17 |
| UI | Jetpack Compose（BOM 2024.09.00）、Material3、Navigation Compose 2.7.7 |
| 存储 | Room 2.6.1（Android）／ localStorage（H5，键 `hg_remotes_v1`） |
| 扫码 | ML Kit barcode-scanning 17.2.0 + CameraX 1.3.1（Android）／ jsQR（H5） |
| 遥控页 | WebView（Android）／ 直接跳转页面（H5） |
| SDK | compileSdk 34、minSdk 24、targetSdk 34；versionCode 10、versionName 1.0.9 |

## 二维码格式

`qr/QrParser.kt` 按前缀分派三种格式，解析结果统一为 `ParsedRemote(host, port, fullUrl, suggestedName)`：

| 输入 | 处理 |
| --- | --- |
| `http://192.168.31.38:37579/2ac602bc…` | 红果TV 实际格式。保留完整 URL（含会话令牌路径），端口缺省按 80 |
| `hongguo://remote?host=…&port=…&name=…` | 自定义 scheme，拼成 `http://host:port/`，端口缺省 8080 |
| `192.168.1.50:8080` | 纯文本，补 `http://` 前缀 |

H5 侧的 `parseQr` 只做轻量校验：`http(s)://` 开头直接采用，`host:port/path` 形式自动补协议，其余判为「不是有效的红果TV遥控二维码」。

## 数据存储

Android 用 Room，实体 `RemoteControl` 字段为 `id / name / host / port / baseUrl / fullUrl`（DB v2 新增 `fullUrl`，用于保存带令牌的完整地址）。H5 用 localStorage 存 JSON 数组，结构与上表对齐（`name / url / ts`）。

`baseUrl` 仅作展示与兜底，实际加载优先 `fullUrl`——少了令牌路径，官方遥控页会拒绝连接。

## 实现要点与踩坑

红果TV 遥控页要在 **Chrome 99 级别的老内核**（MIUI WebView、夸克、X5 等）上正常显示，踩过的坑按「现象 → 根因 → 对策」记录如下。这部分是复用时最需要知道的信息。

| 现象 | 根因 | 对策 |
| --- | --- | --- |
| 「继续观看」卡片整块不可见 | 老内核把 `html,body{height:100%}` 解析为 0，整条 flex 链塌陷，`overflow:auto` 区域被裁掉 | `WebRemoteScreen.kt` 注入 `LEGACY_KERNEL_FIX_JS`，改用 `window.innerHeight` 像素高度 |
| 「更多」底部抽屉高度为 0 | 同源问题，`vh` 单位也被解析为 0（如 `max-height:82vh`） | 同一段注入脚本扫描 `document.styleSheets`，把含 `vh` 的声明按视口换算成像素后 `!important` 覆盖（`#hg-vh-fix`），`resize`/`orientationchange` 时重算 |
| 夸克打开扫码页只有黑底和取景框 | 夸克不支持 CSS `inset:0` 简写，全屏容器与 `<video>` 尺寸塌成 0 | H5 全部改用 `top/left/right/bottom` + `width/height:100%` 长写法，`<video>` 补 `webkit-playsinline` |
| 夸克扫码一直停在「请求权限」 | 夸克 `getUserMedia` 是假实现：既不回调也不报错；老式 `webkitGetUserMedia` 同样无响应 | 8s（后收紧到 5s）超时 + `{video:true}` 降级重试 + 拍照识别兜底 + 「复制链接，用系统浏览器打开」逃生按钮 |
| 夸克点拍照按钮毫无反应 | `display:none` 的 `<input type=file>` 被调 `click()` 时被静默忽略 | 改 `<label for="filePick">` 原生转发触发，配合离屏可见（`top:-300px` + `opacity:0`）的文件框，并去掉 `capture` 属性 |
| 从遥控页侧滑返回，落在扫码页 | 跳转瞬间扫码浮层正开着，浏览器 bfcache 原样恢复快照（摄像头已断，只剩黑浮层） | 三道防线：`finishAdd` 跳转前先 `closeScan()`；`pagehide` 时 `resetOverlays()`；`pageshow` 恢复时再重置一次 |
| 夸克进遥控页提示「请求超时」，多刷几次才进 | 夸克云加速把请求绕到自家云端代理，`192.168.x.x` 局域网地址走代理必然超时 | 页面代码无法绕过，改为检测夸克 UA 时显示说明卡（可关闭），指引关云加速或换系统浏览器 |
| 腾讯移动应用引擎提示「未检测到安全信息」 | 只识别 V1(JAR) 签名的安全扫描 | `app/build.gradle.kts` 显式 `enableV1Signing = true`（V2/V3 同时开启） |
| APK 32.5 MB 过大 | Compose 图标库 + ML Kit 各架构 `.so` 全打包 | R8 混淆 + `shrinkResources` + ABI splits，总体降到 7.5 MB（-77%） |
| Compose 编译器版本错配 | Kotlin 1.9.x 与 Compose compiler 扩展版本不对应 | 升级到 Kotlin 2.0.21 并使用 `org.jetbrains.kotlin.plugin.compose`（编译器内置于 Kotlin 版本） |
| `PullToRefreshBox` 下拉无反应 | WebView 不参与 Compose 嵌套滚动 | 换 `SwipeRefreshLayout` 包裹 WebView |

其他实现细节：

- **UA 去标记**：加载遥控页前把 WebView UA 里的 `; wv)` 去掉，避免页面按 WebView 降级渲染
- **混合内容与 Cookie**：`mixedContentMode = ALWAYS_ALLOW`、接受第三方 Cookie，否则 http 页面里的 https 海报 CDN 与图片鉴权会失败
- **错误页只在主文档失败时出现**：`onReceivedError` 判断 `isForMainFrame`，单张图片 404 不至于整页被替换
- **权限极简**：只有 CAMERA / INTERNET / ACCESS_NETWORK_STATE。页面语音搜索走输入法语音输入，不需要 RECORD_AUDIO（v1.0.9 已移除）
- **navigation 显隐**：扫码页与遥控页属于全屏子页面，`showBottomBar` 只在这两个主路由为 true；单条遥控器自动进入时导航栏也随之隐藏

## 签名与发版

签名配置从 `keystore.properties` 读取（文件不存在时自动跳过签名，可正常做 debug 构建）：

```
storeFile=keystore/hongguo_remote.jks
keyAlias=hongguo_remote
```

流程：改 `app/build.gradle.kts` 里的 `versionCode` / `versionName` → 构建 release → 取 `arm64-v8a` 包 → `adb install -r`。

密钥文件必须单独备份：同名应用后续所有更新包都依赖这一个 keystore，丢了就无法覆盖安装。

## 已知限制

- **必须同一局域网**：遥控地址是电视的内网 IP，手机与电视不在同一 WiFi 下必然连不上
- **夸克类内置浏览器能力受限**：摄像头与文件选择器均可能被产品级封锁，只能走拍照识别或换浏览器；云加速还会代理局域网请求
- **扫码令牌有时效**：电视端重启或刷新二维码后，旧地址会失效，需要重新扫码导入
- **H5 依赖托管链接**：链接本身长期有效，但若要彻底解耦，`web/index.html` 是单文件、可整体迁移到任意 HTTPS 静态托管；Android 版即为此场景的离线备份
