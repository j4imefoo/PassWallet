package org.ligi.passandroid.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
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
import org.ligi.passandroid.model.pass.BoardingTransitType
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.model.pass.PassType
import org.ligi.passandroid.model.pass.PassVisualClassifier
import org.ligi.passandroid.model.pass.PassVisualType
import org.ligi.passandroid.ui.rendering.BoardingTransitIcon
import org.ligi.passandroid.ui.rendering.PassRenderers

private const val LINKIFY_MASK = Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES or Linkify.PHONE_NUMBERS
private val CREDENTIAL_PURPLE = Color.rgb(102, 51, 153)

class PassViewPKFragment : Fragment() {

    private val passViewHelper by lazy { PassViewHelper(requireActivity()) }
    internal val passStore: PassStore by inject()
    lateinit var pass: Pass

    private fun processImage(view: ImageView, name: String, pass: Pass) {
        processBitmapImage(view, name, pass.getBitmap(passStore, name))
    }

    private fun processBitmapImage(view: ImageView, name: String, bitmap: Bitmap?) {
        if (bitmap != null && bitmap.width > 300) {
            view.setOnClickListener {
                val intent = Intent(view.context, TouchImageActivity::class.java)
                intent.putExtra("IMAGE", name)
                startActivity(intent)
            }
        } else {
            view.setOnClickListener(null)
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
                moreTextView.setText(if (pass.type == PassType.LOYALTY) R.string.pass_details else R.string.more)
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
        val isCredential = ensureCredentialVisualType()
        val isStoreCard = pass.type == PassType.LOYALTY
        val renderer = PassRenderers.forPass(pass)
        val frontValueColor = if (isCredential) CREDENTIAL_PURPLE else foregroundColor
        val frontLabelColor = if (isCredential) CREDENTIAL_PURPLE else labelColor

        val openFullscreenBarcode = View.OnClickListener { view ->
            passStore.currentPass = pass
            val intent = Intent(view.context, FullscreenBarcodeActivity::class.java)
            intent.putExtra(PassViewActivityBase.EXTRA_KEY_UUID, pass.id)
            startActivity(intent)
        }
        root.findViewById<View>(R.id.barcode_img).setOnClickListener(openFullscreenBarcode)
        root.findViewById<View>(R.id.barcode_panel).setOnClickListener(openFullscreenBarcode)

        BarcodeUIController(requireView(), pass.barCode, requireActivity(), passViewHelper)
        BoardingTransitIcon.ensureTransitType(pass, passStore)

        val explicitLogoBitmap = pass.getBitmap(passStore, PassBitmapDefinitions.BITMAP_LOGO)
        val logoBitmap = explicitLogoBitmap ?: pass.takeIconAsDetailLogoIfManualSimplePass()
        val logoBitmapName = if (explicitLogoBitmap == null && logoBitmap != null) PassBitmapDefinitions.BITMAP_ICON else PassBitmapDefinitions.BITMAP_LOGO
        processBitmapImage(root.findViewById(R.id.logo_img_view), logoBitmapName, logoBitmap)
        processImage(root.findViewById(R.id.footer_img_view), PassBitmapDefinitions.BITMAP_FOOTER, pass)
        processImage(root.findViewById(R.id.thumbnail_img_view), PassBitmapDefinitions.BITMAP_THUMBNAIL, pass)
        processImage(root.findViewById(R.id.strip_img_view), PassBitmapDefinitions.BITMAP_STRIP, pass)
        if (isCredential) {
            applyCredentialLayout(root)
        } else if (isStoreCard) {
            applyStoreCardLayout(root)
        }

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
                val renderedLabel = renderer.detailLabel(field)
                key?.text = renderedLabel
                key?.visibility = if (renderedLabel.isNullOrBlank()) View.GONE else View.VISIBLE
                key?.setTextColor(frontLabelColor)
                val value = v?.findViewById<TextView>(R.id.value)
                value?.text = renderer.detailValue(field)
                value?.setTextColor(frontValueColor)

                if (hint == "headerFields") {
                    if (isCredential) {
                        key?.visibility = View.GONE
                        value?.textSize = 20f
                        value?.gravity = Gravity.RIGHT
                    } else {
                        key?.visibility = View.GONE
                        value?.textSize = 13f
                        value?.gravity = Gravity.RIGHT
                    }
                }

                if (isCredential && hint == "secondaryFields") {
                    key?.textSize = 12f
                    value?.textSize = 22f
                    value?.gravity = Gravity.LEFT
                    key?.gravity = Gravity.LEFT
                }

                if (isCredential && hint == "auxiliaryFields") {
                    key?.textSize = 11f
                    value?.textSize = 17f
                }

                if (isStoreCard && hint == "primaryFields") {
                    value?.textSize = 20f
                    value?.gravity = Gravity.LEFT
                    key?.gravity = Gravity.LEFT
                }

                if (isStoreCard && hint == "secondaryFields") {
                    key?.textSize = 11f
                    value?.textSize = 18f
                    value?.gravity = Gravity.LEFT
                    key?.gravity = Gravity.LEFT
                }

                if (hint == "primaryFields") {
                    value?.textSize = when {
                        pass.type == PassType.EVENT -> 28f
                        isStoreCard -> 20f
                        else -> 32f
                    }
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

        addBoardingTransitIconIfNeeded(
            primaryContainer = fieldMap["primaryFields"],
            primaryFieldCount = fieldCount["primaryFields"] ?: 0,
            foregroundColor = foregroundColor
        )

        if (backStrBuilder.isNotEmpty()) {
            backFields.text = "$backStrBuilder".parseAsHtml()
            moreTextView.visibility = View.VISIBLE
            moreTextView.setText(if (isStoreCard) R.string.pass_details else R.string.more)
        } else {
            moreTextView.visibility = View.GONE
        }

        LinkifyCompat.addLinks(backFields, LINKIFY_MASK)

        applyPkPassColors(root, pass.accentColor, foregroundColor, isCredential)
    }

    private fun addBoardingTransitIconIfNeeded(primaryContainer: ViewGroup?, primaryFieldCount: Int, foregroundColor: Int) {
        if (pass.type != PassType.PKBOARDING && pass.type != PassType.BOARDING) return
        if (primaryFieldCount < 2 || primaryContainer !is RelativeLayout) return

        val iconView = ImageView(requireContext()).apply {
            setImageResource(transitIconFor(pass.boardingTransitType))
            setColorFilter(foregroundColor)
            contentDescription = getString(R.string.boarding_transit_icon)
            setPadding(dp(2), dp(2), dp(2), dp(2))
            layoutParams = RelativeLayout.LayoutParams(dp(34), dp(34)).apply {
                addRule(RelativeLayout.CENTER_IN_PARENT)
            }
        }

        primaryContainer.addView(iconView)
    }

    private fun transitIconFor(transitType: BoardingTransitType): Int {
        return BoardingTransitIcon.drawableFor(transitType)
    }

    private fun ensureCredentialVisualType(): Boolean {
        val classifiedVisualType = PassVisualClassifier.classify(pass)
        if (classifiedVisualType == PassVisualType.CREDENTIAL && pass.visualType != PassVisualType.CREDENTIAL) {
            pass.visualType = PassVisualType.CREDENTIAL
        }
        return pass.visualType == PassVisualType.CREDENTIAL
    }

    private fun applyCredentialLayout(root: View) {
        val logoView = root.findViewById<ImageView>(R.id.logo_img_view)
        logoView.scaleType = ImageView.ScaleType.CENTER_CROP
        logoView.setPadding(0, 0, 0, 0)
        logoView.layoutParams = FrameLayout.LayoutParams(dp(46), dp(58), Gravity.TOP or Gravity.LEFT)

        root.findViewById<ImageView>(R.id.thumbnail_img_view).visibility = View.GONE
        root.findViewById<ImageView>(R.id.footer_img_view).visibility = View.GONE

        val headerContainer = root.findViewById<LinearLayout>(R.id.header_field_container)
        headerContainer.gravity = Gravity.RIGHT
        headerContainer.setPadding(0, 0, 0, 0)

        val stripView = root.findViewById<ImageView>(R.id.strip_img_view)
        stripView.scaleType = ImageView.ScaleType.FIT_CENTER
        stripView.setPadding(0, 0, 0, 0)
        stripView.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            dp(150),
            Gravity.TOP or Gravity.CENTER_HORIZONTAL
        ).apply {
            topMargin = dp(70)
        }

        val barcodeSize = dp(150)
        root.findViewById<ImageView>(R.id.barcode_img).layoutParams = FrameLayout.LayoutParams(
            barcodeSize,
            barcodeSize,
            Gravity.CENTER
        )
        root.findViewById<View>(R.id.barcode_panel).setPadding(0, 0, 0, 0)
    }

    private fun applyStoreCardLayout(root: View) {
        val logoView = root.findViewById<ImageView>(R.id.logo_img_view)
        logoView.scaleType = ImageView.ScaleType.FIT_CENTER
        logoView.setPadding(0, dp(6), dp(6), dp(2))
        logoView.layoutParams = RelativeLayout.LayoutParams(dp(124), dp(56)).apply {
            addRule(RelativeLayout.ALIGN_PARENT_LEFT)
            addRule(RelativeLayout.ALIGN_PARENT_START)
            addRule(RelativeLayout.ALIGN_PARENT_TOP)
        }

        val headerContainer = root.findViewById<LinearLayout>(R.id.header_field_container)
        headerContainer.gravity = Gravity.RIGHT
        headerContainer.setPadding(dp(8), dp(6), 0, 0)

        root.findViewById<ImageView>(R.id.thumbnail_img_view).visibility = View.GONE
        root.findViewById<ImageView>(R.id.strip_img_view).visibility = View.GONE
        root.findViewById<ImageView>(R.id.footer_img_view).visibility = View.GONE

        root.findViewById<RelativeLayout>(R.id.primary_field_container).setPadding(0, dp(10), 0, dp(8))
        root.findViewById<LinearLayout>(R.id.secondary_field_container).setPadding(0, 0, 0, dp(4))
        root.findViewById<LinearLayout>(R.id.auxiliary_field_container).setPadding(0, 0, 0, dp(2))
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

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun readableForegroundFor(backgroundColor: Int): Int {
        return if (ColorUtils.calculateLuminance(backgroundColor) > 0.55) Color.BLACK else Color.WHITE
    }

    private fun applyPkPassColors(root: View, backgroundColor: Int, foregroundColor: Int, isCredential: Boolean) {
        val cardBackground = if (isCredential) Color.WHITE else backgroundColor
        val textColor = if (isCredential) CREDENTIAL_PURPLE else foregroundColor
        root.findViewById<CardView>(R.id.pass_card).setCardBackgroundColor(cardBackground)
        root.findViewById<TextView>(R.id.moreTextView).setTextColor(textColor)
        root.findViewById<TextView>(R.id.back_fields).setTextColor(textColor)
        root.findViewById<TextView>(R.id.barcode_alt_text).setTextColor(textColor)
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
        val passExtrasLayout = if (ensureCredentialVisualType()) {
            R.layout.pkpass_view_credential_data
        } else {
            R.layout.pkpass_view_extra_data
        }
        val passExtrasView = layoutInflater.inflate(passExtrasLayout, passExtrasContainer, false)
        passExtrasContainer.addView(passExtrasView)
    }
}
