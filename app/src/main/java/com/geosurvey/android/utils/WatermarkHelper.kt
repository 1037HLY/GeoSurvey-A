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
        val showCoordinates: Boolean = true,
        val showTime: Boolean = true,
        val showLocation: Boolean = true,
        val showAttitude: Boolean = true,
        val showNote: Boolean = true,
        val textSize: Float = 40f,
        val position: Position = Position.BOTTOM_RIGHT,
        val transparency: Float = 0.7f
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
            setShadowLayer(4f, 2f, 2f, Color.BLACK)
        }

        val lines = mutableListOf<String>()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        if (config.showCoordinates) {
            lines.add("📍 ${String.format("%.6f", data.latitude)}, ${String.format("%.6f", data.longitude)}")
            data.altitude?.let {
                lines.add("🏔️ 海拔: ${String.format("%.1f", it)}m")
            }
        }

        if (config.showTime) {
            lines.add("🕐 ${sdf.format(Date(data.timestamp))}")
        }

        if (config.showLocation && data.locationName.isNotEmpty()) {
            lines.add("📍 ${data.locationName}")
        }

        if (config.showAttitude && data.dipDirection != null) {
            lines.add("📐 倾向: ${String.format("%.1f", data.dipDirection)}° | 倾角: ${String.format("%.1f", data.dipAngle)}° | 走向: ${String.format("%.1f", data.strike)}°")
        }

        if (config.showNote && data.note.isNotEmpty()) {
            lines.add("📝 ${data.note}")
        }

        // 计算文字位置
        var currentY = when (config.position) {
            Position.TOP_LEFT, Position.TOP_RIGHT -> {
                config.textSize + 40f
            }
            else -> {
                bitmap.height - config.textSize - 40f - (lines.size - 1) * (config.textSize + 20f)
            }
        }

        val startX = when (config.position) {
            Position.TOP_LEFT, Position.BOTTOM_LEFT -> 40f
            else -> bitmap.width - 40f
        }

        // 绘制背景
        val textWidth = lines.maxByOrNull { it.length }?.let {
            paint.measureText(it)
        } ?: 200f

        val bgWidth = textWidth + 80f
        val bgHeight = lines.size * (config.textSize + 20f) + 40f

        val bgLeft = when (config.position) {
            Position.TOP_LEFT, Position.BOTTOM_LEFT -> 20f
            else -> bitmap.width - bgWidth - 20f
        }
        val bgTop = currentY - config.textSize - 20f

        canvas.drawRoundRect(bgLeft, bgTop, bgLeft + bgWidth, bgTop + bgHeight, 20f, 20f, Paint().apply {
            color = Color.argb(150, 0, 0, 0)
            style = Paint.Style.FILL
        })

        // 绘制每行文字
        lines.forEach { line ->
            val x = when (config.position) {
                Position.TOP_LEFT, Position.BOTTOM_LEFT -> startX
                else -> startX - paint.measureText(line)
            }
            canvas.drawText(line, x, currentY, paint)
            currentY += config.textSize + 20f
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
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
            }

            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
