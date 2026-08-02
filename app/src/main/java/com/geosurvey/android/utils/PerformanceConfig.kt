package com.geosurvey.android.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

/**
 * 性能优化配置
 * 根据设备性能动态调整应用行为
 */
object PerformanceConfig {
    /**
     * 根据设备CPU核心数调整UI更新频率
     * @return 更新间隔（毫秒）
     */
    fun getUpdateInterval(): Long {
        val cpuCount = Runtime.getRuntime().availableProcessors()
        return when {
            cpuCount >= 8 -> 500L   // 高性能设备（8核以上）
            cpuCount >= 4 -> 1000L  // 中端设备（4-8核）
            else -> 2000L           // 低端设备（4核以下）
        }
    }

    /**
     * 检查系统是否启用动画
     * @param context 上下文
     * @return 是否启用动画
     */
    fun isAnimationEnabled(context: Context): Boolean {
        val settings = context.contentResolver
        val value = android.provider.Settings.Global.getFloat(
            settings,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
        return value > 0f
    }

    /**
     * 触感反馈（震动）
     * @param context 上下文
     * @param duration 震动时长（毫秒）
     */
    fun vibrate(context: Context, duration: Long = 50) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }
}
