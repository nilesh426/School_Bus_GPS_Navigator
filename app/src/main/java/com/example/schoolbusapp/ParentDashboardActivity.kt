package com.example.schoolbusapp

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ParentDashboardActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var tvStudentName: TextView
    private lateinit var tvStudentClass: TextView
    private lateinit var tvBusId: TextView
    private lateinit var tvFeePlan: TextView
    private lateinit var tvFeeStatus: TextView
    private lateinit var tvBusStatus: TextView
    private lateinit var tvLastUpdated: TextView
    private lateinit var tvBoardedAt: TextView
    private lateinit var tvDroppedAt: TextView
    private lateinit var btnRecenterBus: Button
    private lateinit var spChildren: Spinner
    private lateinit var tvSelectChildLabel: TextView

    private var googleMap: GoogleMap? = null
    private var busMarker: Marker? = null
    private var currentBusId: String = ""
    private var currentStudentId: String = ""
    private var lastBusTimestamp: Long = 0L

    private val firestore = FirebaseFirestore.getInstance()

    private val statusHandler = Handler(Looper.getMainLooper())
    private val statusRunnable = object : Runnable {
        override fun run() {
            updateBusStatusUI()
            statusHandler.postDelayed(this, 1000)
        }
    }

    private var busLocationRef: DatabaseReference? = null
    private var busLocationListener: ValueEventListener? = null
    private var attendanceListener: ListenerRegistration? = null

    private var isFirstLocationUpdate = true

    private val parentStudents = mutableListOf<ParentStudent>()

    data class ParentStudent(
        val id: String,
        val name: String,
        val studentClass: String,
        val busId: String,
        val feePlan: String,
        val feeStatus: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_dashboard)

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

        tvStudentName = findViewById(R.id.tvStudentName)
        tvStudentClass = findViewById(R.id.tvStudentClass)
        tvBusId = findViewById(R.id.tvBusId)
        tvFeePlan = findViewById(R.id.tvFeePlan)
        tvFeeStatus = findViewById(R.id.tvFeeStatus)
        tvBusStatus = findViewById(R.id.tvBusStatus)
        tvLastUpdated = findViewById(R.id.tvLastUpdated)
        tvBoardedAt = findViewById(R.id.tvBoardedAt)
        tvDroppedAt = findViewById(R.id.tvDroppedAt)
        btnRecenterBus = findViewById(R.id.btnRecenterBus)
        spChildren = findViewById(R.id.spChildren)
        tvSelectChildLabel = findViewById(R.id.tvSelectChildLabel)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        loadParentStudentDetails()
        statusHandler.post(statusRunnable)

        btnRecenterBus.setOnClickListener {
            recenterBusOnMap()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        startLiveBusTracking()
    }

    private fun loadParentStudentDetails() {
        val parentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        firestore.collection("students")
            .whereEqualTo("parentUid", parentUid)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    spChildren.visibility = View.GONE
                    tvSelectChildLabel.visibility = View.GONE

                    tvStudentName.text = "Student: Not assigned"
                    tvStudentClass.text = "Class: -"
                    tvBusId.text = "Bus ID: -"
                    tvFeePlan.text = "Fee Plan: -"
                    tvFeeStatus.text = "Fee Status: -"
                    tvBusStatus.text = "Bus Status: -"
                    tvLastUpdated.text = "Last Updated: -"
                    tvBoardedAt.text = "Boarded at: -"
                    tvDroppedAt.text = "Dropped at: -"
                    return@addOnSuccessListener
                }

                parentStudents.clear()

                querySnapshot.documents.forEach { doc ->
                    parentStudents.add(
                        ParentStudent(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            studentClass = doc.getString("class") ?: "",
                            busId = doc.getString("busId") ?: "",
                            feePlan = doc.getString("feePlan") ?: "",
                            feeStatus = doc.getString("feeStatus") ?: ""
                        )
                    )
                }

                if (parentStudents.size == 1) {
                    spChildren.visibility = View.GONE
                    tvSelectChildLabel.visibility = View.GONE
                    showStudentDetails(parentStudents[0])
                } else {
                    spChildren.visibility = View.VISIBLE
                    tvSelectChildLabel.visibility = View.VISIBLE

                    val studentNames = parentStudents.map { it.name }
                    val adapter = ArrayAdapter(
                        this,
                        android.R.layout.simple_spinner_item,
                        studentNames
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spChildren.adapter = adapter

                    spChildren.setSelection(0, false)
                    showStudentDetails(parentStudents[0])

                    spChildren.onItemSelectedListener =
                        object : android.widget.AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(
                                parent: android.widget.AdapterView<*>?,
                                view: View?,
                                position: Int,
                                id: Long
                            ) {
                                showStudentDetails(parentStudents[position])
                            }

                            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                            }
                        }
                }
            }
    }

    private fun showStudentDetails(student: ParentStudent) {
        currentStudentId = student.id
        currentBusId = student.busId
        lastBusTimestamp = 0L
        isFirstLocationUpdate = true

        tvStudentName.text = "Student: ${student.name}"
        tvStudentClass.text = "Class: ${student.studentClass}"
        tvBusId.text = "Bus ID: ${student.busId}"
        tvFeePlan.text = "Fee Plan: ${student.feePlan}"
        tvFeeStatus.text = "Fee Status: ${student.feeStatus}"
        tvBusStatus.text = "Bus Status: Offline"
        tvLastUpdated.text = "Last Updated: -"
        tvBoardedAt.text = "Boarded at: -"
        tvDroppedAt.text = "Dropped at: -"

        busMarker?.remove()
        busMarker = null

        startLiveBusTracking()
        startAttendanceTracking()
    }

    private fun startAttendanceTracking() {
        if (currentStudentId.isBlank()) return

        attendanceListener?.remove()

        val today = getTodayDate()
        val docId = "${currentStudentId}_$today"

        attendanceListener = firestore.collection("student_attendance")
            .document(docId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null || !snapshot.exists()) {
                    tvBoardedAt.text = "Boarded at: -"
                    tvDroppedAt.text = "Dropped at: -"
                    return@addSnapshotListener
                }

                val boardedAt = snapshot.getLong("boardedAt")
                val droppedAt = snapshot.getLong("droppedAt")

                tvBoardedAt.text = if (boardedAt != null) {
                    "Boarded at: ${formatTime(boardedAt)}"
                } else {
                    "Boarded at: -"
                }

                tvDroppedAt.text = if (droppedAt != null) {
                    "Dropped at: ${formatTime(droppedAt)}"
                } else {
                    "Dropped at: -"
                }
            }
    }

    private fun startLiveBusTracking() {
        val map = googleMap ?: return
        if (currentBusId.isBlank()) return

        busLocationListener?.let { listener ->
            busLocationRef?.removeEventListener(listener)
        }

        busLocationRef = FirebaseDatabase.getInstance()
            .getReference("buses")
            .child(currentBusId)

        busLocationListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lat = snapshot.child("lat").getValue(Double::class.java)
                val lng = snapshot.child("lng").getValue(Double::class.java)
                val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L

                lastBusTimestamp = timestamp
                updateBusStatusUI()

                if (lat == null || lng == null) return

                val newLocation = LatLng(lat, lng)

                if (busMarker == null) {
                    busMarker = map.addMarker(
                        MarkerOptions()
                            .position(newLocation)
                            .title("School Bus - $currentBusId")
                        // .icon(getResizedBusIcon())
                    )
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(newLocation, 16f))
                    isFirstLocationUpdate = false
                } else {
                    animateMarker(busMarker!!, newLocation)
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        }

        busLocationRef!!.addValueEventListener(busLocationListener!!)
    }

    private fun animateMarker(marker: Marker, toPosition: LatLng) {
        val startPosition = marker.position

        val valueAnimator = ValueAnimator.ofFloat(0f, 1f)
        valueAnimator.duration = 1000
        valueAnimator.interpolator = LinearInterpolator()

        valueAnimator.addUpdateListener { animation ->
            val fraction = animation.animatedFraction
            val lat = startPosition.latitude + (toPosition.latitude - startPosition.latitude) * fraction
            val lng = startPosition.longitude + (toPosition.longitude - startPosition.longitude) * fraction
            marker.position = LatLng(lat, lng)
        }

        valueAnimator.start()
    }

    private fun updateBusStatusUI() {
        if (lastBusTimestamp == 0L) {
            tvBusStatus.text = "Bus Status: Offline"
            tvLastUpdated.text = "Last Updated: -"
            return
        }

        val diffSeconds = ((System.currentTimeMillis() - lastBusTimestamp) / 1000).toInt()

        tvLastUpdated.text = "Last Updated: ${formatSeconds(diffSeconds)} ago"
        tvBusStatus.text = if (diffSeconds <= 15) {
            "Bus Status: Live"
        } else {
            "Bus Status: Offline"
        }
    }

    private fun formatSeconds(seconds: Int): String {
        return when {
            seconds < 60 -> "$seconds sec"
            seconds < 3600 -> "${seconds / 60} min"
            else -> "${seconds / 3600} hr"
        }
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun getTodayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun recenterBusOnMap() {
        val map = googleMap ?: return
        val marker = busMarker ?: return

        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(marker.position, 16f)
        )
    }

    private fun getResizedBusIcon(): BitmapDescriptor {
        val drawable = resources.getDrawable(R.drawable.bus_icon, null)
        val bitmap = (drawable as android.graphics.drawable.BitmapDrawable).bitmap
        val smallBitmap = Bitmap.createScaledBitmap(bitmap, 100, 100, false)
        return BitmapDescriptorFactory.fromBitmap(smallBitmap)
    }

    override fun onDestroy() {
        super.onDestroy()

        statusHandler.removeCallbacks(statusRunnable)

        busLocationListener?.let { listener ->
            busLocationRef?.removeEventListener(listener)
        }

        attendanceListener?.remove()
    }
}