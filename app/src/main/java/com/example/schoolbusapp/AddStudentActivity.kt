package com.example.schoolbusapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.firestore.FirebaseFirestore

class AddStudentActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_student)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        val etStudentName = findViewById<EditText>(R.id.etStudentName)
        val etParentEmail = findViewById<EditText>(R.id.etParentEmail)
        val etStudentClass = findViewById<EditText>(R.id.etStudentClass)
        val etBusId = findViewById<EditText>(R.id.etBusId)
        val spFeePlan = findViewById<Spinner>(R.id.spFeePlan)
        val spFeeStatus = findViewById<Spinner>(R.id.spFeeStatus)
        val btnSaveStudent = findViewById<Button>(R.id.btnSaveStudent)

        btnSaveStudent.setOnClickListener {
            val name = etStudentName.text.toString().trim()
            val studentClass = etStudentClass.text.toString().trim()
            val parentEmail = etParentEmail.text.toString().trim()
            val busId = etBusId.text.toString().trim()

            val feePlan = when (spFeePlan.selectedItem.toString()) {
                "Monthly Plan" -> "monthly"
                "Quarterly Plan" -> "quarterly"
                "Yearly Plan" -> "yearly"
                else -> "monthly"
            }

            val feeStatus = when (spFeeStatus.selectedItem.toString()) {
                "Pending Payment" -> "pending"
                "Payment Completed" -> "paid"
                else -> "pending"
            }

            if (name.isEmpty() || parentEmail.isEmpty() || studentClass.isEmpty() || busId.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            db.collection("users")
                .whereEqualTo("email", parentEmail)
                .whereEqualTo("role", "parent")
                .get()
                .addOnSuccessListener { querySnapshot ->

                    if (querySnapshot.isEmpty) {
                        Toast.makeText(this, "Parent not found. Ask parent to register first.", Toast.LENGTH_LONG).show()
                        return@addOnSuccessListener
                    }

                    val parentUid = querySnapshot.documents[0].id

                    val student = hashMapOf(
                        "name" to name,
                        "parentEmail" to parentEmail,
                        "parentUid" to parentUid,
                        "class" to studentClass,
                        "busId" to busId,
                        "feePlan" to feePlan,
                        "feeStatus" to feeStatus
                    )

                    db.collection("students")
                        .add(student)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Student added successfully", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error finding parent: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
}