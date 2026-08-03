// 在 CameraScreen 函数中添加状态变量
var showCoordinates by remember { mutableStateOf(true) }
var showTime by remember { mutableStateOf(true) }
var showLocation by remember { mutableStateOf(true) }
var showNote by remember { mutableStateOf(true) }

// 在水印设置卡片中添加开关
Text(
    text = "⚙️ 水印选项",
    fontSize = 13.sp,
    fontWeight = FontWeight.SemiBold,
    color = Color(0xFF0F172A)
)
Spacer(modifier = Modifier.height(4.dp))

Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceEvenly
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = showCoordinates,
            onCheckedChange = { showCoordinates = it }
        )
        Text("坐标", fontSize = 11.sp)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = showTime,
            onCheckedChange = { showTime = it }
        )
        Text("时间", fontSize = 11.sp)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = showLocation,
            onCheckedChange = { showLocation = it }
        )
        Text("地点", fontSize = 11.sp)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = showNote,
            onCheckedChange = { showNote = it }
        )
        Text("备注", fontSize = 11.sp)
    }
}

// 优化保存速度：使用缓存和异步处理
// 在保存按钮点击时：
isSaving = true
coroutineScope.launch(Dispatchers.IO) {
    // 在IO线程执行耗时操作
    val helper = WatermarkHelper(context)
    val geocodingHelper = GeocodingHelper(context)
    val loc = location
    
    // 缓存位置名称
    val locationName = if (loc != null && showLocation) {
        runCatching {
            geocodingHelper.getSimpleLocationName(loc.latitude, loc.longitude)
        }.getOrNull() ?: ""
    } else {
        ""
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

    val config = WatermarkHelper.WatermarkConfig().apply {
        showCoordinates = showCoordinates
        showTime = showTime
        showLocation = showLocation
        showNote = showNote
        showAttitude = false
        textSize = 48f
        position = WatermarkHelper.Position.BOTTOM_RIGHT
        transparency = 0.8f
    }

    val watermarked = helper.addWatermark(capturedImage!!, data, config)
    val path = helper.saveBitmap(watermarked)

    withContext(Dispatchers.Main) {
        isSaving = false
        if (path != null) {
            cameraViewModel.savePhoto(path)
            toastMessage = "✅ 照片已保存！"
            showToast = true
            showPreview = false
            capturedImage = null
            noteText = ""
        } else {
            toastMessage = "❌ 保存失败"
            showToast = true
        }
    }
}
