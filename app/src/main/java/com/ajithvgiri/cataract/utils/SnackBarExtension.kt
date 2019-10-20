package com.ajithvgiri.cataract.utils

import android.view.View
import androidx.core.content.ContextCompat
import com.ajithvgiri.cataract.R
import com.google.android.material.snackbar.Snackbar


inline fun View.snack(messageRes: Int, f: Snackbar.() -> Unit) {
    snack(resources.getString(messageRes), f)
}

inline fun View.snack(message: String, f: Snackbar.() -> Unit) {
    val snack = Snackbar.make(this, message, Snackbar.LENGTH_LONG)
    snack.f()
    snack.show()
}

fun Snackbar.action(actionRes: Int, color: Int? = null, listener: (View) -> Unit) {
    action(view.resources.getString(actionRes), color, listener)
}

fun Snackbar.action(action: String, color: Int? = R.color.colorAccent, listener: (View) -> Unit) {
    setAction(action, listener)
    color?.let { setActionTextColor(ContextCompat.getColor(context, color)) }
}
