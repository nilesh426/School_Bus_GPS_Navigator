package com.example.schoolbusapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RouteAdapter(
    private val routes: List<Route>,
    private val onEditClick: (Route) -> Unit,
    private val onDeleteClick: (Route) -> Unit,
    private val onAssignClick: (Route) -> Unit
) : RecyclerView.Adapter<RouteAdapter.RouteViewHolder>() {

    class RouteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvRouteName: TextView = itemView.findViewById(R.id.tvRouteName)
        val tvStopsCount: TextView = itemView.findViewById(R.id.tvStopsCount)
        val btnEditRoute: Button = itemView.findViewById(R.id.btnEditRoute)
        val btnDeleteRoute: Button = itemView.findViewById(R.id.btnDeleteRoute)
        val btnAssignRoute: Button = itemView.findViewById(R.id.btnAssignRoute)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_route, parent, false)
        return RouteViewHolder(view)
    }

    override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
        val route = routes[position]

        holder.tvRouteName.text = "Route: ${route.name}"
        holder.tvStopsCount.text = "Stops: ${route.stops.size}"

        holder.btnEditRoute.setOnClickListener {
            onEditClick(route)
        }

        holder.btnDeleteRoute.setOnClickListener {
            onDeleteClick(route)
        }

        holder.btnAssignRoute.setOnClickListener {
            onAssignClick(route)
        }
    }

    override fun getItemCount(): Int = routes.size
}
