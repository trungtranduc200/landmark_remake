package com.example.landmarkremake.custom

import android.R
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import com.example.landmarkremake.databinding.LayoutInputNoteBinding
import com.example.landmarkremake.model.Note


open class DialogNote(context: Context) : Dialog(context) {
    private var context: Context
    private lateinit var binding: LayoutInputNoteBinding
    private var onClickSave: () -> Unit = {}
    private var callbackNote: ((Note) -> Unit)? = null
    private lateinit var name: String
    private lateinit var note: String
    private var update: Boolean = true

    init {
        this.context = context
    }

    fun setCallbackGetContentNote(callbackNote: (Note) -> Unit): DialogNote {
        this.callbackNote = callbackNote
        return this
    }

    fun setContent(name: String, note: String): DialogNote {
        this.name = name
        this.note = note
        return this
    }

    fun isUpdate(update: Boolean) {
        this.update = update
    }

    fun setOnClick(onClickSave: () -> Unit): DialogNote {
        this.onClickSave = onClickSave
        return this
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutInputNoteBinding.inflate(layoutInflater)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)
        window?.setBackgroundDrawableResource(R.color.transparent)
        val width = ViewGroup.LayoutParams.MATCH_PARENT
        val height = ViewGroup.LayoutParams.WRAP_CONTENT
        window!!.setLayout(width, height)
        setCanceledOnTouchOutside(true)
        init()
        onClick()
    }

    private fun init() {
        binding.layoutInputNoteEdtNote.setText(note)
        binding.layoutInputNoteEdtName.setText(name)

        if (update) {
            binding.layoutInputNoteEdtNote.isEnabled = true
            binding.layoutInputNoteEdtName.isEnabled = true
            binding.layoutInputNoteBtnSaveNote.visibility = View.VISIBLE
        } else {
            binding.layoutInputNoteEdtNote.isEnabled = false
            binding.layoutInputNoteEdtName.isEnabled = false
            binding.layoutInputNoteBtnSaveNote.visibility = View.GONE
        }
    }

    private fun onClick() {
        binding.layoutInputNoteBtnSaveNote.setOnClickListener {
            callbackNote?.invoke(
                Note(
                    0.0,
                    0.0,
                    binding.layoutInputNoteEdtNote.text.toString().trim(),
                    binding.layoutInputNoteEdtName.text.toString().trim()
                )
            )
            dismiss()
        }
    }

}