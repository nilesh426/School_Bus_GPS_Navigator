package com.example.schoolbusapp

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class EditRouteActivity : AppCompatActivity() {

    private lateinit var etRouteName: EditText
    private lateinit var rvStops: RecyclerView
    private lateinit var btnAddStop: Button
    private lateinit var btnSaveRoute: Button

    private val db = FirebaseFirestore.getInstance()
    private var routeId: String = ""
    private val stops = mutableListOf<Stop>()
    private lateinit var stopAdapter: StopAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_route)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        routeId = intent.getStringExtra("routeId") ?: ""

        etRouteName = findViewById(R.id.etRouteName)
        rvStops = findViewById(R.id.rvStops)
        btnAddStop = findViewById(R.id.btnAddStop)
        btnSaveRoute = findViewById(R.id.btnSaveRoute)

        rvStops.layoutManager = LinearLayoutManager(this)
        stopAdapter = StopAdapter(stops, onDeleteClick = { stop ->
            stops.remove(stop)
            stopAdapter.notifyDataSetChanged()
        })
        rvStops.adapter = stopAdapter

        if (routeId.isNotEmpty()) {
            loadRoute()
        }

        btnAddStop.setOnClickListener {
            showAddStopDialog()
        }

        btnSaveRoute.setOnClickListener {
            saveRoute()
        }
    }

    private fun loadRoute() {
        db.collection("routes").document(routeId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    etRouteName.setText(doc.getString("name") ?: "")
                    val loadedStops = (doc.get("stops") as? List<Map<String, Any>>)?.map { stopMap ->
                        Stop(
                            order = (stopMap["order"] as? Long)?.toInt() ?: 0,
                            name = stopMap["name"] as? String ?: "",
                            lat = (stopMap["lat"] as? Double) ?: 0.0,
                            lng = (stopMap["lng"] as? Double) ?: 0.0
                        )
                    } ?: emptyList()
                    stops.clear()
                    stops.addAll(loadedStops)
                    stopAdapter.notifyDataSetChanged()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load route", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showAddStopDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_stop, null)
        val etOrder = dialogView.findViewById<EditText>(R.id.etOrder)
        val etStopName = dialogView.findViewById<EditText>(R.id.etStopName)
        val etLat = dialogView.findViewById<EditText>(R.id.etLat)
        val etLng = dialogView.findViewById<EditText>(R.id.etLng)

        AlertDialog.Builder(this)
            .setTitle("Add Stop")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val order = etOrder.text.toString().toIntOrNull() ?: 0
                val name = etStopName.text.toString().trim()
                val lat = etLat.text.toString().toDoubleOrNull() ?: 0.0
                val lng = etLng.text.toString().toDoubleOrNull() ?: 0.0

                if (name.isEmpty()) {
                    Toast.makeText(this, "Enter stop name", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                stops.add(Stop(order, name, lat, lng))
                stops.sortBy { it.order }
                stopAdapter.notifyDataSetChanged()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveRoute() {
        val name = etRouteName.text.toString().trim()

        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter route name", Toast.LENGTH_SHORT).show()
            return
        }

        val stopsData = stops.map { stop ->
            mapOf(
                "order" to stop.order,
                "name" to stop.name,
                "lat" to stop.lat,
                "lng" to stop.lng
            )
        }

        val updates = mapOf(
            "name" to name,
            "stops" to stopsData
        )

        db.collection("routes").document(routeId)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Route updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error updating route", Toast.LENGTH_SHORT).show()
            }
    }
}
