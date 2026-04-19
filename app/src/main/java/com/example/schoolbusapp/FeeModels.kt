package com.example.schoolbusapp

data class FeePlan(
    val id: String = "",
    val planType: String = "", // "monthly", "quarterly", "yearly"
    val amount: Double = 0.0,
    val description: String = ""
)

data class Payment(
    val id: String = "",
    val studentId: String = "",
    val parentUid: String = "",
    val amount: Double = 0.0,
    val planType: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "pending", // "pending", "completed", "failed"
    val transactionId: String = "",
    val razorpayOrderId: String = "",
    val razorpayPaymentId: String = ""
)
