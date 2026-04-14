package com.example.schoolbusapp

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.animation.LinearInterpolator
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AdminMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private var googleMap: GoogleMap? = null
    private val busMarkers = mutableMapOf<String, Marker>()

    private lateinit var busRef: DatabaseReference
    private lateinit var busListener: ValueEventListener
    private lateinit var btnRecenterAdminMap: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_map)

        btnRecenterAdminMap = findViewById(R.id.btnRecenterAdminMap)
        btnRecenterAdminMap.setOnClickListener {
            recenterAllBuses()
        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
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