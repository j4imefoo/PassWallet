package org.ligi.passandroid.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import androidx.core.net.toUri
import org.ligi.passandroid.R
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.model.pass.PassLocation
import java.io.UnsupportedEncodingException
import java.net.URLEncoder


fun Activity.showNavigateToLocationsDialog(pass: Pass, finishOnDone: Boolean) {
    val locations = pass.locations

    when {
        locations.isEmpty() -> done(this, finishOnDone)
        locations.size == 1 -> {
            startIntentForLocation(this, locations.first(), pass)
            done(this, finishOnDone)
        }
        locations.size > 1 -> {
            showLocationPickerSheet(this, pass, locations, finishOnDone)
        }
    }
}

private fun showLocationPickerSheet(
    activity: Activity,
    pass: Pass,
    locations: List<PassLocation>,
    finishOnDone: Boolean,
) {
    val inflater = LayoutInflater.from(activity)
    val sheet = BottomSheetDialog(activity)
    val sheetView = inflater.inflate(R.layout.location_pick_sheet, null)
    val container = sheetView.findViewById<LinearLayout>(R.id.locationPickContainer)
    val rowMargin = activity.resources.getDimensionPixelSize(R.dimen.rhythm)

    locations.forEach { location ->
        val row = inflater.inflate(R.layout.item_location_pick, container, false) as MaterialCardView
        row.findViewById<TextView>(R.id.locationPickName).text = location.getNameWithFallback(pass)
        row.findViewById<TextView>(R.id.locationPickCoordinates).text = location.getCommaSeparated()
        row.setOnClickListener {
            sheet.dismiss()
            startIntentForLocation(activity, location, pass)
            done(activity, finishOnDone)
        }
        container.addView(
            row,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = rowMargin
            }
        )
    }

    sheet.setContentView(sheetView)
    sheet.show()
}

private fun done(activity: Activity, finishOnDone: Boolean) {
    if (finishOnDone) {
        activity.finish()
    }
}

private fun startIntentForLocation(activity: Activity, location: PassLocation, pass: Pass) {
    val i = Intent(Intent.ACTION_VIEW)

    val description = getEncodedDescription(location, pass)

    val latAndLonStr = location.getCommaSeparated()
    i.data = "geo:$latAndLonStr?q=$latAndLonStr($description)".toUri()
    try {
        activity.startActivity(i)
    } catch (e: ActivityNotFoundException) {
        i.data = "https://www.openstreetmap.org/?mlat=${location.lat}&mlon=${location.lon}#map=16/${location.lat}/${location.lon}".toUri()
        activity.startActivity(i)
        // TODO also the browser could not be found -> handle
    }

}

private fun getEncodedDescription(location: PassLocation, pass: Pass) = try {
    URLEncoder.encode(location.getNameWithFallback(pass), "UTF-8")
} catch (e1: UnsupportedEncodingException) {
    // OK - no description
    ""
}
