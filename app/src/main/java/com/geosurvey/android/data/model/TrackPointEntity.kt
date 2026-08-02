package com.geosurvey.android.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 轨迹点实体类
 * 对应数据库表 track_points
 */
@Entity(tableName = "track_points")
data class TrackPointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val latitude: Double,           // 纬度
    val longitude: Double,          // 经度
    val altitude: Double? = null,   // 海拔 (米)
    val speed: Float? = null,       // 速度 (米/秒)
    val bearing: Float? = null,     // 方向 (度)
    val accuracy: Float? = null,    // 精度 (米)
    val satelliteCount: Int? = null, // 卫星数量
    val hdop: Float? = null,        // HDOP
    val pdop: Float? = null,        // PDOP
    val timestamp: Long = System.currentTimeMillis(), // 时间戳
    val isProcessed: Boolean = false // 是否已处理
)
