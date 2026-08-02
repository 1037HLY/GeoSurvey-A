package com.geosurvey.android.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 坐标记录实体
 */
@Entity(tableName = "coordinate_records")
data class CoordinateRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val system: String = "WGS84",  // WGS84, CGCS2000, GCJ02, BD09
    val gaussX: Double? = null,    // 高斯投影北坐标
    val gaussY: Double? = null,    // 高斯投影东坐标
    val zone: Int? = null,         // 带号
    val locationName: String = "", // 地点名称
    val note: String = "",         // 备注
    val timestamp: Long = System.currentTimeMillis()
)
