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
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class ViewBusesActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    private val allBuses = mutableListOf<Bus>()
    private val filteredBuses = mutableListOf<Bus>()

    private lateinit var adapter: BusAdapter
    private lateinit var etSearchBus: EditText
    private lateinit var rvBuses: RecyclerView
    private lateinit var tvEmptyBuses: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_buses)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        etSearchBus = findViewById(R.id.etSearchBus)
        rvBuses = findViewById(R.id.rvBuses)
        tvEmptyBuses = findViewById(R.id.tvEmptyBuses)

        rvBuses.layoutManager = LinearLayoutManager(this)

        adapter = BusAdapter(
            filteredBuses,
            onEditClick = { bus ->
                val intent = Intent(this, EditBusActivity::class.java)
                intent.putExtra("busId", bus.busId)
                intent.putExtra("route", bus.route)
                intent.putExtra("driverEmail", bus.driverEmail)
                startActivity(intent)
            },
            onDeleteClick = { bus ->
                showDeleteConfirmationDialog(bus)
            }
        )

        rvBuses.adapter = adapter

        etSearchBus.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterBuses(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })

        loadBuses()
    }

    private fun loadBuses() {
        db.collection("buses")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Toast.makeText(this, "Error: ${err.message}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }

                allBuses.clear()

                snap?.documents?.forEach { doc ->
                    allBuses.add(
                        Bus(
                            id = doc.id,
                            busId = doc.getString("busId") ?: "",
                            route = doc.getString("route") ?: "",
                            driverEmail = doc.getString("driverEmail") ?: ""
                        )
                    )
                }

                filterBuses(etSearchBus.text.toString())
            }
    }

    private fun filterBuses(query: String) {
        filteredBuses.clear()

        if (query.isBlank()) {
            filteredBuses.addAll(allBuses)
        } else {
            val searchText = query.trim().lowercase()
            for (bus in allBuses) {
                if (bus.busId.lowercase().contains(searchText) ||
                    bus.route.lowercase().contains(searchText)
                ) {
                    filteredBuses.add(bus)
                }
            }
        }

        adapter.notifyDataSetChanged()
        updateEmptyState(query)
    }

    private fun updateEmptyState(query: String) {
        if (filteredBuses.isEmpty()) {
            rvBuses.visibility = View.GONE
            tvEmptyBuses.visibility = View.VISIBLE

            tvEmptyBuses.text = if (query.isBlank()) {
                "No buses available"
            } else {
                "No buses match your search"
            }
        } else {
            rvBuses.visibility = View.VISIBLE
            tvEmptyBuses.visibility = View.GONE
        }
    }

    private fun showDeleteConfirmationDialog(bus: Bus) {
        AlertDialog.Builder(this)
            .setTitle("Delete Bus")
            .setMessage("Are you sure you want to delete bus ${bus.busId}?")
            .setPositiveButton("Yes") { _, _ ->
                deleteBus(bus)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deleteBus(bus: Bus) {
        db.collection("buses")
            .document(bus.busId)
            .delete()
            .addOnSuccessListener {
                FirebaseDatabase.getInstance()
                    .getReference("buses")
                    .child(bus.busId)
                    .removeValue()

                Toast.makeText(this, "Bus deleted successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to delete bus: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}