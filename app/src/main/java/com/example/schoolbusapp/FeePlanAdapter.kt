package com.example.schoolbusapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FeePlanAdapter(
    private val feePlans: List<FeePlan>,
    private val onItemClick: (FeePlan) -> Unit
) : RecyclerView.Adapter<FeePlanAdapter.FeePlanViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeePlanViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_fee_plan, parent, false)
        return FeePlanViewHolder(view)
    }

    override fun onBindViewHolder(holder: FeePlanViewHolder, position: Int) {
        val feePlan = feePlans[position]
        holder.tvPlanType.text = feePlan.planType.capitalize()
        holder.tvAmount.text = "₹${feePlan.amount}"
        holder.tvDescription.text = feePlan.description.ifEmpty { "No description" }

        holder.itemView.setOnClickListener {
            onItemClick(feePlan)
        }
    }

    override fun getItemCount(): Int = feePlans.size

    class FeePlanViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvPlanType: TextView = itemView.findViewById(R.id.tvPlanType)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
    }
}
