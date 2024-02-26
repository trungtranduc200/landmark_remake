package com.example.landmarkremake.callback

interface OnSaveNoteListener {
    fun onSaveNoteSuccess(message:String)
    fun onSaveNoteError(message:String)
}