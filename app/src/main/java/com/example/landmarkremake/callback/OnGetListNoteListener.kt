package com.example.landmarkremake.callback

import com.example.landmarkremake.model.Note

interface OnGetListNoteListener {
    fun onGetNoteSuccess(data:List<Note>)
    fun onGetNoteError(message:String)
}