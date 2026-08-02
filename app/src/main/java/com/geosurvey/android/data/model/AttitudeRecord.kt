package com.geosurvey.android.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 地质产状记录实体
 */
@Entity(tableName = "attitude_records")
data class AttitudeRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dipDirection: Float,      // 倾向 (0-360度)
    val dipAngle: Float,          // 倾角 (0-90度)
    val strike: Float,            // 走向 (0-360度)
    val latitude: Double,         // 纬度
    val longitude: Double,        // 经度
    val altitude: Double? = null, // 海拔
    val accuracy: Float? = null,  // 定位精度
    val note: String = "",        // 备注
    val timestamp: Long = System.currentTimeMillis()
)
