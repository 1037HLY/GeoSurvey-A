package com.geosurvey.android.domain.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.geosurvey.android.GeoSurveyApplication
import com.geosurvey.android.presentation.MainActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class TrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                // 发送广播通知主应用
                val intent = Intent("LOCATION_UPDATE").apply {
                    putExtra("latitude", location.latitude)
                    putExtra("longitude", location.longitude)
                    putExtra("altitude", location.altitude ?: 0.0)
                    putExtra("speed", location.speed ?: 0f)
                    putExtra("accuracy", location.accuracy ?: 0f)
                    putExtra("bearing", location.bearing ?: 0f)
                }
                sendBroadcast(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            GeoSurveyApplication.NOTIFICATION_ID,
            createNotification()
        )
        startLocationUpdates()
        return START_STICKY
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 2000
        ).apply {
            setMinUpdateIntervalMillis(1000)
            setMaxUpdateDelayMillis(5000)
        }.build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            android.os.Looper.getMainLooper()
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, GeoSurveyApplication.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("📍 轨迹记录中")
            .setContentText("正在后台记录GPS轨迹")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
}
