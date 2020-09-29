package com.example.dailylog.filemanager

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import android.widget.Toast
import com.example.dailylog.Constants
import com.example.dailylog.R


class FileFormatSettingsView(private var dateTimeFormat: String?, var filename: String?, private var view: View, private var context: Context) {
    private var presenter: FileFormatSettingsPresenter? = null

    fun setPresenter(presenter: FileFormatSettingsPresenter) {
        this.presenter = presenter
    }

    fun render() {
        renderDateFormatRow()
        renderFileNameRow()
    }

    private fun renderDateFormatRow() {
        val dateFormatEditText = view.findViewById<TextView>(R.id.dateFormat)
        dateFormatEditText.text = dateTimeFormat
        dateFormatEditText.hint = context.resources.getString(
            R.string.defaultStringPlaceholder,
            Constants.DATE_TIME_DEFAULT_FORMAT
        )
        dateFormatEditText.setOnEditorActionListener(OnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val saved = this.presenter?.saveDateTimeFormat(v.text.toString())
                if (saved == null || !saved) {
                    dateFormatEditText.setTextColor(Color.RED)
                    Toast.makeText(context, "Unsupported date format, try something else", Toast.LENGTH_LONG).show()
                    return@OnEditorActionListener true
                } else {
                    dateFormatEditText.setTextColor(context.getColor(R.color.primaryText))
                }
            }
            false
        })
    }

    private fun renderFileNameRow() {
        val fileNameTitle = view.findViewById<TextView>(R.id.fileName)
        fileNameTitle.text = filename
    }

}