package com.example.schoolbusapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.firestore.FirebaseFirestore

class AddRouteActivity : AppCompatActivity() {

    private lateinit var etRouteName: EditText
    private lateinit var btnSaveRoute: Button

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_route)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        etRouteName = findViewById(R.id.etRouteName)
        btnSaveRoute = findViewById(R.id.btnSaveRoute)

        btnSaveRoute.setOnClickListener {
            saveRoute()
        }
    }

    private fun saveRoute() {
        val name = etRouteName.text.toString().trim()

        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter route name", Toast.LENGTH_SHORT).show()
            return
        }

        val routeData = hashMapOf(
            "name" to name,
            "stops" to emptyList<Map<String, Any>>()
        )

        db.collection("routes")
            .add(routeData)
            .addOnSuccessListener {
                Toast.makeText(this, "Route added successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error adding route", Toast.LENGTH_SHORT).show()
            }
    }
}
