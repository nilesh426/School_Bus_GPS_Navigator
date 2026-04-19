package com.example.schoolbusapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DriverRouteMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private var googleMap: GoogleMap? = null
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var currentPolylines = mutableListOf<com.google.android.gms.maps.model.Polyline>()
    private var visitedStopIndices = mutableSetOf<Int>()
    private var currentStops = listOf<Stop>()
    private lateinit var stopDetectionManager: StopDetectionManager
    private var currentBusId: String = ""

    private companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST = 102
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_route_map)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        toolbar.setNavigationOnClickListener { finish() }

        stopDetectionManager = StopDetectionManager(this)

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST
                )
            }
        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        loadDriverRouteAndStops()
    }

    private fun loadDriverRouteAndStops() {
        val driverEmail = auth.currentUser?.email ?: return

        // Get driver's assigned bus
        db.collection("buses")
            .whereEqualTo("driverEmail", driverEmail)
            .limit(1)
            .get()
            .addOnSuccessListener { busSnapshot ->
                if (busSnapshot.isEmpty) {
                    Toast.makeText(this, "No bus assigned", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val bus = busSnapshot.documents[0].toObject(Bus::class.java) ?: return@addOnSuccessListener
                currentBusId = bus.busId // Store bus ID for stop detection
                val routeName = bus.route

                // Get the route details
                db.collection("routes")
                    .whereEqualTo("name", routeName)
                    .limit(1)
                    .get()
                    .addOnSuccessListener routeListener@{ routeSnapshot ->
                        if (routeSnapshot.isEmpty) {
                            Toast.makeText(this, "Route not found", Toast.LENGTH_SHORT).show()
                            return@routeListener
                        }

                        val route = routeSnapshot.documents[0].toObject(Route::class.java) ?: return@routeListener
                        displayRoute(route)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to load route: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load bus: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun displayRoute(route: Route) {
        val map = googleMap ?: return

        if (route.stops.isEmpty()) {
            Toast.makeText(this, "Route has no stops", Toast.LENGTH_SHORT).show()
            return
        }

        val sortedStops = route.stops.sortedBy { it.order }
        currentStops = sortedStops
        visitedStopIndices.clear()

        // Add markers for each stop with color coding
        sortedStops.forEachIndexed { index, stop ->
            val latLng = LatLng(stop.lat, stop.lng)
            
            val markerColor = when {
                index == 0 -> {
                    // START - Green
                    com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_GREEN
                }
                else -> {
                    // ALL OTHER STOPS - Blue
                    com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_BLUE
                }
            }

            map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("${stop.order}. ${stop.name}")
                    .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(markerColor))
            )
        }

        // Load driver's current location and draw dotted line to first stop
        loadDriverLocation(map, sortedStops)

        // Draw smooth polyline connecting stops
        if (sortedStops.size > 1) {
            val polyline = drawSmoothPolylineWithObject(map, sortedStops)
            currentPolylines.add(polyline)
        }

        // Center map on route
        if (sortedStops.isNotEmpty()) {
            val boundsBuilder = LatLngBounds.Builder()
            sortedStops.forEach { stop ->
                boundsBuilder.include(LatLng(stop.lat, stop.lng))
            }
            val bounds = boundsBuilder.build()
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150))
        }
    }

    private fun loadDriverLocation(map: GoogleMap, stops: List<Stop>) {
        val driverEmail = auth.currentUser?.email ?: return

        // Get driver's bus ID
        db.collection("buses")
            .whereEqualTo("driverEmail", driverEmail)
            .limit(1)
            .get()
            .addOnSuccessListener { busSnapshot ->
                if (busSnapshot.isEmpty) return@addOnSuccessListener

                val busId = busSnapshot.documents[0].getString("busId") ?: return@addOnSuccessListener

                // Get driver's current location from Realtime Database
                val busRef = com.google.firebase.database.FirebaseDatabase.getInstance()
                    .getReference("buses")
                    .child(busId)

                busRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                    override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                        val lat = snapshot.child("lat").getValue(Double::class.java)
                        val lng = snapshot.child("lng").getValue(Double::class.java)
                        val isSharing = snapshot.child("isSharing").getValue(Boolean::class.java) ?: false

                        // Only show driver location and dotted line if sharing
                        if (lat != null && lng != null && isSharing) {
                            val driverLocation = LatLng(lat, lng)

                            // Add driver marker (yellow/orange color)
                            map.addMarker(
                                MarkerOptions()
                                    .position(driverLocation)
                                    .title("Your Location")
                                    .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_ORANGE))
                            )

                            // Draw dotted line from driver to first stop
                            if (stops.isNotEmpty()) {
                                drawDottedPolyline(map, driverLocation, LatLng(stops[0].lat, stops[0].lng))
                            }

                            // Check proximity to all stops
                            checkProximityToStops(driverLocation, stops)
                        }
                    }

                    override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
                })
            }
    }

    private fun checkProximityToStops(driverLocation: LatLng, stops: List<Stop>) {
        val proximityRadius = 100.0 // 100 meters
        val map = googleMap ?: return
        var hasNewVisit = false

        // Use StopDetectionManager to check stops and send notifications
        // This now runs on background thread
        stopDetectionManager.checkAndNotifyStopsReached(driverLocation, stops, currentBusId) { reachedStop ->
            // Additional UI feedback when a stop is reached
            try {
                Toast.makeText(
                    this,
                    "⚠️ ${reachedStop.order}. ${reachedStop.name} is nearby!",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Run distance calculations on background thread
        Thread(Runnable {
            stops.forEachIndexed { index, stop ->
                val stopLocation = LatLng(stop.lat, stop.lng)
                val distance = calculateDistance(driverLocation, stopLocation)

                // If within proximity and not yet visited
                if (distance < proximityRadius && !visitedStopIndices.contains(index)) {
                    synchronized(visitedStopIndices) {
                        if (!visitedStopIndices.add(index)) {
                            hasNewVisit = true
                        }
                    }
                }
            }

            // If a new stop was visited, redraw the polylines on main thread
            if (hasNewVisit) {
                runOnUiThread {
                    try {
                        redrawPolylines(map, driverLocation, stops)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }).start()
    }

    private fun redrawPolylines(map: GoogleMap, driverLocation: LatLng, stops: List<Stop>) {
        // Clear old polylines
        currentPolylines.forEach { it.remove() }
        currentPolylines.clear()

        // Find the next unvisited stop
        val nextStopIndex = stops.indices.firstOrNull { !visitedStopIndices.contains(it) } ?: return

        // Draw new dotted line from driver to next unvisited stop
        if (nextStopIndex < stops.size) {
            val nextStop = stops[nextStopIndex]
            val nextStopLocation = LatLng(nextStop.lat, nextStop.lng)

            val dottedPolyline = drawDottedPolylineWithObject(map, driverLocation, nextStopLocation)
            currentPolylines.add(dottedPolyline)

            // Draw solid line from next stop through remaining stops
            if (nextStopIndex < stops.size - 1) {
                val remainingStops = stops.subList(nextStopIndex, stops.size)
                val solidPolyline = drawSmoothPolylineWithObject(map, remainingStops)
                currentPolylines.add(solidPolyline)
            }
        }
    }

    private fun drawDottedPolylineWithObject(map: GoogleMap, start: LatLng, end: LatLng): com.google.android.gms.maps.model.Polyline {
        val polylinePoints = mutableListOf<LatLng>()
        polylinePoints.add(start)

        val intermediatePoints = getIntermediatePoints(start, end, 5)
        polylinePoints.addAll(intermediatePoints)
        polylinePoints.add(end)

        return map.addPolyline(
            PolylineOptions()
                .addAll(polylinePoints)
                .color(android.graphics.Color.GRAY)
                .width(4f)
                .geodesic(true)
                .pattern(listOf(
                    com.google.android.gms.maps.model.Dash(10f),
                    com.google.android.gms.maps.model.Gap(10f)
                ))
        )
    }

    private fun drawSmoothPolylineWithObject(map: GoogleMap, stops: List<Stop>): com.google.android.gms.maps.model.Polyline {
        val polylinePoints = mutableListOf<LatLng>()

        for (i in 0 until stops.size) {
            val currentStop = stops[i]
            polylinePoints.add(LatLng(currentStop.lat, currentStop.lng))

            if (i < stops.size - 1) {
                val nextStop = stops[i + 1]
                val intermediatePoints = getIntermediatePoints(
                    LatLng(currentStop.lat, currentStop.lng),
                    LatLng(nextStop.lat, nextStop.lng),
                    5
                )
                polylinePoints.addAll(intermediatePoints)
            }
        }

        return map.addPolyline(
            PolylineOptions()
                .addAll(polylinePoints)
                .color(android.graphics.Color.BLUE)
                .width(6f)
                .geodesic(true)
        )
    }

    private fun calculateDistance(location1: LatLng, location2: LatLng): Double {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            location1.latitude,
            location1.longitude,
            location2.latitude,
            location2.longitude,
            results
        )
        return results[0].toDouble() // Distance in meters
    }

    private fun drawDottedPolyline(map: GoogleMap, start: LatLng, end: LatLng) {
        val polylinePoints = mutableListOf<LatLng>()
        polylinePoints.add(start)

        // Add intermediate points for smooth curve
        val intermediatePoints = getIntermediatePoints(start, end, 5)
        polylinePoints.addAll(intermediatePoints)
        polylinePoints.add(end)

        map.addPolyline(
            PolylineOptions()
                .addAll(polylinePoints)
                .color(android.graphics.Color.GRAY)  // Gray color for dotted line
                .width(4f)
                .geodesic(true)
                .pattern(listOf(
                    com.google.android.gms.maps.model.Dash(10f),
                    com.google.android.gms.maps.model.Gap(10f)
                ))
        )
    }

    private fun drawSmoothPolyline(map: GoogleMap, stops: List<Stop>) {
        val polylinePoints = mutableListOf<LatLng>()

        for (i in 0 until stops.size) {
            val currentStop = stops[i]
            polylinePoints.add(LatLng(currentStop.lat, currentStop.lng))

            // Add intermediate points between stops for smooth curve
            if (i < stops.size - 1) {
                val nextStop = stops[i + 1]
                val intermediatePoints = getIntermediatePoints(
                    LatLng(currentStop.lat, currentStop.lng),
                    LatLng(nextStop.lat, nextStop.lng),
                    5  // Number of intermediate points
                )
                polylinePoints.addAll(intermediatePoints)
            }
        }

        map.addPolyline(
            PolylineOptions()
                .addAll(polylinePoints)
                .color(android.graphics.Color.BLUE)
                .width(6f)
                .geodesic(true)
        )
        
        Toast.makeText(this, "Route loaded", Toast.LENGTH_SHORT).show()
    }

    private fun getIntermediatePoints(start: LatLng, end: LatLng, numPoints: Int): List<LatLng> {
        val points = mutableListOf<LatLng>()
        
        for (i in 1..numPoints) {
            val fraction = i.toDouble() / (numPoints + 1)
            val lat = start.latitude + (end.latitude - start.latitude) * fraction
            val lng = start.longitude + (end.longitude - start.longitude) * fraction
            points.add(LatLng(lat, lng))
        }
        
        return points
    }

    override fun onDestroy() {
        super.onDestroy()
        // Reset visited stops when activity is closed
        stopDetectionManager.resetVisitedStops()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == NOTIFICATION_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Notification permission granted
            } else {
                Toast.makeText(
                    this,
                    "Notification permission denied. Parents won't receive stop notifications.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
