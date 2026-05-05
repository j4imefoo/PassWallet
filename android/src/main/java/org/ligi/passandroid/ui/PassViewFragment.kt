package org.ligi.passandroid.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.text.util.Linkify
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.graphics.ColorUtils
import androidx.core.text.parseAsHtml
import androidx.core.text.util.LinkifyCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import org.koin.android.ext.android.inject
import org.ligi.passandroid.R
import org.ligi.passandroid.model.PassBitmapDefinitions
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.pass.Pass

private const val LINKIFY_MASK = Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES or Linkify.PHONE_NUMBERS

class PassViewFragment : Fragment() {

    private val passViewHelper by lazy { PassViewHelper(requireActivity()) }
    internal val passStore: PassStore by inject()
    lateinit var pass: Pass

    private fun processImage(view: ImageView, name: String, pass: Pass) {
        processBitmapImage(view, name, pass, pass.getBitmap(passStore, name))
    }

    private fun processBitmapImage(view: ImageView, name: String, pass: Pass, bitmap: Bitmap?) {
        if (bitmap != null && bitmap.width > 300) {
            view.setOnClickListener {
                val intent = Intent(view.context, TouchImageActivity::class.java)
                intent.putExtra("IMAGE", name)
                startActivity(intent)
            }
        }
        passViewHelper.setBitmapSafe(view, bitmap)
    }

    override fun onResume() {
        super.onResume()

        val root = requireView()
        val isStoreCardLike = pass.looksLikeLegacyStoreCard()
        val moreTextView = root.findViewById<TextView>(R.id.moreTextView)
        val back_fields = root.findViewById<TextView>(R.id.back_fields)
        moreTextView.setOnClickListener {

            if (back_fields.isVisible) {
                back_fields.visibility = View.GONE
                moreTextView.setText(if (isStoreCardLike) R.string.pass_details else R.string.more)
            } else {
                back_fields.visibility = View.VISIBLE
                moreTextView.setText(R.string.less)
            }
        }

        val openFullscreenBarcode = View.OnClickListener { view ->
            passStore.currentPass = pass
            val intent = Intent(view.context, FullscreenBarcodeActivity::class.java)
            intent.putExtra(PassViewActivityBase.EXTRA_KEY_UUID, pass.id)
            startActivity(intent)
        }
        root.findViewById<View>(R.id.barcode_img).setOnClickListener(openFullscreenBarcode)
        root.findViewById<View>(R.id.barcode_panel).setOnClickListener(openFullscreenBarcode)

        BarcodeUIController(requireView(), pass.barCode, requireActivity(), passViewHelper)

        val explicitLogoBitmap = pass.getBitmap(passStore, PassBitmapDefinitions.BITMAP_LOGO)
        val logoBitmap = explicitLogoBitmap ?: pass.takeIconAsDetailLogoIfManualSimplePass()
        val logoBitmapName = if (explicitLogoBitmap == null && logoBitmap != null) PassBitmapDefinitions.BITMAP_ICON else PassBitmapDefinitions.BITMAP_LOGO
        processBitmapImage(root.findViewById(R.id.logo_img_view), logoBitmapName, pass, logoBitmap)
        processImage(root.findViewById(R.id.footer_img_view), PassBitmapDefinitions.BITMAP_FOOTER, pass)
        processImage(root.findViewById(R.id.thumbnail_img_view), PassBitmapDefinitions.BITMAP_THUMBNAIL, pass)
        processImage(root.findViewById(R.id.strip_img_view), PassBitmapDefinitions.BITMAP_STRIP, pass)
        if (isStoreCardLike || logoBitmap != null && pass.getBitmap(passStore, PassBitmapDefinitions.BITMAP_LOGO) == null) {
            applyLegacyStoreCardImagePosition(root)
        }

        val backStrBuilder = StringBuilder()
        val foregroundColor = readableForegroundFor(pass.accentColor)
        val secondaryColor = ColorUtils.setAlphaComponent(foregroundColor, 210)

        val front_field_container = root.findViewById<LinearLayout>(R.id.front_field_container)
        front_field_container.removeAllViews()

        for (field in pass.fields) {
            if (field.hide) {
                backStrBuilder.append(field.toHtmlSnippet())
            } else {
                if (isStoreCardLike && field.isGeneratedTypeField()) continue
                val v = requireActivity().layoutInflater.inflate(R.layout.main_field_item, front_field_container, false)
                val key = v?.findViewById<TextView>(R.id.key)
                val value = v?.findViewById<TextView>(R.id.value)
                val fieldLabel = field.label
                val fieldValue = field.value
                val displayValue = if (isStoreCardLike && fieldValue.isNullOrBlank()) fieldLabel else fieldValue
                val displayLabel = if (isStoreCardLike && fieldValue.isNullOrBlank()) null else fieldLabel
                key?.text = displayLabel
                key?.visibility = if (displayLabel.isNullOrBlank()) View.GONE else View.VISIBLE
                key?.setTextColor(secondaryColor)
                value?.text = displayValue
                value?.setTextColor(foregroundColor)

                if (isStoreCardLike && field.key.equals("caduca", ignoreCase = true)) {
                    key?.visibility = View.GONE
                    value?.textSize = 13f
                    value?.gravity = Gravity.RIGHT
                }

                front_field_container.addView(v)
                key?.let { LinkifyCompat.addLinks(it, LINKIFY_MASK) }
                value?.let { LinkifyCompat.addLinks(it, LINKIFY_MASK) }
            }
        }

        if (backStrBuilder.isNotEmpty()) {
            back_fields.text = "$backStrBuilder".parseAsHtml()
            moreTextView.visibility = View.VISIBLE
            moreTextView.setText(if (isStoreCardLike) R.string.pass_details else R.string.more)
        } else {
            moreTextView.visibility = View.GONE
        }

        LinkifyCompat.addLinks(back_fields, LINKIFY_MASK)
        applyGenericPassColors(root, pass.accentColor, foregroundColor)
    }

    private fun readableForegroundFor(backgroundColor: Int): Int {
        return if (ColorUtils.calculateLuminance(backgroundColor) > 0.55) Color.BLACK else Color.WHITE
    }

    private fun applyGenericPassColors(root: View, backgroundColor: Int, foregroundColor: Int) {
        root.findViewById<CardView>(R.id.pass_card).setCardBackgroundColor(backgroundColor)
        root.findViewById<TextView>(R.id.moreTextView).setTextColor(foregroundColor)
        root.findViewById<TextView>(R.id.back_fields).setTextColor(foregroundColor)
        root.findViewById<TextView>(R.id.barcode_alt_text).setTextColor(foregroundColor)
    }

    private fun applyLegacyStoreCardImagePosition(root: View) {
        root.findViewById<ImageView>(R.id.thumbnail_img_view).visibility = View.GONE
        root.findViewById<ImageView>(R.id.strip_img_view).visibility = View.GONE
        root.findViewById<ImageView>(R.id.footer_img_view).visibility = View.GONE

        val logoView = root.findViewById<ImageView>(R.id.logo_img_view)
        val parent = logoView.parent as? LinearLayout ?: return
        parent.removeView(logoView)
        logoView.scaleType = ImageView.ScaleType.FIT_CENTER
        logoView.setPadding(dp(14), dp(10), dp(14), 0)
        logoView.layoutParams = LinearLayout.LayoutParams(dp(150), dp(64)).apply {
            gravity = Gravity.LEFT
        }
        parent.addView(logoView, 0)
    }

    private fun Pass.looksLikeLegacyStoreCard(): Boolean {
        val haystack = buildString {
            append(description.orEmpty()).append(' ')
            append(creator.orEmpty()).append(' ')
            fields.forEach { field ->
                append(field.key.orEmpty()).append(' ')
                append(field.label.orEmpty()).append(' ')
                append(field.value.orEmpty()).append(' ')
            }
        }.lowercase()
        return haystack.contains("renfe") ||
            haystack.contains("numtarjeta") ||
            haystack.contains("nº mas") ||
            haystack.contains("tarjeta plata")
    }

    private fun Pass.takeIconAsDetailLogoIfManualSimplePass(): Bitmap? {
        if (app != "passandroid") return null
        if (getBitmap(passStore, PassBitmapDefinitions.BITMAP_LOGO) != null) return null
        if (getBitmap(passStore, PassBitmapDefinitions.BITMAP_STRIP) != null) return null
        if (getBitmap(passStore, PassBitmapDefinitions.BITMAP_THUMBNAIL) != null) return null
        if (getBitmap(passStore, PassBitmapDefinitions.BITMAP_FOOTER) != null) return null
        if (barCode == null) return null
        if (fields.count { !it.hide } > 2) return null
        return getBitmap(passStore, PassBitmapDefinitions.BITMAP_ICON)
    }

    private fun org.ligi.passandroid.model.pass.PassField.isGeneratedTypeField(): Boolean {
        return key.isNullOrBlank() && label.equals(getString(R.string.type), ignoreCase = true)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {

        val rootView = inflater.inflate(R.layout.activity_pass_view_page, container, false)
        arguments?.takeIf { it.containsKey(PassViewActivityBase.EXTRA_KEY_UUID) }?.apply {
            val uuid = getString(PassViewActivityBase.EXTRA_KEY_UUID)
            pass = if (uuid != null) {
                passStore.getPassbookForId(uuid) ?: passStore.currentPass
            } else {
                passStore.currentPass
            } ?: return rootView
        }

        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val passExtrasContainer = view.findViewById<LinearLayout>(R.id.passExtrasContainer)

        val passExtrasView = layoutInflater.inflate(R.layout.pass_view_extra_data, passExtrasContainer, false)
        passExtrasContainer.addView(passExtrasView)
    }
}
