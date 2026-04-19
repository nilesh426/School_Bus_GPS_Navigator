package com.example.schoolbusapp

data class ParentNotification(
    val id: String = "",
    val text: String = "",
    val read: Boolean = false,
    val timestamp: Long = 0L
)
