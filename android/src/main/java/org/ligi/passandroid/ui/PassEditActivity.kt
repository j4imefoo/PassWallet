package org.ligi.passandroid.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.annotation.IdRes
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.koin.android.ext.android.inject
import org.ligi.kaxt.doAfterEdit
import org.ligi.passandroid.R
import org.ligi.passandroid.databinding.EditBinding
import org.ligi.passandroid.databinding.PassAppearanceSheetBinding
import org.ligi.passandroid.model.PassBitmapDefinitions
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.pass.BarCode
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.model.pass.PassBarCodeFormat
import org.ligi.passandroid.model.pass.PassImpl
import org.ligi.passandroid.ui.edit.FieldsEditFragment
import org.ligi.passandroid.ui.edit.ImageEditHelper
import org.ligi.passandroid.ui.edit.dialogs.showBarcodeEditDialog
import org.ligi.passandroid.ui.edit.dialogs.showCategoryPickDialog
import org.ligi.passandroid.ui.edit.dialogs.showColorPickDialog
import org.ligi.passandroid.ui.pass_view_holder.EditViewHolder

class PassEditActivity : AppCompatActivity() {

    private lateinit var binding: EditBinding
    private lateinit var currentPass: PassImpl
    private val imageEditHelper by lazy { ImageEditHelper(this, passStore) }
    private var pendingImageName: String? = null
    private var pendingScanCallback: ((String, String) -> Unit)? = null

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val imageName = pendingImageName
        if (uri != null && imageName != null) {
            imageEditHelper.importImage(uri, imageName)
            refresh(currentPass)
        }
        pendingImageName = null
    }

    private val barcodeScanLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        val scanResultFormat = data?.getStringExtra("SCAN_RESULT_FORMAT")
        val scanResult = data?.getStringExtra("SCAN_RESULT")
        if (scanResultFormat != null && scanResult != null) {
            pendingScanCallback?.invoke(scanResultFormat, scanResult)
        }
        pendingScanCallback = null
    }

    internal val passStore: PassStore by inject()

    private val passViewHelper: PassViewHelper by lazy { PassViewHelper(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = EditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        applyMaterialInsets(
            root = binding.editRoot,
            appBar = binding.appbar,
            content = binding.editScroll,
        )

        binding.categoryView.setOnClickListener {
            showAppearanceSheet()
        }
        binding.passTitle.doAfterEdit {
            currentPass.description = "$it"
        }

        val currentPass = passStore.currentPass
        if (currentPass != null) {
            this.currentPass = currentPass as PassImpl
        } else {
            finish()
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        supportFragmentManager.commit {
            add(R.id.container_for_primary_fields, FieldsEditFragment.create(false))
            add(R.id.container_for_secondary_fields, FieldsEditFragment.create(true))
        }

        binding.addBarcodeButton.setOnClickListener {
            showBarcodeEditDialog(this@PassEditActivity,
                    refreshCallback,
                    this@PassEditActivity.currentPass,
                    BarCode(PassBarCodeFormat.QR_CODE, ""),
                    ::launchBarcodeScan)
        }
    }

    private fun showAppearanceSheet() {
        val sheet = BottomSheetDialog(this)
        val sheetBinding = PassAppearanceSheetBinding.inflate(layoutInflater)
        sheet.setContentView(sheetBinding.root)

        sheetBinding.selectCategory.setOnClickListener {
            sheet.dismiss()
            showCategoryPickDialog(this@PassEditActivity, currentPass, passStore, refreshCallback)
        }
        sheetBinding.selectColor.setOnClickListener {
            sheet.dismiss()
            showColorPickDialog(this@PassEditActivity, currentPass, refreshCallback)
        }
        sheetBinding.selectIcon.setOnClickListener {
            sheet.dismiss()
            pickWithPermissionCheck(PassBitmapDefinitions.BITMAP_ICON)
        }

        sheet.show()
    }

    private fun pickWithPermissionCheck(@Pass.PassBitmap imageName: String) {
        pendingImageName = imageName
        imagePickerLauncher.launch("image/*")
    }

    private fun launchBarcodeScan(onScanResult: (String, String) -> Unit) {
        pendingScanCallback = onScanResult
        barcodeScanLauncher.launch(Intent(this, ScanBarcodeActivity::class.java))
    }

    val refreshCallback = { refresh(currentPass) }

    private fun refresh(pass: Pass) {
        val passViewHolder = EditViewHolder(binding.passCard)

        passViewHolder.apply(pass, passStore, this)

        prepareImageUI(R.id.logo_img, R.id.add_logo, PassBitmapDefinitions.BITMAP_LOGO)
        prepareImageUI(R.id.strip_img, R.id.add_strip, PassBitmapDefinitions.BITMAP_STRIP)
        prepareImageUI(R.id.footer_img, R.id.add_footer, PassBitmapDefinitions.BITMAP_FOOTER)

        binding.addBarcodeButton.visibility = if (pass.barCode == null) View.VISIBLE else View.GONE
        val barcodeUIController = BarcodeUIController(window.decorView, pass.barCode, this, passViewHelper)
        barcodeUIController.getBarcodeView().setOnClickListener {
            showBarcodeEditDialog(this@PassEditActivity, refreshCallback, currentPass, currentPass.barCode!!, ::launchBarcodeScan)
        }
    }

    @Pass.PassBitmap
    private fun prepareImageUI(@IdRes logo_img: Int, @IdRes add_logo: Int, imageString: String) {
        val bitmap = currentPass.getBitmap(passStore, imageString)

        val addButton = findViewById<Button>(add_logo)!!
        addButton.visibility = if (bitmap == null) View.VISIBLE else View.GONE

        val listener = View.OnClickListener {
            pickWithPermissionCheck(imageString)
        }

        val logoImage = findViewById<ImageView>(logo_img)
        passViewHelper.setBitmapSafe(logoImage, bitmap)
        logoImage.setOnClickListener(listener)
        addButton.setOnClickListener(listener)
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        refresh(currentPass)
    }


    override fun onPause() {
        passStore.save(currentPass)
        passStore.notifyChange()
        super.onPause()
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}
