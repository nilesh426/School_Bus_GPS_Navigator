package com.example.schoolbusapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class StudentFeeAdapter(
    private val list: List<StudentFee>,
    private val onToggleClick: (StudentFee) -> Unit,
    private val onEditClick: (StudentFee) -> Unit,
    private val onDeleteClick: (StudentFee) -> Unit
) : RecyclerView.Adapter<StudentFeeAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvName: TextView = v.findViewById(R.id.tvName)
        val tvPlan: TextView = v.findViewById(R.id.tvPlan)
        val tvStatus: TextView = v.findViewById(R.id.tvStatus)
        val btnToggle: Button = v.findViewById(R.id.btnToggle)
        val btnEditStudent: Button = v.findViewById(R.id.btnEditStudent)
        val btnDeleteStudent: Button = v.findViewById(R.id.btnDeleteStudent)
        val tvClass: TextView = v.findViewById(R.id.tvClass)
        val tvBusId: TextView = v.findViewById(R.id.tvBusId)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student_fee, parent, false)
        return VH(v)
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(h: VH, position: Int) {
        val s = list[position]
        h.tvName.text = s.name
        h.tvPlan.text = "Plan: ${s.feePlan}"
        h.tvStatus.text = "Status: ${s.feeStatus}"
        h.tvClass.text = "Class: ${s.studentClass}"
        h.tvBusId.text = "Bus: ${s.busId}"

        h.btnToggle.text = if (s.feeStatus == "paid") "Pending" else "Paid"
        h.btnToggle.setOnClickListener { onToggleClick(s) }
        h.btnEditStudent.setOnClickListener { onEditClick(s) }
        h.btnDeleteStudent.setOnClickListener { onDeleteClick(s) }
    }
}