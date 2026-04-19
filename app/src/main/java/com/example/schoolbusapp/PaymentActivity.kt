package com.example.schoolbusapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import org.json.JSONObject

class PaymentActivity : AppCompatActivity(), PaymentResultListener {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var rgFeePlans: RadioGroup
    private lateinit var tvStudentInfo: TextView
    private lateinit var tvAmount: TextView
    private lateinit var btnPayNow: Button

    private var selectedFeePlan: FeePlan? = null
    private var studentId: String = ""
    private var busId: String = ""
    private val feePlans = mutableListOf<FeePlan>()

    // Razorpay test credentials
    private val razorpayKeyId = "rzp_test_SeCgmSsyTikUjx"
    private val razorpayKeySecret = "FsZsDD73CqzYwmLwpcHfu145"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        studentId = intent.getStringExtra("studentId") ?: ""
        busId = intent.getStringExtra("busId") ?: ""

        rgFeePlans = findViewById(R.id.rgFeePlans)
        tvStudentInfo = findViewById(R.id.tvStudentInfo)
        tvAmount = findViewById(R.id.tvAmount)
        btnPayNow = findViewById(R.id.btnPayNow)

        // Initialize Razorpay
        Checkout.preload(applicationContext)

        loadStudentInfo()
        loadFeePlans()

        rgFeePlans.setOnCheckedChangeListener { _, checkedId ->
            val radioButton = findViewById<RadioButton>(checkedId)
            val planType = radioButton.tag.toString()
            selectedFeePlan = feePlans.find { it.planType == planType }
            updateAmountDisplay()
        }

        btnPayNow.setOnClickListener {
            if (selectedFeePlan != null) {
                startPayment()
            } else {
                Toast.makeText(this, "Please select a fee plan", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadStudentInfo() {
        if (studentId.isBlank()) return

        db.collection("students")
            .document(studentId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("name") ?: ""
                    val studentClass = document.getString("class") ?: ""
                    tvStudentInfo.text = "Student: $name\nClass: $studentClass\nBus ID: $busId"
                }
            }
    }

    private fun loadFeePlans() {
        db.collection("fee_plans")
            .get()
            .addOnSuccessListener { querySnapshot ->
                feePlans.clear()
                rgFeePlans.removeAllViews()

                for (document in querySnapshot.documents) {
                    val feePlan = FeePlan(
                        id = document.id,
                        planType = document.getString("planType") ?: "",
                        amount = document.getDouble("amount") ?: 0.0,
                        description = document.getString("description") ?: ""
                    )
                    feePlans.add(feePlan)

                    val radioButton = RadioButton(this).apply {
                        text = "${feePlan.planType.capitalize()} Plan - ₹${feePlan.amount}"
                        tag = feePlan.planType
                        setPadding(16, 16, 16, 16)
                    }
                    rgFeePlans.addView(radioButton)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading fee plans: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateAmountDisplay() {
        selectedFeePlan?.let { plan ->
            tvAmount.text = "Amount to Pay: ₹${plan.amount}"
        } ?: run {
            tvAmount.text = "Amount to Pay: ₹0"
        }
    }

    private fun startPayment() {
        val feePlan = selectedFeePlan ?: return

        val checkout = Checkout()
        checkout.setKeyID(razorpayKeyId)

        try {
            val options = JSONObject()
            options.put("name", "School Bus Fee Payment")
            options.put("description", "${feePlan.planType.capitalize()} Plan Fee")
            options.put("currency", "INR")
            options.put("amount", (feePlan.amount * 100).toInt()) // Amount in paisa

            val prefill = JSONObject()
            prefill.put("email", FirebaseAuth.getInstance().currentUser?.email ?: "")
            options.put("prefill", prefill)

            checkout.open(this, options)
        } catch (e: Exception) {
            Toast.makeText(this, "Error in payment: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?) {
        Toast.makeText(this, "Payment successful!", Toast.LENGTH_SHORT).show()

        // Save payment record
        savePaymentRecord(razorpayPaymentId, "completed")

        // Update student fee status
        updateStudentFeeStatus()

        // Go back to dashboard
        finish()
    }

    override fun onPaymentError(code: Int, response: String?) {
        Toast.makeText(this, "Payment failed: $response", Toast.LENGTH_SHORT).show()

        // Save failed payment record
        savePaymentRecord(null, "failed")
    }

    private fun savePaymentRecord(razorpayPaymentId: String?, status: String) {
        val feePlan = selectedFeePlan ?: return
        val parentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val payment = hashMapOf(
            "studentId" to studentId,
            "parentUid" to parentUid,
            "amount" to feePlan.amount,
            "planType" to feePlan.planType,
            "timestamp" to System.currentTimeMillis(),
            "status" to status,
            "transactionId" to (razorpayPaymentId ?: ""),
            "razorpayOrderId" to "",
            "razorpayPaymentId" to (razorpayPaymentId ?: "")
        )

        db.collection("payments")
            .add(payment)
            .addOnSuccessListener {
                if (status == "completed") {
                    Toast.makeText(this, "Payment record saved successfully", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving payment record: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateStudentFeeStatus() {
        val feePlan = selectedFeePlan ?: return

        db.collection("students")
            .document(studentId)
            .update("feeStatus", "Paid (${feePlan.planType})")
            .addOnSuccessListener {
                Toast.makeText(this, "Fee status updated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error updating fee status: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
