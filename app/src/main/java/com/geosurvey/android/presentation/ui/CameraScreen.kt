package com.geosurvey.android.presentation.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Looper
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.geosurvey.android.GeoSurveyApplication
import com.geosurvey.android.presentation.viewmodel.CameraViewModel
import com.geosurvey.android.utils.GeocodingHelper
import com.geosurvey.android.utils.WatermarkHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val application = context.applicationContext as GeoSurveyApplication
    val lifecycleOwner = context as? LifecycleOwner

    val cameraViewModel = remember { CameraViewModel.getInstance(application) }
    val state by cameraViewModel.state.collectAsState()

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var location by remember { mutableStateOf<android.location.Location?>(null) }
    var capturedImage by remember { mutableStateOf<Bitmap?>(null) }
    var showPreview by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf("") }
    var locationNameText by remember { mutableStateOf("") }
    var showToast by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraExecutor by remember { mutableStateOf<ExecutorService?>(null) }

    val coroutineScope = rememberCoroutineScope()

    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let {
                    location = it
                    cameraViewModel.updateLocation(it)
                }
            }
        }
    }

    val cameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
    val fineLocationPermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    )

    LaunchedEffect(Unit) {
        if (fineLocationPermission == PackageManager.PERMISSION_GRANTED) {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 2000
            ).apply {
                setMinUpdateIntervalMillis(1000)
                setMaxUpdateDelayMillis(5000)
            }.build()
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                fusedLocationClient.removeLocationUpdates(locationCallback)
                cameraExecutor?.shutdown()
            } catch (e: Exception) { }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "📷 水印相机",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .clip(RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A2E)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            if (cameraPermission == PackageManager.PERMISSION_GRANTED && lifecycleOwner != null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).apply {
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                cameraProviderFuture.addListener({
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(surfaceProvider)
                                    }

                                    imageCapture = ImageCapture.Builder()
                                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                        .build()

                                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                                    try {
                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            cameraSelector,
                                            preview,
                                            imageCapture
                                        )
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }, ContextCompat.getMainExecutor(ctx))
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        FloatingActionButton(
                            onClick = {
                                imageCapture?.takePicture(
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(image: ImageProxy) {
                                            val bitmap = imageProxyToBitmap(image)
                                            image.close()
                                            if (bitmap != null) {
                                                capturedImage = bitmap
                                                showPreview = true
                                            }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            toastMessage = "拍照失败: ${exception.message}"
                                            showToast = true
                                        }
                                    }
                                )
                            },
                            containerColor = Color.White,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(30.dp))
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(27.dp))
                                        .background(Color(0xFF0EA5E9))
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = if (location != null) "📍 已定位" else "⏳ 定位中...",
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "📷",
                            fontSize = 48.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "需要相机权限",
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = "🖊️ 水印信息",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (location != null) {
                        Text(
                            text = "📍 ${String.format("%.6f", location!!.latitude)}, ${String.format("%.6f", location!!.longitude)}",
                            fontSize = 11.sp,
                            color = Color(0xFF475569)
                        )
                    }
                }

                OutlinedTextField(
                    value = noteText,
                    onValueChange = {
                        noteText = it
                        cameraViewModel.updateNote(it)
                    },
                    label = { Text("备注") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (showPreview && capturedImage != null) {
                        Button(
                            onClick = {
                                isSaving = true
                                coroutineScope.launch {
                                    val helper = WatermarkHelper(context)
                                    val geocodingHelper = GeocodingHelper(context)
                                    
                                    // ⭐ 直接从 location 变量获取位置
                                    val loc = location
                                    
                                    // 获取位置名称
                                    val locationName = if (loc != null) {
                                        try {
                                            geocodingHelper.getSimpleLocationName(loc.latitude, loc.longitude)
                                        } catch (e: Exception) {
                                            "获取位置失败"
                                        }
                                    } else {
                                        "未知位置"
                                    }

                                    val data = WatermarkHelper.WatermarkData(
                                        latitude = loc?.latitude ?: 0.0,
                                        longitude = loc?.longitude ?: 0.0,
                                        altitude = loc?.altitude,
                                        locationName = locationName,
                                        dipDirection = state.dipDirection,
                                        dipAngle = state.dipAngle,
                                        strike = state.strike,
                                        note = noteText,
                                        timestamp = System.currentTimeMillis()
                                    )

                                    val config = WatermarkHelper.WatermarkConfig(
                                        showCoordinates = true,
                                        showTime = true,
                                        showLocation = true,
                                        showAttitude = false,
                                        showNote = true,
                                        textSize = 56f,
                                        position = WatermarkHelper.Position.BOTTOM_RIGHT,
                                        transparency = 0.8f
                                    )

                                    val watermarked = helper.addWatermark(capturedImage!!, data, config)
                                    val path = helper.saveBitmap(watermarked)

                                    withContext(Dispatchers.Main) {
                                        isSaving = false
                                        if (path != null) {
                                            val success = cameraViewModel.savePhoto(path)
                                            if (success) {
                                                toastMessage = "✅ 照片已保存！"
                                                showToast = true
                                                showPreview = false
                                                capturedImage = null
                                                noteText = ""
                                            } else {
                                                toastMessage = "❌ 保存失败"
                                                showToast = true
                                            }
                                        } else {
                                            toastMessage = "❌ 保存失败"
                                            showToast = true
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isSaving
                        ) {
                            Text(if (isSaving) "保存中..." else "✅ 保存")
                        }

                        Button(
                            onClick = {
                                showPreview = false
                                capturedImage = null
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEF4444)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("❌ 取消")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (showToast) {
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(2500)
                showToast = false
            }
            Snackbar(
                modifier = Modifier.padding(8.dp),
                containerColor = when {
                    toastMessage.contains("✅") -> Color(0xFF10B981)
                    toastMessage.contains("❌") -> Color(0xFFEF4444)
                    else -> Color(0xFFF59E0B)
                }
            ) {
                Text(toastMessage, color = Color.White)
            }
        }
    }
}

fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}
