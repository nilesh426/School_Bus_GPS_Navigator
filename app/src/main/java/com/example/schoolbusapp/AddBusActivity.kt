package com.example.schoolbusapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.firestore.FirebaseFirestore

class AddBusActivity : AppCompatActivity() {

    private lateinit var etBusId: EditText
    private lateinit var etRoute: EditText
    private lateinit var etDriverEmail: EditText
    private lateinit var btnSaveBus: Button

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_bus)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        etBusId = findViewById(R.id.etBusId)
        etRoute = findViewById(R.id.etRoute)
        etDriverEmail = findViewById(R.id.etDriverEmail)
        btnSaveBus = findViewById(R.id.btnSaveBus)

        btnSaveBus.setOnClickListener {
            saveBus()
        }
    }

    private fun saveBus() {
        val busId = etBusId.text.toString().trim()
        val route = etRoute.text.toString().trim()
        val driverEmail = etDriverEmail.text.toString().trim()

        if (busId.isEmpty() || route.isEmpty() || driverEmail.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val busData = hashMapOf(
            "busId" to busId,
            "route" to route,
            "driverEmail" to driverEmail
        )

        db.collection("buses")
            .document(busId)
            .set(busData)
            .addOnSuccessListener {
                Toast.makeText(this, "Bus added successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error adding bus", Toast.LENGTH_SHORT).show()
            }
    }
}