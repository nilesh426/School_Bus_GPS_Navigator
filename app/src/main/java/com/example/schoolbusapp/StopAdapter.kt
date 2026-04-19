package com.example.schoolbusapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class StopAdapter(
    private val stops: List<Stop>,
    private val onDeleteClick: (Stop) -> Unit
) : RecyclerView.Adapter<StopAdapter.StopViewHolder>() {

    class StopViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvStopInfo: TextView = itemView.findViewById(R.id.tvStopInfo)
        val btnDeleteStop: Button = itemView.findViewById(R.id.btnDeleteStop)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StopViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_stop, parent, false)
        return StopViewHolder(view)
    }

    override fun onBindViewHolder(holder: StopViewHolder, position: Int) {
        val stop = stops[position]

        holder.tvStopInfo.text = "${stop.order}. ${stop.name} (${stop.lat}, ${stop.lng})"

        holder.btnDeleteStop.setOnClickListener {
            onDeleteClick(stop)
        }
    }

    override fun getItemCount(): Int = stops.size
}
