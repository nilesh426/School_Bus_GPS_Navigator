package com.example.schoolbusapp

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.database.FirebaseDatabase

class LocationForegroundService : Service() {

    private lateinit var fused: FusedLocationProviderClient
    private lateinit var request: LocationRequest
    private var callback: LocationCallback? = null

    private val channelId = "bus_location_channel"
    private val notificationId = 101

    private var busId: String = ""

    override fun onCreate() {
        super.onCreate()

        fused = LocationServices.getFusedLocationProviderClient(this)

        request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
            .setMinUpdateIntervalMillis(3000L)
            .build()

        startForeground(notificationId, buildNotification("Sharing live location..."))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val newBusId = intent?.getStringExtra("busId") ?: ""

        if (newBusId.isBlank()) {
            stopSelf()
            return START_NOT_STICKY
        }

        busId = newBusId

        FirebaseDatabase.getInstance()
            .getReference("buses")
            .child(busId)
            .child("serviceStarted")
            .setValue(System.currentTimeMillis())

        startLocationUpdates()
        return START_STICKY
    }

    private fun startLocationUpdates() {
        if (callback != null) return

        if (
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            stopSelf()
            return
        }

        callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return

                val updates = mapOf<String, Any>(
                    "lat" to loc.latitude,
                    "lng" to loc.longitude,
                    "timestamp" to System.currentTimeMillis()
                )

                FirebaseDatabase.getInstance()
                    .getReference("buses")
                    .child(busId)
                    .updateChildren(updates)
            }
        }

        try {
            fused.requestLocationUpdates(request, callback!!, Looper.getMainLooper())
        } catch (e: SecurityException) {
            stopSelf()
        }
    }

    private fun stopLocationUpdates() {
        callback?.let { fused.removeLocationUpdates(it) }
        callback = null
    }

    private fun buildNotification(text: String): Notification {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Bus Location Sharing",
                NotificationManager.IMPORTANCE_LOW
            )
            nm.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("School Bus App")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        stopLocationUpdates()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}