package com.geosurvey.android.presentation.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geosurvey.android.GeoSurveyApplication
import com.geosurvey.android.data.model.WatermarkPhoto
import com.geosurvey.android.presentation.viewmodel.CameraViewModel
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun PhotoGalleryScreen() {
    val context = LocalContext.current
    val application = context.applicationContext as GeoSurveyApplication
    val viewModel = remember { CameraViewModel.getInstance(application) }
    val state by viewModel.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val photos = state.photos

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 标题
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🖼️ 照片画廊",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            if (photos.isNotEmpty()) {
                Text(
                    text = "${photos.size} 张",
                    fontSize = 14.sp,
                    color = Color(0xFF475569)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (photos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "🖼️",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "暂无照片",
                        fontSize = 16.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "点击「📷 相机」拍摄水印照片",
                        fontSize = 14.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(photos) { photo ->
                    PhotoGridItem(photo = photo)
                }
            }
        }

        // 删除按钮
        if (photos.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    coroutineScope.launch {
                        viewModel.deleteAllPhotos()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF4444).copy(alpha = 0.8f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("🗑️ 删除所有照片")
            }
        }
    }
}

@Composable
fun PhotoGridItem(photo: WatermarkPhoto) {
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(photo.imagePath) {
        try {
            val file = File(photo.imagePath)
            if (file.exists()) {
                bitmap = BitmapFactory.decodeFile(photo.imagePath)
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .clickable {
                // 可以查看大图
            },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF1F5F9)
        )
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "水印照片",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📷",
                    fontSize = 24.sp
                )
            }
        }
    }
}
