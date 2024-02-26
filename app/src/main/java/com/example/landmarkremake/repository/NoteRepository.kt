package com.example.landmarkremake.repository

import android.annotation.SuppressLint
import com.example.landmarkremake.model.Note
import com.example.landmarkremake.utils.Constants
import com.example.landmarkremake.utils.UtilHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.tasks.await

object NoteRepository {
    @SuppressLint("StaticFieldLeak")
    private val db = FirebaseFirestore.getInstance()

    suspend fun saveNote(note: Note) {
        try {
            db.collection(Constants.DB_NAME)
                .document(UtilHelper.getUid())
                .collection(Constants.LIST_NOTE)
                .document(UtilHelper.getDateTime())
                .set(note)
                .await()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun getData():List<Note>{
        val dataList = mutableListOf<Note>()
        try {
            val data = db.collection(Constants.DB_NAME)
                .document(UtilHelper.getUid())
                .collection(Constants.LIST_NOTE)
                .get()
                .await()
            if (data.documents.isNotEmpty()){
                for (item in data){
                    val dataItem = item.toObject(Note::class.java)
                    dataList.add(dataItem)
                }
            }
        } catch (e: Exception) {
            throw e
        }
        return dataList
    }
}