package com.example.schoolbusapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PaymentHistoryAdapter(
    private val payments: List<Payment>
) : RecyclerView.Adapter<PaymentHistoryAdapter.PaymentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_payment_history, parent, false)
        return PaymentViewHolder(view)
    }

    override fun onBindViewHolder(holder: PaymentViewHolder, position: Int) {
        val payment = payments[position]
        holder.tvPlanType.text = "${payment.planType.capitalize()} Plan"
        holder.tvAmount.text = "₹${payment.amount}"
        holder.tvDate.text = formatDate(payment.timestamp)
        holder.tvStatus.text = payment.status.capitalize()

        // Set status color
        val statusColor = when (payment.status.lowercase()) {
            "completed" -> android.graphics.Color.GREEN
            "failed" -> android.graphics.Color.RED
            else -> android.graphics.Color.GRAY
        }
        holder.tvStatus.setTextColor(statusColor)

        if (payment.transactionId.isNotBlank()) {
            holder.tvTransactionId.text = "Txn ID: ${payment.transactionId}"
            holder.tvTransactionId.visibility = View.VISIBLE
        } else {
            holder.tvTransactionId.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = payments.size

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    class PaymentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvPlanType: TextView = itemView.findViewById(R.id.tvPlanType)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvTransactionId: TextView = itemView.findViewById(R.id.tvTransactionId)
    }
}
