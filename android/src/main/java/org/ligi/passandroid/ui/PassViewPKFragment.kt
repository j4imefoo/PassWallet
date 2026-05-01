package org.ligi.passandroid.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.RelativeLayout
import android.widget.LinearLayout
import androidx.cardview.widget.CardView
import androidx.core.graphics.ColorUtils
import androidx.core.text.parseAsHtml
import androidx.core.text.util.LinkifyCompat
import androidx.fragment.app.Fragment
import org.koin.android.ext.android.inject
import org.ligi.passandroid.R
import org.ligi.passandroid.model.PassBitmapDefinitions
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.model.pass.PassType
import org.ligi.passandroid.ui.pass_view_holder.VerbosePassViewHolder
import org.ligi.passandroid.ui.rendering.PassRenderers

private const val LINKIFY_MASK = Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES or Linkify.PHONE_NUMBERS

class PassViewPKFragment : Fragment() {

    private val passViewHelper by lazy { PassViewHelper(requireActivity()) }
    internal val passStore: PassStore by inject()
    lateinit var pass: Pass

    private fun processImage(view: ImageView, name: String, pass: Pass) {
        val bitmap = pass.getBitmap(passStore, name)
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
        val backFields = root.findViewById<TextView>(R.id.back_fields)
        val moreTextView = root.findViewById<TextView>(R.id.moreTextView)

        moreTextView.setOnClickListener {
            if (backFields.visibility == View.VISIBLE) {
                backFields.visibility = View.GONE
                moreTextView.setText(R.string.more)
            } else {
                backFields.visibility = View.VISIBLE
                moreTextView.setText(R.string.less)
            }
        }

        val fieldMap : Map<String, ViewGroup> = mapOf(
            "primaryFields" to root.findViewById(R.id.primary_field_container),
            "auxiliaryFields" to root.findViewById(R.id.auxiliary_field_container),
            "headerFields" to root.findViewById(R.id.header_field_container),
            "secondaryFields" to root.findViewById(R.id.secondary_field_container)
        )

        val fieldCount = mutableMapOf<String, Int>()

        fieldMap.forEach {
            fieldCount[it.key] = 0
        }

        val foregroundColor = readableForegroundFor(pass.accentColor)
        val labelColor = ColorUtils.setAlphaComponent(foregroundColor, 210)
        val renderer = PassRenderers.forPass(pass)

        val openFullscreenBarcode = View.OnClickListener { view ->
            passStore.currentPass = pass
            val intent = Intent(view.context, FullscreenBarcodeActivity::class.java)
            intent.putExtra(PassViewActivityBase.EXTRA_KEY_UUID, pass.id)
            startActivity(intent)
        }
        root.findViewById<View>(R.id.barcode_img).setOnClickListener(openFullscreenBarcode)
        root.findViewById<View>(R.id.barcode_panel).setOnClickListener(openFullscreenBarcode)

        BarcodeUIController(requireView(), pass.barCode, requireActivity(), passViewHelper)

        processImage(root.findViewById(R.id.logo_img_view), PassBitmapDefinitions.BITMAP_LOGO, pass)
        processImage(root.findViewById(R.id.footer_img_view), PassBitmapDefinitions.BITMAP_FOOTER, pass)
        processImage(root.findViewById(R.id.thumbnail_img_view), PassBitmapDefinitions.BITMAP_THUMBNAIL, pass)
        processImage(root.findViewById(R.id.strip_img_view), PassBitmapDefinitions.BITMAP_STRIP, pass)

        val backStrBuilder = StringBuilder()

        fieldMap.forEach {
            it.value.removeAllViews()
        }

        for (field in pass.fields) {
            val hint = field.hint
            if (field.hide) {
                backStrBuilder.append(field.toHtmlSnippet())
            } else if (hint != null && renderer.showOnDetailFront(field)) {
                val v = requireActivity().layoutInflater.inflate(R.layout.vertical_field_item, root.findViewById(R.id.header_field_container), false)
                val key = v?.findViewById<TextView>(R.id.key)
                key?.text = renderer.detailLabel(field)
                key?.setTextColor(labelColor)
                val value = v?.findViewById<TextView>(R.id.value)
                value?.text = renderer.detailValue(field)
                value?.setTextColor(foregroundColor)

                if (hint == "headerFields") {
                    key?.visibility = View.GONE
                    value?.textSize = 13f
                    value?.gravity = Gravity.RIGHT
                }

                if (hint == "primaryFields") {
                    value?.textSize = if (pass.type == PassType.EVENT) 28f else 32f
                    key?.textSize = 13f
                    val params = RelativeLayout.LayoutParams(
                        if (pass.type == PassType.EVENT) RelativeLayout.LayoutParams.MATCH_PARENT else RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT
                    )
                    if (fieldCount[hint]!! == 0) {
                        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
                        value?.gravity = Gravity.LEFT
                        key?.gravity = Gravity.LEFT
                        if (pass.type == PassType.EVENT) {
                            value?.maxLines = 1
                            value?.setSingleLine(true)
                        }
                        v?.layoutParams = params
                    } else {
                        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
                        value?.gravity = Gravity.RIGHT
                        key?.gravity = Gravity.RIGHT
                        v?.layoutParams = params
                    }
                }
                fieldMap[hint]!!.addView(v)
                key?.let { LinkifyCompat.addLinks(it, LINKIFY_MASK) }
                value?.let { LinkifyCompat.addLinks(it, LINKIFY_MASK) }
                fieldCount[hint] = 1 + fieldCount[hint]!!
            }
        }

        if (backStrBuilder.isNotEmpty()) {
            backFields.text = "$backStrBuilder".parseAsHtml()
            moreTextView.visibility = View.VISIBLE
        } else {
            moreTextView.visibility = View.GONE
        }

        LinkifyCompat.addLinks(backFields, LINKIFY_MASK)

        val passViewHolder = VerbosePassViewHolder(root.findViewById(R.id.pass_card))
        passViewHolder.apply(pass, passStore, requireActivity())
        if (pass.type == PassType.EVENT) {
            root.findViewById<View>(R.id.pass_top).visibility = View.GONE
        } else {
            root.findViewById<View>(R.id.pass_top).visibility = View.VISIBLE
        }
        applyPkPassColors(root, pass.accentColor, foregroundColor, labelColor)
    }

    private fun readableForegroundFor(backgroundColor: Int): Int {
        return if (ColorUtils.calculateLuminance(backgroundColor) > 0.55) Color.BLACK else Color.WHITE
    }

    private fun isGeneratedTypeField(label: String?): Boolean {
        return label == getString(R.string.type)
    }

    private fun applyPkPassColors(root: View, backgroundColor: Int, foregroundColor: Int, labelColor: Int) {
        root.findViewById<CardView>(R.id.pass_card).setCardBackgroundColor(backgroundColor)
        root.findViewById<TextView>(R.id.passTitle).setTextColor(foregroundColor)
        root.findViewById<TextView>(R.id.date).setTextColor(labelColor)
        root.findViewById<TextView>(R.id.moreTextView).setTextColor(foregroundColor)
        root.findViewById<TextView>(R.id.back_fields).setTextColor(foregroundColor)
        root.findViewById<TextView>(R.id.barcode_alt_text).setTextColor(foregroundColor)
        root.findViewById<View>(R.id.actionsSeparator).setBackgroundColor(ColorUtils.setAlphaComponent(foregroundColor, 70))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {

        val rootView = inflater.inflate(R.layout.activity_pass_view_page, container, false)
        arguments?.takeIf { it.containsKey(PassViewActivityBase.EXTRA_KEY_UUID) }?.apply {
            val uuid = getString(PassViewActivityBase.EXTRA_KEY_UUID)
            pass = if (uuid != null) {
                passStore.getPassbookForId(uuid) ?: passStore.currentPass!!
            } else {
                passStore.currentPass!!
            }
        }
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val passExtrasContainer = view.findViewById<LinearLayout>(R.id.passExtrasContainer)
        val passExtrasView = layoutInflater.inflate(R.layout.pkpass_view_extra_data, passExtrasContainer, false)
        passExtrasContainer.addView(passExtrasView)
    }
}
