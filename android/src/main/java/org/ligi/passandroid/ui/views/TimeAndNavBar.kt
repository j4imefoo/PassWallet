package org.ligi.passandroid.ui.views

import android.annotation.SuppressLint
import android.content.Context
import androidx.appcompat.widget.AppCompatDrawableManager
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import org.ligi.passandroid.R

@SuppressLint("RestrictedApi") // FIXME: temporary workaround for false-positive
class TimeAndNavBar constructor(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {

    init {
        LayoutInflater.from(context).inflate(R.layout.time_and_nav, this)
        AppCompatDrawableManager.get().apply {

            val timeDrawable = getDrawable(context, R.drawable.ic_card_date_20)

            findViewById<TextView>(R.id.timeButton).setCompoundDrawablesRelativeWithIntrinsicBounds(timeDrawable, null, null, null)

            val navDrawable = getDrawable(context, R.drawable.ic_card_location_20)
            findViewById<TextView>(R.id.locationButton).setCompoundDrawablesRelativeWithIntrinsicBounds(navDrawable, null, null, null)
        }
    }

}
