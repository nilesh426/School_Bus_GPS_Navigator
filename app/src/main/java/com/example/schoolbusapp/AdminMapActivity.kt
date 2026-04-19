package com.example.schoolbusapp

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.animation.LinearInterpolator
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore

class AdminMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private var googleMap: GoogleMap? = null
    private val busMarkers = mutableMapOf<String, Marker>()
    private val routes = mutableListOf<Route>()

    private lateinit var busRef: DatabaseReference
    private lateinit var busListener: ValueEventListener
    private lateinit var btnRecenterAdminMap: Button
    private lateinit var spRoutes: Spinner
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_map)

        btnRecenterAdminMap = findViewById(R.id.btnRecenterAdminMap)
        btnRecenterAdminMap.setOnClickListener {
            recenterAllBuses()
        }

        spRoutes = findViewById(R.id.spRoutes)
        loadRoutes()

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        startTrackingAllBuses()
    }

    private fun loadRoutes() {
        db.collection("routes")
            .get()
            .addOnSuccessListener { snapshot ->
                routes.clear()
                snapshot.documents.forEach { doc ->
                    val route = doc.toObject(Route::class.java)
                    if (route != null) {
                        routes.add(route)
                    }
                }

                val routeNames = mutableListOf("All Buses")
                routeNames.addAll(routes.map { it.name })

                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, routeNames)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spRoutes.adapter = adapter

                spRoutes.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                        if (position == 0) {
                            clearRouteMarkers()
                        } else {
                            showRouteOnMap(routes[position - 1])
                        }
                    }

                    override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                })
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load routes: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showRouteOnMap(route: Route) {
        val map = googleMap ?: return
        clearRouteMarkers()

        if (route.stops.isEmpty()) return

        val sortedStops = route.stops.sortedBy { it.order }

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

        // Draw smooth polyline connecting stops
        if (sortedStops.size > 1) {
            drawSmoothPolyline(map, sortedStops)
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



    private fun clearRouteMarkers() {
        val map = googleMap ?: return
        map.clear()
        busMarkers.clear()
        startTrackingAllBuses()
    }

    private fun startTrackingAllBuses() {
        val map = googleMap ?: return

        busRef = FirebaseDatabase.getInstance().getReference("buses")

        busListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                val existingBusIds = mutableSetOf<String>()

                for (busSnapshot in snapshot.children) {
                    val busId = busSnapshot.key ?: continue

                    val lat = busSnapshot.child("lat").getValue(Double::class.java)
                    val lng = busSnapshot.child("lng").getValue(Double::class.java)

                    if (lat == null || lng == null) continue

                    existingBusIds.add(busId)

                    val newLocation = LatLng(lat, lng)

                    if (busMarkers.containsKey(busId)) {
                        val marker = busMarkers[busId]
                        if (marker != null) {
                            animateMarker(marker, newLocation)
                        }
                    } else {
                        val marker = map.addMarker(
                            MarkerOptions()
                                .position(newLocation)
                                .title("Bus: $busId")
                        )
                        if (marker != null) {
                            busMarkers[busId] = marker
                        }
                    }
                }

                val iterator = busMarkers.iterator()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    if (!existingBusIds.contains(entry.key)) {
                        entry.value.remove()
                        iterator.remove()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        }

        busRef.addValueEventListener(busListener)
    }

    private fun animateMarker(marker: Marker, toPosition: LatLng) {
        val startPosition = marker.position

        if (startPosition == toPosition) return

        val valueAnimator = ValueAnimator.ofFloat(0f, 1f)
        valueAnimator.duration = 1000
        valueAnimator.interpolator = LinearInterpolator()

        valueAnimator.addUpdateListener { animation ->
            val fraction = animation.animatedFraction

            val lat = startPosition.latitude +
                    (toPosition.latitude - startPosition.latitude) * fraction
            val lng = startPosition.longitude +
                    (toPosition.longitude - startPosition.longitude) * fraction

            marker.position = LatLng(lat, lng)
        }

        valueAnimator.start()
    }

    private fun recenterAllBuses() {
        val map = googleMap ?: return
        if (busMarkers.isEmpty()) return

        if (busMarkers.size == 1) {
            val marker = busMarkers.values.first()
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(marker.position, 16f)
            )
            return
        }

        val boundsBuilder = LatLngBounds.Builder()
        busMarkers.values.forEach { marker ->
            boundsBuilder.include(marker.position)
        }

        val bounds = boundsBuilder.build()
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150))
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::busRef.isInitialized && ::busListener.isInitialized) {
            busRef.removeEventListener(busListener)
        }
    }
}