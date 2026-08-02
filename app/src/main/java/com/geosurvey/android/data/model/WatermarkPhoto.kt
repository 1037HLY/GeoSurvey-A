package com.geosurvey.android.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 水印照片实体
 */
@Entity(tableName = "watermark_photos")
data class WatermarkPhoto(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val imagePath: String,          // 照片存储路径
    val latitude: Double,           // 纬度
    val longitude: Double,          // 经度
    val altitude: Double? = null,   // 海拔
    val locationName: String = "",  // 地点名称
    val dipDirection: Float? = null, // 倾向
    val dipAngle: Float? = null,    // 倾角
    val strike: Float? = null,      // 走向
    val note: String = "",          // 备注
    val timestamp: Long = System.currentTimeMillis()
)
