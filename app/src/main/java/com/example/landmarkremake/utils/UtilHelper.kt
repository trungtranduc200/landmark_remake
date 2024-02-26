package com.example.landmarkremake.utils

import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object UtilHelper {
    fun getUid():String {
        return FirebaseAuth.getInstance().uid ?: "uid"
    }

    fun getDateTime(): String {
        val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }
}