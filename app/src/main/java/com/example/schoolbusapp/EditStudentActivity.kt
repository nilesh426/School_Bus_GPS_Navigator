package com.example.schoolbusapp

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.firestore.FirebaseFirestore

class EditStudentActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    private lateinit var etStudentName: EditText
    private lateinit var etParentEmail: EditText
    private lateinit var etStudentClass: EditText
    private lateinit var etBusId: EditText
    private lateinit var spFeePlan: Spinner
    private lateinit var spFeeStatus: Spinner
    private lateinit var btnUpdateStudent: Button

    private var studentId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_student)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        etStudentName = findViewById(R.id.etStudentName)
        etParentEmail = findViewById(R.id.etParentEmail)
        etStudentClass = findViewById(R.id.etStudentClass)
        etBusId = findViewById(R.id.etBusId)
        spFeePlan = findViewById(R.id.spFeePlan)
        spFeeStatus = findViewById(R.id.spFeeStatus)
        btnUpdateStudent = findViewById(R.id.btnUpdateStudent)

        val feePlanAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.fee_plans,
            android.R.layout.simple_spinner_item
        )
        feePlanAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spFeePlan.adapter = feePlanAdapter

        val feeStatusAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.fee_status,
            android.R.layout.simple_spinner_item
        )
        feeStatusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spFeeStatus.adapter = feeStatusAdapter

        studentId = intent.getStringExtra("studentId") ?: ""
        val name = intent.getStringExtra("name") ?: ""
        val parentEmail = intent.getStringExtra("parentEmail") ?: ""
        val studentClass = intent.getStringExtra("studentClass") ?: ""
        val busId = intent.getStringExtra("busId") ?: ""
        val feePlan = intent.getStringExtra("feePlan") ?: "monthly"
        val feeStatus = intent.getStringExtra("feeStatus") ?: "pending"

        etStudentName.setText(name)
        etParentEmail.setText(parentEmail)
        etStudentClass.setText(studentClass)
        etBusId.setText(busId)

        spFeePlan.setSelection(
            when (feePlan) {
                "monthly" -> 0
                "quarterly" -> 1
                "yearly" -> 2
                else -> 0
            }
        )

        spFeeStatus.setSelection(
            when (feeStatus) {
                "pending" -> 0
                "paid" -> 1
                else -> 0
            }
        )

        btnUpdateStudent.setOnClickListener {
            updateStudent()
        }
    }

    private fun updateStudent() {
        val name = etStudentName.text.toString().trim()
        val parentEmail = etParentEmail.text.toString().trim()
        val studentClass = etStudentClass.text.toString().trim()
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

        if (studentId.isBlank() || name.isEmpty() || parentEmail.isEmpty() || studentClass.isEmpty() || busId.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
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

                val updatedStudent = hashMapOf(
                    "name" to name,
                    "parentEmail" to parentEmail,
                    "parentUid" to parentUid,
                    "class" to studentClass,
                    "busId" to busId,
                    "feePlan" to feePlan,
                    "feeStatus" to feeStatus
                )

                db.collection("students")
                    .document(studentId)
                    .set(updatedStudent)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Student updated successfully", Toast.LENGTH_SHORT).show()
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