package net.steamcrafted.loadtoast

import android.app.Activity
import android.widget.Toast

class LoadToast(private val activity: Activity) {

    private var text: String = ""

    fun setTranslationY(pixels: Int): LoadToast = this

    fun setText(message: String): LoadToast {
        text = message
        return this
    }

    fun setTextColor(color: Int): LoadToast = this

    fun setBackgroundColor(color: Int): LoadToast = this

    fun setProgressColor(color: Int): LoadToast = this

    fun show(): LoadToast {
        if (text.isNotEmpty()) {
            Toast.makeText(activity, text, Toast.LENGTH_SHORT).show()
        }
        return this
    }

    fun success() {
    }

    fun error() {
        if (text.isNotEmpty()) {
            Toast.makeText(activity, text, Toast.LENGTH_SHORT).show()
        }
    }
}
