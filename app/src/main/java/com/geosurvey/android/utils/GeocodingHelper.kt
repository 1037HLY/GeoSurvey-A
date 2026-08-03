package com.geosurvey.android.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

/**
 * 逆地理编码工具
 * 根据经纬度获取位置名称（省市县乡镇）
 */
class GeocodingHelper(private val context: Context) {

    /**
     * 位置信息数据类
     */
    data class LocationInfo(
        val province: String = "",
        val city: String = "",
        val district: String = "",
        val town: String = "",
        val village: String = "",
        val fullName: String = ""
    )

    /**
     * 根据经纬度获取位置名称（省市县乡镇）
     */
    suspend fun getLocationName(lat: Double, lon: Double): LocationInfo {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses: List<Address>? = geocoder.getFromLocation(lat, lon, 1)

                if (addresses != null && addresses.isNotEmpty()) {
                    val address = addresses[0]
                    val province = address.adminArea ?: ""
                    val city = address.locality ?: address.subAdminArea ?: ""
                    val district = address.subLocality ?: ""
                    val town = address.thoroughfare ?: ""
                    val village = address.featureName ?: ""

                    val fullName = buildString {
                        if (province.isNotEmpty()) append(province)
                        if (city.isNotEmpty()) append(city)
                        if (district.isNotEmpty()) append(district)
                        if (town.isNotEmpty()) append(town)
                        if (village.isNotEmpty()) append(village)
                        if (isEmpty()) append("未知位置")
                    }

                    LocationInfo(
                        province = province,
                        city = city,
                        district = district,
                        town = town,
                        village = village,
                        fullName = fullName
                    )
                } else {
                    LocationInfo(fullName = "未知位置")
                }
            } catch (e: Exception) {
                LocationInfo(fullName = "获取位置失败")
            }
        }
    }

    /**
     * 获取简洁位置名称（省市县）
     */
    suspend fun getSimpleLocationName(lat: Double, lon: Double): String {
        val info = getLocationName(lat, lon)
        return info.fullName
    }
}
