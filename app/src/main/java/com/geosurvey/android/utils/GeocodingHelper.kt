package com.geosurvey.android.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class GeocodingHelper(private val context: Context) {

    data class LocationInfo(
        val province: String = "",
        val city: String = "",
        val district: String = "",
        val town: String = "",
        val village: String = "",
        val fullName: String = ""
    )

    /**
     * 检查网络是否可用
     */
    private fun isNetworkAvailable(): Boolean {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return false
                val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
                return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                return networkInfo != null && networkInfo.isConnected
            }
        } catch (e: Exception) {
            return false
        }
    }

    suspend fun getLocationName(lat: Double, lon: Double): LocationInfo {
        return withContext(Dispatchers.IO) {
            try {
                if (!isNetworkAvailable()) {
                    return@withContext LocationInfo(fullName = "无网络连接")
                }

                val geocoder = Geocoder(context, Locale.CHINA)
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
                    LocationInfo(fullName = "无法获取位置名称")
                }
            } catch (e: Exception) {
                LocationInfo(fullName = "获取位置失败: ${e.message}")
            }
        }
    }

    suspend fun getSimpleLocationName(lat: Double, lon: Double): String {
        val info = getLocationName(lat, lon)
        return info.fullName
    }
}
