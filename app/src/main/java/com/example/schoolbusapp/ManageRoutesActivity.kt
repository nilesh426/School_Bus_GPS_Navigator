package com.example.schoolbusapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class ManageRoutesActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    private val allRoutes = mutableListOf<Route>()
    private val filteredRoutes = mutableListOf<Route>()

    private lateinit var adapter: RouteAdapter
    private lateinit var etSearchRoute: EditText
    private lateinit var rvRoutes: RecyclerView
    private lateinit var tvEmptyRoutes: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_routes)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        toolbar.setNavigationOnClickListener {
            finish()
        }
        toolbar.inflateMenu(R.menu.menu_add)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_add -> {
                    startActivity(Intent(this, AddRouteActivity::class.java))
                    true
                }
                else -> false
            }
        }

        etSearchRoute = findViewById(R.id.etSearchRoute)
        rvRoutes = findViewById(R.id.rvRoutes)
        tvEmptyRoutes = findViewById(R.id.tvEmptyRoutes)

        rvRoutes.layoutManager = LinearLayoutManager(this)

        adapter = RouteAdapter(
            filteredRoutes,
            onEditClick = { route ->
                val intent = Intent(this, EditRouteActivity::class.java)
                intent.putExtra("routeId", route.id)
                startActivity(intent)
            },
            onDeleteClick = { route ->
                showDeleteConfirmationDialog(route)
            },
            onAssignClick = { route ->
                assignRouteToBus(route)
            }
        )

        rvRoutes.adapter = adapter

        etSearchRoute.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterRoutes(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })

        loadRoutes()
    }

    private fun loadRoutes() {
        db.collection("routes")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Toast.makeText(this, "Error: ${err.message}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }

                allRoutes.clear()

                snap?.documents?.forEach { doc ->
                    val stops = (doc.get("stops") as? List<Map<String, Any>>)?.map { stopMap ->
                        Stop(
                            order = (stopMap["order"] as? Long)?.toInt() ?: 0,
                            name = stopMap["name"] as? String ?: "",
                            lat = (stopMap["lat"] as? Double) ?: 0.0,
                            lng = (stopMap["lng"] as? Double) ?: 0.0
                        )
                    } ?: emptyList()

                    allRoutes.add(
                        Route(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            stops = stops
                        )
                    )
                }

                filterRoutes(etSearchRoute.text.toString())
            }
    }

    private fun filterRoutes(query: String) {
        filteredRoutes.clear()

        if (query.isBlank()) {
            filteredRoutes.addAll(allRoutes)
        } else {
            val searchText = query.trim().lowercase()
            for (route in allRoutes) {
                if (route.name.lowercase().contains(searchText)) {
                    filteredRoutes.add(route)
                }
            }
        }

        adapter.notifyDataSetChanged()
        updateEmptyState(query)
    }

    private fun updateEmptyState(query: String) {
        if (filteredRoutes.isEmpty()) {
            rvRoutes.visibility = View.GONE
            tvEmptyRoutes.visibility = View.VISIBLE

            tvEmptyRoutes.text = if (query.isBlank()) {
                "No routes available"
            } else {
                "No routes match your search"
            }
        } else {
            rvRoutes.visibility = View.VISIBLE
            tvEmptyRoutes.visibility = View.GONE
        }
    }

    private fun showDeleteConfirmationDialog(route: Route) {
        AlertDialog.Builder(this)
            .setTitle("Delete Route")
            .setMessage("Are you sure you want to delete route ${route.name}?")
            .setPositiveButton("Yes") { _, _ ->
                deleteRoute(route)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deleteRoute(route: Route) {
        db.collection("routes")
            .document(route.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Route deleted successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to delete route: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun assignRouteToBus(route: Route) {
        // Load buses and show selection dialog
        db.collection("buses")
            .get()
            .addOnSuccessListener { snap ->
                val buses = mutableListOf<Bus>()
                snap.documents.forEach { doc ->
                    buses.add(
                        Bus(
                            id = doc.id,
                            busId = doc.getString("busId") ?: "",
                            route = doc.getString("route") ?: "",
                            driverEmail = doc.getString("driverEmail") ?: "",
                            routeId = doc.getString("routeId") ?: ""
                        )
                    )
                }

                if (buses.isEmpty()) {
                    Toast.makeText(this, "No buses available", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val busNames = buses.map { "${it.busId} (${it.driverEmail})" }.toTypedArray()

                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Assign Route to Bus")
                    .setItems(busNames) { _, which ->
                        val selectedBus = buses[which]
                        assignRouteToSelectedBus(route, selectedBus)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load buses", Toast.LENGTH_SHORT).show()
            }
    }

    private fun assignRouteToSelectedBus(route: Route, bus: Bus) {
        val updates = mapOf(
            "routeId" to route.id,
            "route" to route.name  // Also update the route name for compatibility
        )

        db.collection("buses").document(bus.id)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Route assigned to bus ${bus.busId}", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to assign route", Toast.LENGTH_SHORT).show()
            }
    }
}
