package com.example.schoolbusapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat

class NotificationHelper(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val stopChannelId = "stop_notification_channel"
    private val sharingChannelId = "bus_sharing_channel"
    private val stopChannelName = "Bus Stop Notifications"
    private val sharingChannelName = "Bus Sharing Status"
    private val notificationBaseId = 1000
    private val sharingNotificationId = 2000

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Stop notification channel
            val stopChannel = NotificationChannel(stopChannelId, stopChannelName, NotificationManager.IMPORTANCE_HIGH)
            stopChannel.description = "Notifications when bus reaches a stop"
            notificationManager.createNotificationChannel(stopChannel)

            // Sharing status channel
            val sharingChannel = NotificationChannel(sharingChannelId, sharingChannelName, NotificationManager.IMPORTANCE_DEFAULT)
            sharingChannel.description = "Bus sharing status notifications"
            notificationManager.createNotificationChannel(sharingChannel)
        }
    }

    fun sendStopNotification(
        stopName: String,
        busId: String,
        parentUid: String,
        studentName: String
    ) {
        // Check notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val notificationId = (notificationBaseId + stopName.hashCode()).coerceIn(1, Int.MAX_VALUE)

        val intent = Intent(context, ParentDashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, stopChannelId)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("🚌 Bus Reached Stop")
            .setContentText("$busId has reached $stopName")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Your child's bus has reached $stopName.\nStudent: $studentName"))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun sendBusStartedSharingNotification(busId: String) {
        // Check notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val intent = Intent(context, ParentDashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP

        val pendingIntent = PendingIntent.getActivity(
            context,
            sharingNotificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, sharingChannelId)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("🚌 Bus Sharing Live Location")
            .setContentText("Bus $busId started sharing live location")
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .build()

        notificationManager.notify(sharingNotificationId, notification)
    }

    fun sendBusStoppedSharingNotification(busId: String) {
        // Check notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val intent = Intent(context, ParentDashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP

        val pendingIntent = PendingIntent.getActivity(
            context,
            sharingNotificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, sharingChannelId)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("⏹️ Bus Stopped Sharing")
            .setContentText("Bus $busId stopped sharing location")
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .build()

        notificationManager.notify(sharingNotificationId, notification)
    }

    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    fun cancelSharingNotification() {
        notificationManager.cancel(sharingNotificationId)
    }
}

