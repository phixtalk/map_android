package com.mapapp.utils

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.mapapp.R

fun Context.toast(message: String){
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

fun View.setRoundedStrokeBackground(radius: Float?, backgroundColor: Int? = null, strokeWidth: Int? = null, strokeColor: Int? = null) {
    addOnLayoutChangeListener(object: View.OnLayoutChangeListener {
        override fun onLayoutChange(v: View?, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int) {

            val shape = GradientDrawable()

            if(radius!=null)
                shape.cornerRadius = radius

            if(strokeWidth!=null && strokeColor!=null)
                shape.setStroke(strokeWidth, ContextCompat.getColor(context, strokeColor))

            if(backgroundColor!=null)
                shape.setColor(ContextCompat.getColor(context, backgroundColor))

            background = shape

            removeOnLayoutChangeListener(this)
        }
    })
}

fun showDialog(context: Context, title: String, message: String, functionCall: (() -> Unit)) {
    val builder: AlertDialog.Builder = AlertDialog.Builder(context)
    builder.setTitle(title)
        .setMessage(message)
        .setPositiveButton(context.getString(R.string.ok)) {
                _, _ -> functionCall()
        }
        .setNegativeButton(context.getString(R.string.cancel)) {
                dialog, _ -> dialog.cancel()
        }
    val alert: AlertDialog = builder.create()
    alert.show()
}