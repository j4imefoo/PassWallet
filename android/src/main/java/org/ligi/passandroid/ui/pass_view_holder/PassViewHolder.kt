package org.ligi.passandroid.ui.pass_view_holder

import android.app.Activity
import android.graphics.Color
import android.text.format.DateUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import org.ligi.passandroid.R
import org.ligi.passandroid.model.PassBitmapDefinitions
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.model.pass.PassField
import org.ligi.passandroid.model.pass.PassType
import org.ligi.passandroid.ui.rendering.BoardingTransitIcon
import org.ligi.passandroid.ui.rendering.PassRenderers
import org.ligi.passandroid.ui.views.CategoryIndicatorViewWithIcon
import org.threeten.bp.ZonedDateTime

abstract class PassViewHolder(val view: CardView) : RecyclerView.ViewHolder(view) {

    open fun apply(pass: Pass, passStore: PassStore, activity: Activity) {
        refresh(pass, passStore)
    }

    protected open fun refresh(pass: Pass, passStore: PassStore) {
        val iconBitmap = pass.getBitmap(passStore, PassBitmapDefinitions.BITMAP_ICON)
            ?: pass.getBitmap(passStore, PassBitmapDefinitions.BITMAP_LOGO)

        val categoryView = view.findViewById<CategoryIndicatorViewWithIcon>(R.id.categoryView)
        iconBitmap?.let { categoryView.setIcon(it) }

        BoardingTransitIcon.ensureTransitType(pass, passStore)
        categoryView.setImageByCategory(pass.type)
        if (pass.type == PassType.PKBOARDING || pass.type == PassType.BOARDING) {
            categoryView.findViewById<ImageView>(R.id.topImageView)
                .setImageResource(BoardingTransitIcon.drawableFor(pass.boardingTransitType))
        }
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
}
