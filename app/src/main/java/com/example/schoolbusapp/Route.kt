package com.example.schoolbusapp

data class Route(
    val id: String = "",
    val name: String = "",
    val stops: List<Stop> = emptyList()
)
