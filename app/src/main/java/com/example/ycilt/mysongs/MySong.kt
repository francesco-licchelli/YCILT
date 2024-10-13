package com.example.ycilt.mysongs

data class MySong(
    val id: Int,
    val latitude: Double,
    val longitude: Double,
    var hidden: Boolean,
    val duration: Long, // Durata in millisecondi
    val date: String // Orario di registrazione
)
