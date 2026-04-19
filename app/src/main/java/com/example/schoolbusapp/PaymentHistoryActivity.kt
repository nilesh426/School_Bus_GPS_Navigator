package com.example.schoolbusapp

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PaymentHistoryActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var rvPaymentHistory: RecyclerView
    private val payments = mutableListOf<Payment>()
    private lateinit var adapter: PaymentHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_history)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        rvPaymentHistory = findViewById(R.id.rvPaymentHistory)
        adapter = PaymentHistoryAdapter(payments)

        rvPaymentHistory.layoutManager = LinearLayoutManager(this)
        rvPaymentHistory.adapter = adapter

        loadPaymentHistory()
    }

    private fun loadPaymentHistory() {
        val parentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("payments")
            .whereEqualTo("parentUid", parentUid)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                payments.clear()
                for (document in querySnapshot.documents) {
                    val payment = Payment(
                        id = document.id,
                        studentId = document.getString("studentId") ?: "",
                        parentUid = document.getString("parentUid") ?: "",
                        amount = document.getDouble("amount") ?: 0.0,
                        planType = document.getString("planType") ?: "",
                        timestamp = document.getLong("timestamp") ?: 0L,
                        status = document.getString("status") ?: "",
                        transactionId = document.getString("transactionId") ?: "",
                        razorpayOrderId = document.getString("razorpayOrderId") ?: "",
                        razorpayPaymentId = document.getString("razorpayPaymentId") ?: ""
                    )
                    payments.add(payment)
                }
                adapter.notifyDataSetChanged()

                if (payments.isEmpty()) {
                    Toast.makeText(this, "No payment history found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading payment history: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
