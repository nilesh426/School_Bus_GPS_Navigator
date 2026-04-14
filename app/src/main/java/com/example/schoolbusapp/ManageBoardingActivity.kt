package com.example.schoolbusapp

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ManageBoardingActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val students = mutableListOf<BoardingStudent>()
    private lateinit var adapter: BoardingStudentAdapter

    private var busId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_boarding)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        toolbar.setNavigationOnClickListener { finish() }

        busId = intent.getStringExtra("busId") ?: ""

        val btnBoardAll = findViewById<Button>(R.id.btnBoardAll)
        val btnDropAll = findViewById<Button>(R.id.btnDropAll)

        val rvStudents = findViewById<RecyclerView>(R.id.rvBoardingStudents)
        rvStudents.layoutManager = LinearLayoutManager(this)

        adapter = BoardingStudentAdapter(
            students,
            onBoardedClick = { student -> markBoarded(student) },
            onDroppedClick = { student -> markDropped(student) }
        )
        rvStudents.adapter = adapter

        btnBoardAll.setOnClickListener {
            if (students.isEmpty()) {
                Toast.makeText(this, "No students available", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(this)
                .setTitle("Board All Students")
                .setMessage("Are you sure you want to mark all students as boarded?")
                .setPositiveButton("Yes") { _, _ ->
                    students.forEach { student ->
                        if (!student.boarded) {
                            markBoarded(student, false)
                        }
                    }
                    Toast.makeText(this, "All students marked boarded", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("No", null)
                .show()
        }

        btnDropAll.setOnClickListener {
            if (students.isEmpty()) {
                Toast.makeText(this, "No students available", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(this)
                .setTitle("Drop All Students")
                .setMessage("Are you sure you want to mark all students as dropped?")
                .setPositiveButton("Yes") { _, _ ->
                    students.forEach { student ->
                        if (student.boarded && !student.dropped) {
                            markDropped(student, false)
                        }
                    }
                    Toast.makeText(this, "All students marked dropped", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("No", null)
                .show()
        }

        loadStudentsForBus()
    }

    private fun loadStudentsForBus() {
        if (busId.isBlank()) return

        db.collection("students")
            .whereEqualTo("busId", busId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                students.clear()

                querySnapshot.documents.forEach { doc ->
                    students.add(
                        BoardingStudent(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            studentClass = doc.getString("class") ?: "",
                            parentUid = doc.getString("parentUid") ?: "",
                            busId = doc.getString("busId") ?: ""
                        )
                    )
                }

                loadTodayAttendanceStatuses()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load students: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun loadTodayAttendanceStatuses() {
        val today = getTodayDate()

        students.forEach { student ->
            val docId = "${student.id}_$today"

            db.collection("student_attendance")
                .document(docId)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        student.boarded = doc.getBoolean("boarded") ?: false
                        student.dropped = doc.getBoolean("dropped") ?: false
                        student.boardedAt = doc.getLong("boardedAt")
                        student.droppedAt = doc.getLong("droppedAt")
                    }
                    adapter.notifyDataSetChanged()
                }
        }

        adapter.notifyDataSetChanged()
    }

    private fun markBoarded(student: BoardingStudent, showToast: Boolean = true) {
        val today = getTodayDate()
        val now = System.currentTimeMillis()
        val docId = "${student.id}_$today"

        val data = hashMapOf(
            "studentId" to student.id,
            "studentName" to student.name,
            "parentUid" to student.parentUid,
            "busId" to student.busId,
            "date" to today,
            "boarded" to true,
            "boardedAt" to now
        )

        db.collection("student_attendance")
            .document(docId)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                student.boarded = true
                student.boardedAt = now
                adapter.notifyDataSetChanged()

                if (showToast) {
                    Toast.makeText(this, "${student.name} marked boarded", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun markDropped(student: BoardingStudent, showToast: Boolean = true) {
        val today = getTodayDate()
        val now = System.currentTimeMillis()
        val docId = "${student.id}_$today"

        val data = hashMapOf(
            "studentId" to student.id,
            "studentName" to student.name,
            "parentUid" to student.parentUid,
            "busId" to student.busId,
            "date" to today,
            "dropped" to true,
            "droppedAt" to now
        )

        db.collection("student_attendance")
            .document(docId)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                student.dropped = true
                student.droppedAt = now
                adapter.notifyDataSetChanged()

                if (showToast) {
                    Toast.makeText(this, "${student.name} marked dropped", Toast.LENGTH_SHORT).show()
                }
            }
           .addOnFailureListener { e ->
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun getTodayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
}