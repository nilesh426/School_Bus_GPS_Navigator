package com.example.schoolbusapp

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.concurrent.thread

class StopDetectionManager(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val notificationHelper = NotificationHelper(context)
    private val visitedStops = mutableSetOf<String>()
    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        // Radius in meters - when driver is within this distance from stop
        private const val STOP_PROXIMITY_RADIUS = 150.0
    }

    /**
     * Check if driver is near any stop and send notification to parents
     * @param driverLocation Current driver location
     * @param stops List of stops on the route
     * @param busId ID of the bus
     */
    fun checkAndNotifyStopsReached(
        driverLocation: LatLng,
        stops: List<Stop>,
        busId: String,
        onStopReached: ((Stop) -> Unit)? = null
    ) {
        // Run distance calculation on background thread to avoid blocking UI
        thread {
            stops.forEach { stop ->
                val stopKey = "${busId}_${stop.order}_${stop.name}"
                val distance = calculateDistance(driverLocation, LatLng(stop.lat, stop.lng))

                // If within proximity and not yet notified
                if (distance < STOP_PROXIMITY_RADIUS && !visitedStops.contains(stopKey)) {
                    synchronized(visitedStops) {
                        if (!visitedStops.contains(stopKey)) {
                            visitedStops.add(stopKey)
                            
                            // Send notifications on background thread
                            sendNotificationsToParents(stop, busId)
                            
                            // Post callback to main thread
                            mainHandler.post {
                                onStopReached?.invoke(stop)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Send notifications to all parents whose children are on this bus
     */
    private fun sendNotificationsToParents(stop: Stop, busId: String) {
        // Get all students on this bus (runs on background thread)
        db.collection("students")
            .whereEqualTo("busId", busId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                // Send notifications on background thread to avoid blocking UI
                thread {
                    try {
                        querySnapshot.documents.forEach { doc ->
                            val studentName = doc.getString("name") ?: "Student"
                            val parentUid = doc.getString("parentUid") ?: return@forEach

                            // Send notification to parent
                            notificationHelper.sendStopNotification(
                                stopName = stop.name,
                                busId = busId,
                                parentUid = parentUid,
                                studentName = studentName
                            )
                            // Add to Firestore for in-app notification panel
                            val notificationData = hashMapOf(
                                "text" to "Bus $busId reached ${stop.name}",
                                "timestamp" to System.currentTimeMillis()
                            )
                            db.collection("users")
                                .document(parentUid)
                                .collection("notifications")
                                .add(notificationData)
                            
                            // Small delay to prevent notification spam
                            Thread.sleep(100)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            .addOnFailureListener { e ->
                // Handle error silently to not interrupt driver experience
                e.printStackTrace()
            }
    }

    /**
     * Reset visited stops (call when driver starts new route or day)
     */
    fun resetVisitedStops() {
        visitedStops.clear()
    }

    /**
     * Calculate distance between two locations in meters
     */
    private fun calculateDistance(location1: LatLng, location2: LatLng): Double {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            location1.latitude,
            location1.longitude,
            location2.latitude,
            location2.longitude,
            results
        )
        return results[0].toDouble()
    }

    /**
     * Check if driver has reached a specific stop
     */
    fun hasReachedStop(driverLocation: LatLng, stop: Stop): Boolean {
        val distance = calculateDistance(driverLocation, LatLng(stop.lat, stop.lng))
        return distance < STOP_PROXIMITY_RADIUS
    }
}
