package com.geosurvey.android.utils

import android.content.Context
import android.graphics.*
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class WatermarkHelper(private val context: Context) {

    data class WatermarkConfig(
        var showCoordinates: Boolean = true,
        var showTime: Boolean = true,
        var showLocation: Boolean = true,
        var showAttitude: Boolean = true,
        var showNote: Boolean = true,
        var textSize: Float = 48f,
        var position: Position = Position.BOTTOM_RIGHT,
        var transparency: Float = 0.8f
    )

    enum class Position {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }

    data class WatermarkData(
        val latitude: Double,
        val longitude: Double,
        val altitude: Double? = null,
        val locationName: String = "",
        val dipDirection: Float? = null,
        val dipAngle: Float? = null,
        val strike: Float? = null,
        val note: String = "",
        val timestamp: Long = System.currentTimeMillis()
    )

    fun addWatermark(bitmap: Bitmap, data: WatermarkData, config: WatermarkConfig = WatermarkConfig()): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        val canvas = Canvas(result)
        canvas.drawBitmap(bitmap, 0f, 0f, null)

        val paint = Paint().apply {
            color = Color.WHITE
            textSize = config.textSize
            style = Paint.Style.FILL
            alpha = (255 * config.transparency).toInt()
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(6f, 3f, 3f, Color.BLACK)
        }

        // 修复中文字体
        try {
            val typeface = Typeface.create("sans-serif", Typeface.BOLD)
            paint.typeface = typeface
        } catch (e: Exception) {
            // 使用默认字体
        }

        val lines = mutableListOf<String>()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)

        // ⭐ 只添加一次坐标
        if (config.showCoordinates) {
            lines.add("📍 ${String.format("%.6f", data.latitude)}, ${String.format("%.6f", data.longitude)}")
            data.altitude?.let {
                lines.add("🏔️ 海拔: ${String.format("%.1f", it)}m")
            }
        }

        // ⭐ 位置名称（省市县乡镇村）
        if (config.showLocation && data.locationName.isNotEmpty() && 
            !data.locationName.contains("失败") && 
            !data.locationName.contains("无网络") &&
            !data.locationName.contains("Timeout")) {
            lines.add("📍 ${data.locationName}")
        }

        if (config.showTime) {
            lines.add("🕐 ${sdf.format(Date(data.timestamp))}")
        }

        if (config.showAttitude && data.dipDirection != null) {
            lines.add("📐 倾向: ${String.format("%.1f", data.dipDirection)}° | 倾角: ${String.format("%.1f", data.dipAngle)}° | 走向: ${String.format("%.1f", data.strike)}°")
        }

        if (config.showNote && data.note.isNotEmpty()) {
            lines.add("📝 ${data.note}")
        }

        // 如果没有内容，显示默认坐标
        if (lines.isEmpty()) {
            lines.add("📍 ${String.format("%.6f", data.latitude)}, ${String.format("%.6f", data.longitude)}")
        }

        // 计算文字位置
        var currentY = when (config.position) {
            Position.TOP_LEFT, Position.TOP_RIGHT -> {
                config.textSize + 60f
            }
            else -> {
                bitmap.height - config.textSize - 60f - (lines.size - 1) * (config.textSize + 30f)
            }
        }

        val startX = when (config.position) {
            Position.TOP_LEFT, Position.BOTTOM_LEFT -> 50f
            else -> bitmap.width - 50f
        }

        // 绘制背景
        val textWidth = lines.maxByOrNull { it.length }?.let {
            paint.measureText(it)
        } ?: 300f

        val bgWidth = textWidth + 100f
        val bgHeight = lines.size * (config.textSize + 30f) + 60f

        val bgLeft = when (config.position) {
            Position.TOP_LEFT, Position.BOTTOM_LEFT -> 30f
            else -> bitmap.width - bgWidth - 30f
        }
        val bgTop = currentY - config.textSize - 30f

        canvas.drawRoundRect(bgLeft, bgTop, bgLeft + bgWidth, bgTop + bgHeight, 25f, 25f, Paint().apply {
            color = Color.argb(180, 0, 0, 0)
            style = Paint.Style.FILL
        })

        // 绘制每行文字
        lines.forEach { line ->
            val x = when (config.position) {
                Position.TOP_LEFT, Position.BOTTOM_LEFT -> startX
                else -> startX - paint.measureText(line)
            }
            canvas.drawText(line, x, currentY, paint)
            currentY += config.textSize + 30f
        }

        return result
    }

    fun saveBitmap(bitmap: Bitmap, prefix: String = "watermark"): String? {
        return try {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "GeoSurveyToolbox"
            )
            if (!dir.exists()) {
                dir.mkdirs()
            }

            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val fileName = "${prefix}_${sdf.format(Date())}.jpg"
            val file = File(dir, fileName)

            FileOutputStream(file).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos)
            }

            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
