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

class ViewStudentsActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    private val allStudents = mutableListOf<StudentFee>()
    private val filteredStudents = mutableListOf<StudentFee>()

    private lateinit var adapter: StudentFeeAdapter
    private lateinit var etSearchStudent: EditText
    private lateinit var rvStudents: RecyclerView
    private lateinit var tvEmptyStudents: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_students)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        etSearchStudent = findViewById(R.id.etSearchStudent)
        rvStudents = findViewById(R.id.rvStudents)
        tvEmptyStudents = findViewById(R.id.tvEmptyStudents)

        rvStudents.layoutManager = LinearLayoutManager(this)

        adapter = StudentFeeAdapter(
            filteredStudents,
            onToggleClick = { student ->
                toggleFeeStatus(student)
            },
            onEditClick = { student ->
                openEditStudent(student)
            },
            onDeleteClick = { student ->
                showDeleteConfirmationDialog(student)
            }
        )
        rvStudents.adapter = adapter

        etSearchStudent.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterStudents(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })

        loadStudents()
    }

    private fun loadStudents() {
        db.collection("students")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Toast.makeText(this, "Error: ${err.message}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }

                allStudents.clear()

                snap?.documents?.forEach { doc ->
                    allStudents.add(
                        StudentFee(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            feePlan = doc.getString("feePlan") ?: "",
                            feeStatus = doc.getString("feeStatus") ?: "pending",
                            studentClass = doc.getString("class") ?: "",
                            busId = doc.getString("busId") ?: ""
                        )
                    )
                }

                filterStudents(etSearchStudent.text.toString())
            }
    }

    private fun filterStudents(query: String) {
        filteredStudents.clear()

        if (query.isBlank()) {
            filteredStudents.addAll(allStudents)
        } else {
            val searchText = query.trim().lowercase()
            for (student in allStudents) {
                if (student.name.lowercase().contains(searchText)) {
                    filteredStudents.add(student)
                }
            }
        }

        adapter.notifyDataSetChanged()
        updateEmptyState(query)
    }

    private fun updateEmptyState(query: String) {
        if (filteredStudents.isEmpty()) {
            rvStudents.visibility = View.GONE
            tvEmptyStudents.visibility = View.VISIBLE

            tvEmptyStudents.text = if (query.isBlank()) {
                "No students available"
            } else {
                "No students match your search"
            }
        } else {
            rvStudents.visibility = View.VISIBLE
            tvEmptyStudents.visibility = View.GONE
        }
    }

    private fun toggleFeeStatus(student: StudentFee) {
        val newStatus = if (student.feeStatus == "paid") "pending" else "paid"

        db.collection("students").document(student.id)
            .update("feeStatus", newStatus)
            .addOnSuccessListener {
                Toast.makeText(this, "Updated to $newStatus", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun openEditStudent(student: StudentFee) {
        db.collection("students").document(student.id)
            .get()
            .addOnSuccessListener { doc ->
                val intent = Intent(this, EditStudentActivity::class.java)
                intent.putExtra("studentId", student.id)
                intent.putExtra("name", doc.getString("name") ?: "")
                intent.putExtra("parentEmail", doc.getString("parentEmail") ?: "")
                intent.putExtra("studentClass", doc.getString("class") ?: "")
                intent.putExtra("busId", doc.getString("busId") ?: "")
                intent.putExtra("feePlan", doc.getString("feePlan") ?: "monthly")
                intent.putExtra("feeStatus", doc.getString("feeStatus") ?: "pending")
                startActivity(intent)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load student: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showDeleteConfirmationDialog(student: StudentFee) {
        AlertDialog.Builder(this)
            .setTitle("Delete Student")
            .setMessage("Are you sure you want to delete ${student.name}?")
            .setPositiveButton("Yes") { _, _ ->
                deleteStudent(student)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deleteStudent(student: StudentFee) {
        db.collection("students")
            .document(student.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Student deleted successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}