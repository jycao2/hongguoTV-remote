package com.hongguo.remote.ui.common

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

/** 取景框边长 */
private val FrameSize = 260.dp
/** 取景框整体相对屏幕中心向上偏移，给底部提示文字留空间 */
private val FrameUpOffset = 60.dp
/** 成功/扫描线强调色 */
private val AccentGreen = Color(0xFF3DDC84)

/**
 * 二维码扫描全屏组件：取景框 + 扫描线动画，识别成功后取景框中央出现绿点。
 *
 * @param onQrDetected 识别到二维码内容后回调（仅触发一次）
 * @param onClose      点击关闭按钮回调
 */
@Composable
fun QrScanner(
    onQrDetected: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var detected by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    Box(modifier.fillMaxSize().background(Color.Black)) {
        if (hasPermission) {
            CameraPreview(
                onQrDetected = { value ->
                    detected = true
                    onQrDetected(value)
                }
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("需要相机权限才能扫码", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                    Text("授权相机")
                }
            }
        }

        // 取景框遮罩 + 扫描线 + 成功绿点
        ScannerOverlay(detected = detected, modifier = Modifier.matchParentSize())

        // 顶部关闭按钮
        TextButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
        ) {
            Text("✕ 关闭", color = Color.White)
        }

        // 框下方提示
        Text(
            if (detected) "识别成功" else "将二维码放入框内",
            color = if (detected) AccentGreen else Color.White,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = FrameSize / 2 + 40.dp)
        )
    }
}

/** 半透明遮罩挖空取景框，绘制四角括号与扫描线动画，识别成功时中央画绿点。 */
@Composable
private fun ScannerOverlay(detected: Boolean, modifier: Modifier = Modifier) {
    val scanLine = rememberInfiniteTransition(label = "scan").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1800, easing = LinearEasing)
        ),
        label = "scanLine"
    )

    Canvas(modifier) {
        val frame = FrameSize.toPx()
        val centerX = size.width / 2
        val centerY = size.height / 2 - FrameUpOffset.toPx()
        val left = centerX - frame / 2
        val top = centerY - frame / 2
        val right = centerX + frame / 2
        val bottom = centerY + frame / 2

        // 半透明遮罩，中间挖空取景框
        val mask = Path().apply {
            addRect(Rect(0f, 0f, size.width, size.height))
            addRect(Rect(left, top, right, bottom))
        }
        clipPath(mask, ClipOp.Difference) {
            drawRect(Color.Black.copy(alpha = 0.45f))
        }

        // 四角括号
        val corner = 28.dp.toPx()
        val stroke = 4.dp.toPx()
        val white = Color.White
        // 左上
        drawLine(white, Offset(left, top + corner), Offset(left, top), stroke)
        drawLine(white, Offset(left, top), Offset(left + corner, top), stroke)
        // 右上
        drawLine(white, Offset(right - corner, top), Offset(right, top), stroke)
        drawLine(white, Offset(right, top), Offset(right, top + corner), stroke)
        // 左下
        drawLine(white, Offset(left, bottom - corner), Offset(left, bottom), stroke)
        drawLine(white, Offset(left, bottom), Offset(left + corner, bottom), stroke)
        // 右下
        drawLine(white, Offset(right - corner, bottom), Offset(right, bottom), stroke)
        drawLine(white, Offset(right, bottom), Offset(right, bottom - corner), stroke)

        if (!detected) {
            // 扫描线：在框内上下往复
            val y = top + (bottom - top) * scanLine.value
            drawLine(
                AccentGreen,
                Offset(left + 10f, y),
                Offset(right - 10f, y),
                3.dp.toPx()
            )
        } else {
            // 识别成功：取景框中央绿点
            drawCircle(AccentGreen, radius = 12.dp.toPx(), center = Offset(centerX, centerY))
        }
    }
}

@Composable
private fun CameraPreview(onQrDetected: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scanner = remember { BarcodeScanning.getClient() }
    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }
    var scanned by remember { mutableStateOf(false) }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(analyzerExecutor) { imageProxy ->
                            if (scanned) {
                                imageProxy.close()
                                return@setAnalyzer
                            }
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )
                                scanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        for (barcode in barcodes) {
                                            barcode.rawValue?.let { value ->
                                                scanned = true
                                                onQrDetected(value)
                                            }
                                        }
                                    }
                                    .addOnCompleteListener { imageProxy.close() }
                            } else {
                                imageProxy.close()
                            }
                        }
                    }

                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}
