package com.geosurvey.android.utils

import android.content.Context
import android.os.Build
import android.os.SystemProperties
import java.lang.reflect.Method

/**
 * 鸿蒙系统检测工具
 */
object HarmonyOSDetector {

    private var isHarmonyOS: Boolean? = null
    private var harmonyVersion: String? = null

    /**
     * 检测是否为鸿蒙系统
     */
    fun isHarmonyOS(context: Context? = null): Boolean {
        if (isHarmonyOS != null) {
            return isHarmonyOS!!
        }

        try {
            // 方法1: 通过Build.DISPLAY检测
            if (Build.DISPLAY.contains("HarmonyOS", ignoreCase = true)) {
                isHarmonyOS = true
                return true
            }

            // 方法2: 通过Build.VERSION.INCREMENTAL检测
            if (Build.VERSION.INCREMENTAL.contains("HarmonyOS", ignoreCase = true)) {
                isHarmonyOS = true
                return true
            }

            // 方法3: 通过厂商检测（华为设备）
            val manufacturer = Build.MANUFACTURER
            val brand = Build.BRAND
            if (manufacturer.equals("huawei", ignoreCase = true) ||
                brand.equals("huawei", ignoreCase = true)) {
                // 华为设备，进一步检测
                try {
                    val cls = Class.forName("com.huawei.system.BuildEx")
                    val getOsType = cls.getDeclaredMethod("getOsType")
                    val osType = getOsType.invoke(null) as? String
                    if (osType != null && osType.contains("HarmonyOS", ignoreCase = true)) {
                        isHarmonyOS = true
                        return true
                    }
                } catch (e: Exception) {
                    // 没有鸿蒙API
                }
            }

            // 方法4: 通过系统属性检测
            try {
                val get = Class.forName("android.os.SystemProperties")
                    .getDeclaredMethod("get", String::class.java)
                val version = get.invoke(null, "ro.build.version.harmonyos") as? String
                if (!version.isNullOrEmpty()) {
                    isHarmonyOS = true
                    harmonyVersion = version
                    return true
                }
            } catch (e: Exception) {
                // 没有该属性
            }

            isHarmonyOS = false
            return false
        } catch (e: Exception) {
            isHarmonyOS = false
            return false
        }
    }

    /**
     * 获取鸿蒙版本
     */
    fun getHarmonyVersion(): String? {
        if (harmonyVersion == null) {
            try {
                val get = Class.forName("android.os.SystemProperties")
                    .getDeclaredMethod("get", String::class.java)
                harmonyVersion = get.invoke(null, "ro.build.version.harmonyos") as? String
            } catch (e: Exception) {
                harmonyVersion = "未知"
            }
        }
        return harmonyVersion
    }

    /**
     * 是否为纯鸿蒙应用环境（非兼容模式）
     */
    fun isPureHarmony(): Boolean {
        return try {
            val cls = Class.forName("com.huawei.system.BuildEx")
            val getOsType = cls.getDeclaredMethod("getOsType")
            val osType = getOsType.invoke(null) as? String
            osType != null && osType.equals("HarmonyOS", ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }
}
