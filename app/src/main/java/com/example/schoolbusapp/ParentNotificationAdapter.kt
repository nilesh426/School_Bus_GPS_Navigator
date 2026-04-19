package com.example.schoolbusapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ParentNotificationAdapter(
    private val notifications: MutableList<ParentNotification>,
    private val onDismiss: (ParentNotification) -> Unit
) : RecyclerView.Adapter<ParentNotificationAdapter.NotificationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]
        val timeString = formatTime(notification.timestamp)
        holder.tvNotificationText.text = "$timeString - ${notification.text}"
        holder.btnDismiss.setOnClickListener {
            onDismiss(notification)
        }
    }

    override fun getItemCount(): Int = notifications.size

    fun removeNotification(notification: ParentNotification) {
        val index = notifications.indexOfFirst { it.id == notification.id }
        if (index != -1) {
            notifications.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNotificationText: TextView = itemView.findViewById(R.id.tvNotificationText)
        val btnDismiss: Button = itemView.findViewById(R.id.btnDismiss)
    }
}
