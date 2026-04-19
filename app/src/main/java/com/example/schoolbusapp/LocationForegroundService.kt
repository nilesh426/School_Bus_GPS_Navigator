package com.example.schoolbusapp

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.location.*
import com.google.firebase.database.FirebaseDatabase

class LocationForegroundService : Service() {

    private lateinit var fused: FusedLocationProviderClient
    private lateinit var request: LocationRequest
    private var callback: LocationCallback? = null

    private val channelId = "bus_location_channel"
    private val notificationId = 101
    private val TAG = "LocationService"

    private var busId: String = ""

    override fun onCreate() {
        super.onCreate()

        fused = LocationServices.getFusedLocationProviderClient(this)

        request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L
        )
            .setMinUpdateIntervalMillis(3000L)
            .build()

        createNotificationChannel()
        startForeground(notificationId, buildNotification("Sharing live location..."))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val newBusId = intent?.getStringExtra("busId") ?: ""

        Log.d(TAG, "===== SERVICE STARTED =====")
        Log.d(TAG, "Bus ID: $newBusId")

        if (newBusId.isBlank()) {
            Log.e(TAG, "Bus ID is EMPTY. Stopping service.")
            stopSelf()
            return START_NOT_STICKY
        }

        busId = newBusId

        val ref = FirebaseDatabase.getInstance().getReference("buses").child(busId)

        // Mark service started
        ref.child("serviceStarted").setValue(System.currentTimeMillis())
        ref.child("isSharing").setValue(true)

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
            Log.e(TAG, "Location permission NOT granted!")
            stopSelf()
            return
        }

        callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {

                val loc = result.lastLocation

                if (loc == null) {
                    Log.w(TAG, "Location is NULL, skipping update")
                    return
                }

                Log.d(TAG, "Lat: ${loc.latitude}, Lng: ${loc.longitude}")

                val updates = mapOf<String, Any>(
                    "lat" to loc.latitude,
                    "lng" to loc.longitude,
                    "timestamp" to System.currentTimeMillis()
                )

                FirebaseDatabase.getInstance()
                    .getReference("buses")
                    .child(busId)
                    .updateChildren(updates)
                    .addOnSuccessListener {
                        Log.d(TAG, "Location updated successfully")
                    }
                    .addOnFailureListener {
                        Log.e(TAG, "Firebase update failed: ${it.message}")
                    }
            }
        }

        try {
            fused.requestLocationUpdates(request, callback!!, Looper.getMainLooper())
        } catch (e: SecurityException) {
            Log.e(TAG, "Security Exception: ${e.message}")
            stopSelf()
        }
    }

    private fun stopLocationUpdates() {
        callback?.let {
            fused.removeLocationUpdates(it)
        }
        callback = null
    }

    private fun buildNotification(text: String): Notification {

        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("School Bus App")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Bus Location",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        Log.d(TAG, "===== SERVICE STOPPED =====")

        stopLocationUpdates()

        // Update Firebase: stop sharing
        if (busId.isNotBlank()) {
            FirebaseDatabase.getInstance()
                .getReference("buses")
                .child(busId)
                .child("isSharing")
                .setValue(false)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}