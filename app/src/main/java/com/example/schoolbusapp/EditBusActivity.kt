package com.example.schoolbusapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.firestore.FirebaseFirestore

class EditBusActivity : AppCompatActivity() {

    private lateinit var etEditBusId: EditText
    private lateinit var etEditRoute: EditText
    private lateinit var etEditDriverEmail: EditText
    private lateinit var btnUpdateBus: Button

    private val db = FirebaseFirestore.getInstance()

    private var busId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_bus)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        etEditBusId = findViewById(R.id.etEditBusId)
        etEditRoute = findViewById(R.id.etEditRoute)
        etEditDriverEmail = findViewById(R.id.etEditDriverEmail)
        btnUpdateBus = findViewById(R.id.btnUpdateBus)

        busId = intent.getStringExtra("busId") ?: ""
        val route = intent.getStringExtra("route") ?: ""
        val driverEmail = intent.getStringExtra("driverEmail") ?: ""

        etEditBusId.setText(busId)
        etEditRoute.setText(route)
        etEditDriverEmail.setText(driverEmail)

        btnUpdateBus.setOnClickListener {
            updateBus()
        }
    }

    private fun updateBus() {
        val updatedRoute = etEditRoute.text.toString().trim()
        val updatedDriverEmail = etEditDriverEmail.text.toString().trim()

        if (busId.isBlank() || updatedRoute.isBlank() || updatedDriverEmail.isBlank()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val updatedBusData = hashMapOf(
            "busId" to busId,
            "route" to updatedRoute,
            "driverEmail" to updatedDriverEmail
        )

        db.collection("buses")
            .document(busId)
            .set(updatedBusData)
            .addOnSuccessListener {
                Toast.makeText(this, "Bus updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error updating bus", Toast.LENGTH_SHORT).show()
            }
    }
}