package org.ligi.passandroid.ui.pass_view_holder

import android.app.Activity
import android.graphics.Color
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import android.text.format.DateUtils
import android.view.View
import android.view.View.*
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import org.ligi.passandroid.R
import org.ligi.passandroid.functions.tryAddDateToCalendar
import org.ligi.passandroid.model.PassBitmapDefinitions
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.model.pass.PassField
import org.ligi.passandroid.ui.Visibility
import org.ligi.passandroid.ui.rendering.PassRenderers
import org.ligi.passandroid.ui.showNavigateToLocationsDialog
import org.ligi.passandroid.ui.views.BaseCategoryIndicatorView
import org.ligi.passandroid.ui.views.CategoryIndicatorViewWithIcon
import org.ligi.passandroid.ui.views.TimeAndNavBar
import org.threeten.bp.ZonedDateTime

abstract class PassViewHolder(val view: CardView) : RecyclerView.ViewHolder(view) {

    private val timeAndNavBar = view.findViewById<TimeAndNavBar>(R.id.timeAndNavBar)
    open fun apply(pass: Pass, passStore: PassStore, activity: Activity) {
        setupButtons(activity, pass)

        refresh(pass, passStore)
    }

    open fun setupButtons(activity: Activity, pass: Pass) {
        timeAndNavBar.findViewById<TextView>(R.id.timeButton).text = view.context.getString(R.string.pass_to_calendar)
        timeAndNavBar.findViewById<TextView>(R.id.locationButton).text = view.context.getString(R.string.pass_directions)

        timeAndNavBar.findViewById<TextView>(R.id.timeButton).setOnClickListener {
            getDateOrExtraText(pass)?.let { tryAddDateToCalendar(pass, view, it) }
        }

        timeAndNavBar.findViewById<TextView>(R.id.locationButton).setOnClickListener {
            activity.showNavigateToLocationsDialog(pass, false)
        }
    }

    protected open fun refresh(pass: Pass, passStore: PassStore) {
        val dateOrExtraText = getDateOrExtraText(pass)

        val noButtons = dateOrExtraText == null && pass.locations.isEmpty()

        view.findViewById<View>(R.id.actionsSeparator).visibility = getVisibilityForGlobalAndLocal(noButtons, true)
        timeAndNavBar.findViewById<TextView>(R.id.locationButton).visibility = getVisibilityForGlobalAndLocal(noButtons, pass.locations.isNotEmpty())

        timeAndNavBar.findViewById<TextView>(R.id.timeButton).visibility = getVisibilityForGlobalAndLocal(noButtons, dateOrExtraText != null)

        val iconBitmap = pass.getBitmap(passStore, PassBitmapDefinitions.BITMAP_ICON)
            ?: pass.getBitmap(passStore, PassBitmapDefinitions.BITMAP_LOGO)

        val categoryView = view.findViewById<CategoryIndicatorViewWithIcon>(R.id.categoryView)
        iconBitmap?.let { categoryView.setIcon(it) }

        categoryView.setImageByCategory(pass.type)

        categoryView.setAccentColor(pass.accentColor)
        applyPassColors(pass)
        val renderer = PassRenderers.forPass(pass)
        view.findViewById<TextView>(R.id.passTitle).text = renderer.listTitle(pass)
    }

    protected fun getDisplaySubtitle(pass: Pass): String? {
        return PassRenderers.forPass(pass).listMeta(pass)
    }

    private fun applyPassColors(pass: Pass) {
        val foregroundColor = if (ColorUtils.calculateLuminance(pass.accentColor) > 0.55) Color.BLACK else Color.WHITE
        val secondaryColor = ColorUtils.setAlphaComponent(foregroundColor, 210)
        view.setCardBackgroundColor(pass.accentColor)
        view.findViewById<TextView>(R.id.passTitle).setTextColor(foregroundColor)
        view.findViewById<TextView>(R.id.date).setTextColor(secondaryColor)
        view.findViewById<View>(R.id.actionsSeparator).setBackgroundColor(ColorUtils.setAlphaComponent(foregroundColor, 70))
    }

    fun getExtraString(pass: Pass) = pass.fields.firstOrNull { !it.hide }?.let { getExtraStringForField(it) }


    private fun getExtraStringForField(passField: PassField): String {
        val stringBuilder = StringBuilder()

        if (passField.label != null) {
            stringBuilder.append(passField.label)

            if (passField.value != null) {
                stringBuilder.append(": ")
            }
        }

        if (passField.value != null) {
            stringBuilder.append(passField.value)
        }

        return "$stringBuilder"
    }

    private fun setDateTextFromDateAndPrefix(prefix: String, relevantDate: ZonedDateTime): String {
        val relativeDateTimeString = DateUtils.getRelativeDateTimeString(
            view.context,
            relevantDate.toEpochSecond() * 1000,
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.WEEK_IN_MILLIS,
            0
        )

        return prefix + relativeDateTimeString
    }

    protected fun getTimeInfoString(pass: Pass) = when {
        pass.calendarTimespan?.from != null -> setDateTextFromDateAndPrefix("", pass.calendarTimespan!!.from!!)

        pass.validTimespans.orEmpty().isNotEmpty() && pass.validTimespans!![0].to != null -> {
            val to = pass.validTimespans!![0].to
            setDateTextFromDateAndPrefix(if (to!!.isAfter(ZonedDateTime.now())) "expires " else " expired ", to)
        }
        else -> null
    }

    private fun getDateOrExtraText(pass: Pass) = when {
        pass.calendarTimespan != null -> pass.calendarTimespan
        pass.validTimespans.orEmpty().isNotEmpty() -> pass.validTimespans!![0]
        else -> null
    }

    @Visibility
    protected open fun getVisibilityForGlobalAndLocal(global: Boolean, local: Boolean) = when {
        global -> GONE
        local -> VISIBLE
        else -> INVISIBLE
    }

}
