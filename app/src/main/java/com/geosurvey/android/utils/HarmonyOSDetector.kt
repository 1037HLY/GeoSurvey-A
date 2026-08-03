package com.geosurvey.android.utils

import android.os.Build

/**
 * 鸿蒙系统检测工具
 * 使用反射避免编译依赖
 */
object HarmonyOSDetector {

    private var isHarmonyOS: Boolean? = null
    private var harmonyVersion: String? = null

    /**
     * 检测是否为鸿蒙系统
     */
    fun isHarmonyOS(): Boolean {
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

            // 方法3: 通过厂商检测
            val manufacturer = Build.MANUFACTURER
            val brand = Build.BRAND
            if (manufacturer.equals("huawei", ignoreCase = true) ||
                brand.equals("huawei", ignoreCase = true)) {
                
                // 华为设备，检查系统属性
                try {
                    val version = getSystemProperty("ro.build.version.harmonyos")
                    if (!version.isNullOrEmpty()) {
                        isHarmonyOS = true
                        harmonyVersion = version
                        return true
                    }
                } catch (e: Exception) {
                    // 没有该属性
                }
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
                harmonyVersion = getSystemProperty("ro.build.version.harmonyos")
                if (harmonyVersion.isNullOrEmpty()) {
                    harmonyVersion = "未知"
                }
            } catch (e: Exception) {
                harmonyVersion = "未知"
            }
        }
        return harmonyVersion
    }

    /**
     * 反射获取系统属性
     */
    private fun getSystemProperty(key: String): String? {
        return try {
            val clazz = Class.forName("android.os.SystemProperties")
            val method = clazz.getMethod("get", String::class.java)
            method.invoke(null, key) as? String
        } catch (e: Exception) {
            null
        }
    }
}
