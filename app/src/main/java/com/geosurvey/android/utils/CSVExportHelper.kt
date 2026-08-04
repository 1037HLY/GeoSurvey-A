package com.geosurvey.android.utils

import android.content.Context
import android.os.Environment
import com.geosurvey.android.data.model.DrillSample
import com.geosurvey.android.data.model.NormalSample
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class CSVExportHelper(private val context: Context) {

    fun exportNormalSamples(samples: List<NormalSample>, filename: String = ""): File? {
        if (samples.isEmpty()) return null
        
        val sb = StringBuilder()
        sb.append("编号,名称,类型,重量(kg),纬度,经度,海拔(m),描述,时间\n")
        
        for (sample in samples) {
            sb.append("${sample.sampleNumber},${sample.sampleName},${sample.sampleType},")
            sb.append("${sample.weight},${String.format("%.6f", sample.latitude)},")
            sb.append("${String.format("%.6f", sample.longitude)},")
            sb.append("${sample.altitude?.let { String.format("%.1f", it) } ?: ""},")
            sb.append("${sample.description},${formatTime(sample.timestamp)}\n")
        }
        
        return saveToFile(sb.toString(), filename, "普通样本", "csv")
    }

    fun exportDrillSamples(samples: List<DrillSample>, filename: String = ""): File? {
        if (samples.isEmpty()) return null
        
        val sb = StringBuilder()
        sb.append("编号,名称,井深(自),井深(至),样长(m),岩心长(m),采取率(%),重量(kg),岩心直径(mm),描述,时间\n")
        
        for (sample in samples) {
            sb.append("${sample.sampleNumber},${sample.sampleName},")
            sb.append("${sample.fromDepth},${sample.toDepth},${sample.sampleLength},")
            sb.append("${sample.coreLength},${sample.recoveryRate},${sample.weight},")
            sb.append("${sample.coreDiameter},${sample.description},${formatTime(sample.timestamp)}\n")
        }
        
        return saveToFile(sb.toString(), filename, "钻孔样本", "csv")
    }

    private fun saveToFile(content: String, filename: String, prefix: String, extension: String): File? {
        return try {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "GeoSurveyToolbox/Samples"
            )
            if (!dir.exists()) {
                dir.mkdirs()
            }

            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val fileName = if (filename.isNotEmpty()) {
                "$filename.$extension"
            } else {
                "${prefix}_${sdf.format(Date())}.$extension"
            }

            val file = File(dir, fileName)
            FileOutputStream(file).use { fos ->
                fos.write(content.toByteArray(Charsets.UTF_8))
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
