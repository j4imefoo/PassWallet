package org.ligi.passandroid.ui.pass_view_holder

import android.app.Activity
import androidx.cardview.widget.CardView
import android.view.View.VISIBLE
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import org.ligi.passandroid.R
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.model.pass.PassImpl
import org.ligi.passandroid.ui.Visibility
import org.ligi.passandroid.ui.edit.dialogs.showLocationEditDialog
import org.ligi.passandroid.ui.views.TimeAndNavBar
import org.threeten.bp.Instant
import org.threeten.bp.ZoneOffset
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.temporal.ChronoUnit

class EditViewHolder(view: CardView) : VerbosePassViewHolder(view) {

    private lateinit var time: ZonedDateTime
    private lateinit var pass: PassImpl
    private lateinit var passStore: PassStore

    override fun apply(pass: Pass, passStore: PassStore, activity: Activity) {
        super.apply(pass, passStore, activity)

        this.pass = pass as PassImpl
        this.passStore = passStore

        val calendarTimespan = pass.calendarTimespan
        time = if (calendarTimespan?.from != null) {
            calendarTimespan.from
        } else {
            val roundedTime = ZonedDateTime.now().truncatedTo(ChronoUnit.MINUTES)
            roundedTime.plusMinutes(30L - (roundedTime.minute % 30))
        }
    }

    override fun setupButtons(activity: Activity, pass: Pass) {

        val timeAndNavBar = view.findViewById<TimeAndNavBar>(R.id.timeAndNavBar)
        timeAndNavBar.findViewById<TextView>(R.id.timeButton) .text = view.context.getString(R.string.edit_time)
        timeAndNavBar.findViewById<TextView>(R.id.locationButton) .text = view.context.getString(R.string.edit_location)

        timeAndNavBar.findViewById<TextView>(R.id.timeButton) .setOnClickListener {
            showDatePicker(activity)
        }

        timeAndNavBar.findViewById<TextView>(R.id.locationButton) .setOnClickListener {
            showLocationEditDialog(activity, this.pass) {
                refresh(this.pass, passStore)
            }
        }

    }

    @Visibility
    override fun getVisibilityForGlobalAndLocal(global: Boolean, local: Boolean): Int {
        return VISIBLE
    }

    private fun showDatePicker(activity: Activity) {
        val fragmentActivity = activity as? FragmentActivity ?: return
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.edit_time_date_title)
            .setSelection(time.toUtcDateSelection())
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            val selectedDate = Instant.ofEpochMilli(selection).atZone(ZoneOffset.UTC).toLocalDate()
            time = time
                .withYear(selectedDate.year)
                .withMonth(selectedDate.monthValue)
                .withDayOfMonth(selectedDate.dayOfMonth)
            pass.calendarTimespan = PassImpl.TimeSpan(time, null, null)
            showTimePicker(fragmentActivity)
        }

        picker.show(fragmentActivity.supportFragmentManager, "pass_date_picker")
    }

    private fun showTimePicker(activity: FragmentActivity) {
        val picker = MaterialTimePicker.Builder()
            .setTitleText(R.string.edit_time_time_title)
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(time.hour)
            .setMinute(time.minute)
            .build()

        picker.addOnPositiveButtonClickListener {
            time = time.withHour(picker.hour).withMinute(picker.minute)
            pass.calendarTimespan = PassImpl.TimeSpan(time, null, null)
            refresh(pass, passStore)
        }

        picker.show(activity.supportFragmentManager, "pass_time_picker")
    }

    private fun ZonedDateTime.toUtcDateSelection(): Long =
        toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}
