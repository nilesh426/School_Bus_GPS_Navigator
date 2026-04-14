package com.example.schoolbusapp

data class BoardingStudent(
    val id: String = "",
    val name: String = "",
    val studentClass: String = "",
    val parentUid: String = "",
    val busId: String = "",
    var boarded: Boolean = false,
    var dropped: Boolean = false,
    var boardedAt: Long? = null,
    var droppedAt: Long? = null

)