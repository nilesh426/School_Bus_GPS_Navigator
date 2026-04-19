package com.example.schoolbusapp

//import AddRouteActivity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class AdminDashboardActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    private lateinit var tvTotalStudents: TextView
    private lateinit var tvTotalBuses: TextView
    private lateinit var tvActiveBuses: TextView

    private var studentsListener: ListenerRegistration? = null
    private var busesListener: ListenerRegistration? = null
    private var activeBusesListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

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


        tvTotalStudents = findViewById(R.id.tvTotalStudents)
        tvTotalBuses = findViewById(R.id.tvTotalBuses)
        tvActiveBuses = findViewById(R.id.tvActiveBuses)



        findViewById<Button>(R.id.btnAddStudent).setOnClickListener {
            startActivity(Intent(this, AddStudentActivity::class.java))
        }

        findViewById<Button>(R.id.btnAddBus).setOnClickListener {
            startActivity(Intent(this, AddBusActivity::class.java))
        }

        findViewById<Button>(R.id.btnViewStudents).setOnClickListener {
            startActivity(Intent(this, ViewStudentsActivity::class.java))
        }

        findViewById<Button>(R.id.btnViewBuses).setOnClickListener {
            startActivity(Intent(this, ViewBusesActivity::class.java))
        }

        findViewById<Button>(R.id.btnLiveTracking).setOnClickListener {
            startActivity(Intent(this, AdminMapActivity::class.java))
        }

        findViewById<Button>(R.id.btnManageRoutes).setOnClickListener {
            startActivity(Intent(this, ManageRoutesActivity::class.java))
        }

        findViewById<Button>(R.id.btnManageFeePlans).setOnClickListener {
            startActivity(Intent(this, ManageFeePlansActivity::class.java))
        }

        loadDashboardCountsRealtime()
    }

    private fun loadDashboardCountsRealtime() {
        studentsListener = db.collection("students")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(
                        this,
                        "Failed to load students count: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addSnapshotListener
                }

                tvTotalStudents.text = (snapshot?.size() ?: 0).toString()
            }

        busesListener = db.collection("buses")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(
                        this,
                        "Failed to load buses count: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addSnapshotListener
                }

                tvTotalBuses.text = (snapshot?.size() ?: 0).toString()
            }

        val busesRef = FirebaseDatabase.getInstance().getReference("buses")

        activeBusesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var activeCount = 0
                val currentTime = System.currentTimeMillis()

                for (busSnapshot in snapshot.children) {
                    val timestamp = busSnapshot.child("timestamp").getValue(Long::class.java) ?: 0L

                    if (timestamp > 0L) {
                        val diffSeconds = (currentTime - timestamp) / 1000
                        if (diffSeconds <= 15) {
                            activeCount++
                        }
                    }
                }

                tvActiveBuses.text = activeCount.toString()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@AdminDashboardActivity,
                    "Failed to load active buses",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        busesRef.addValueEventListener(activeBusesListener!!)
    }

    override fun onDestroy() {
        super.onDestroy()

        studentsListener?.remove()
        busesListener?.remove()

        activeBusesListener?.let {
            FirebaseDatabase.getInstance()
                .getReference("buses")
                .removeEventListener(it)
        }
    }
}