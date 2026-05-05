package org.ligi.passandroid.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View.GONE
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.ligi.kaxt.startActivityFromClass
import org.ligi.passandroid.R
import org.ligi.passandroid.Tracker
import org.ligi.passandroid.backup.BackupArchive
import org.ligi.passandroid.databinding.ActivityImportBinding
import org.ligi.passandroid.functions.fromURI
import org.ligi.passandroid.model.PassStore

class PassImportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImportBinding
    val tracker: Tracker by inject()
    val passStore: PassStore by inject()

    private fun importUri(): Uri? {
        intent.data?.let { return it }

        return when (intent.action) {
            Intent.ACTION_SEND -> intent.extraStreamUri()
            else -> null
        }
    }

    private fun Intent.extraStreamUri(): Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(Intent.EXTRA_STREAM)
    }

    private fun doImport() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val uri = importUri()
                if (uri == null) {
                    tracker.trackException("invalid_import_uri", false)
                    withContext(Dispatchers.Main) { finish() }
                    return@launch
                }

                val fromURI = fromURI(this@PassImportActivity, uri, tracker)

                withContext(Dispatchers.Main) {

                    binding.progressContainer.visibility = GONE

                    if (fromURI == null) {
                        showImportFailed(getString(R.string.import_error_file_message))
                    } else if (isPassWalletBackup(uri)) {
                        showImportFailed(getString(R.string.import_error_backup_message))
                    } else {

                        if (isFinishing) {
                            // finish with no UI/Dialogs
                            // let's do it silently TODO check if we need to jump to a service here as the activity is dying
                            val spec = UnzipPassController.InputStreamUnzipControllerSpec(fromURI, application, passStore, null, null)
                            UnzipPassController.processInputStream(spec)
                        } else {
                            UnzipPassDialog.show(fromURI, this@PassImportActivity, passStore) { importedIds ->
                                importedIds.forEach { id ->
                                    passStore.getPassbookForId(id)?.let { pass ->
                                        passStore.classifier.moveToTopic(pass, TopicNames.NEW)
                                    }
                                }

                                if (importedIds.size == 1) {
                                    val importedId = importedIds.first()
                                    val passbookForId = passStore.getPassbookForId(importedId)
                                    passStore.currentPass = passbookForId
                                    startActivity(
                                        Intent(this@PassImportActivity, PassViewActivity::class.java)
                                            .putExtra(PassViewActivityBase.EXTRA_KEY_UUID, importedId)
                                    )
                                } else {
                                    startActivityFromClass(PassListActivity::class.java)
                                }
                                finish()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                if (e.message?.contains("Permission") == true) {
                    withContext(Dispatchers.Main) {
                        onExternalStorageDenied()
                    }
                } else {
                    tracker.trackException("Error in import", e, false)
                    withContext(Dispatchers.Main) {
                        binding.progressContainer.visibility = GONE
                        showImportFailed(getString(R.string.import_error_generic_message, e.localizedMessage ?: e.javaClass.simpleName))
                    }
                }
            }
        }
    }

    private fun isPassWalletBackup(uri: Uri): Boolean {
        return contentResolver.openInputStream(uri)?.use { input ->
            BackupArchive.looksLikePassWalletBackup(input)
        } ?: false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (importUri()?.scheme == null) {
            tracker.trackException("invalid_import_uri", false)
            finish()
            return
        }

        binding = ActivityImportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        doImport()
    }

    private fun onExternalStorageDenied() {
        binding.progressContainer.visibility = GONE
        showImportErrorDialog(
            title = getString(R.string.error_no_permission_title),
            message = getString(R.string.error_no_permission_msg),
            finishOnOk = true,
        )
    }

    private fun showImportFailed(message: String) {
        showImportErrorDialog(
            title = getString(R.string.invalid_passbook_title),
            message = message,
            finishOnOk = true,
        )
    }
}
