package com.example.landmarkremake.model

import java.io.Serializable

data class Note(
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var note: String = "",
    var username: String = ""
) : Serializable
