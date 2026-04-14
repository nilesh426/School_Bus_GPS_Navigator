package com.example.schoolbusapp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BoardingStudentAdapter(
    private val list: List<BoardingStudent>,
    private val onBoardedClick: (BoardingStudent) -> Unit,
    private val onDroppedClick: (BoardingStudent) -> Unit
) : RecyclerView.Adapter<BoardingStudentAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvName: TextView = v.findViewById(R.id.tvStudentName)
        val tvClass: TextView = v.findViewById(R.id.tvStudentClass)
        val tvAttendanceStatus: TextView = v.findViewById(R.id.tvAttendanceStatus)
        val tvBoardedTime: TextView = v.findViewById(R.id.tvBoardedTime)
        val tvDroppedTime: TextView = v.findViewById(R.id.tvDroppedTime)
        val btnBoarded: Button = v.findViewById(R.id.btnBoarded)
        val btnDropped: Button = v.findViewById(R.id.btnDropped)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_boarding_student, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val student = list[position]

        holder.tvName.text = student.name
        holder.tvClass.text = "Class: ${student.studentClass}"

        when {
            student.dropped -> {
                holder.tvAttendanceStatus.text = "Status: Dropped"
                holder.tvAttendanceStatus.setTextColor(Color.parseColor("#2E7D32"))
            }
            student.boarded -> {
                holder.tvAttendanceStatus.text = "Status: Boarded"
                holder.tvAttendanceStatus.setTextColor(Color.parseColor("#1565C0"))
            }
            else -> {
                holder.tvAttendanceStatus.text = "Status: Not Marked"
                holder.tvAttendanceStatus.setTextColor(Color.parseColor("#D32F2F"))
            }
        }

        holder.tvBoardedTime.text = "Boarded at: ${formatTime(student.boardedAt)}"
        holder.tvDroppedTime.text = "Dropped at: ${formatTime(student.droppedAt)}"

        holder.btnBoarded.isEnabled = !student.boarded
        holder.btnDropped.isEnabled = student.boarded && !student.dropped

        holder.btnBoarded.setOnClickListener { onBoardedClick(student) }
        holder.btnDropped.setOnClickListener { onDroppedClick(student) }
    }

    private fun formatTime(timestamp: Long?): String {
        if (timestamp == null) return "-"
        return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
    }
}