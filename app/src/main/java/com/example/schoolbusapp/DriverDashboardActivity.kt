package com.example.schoolbusapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class DriverDashboardActivity : AppCompatActivity() {

    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var btnManageBoarding: Button
    private lateinit var tvStatus: TextView
    private lateinit var tvAssignedBus: TextView

    private var busId: String = ""

    private val LOCATION_PERMISSION_REQUEST = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_dashboard)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.inflateMenu(R.menu.menu_dashboard)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_logout -> {
                    FirebaseAuth.getInstance().signOut()

                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }

        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        btnManageBoarding = findViewById(R.id.btnManageBoarding)
        tvStatus = findViewById(R.id.tvStatus)
        tvAssignedBus = findViewById(R.id.tvAssignedBus)

        btnStop.isEnabled = false

        loadAssignedBus()

        btnStart.setOnClickListener {
            if (busId.isBlank()) {
                Toast.makeText(this, "No bus assigned", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (hasLocationPermission()) {
                startSharingService()
            } else {
                requestLocationPermission()
            }
        }

        btnStop.setOnClickListener {
            stopSharingService()
        }

        btnManageBoarding.setOnClickListener {
            if (busId.isBlank()) {
                Toast.makeText(this, "No bus assigned", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, ManageBoardingActivity::class.java)
            intent.putExtra("busId", busId)
            startActivity(intent)
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST
        )
    }

    private fun startSharingService() {
        val intent = Intent(this, LocationForegroundService::class.java)
        intent.putExtra("busId", busId)

        ContextCompat.startForegroundService(this, intent)

        tvStatus.text = "Status: Sharing live location"
        btnStart.isEnabled = false
        btnStop.isEnabled = true
    }

    private fun stopSharingService() {
        if (busId.isNotBlank()) {
            FirebaseDatabase.getInstance()
                .getReference("buses")
                .child(busId)
                .child("isSharing")
                .setValue(false)
        }

        stopService(Intent(this, LocationForegroundService::class.java))
        tvStatus.text = "Status: Not sharing"
        btnStart.isEnabled = true
        btnStop.isEnabled = false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startSharingService()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadAssignedBus() {
        val driverEmail = FirebaseAuth.getInstance().currentUser?.email ?: return

        FirebaseFirestore.getInstance()
            .collection("buses")
            .whereEqualTo("driverEmail", driverEmail)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val doc = querySnapshot.documents[0]
                    busId = doc.getString("busId") ?: ""
                    tvAssignedBus.text = if (busId.isBlank()) "--" else busId
                    tvStatus.text = if (busId.isBlank()) {
                        "Status: No bus assigned"
                    } else {
                        "Status: Ready to share"
                    }
                } else {
                    tvAssignedBus.text = "--"
                    tvStatus.text = "Status: No bus assigned"
                }
            }
    }
}