package com.example.schoolbusapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class ManageFeePlansActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var rvFeePlans: RecyclerView
    private lateinit var btnAddFeePlan: Button
    private val feePlans = mutableListOf<FeePlan>()
    private lateinit var adapter: FeePlanAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_fee_plans)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        rvFeePlans = findViewById(R.id.rvFeePlans)
        btnAddFeePlan = findViewById(R.id.btnAddFeePlan)

        adapter = FeePlanAdapter(feePlans) { feePlan ->
            showEditDeleteDialog(feePlan)
        }

        rvFeePlans.layoutManager = LinearLayoutManager(this)
        rvFeePlans.adapter = adapter

        btnAddFeePlan.setOnClickListener {
            showAddFeePlanDialog()
        }

        loadFeePlans()
    }

    private fun loadFeePlans() {
        db.collection("fee_plans")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(this, "Error loading fee plans: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                feePlans.clear()
                snapshot?.documents?.forEach { doc ->
                    val feePlan = FeePlan(
                        id = doc.id,
                        planType = doc.getString("planType") ?: "",
                        amount = doc.getDouble("amount") ?: 0.0,
                        description = doc.getString("description") ?: ""
                    )
                    feePlans.add(feePlan)
                }
                adapter.notifyDataSetChanged()
            }
    }

    private fun showAddFeePlanDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_fee_plan, null)
        val spinnerPlanType = dialogView.findViewById<Spinner>(R.id.spinnerPlanType)
        val etAmount = dialogView.findViewById<EditText>(R.id.etAmount)
        val etDescription = dialogView.findViewById<EditText>(R.id.etDescription)

        val planTypes = arrayOf("monthly", "quarterly", "yearly")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, planTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPlanType.adapter = adapter

        AlertDialog.Builder(this)
            .setTitle("Add Fee Plan")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val planType = spinnerPlanType.selectedItem.toString()
                val amount = etAmount.text.toString().toDoubleOrNull()
                val description = etDescription.text.toString()

                if (amount == null || amount <= 0) {
                    Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val feePlan = hashMapOf(
                    "planType" to planType,
                    "amount" to amount,
                    "description" to description
                )

                db.collection("fee_plans")
                    .add(feePlan)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Fee plan added successfully", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error adding fee plan: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditDeleteDialog(feePlan: FeePlan) {
        AlertDialog.Builder(this)
            .setTitle("Manage Fee Plan")
            .setItems(arrayOf("Edit", "Delete")) { _, which ->
                when (which) {
                    0 -> showEditFeePlanDialog(feePlan)
                    1 -> showDeleteConfirmationDialog(feePlan)
                }
            }
            .show()
    }

    private fun showEditFeePlanDialog(feePlan: FeePlan) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_fee_plan, null)
        val spinnerPlanType = dialogView.findViewById<Spinner>(R.id.spinnerPlanType)
        val etAmount = dialogView.findViewById<EditText>(R.id.etAmount)
        val etDescription = dialogView.findViewById<EditText>(R.id.etDescription)

        val planTypes = arrayOf("monthly", "quarterly", "yearly")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, planTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPlanType.adapter = adapter

        // Set current values
        val currentIndex = planTypes.indexOf(feePlan.planType)
        if (currentIndex >= 0) {
            spinnerPlanType.setSelection(currentIndex)
        }
        etAmount.setText(feePlan.amount.toString())
        etDescription.setText(feePlan.description)

        AlertDialog.Builder(this)
            .setTitle("Edit Fee Plan")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val planType = spinnerPlanType.selectedItem.toString()
                val amount = etAmount.text.toString().toDoubleOrNull()
                val description = etDescription.text.toString()

                if (amount == null || amount <= 0) {
                    Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val updatedFeePlan = hashMapOf(
                    "planType" to planType,
                    "amount" to amount,
                    "description" to description
                )

                db.collection("fee_plans")
                    .document(feePlan.id)
                    .update(updatedFeePlan as Map<String, Any>)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Fee plan updated successfully", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error updating fee plan: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteConfirmationDialog(feePlan: FeePlan) {
        AlertDialog.Builder(this)
            .setTitle("Delete Fee Plan")
            .setMessage("Are you sure you want to delete this fee plan?")
            .setPositiveButton("Delete") { _, _ ->
                db.collection("fee_plans")
                    .document(feePlan.id)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Fee plan deleted successfully", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error deleting fee plan: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
