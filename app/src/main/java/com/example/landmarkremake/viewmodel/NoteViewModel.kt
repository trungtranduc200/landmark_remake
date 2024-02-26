package com.example.landmarkremake.viewmodel

import androidx.lifecycle.ViewModel
import com.example.landmarkremake.callback.OnGetListNoteListener
import com.example.landmarkremake.callback.OnSaveNoteListener
import com.example.landmarkremake.model.Note
import com.example.landmarkremake.repository.NoteRepository

class NoteViewModel:ViewModel() {
    suspend fun saveNote(note: Note, listener: OnSaveNoteListener){
        try {
            NoteRepository.saveNote(note)
            listener.onSaveNoteSuccess("Save data success")
        } catch (e: Exception) {
            listener.onSaveNoteError(e.message.toString())
        }
    }

    suspend fun getData(listener: OnGetListNoteListener){
        try {
            val data = NoteRepository.getData()
            listener.onGetNoteSuccess(data)
        } catch (e: Exception) {
            listener.onGetNoteError(e.message.toString())
        }
    }
}