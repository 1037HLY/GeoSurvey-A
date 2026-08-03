package com.geosurvey.android.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 普通样本
 */
@Entity(tableName = "normal_samples")
data class NormalSample(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sampleType: String = "",      // 样本类型
    val sampleNumber: String = "",    // 样本编号
    val sampleName: String = "",      // 样本名称
    val latitude: Double = 0.0,       // 采样位置纬度
    val longitude: Double = 0.0,      // 采样位置经度
    val altitude: Double? = null,     // 采样位置海拔
    val weight: String = "",          // 重量
    val description: String = "",     // 描述
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 钻孔样本
 */
@Entity(tableName = "drill_samples")
data class DrillSample(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sampleNumber: String = "",    // 编号
    val fromDepth: String = "",       // 采样井深(自)
    val toDepth: String = "",         // 采样井深(至)
    val sampleLength: String = "",    // 样长
    val coreLength: String = "",      // 岩心长
    val recoveryRate: String = "",    // 采取率
    val weight: String = "",          // 重量
    val sampleName: String = "",      // 名称
    val coreDiameter: String = "",    // 岩心直径
    val description: String = "",     // 描述
    val timestamp: Long = System.currentTimeMillis()
)
