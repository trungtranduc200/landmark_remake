package com.example.landmarkremake.utils

import android.content.Context
import com.example.landmarkremake.custom.DialogNote
import com.example.landmarkremake.model.Note

object DialogUtils {
    fun createNote(context: Context,name:String,note:String,update:Boolean,callbackNote: (Note) -> Unit){
        val dialog = DialogNote(context)
        dialog.setCallbackGetContentNote(callbackNote).setContent(name,note).isUpdate(update)
        dialog.show()
    }
}