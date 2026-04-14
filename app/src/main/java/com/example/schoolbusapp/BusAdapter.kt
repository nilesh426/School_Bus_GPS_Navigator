package com.example.schoolbusapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BusAdapter(
    private val buses: List<Bus>,
    private val onEditClick: (Bus) -> Unit,
    private val onDeleteClick: (Bus) -> Unit
) : RecyclerView.Adapter<BusAdapter.BusViewHolder>() {

    class BusViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvBusId: TextView = itemView.findViewById(R.id.tvBusId)
        val tvRoute: TextView = itemView.findViewById(R.id.tvRoute)
        val tvDriverEmail: TextView = itemView.findViewById(R.id.tvDriverEmail)
        val btnEditBus: Button = itemView.findViewById(R.id.btnEditBus)
        val btnDeleteBus: Button = itemView.findViewById(R.id.btnDeleteBus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BusViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bus, parent, false)
        return BusViewHolder(view)
    }

    override fun onBindViewHolder(holder: BusViewHolder, position: Int) {
        val bus = buses[position]

        holder.tvBusId.text = "Bus ID: ${bus.busId}"
        holder.tvRoute.text = "Route: ${bus.route}"
        holder.tvDriverEmail.text = "Driver Email: ${bus.driverEmail}"

        holder.btnEditBus.setOnClickListener {
            onEditClick(bus)
        }

        holder.btnDeleteBus.setOnClickListener {
            onDeleteClick(bus)
        }
    }

    override fun getItemCount(): Int = buses.size
}