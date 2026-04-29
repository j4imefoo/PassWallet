package org.ligi.passandroid.ui

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import androidx.preference.PreferenceManager
import com.google.android.material.button.MaterialButton
import org.koin.android.ext.android.inject
import org.ligi.passandroid.BuildConfig
import org.ligi.passandroid.R
import org.ligi.passandroid.backup.BackupArchive
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.Settings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupActivity : AppCompatActivity() {

    private val settings: Settings by inject()
    private val passStore: PassStore by inject()
    private val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }

    private val exportBackupLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let { exportBackup(it) }
    }

    private val importBackupLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { importBackup(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.backup_title)
        setContentView(createContentView())
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun createContentView(): LinearLayout {
        val density = resources.displayMetrics.density
        val padding = (24 * density).toInt()
        val gap = (16 * density).toInt()

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )

            addView(TextView(context).apply {
                text = getString(R.string.backup_description)
                textSize = 16f
            })

            addView(
                MaterialButton(context).apply {
                    text = getString(R.string.backup_export_title)
                    setOnClickListener { exportBackupLauncher.launch(defaultBackupFileName()) }
                },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = gap },
            )

            addView(TextView(context).apply {
                text = getString(R.string.backup_export_summary)
            })

            addView(
                MaterialButton(context).apply {
                    text = getString(R.string.backup_import_title)
                    setOnClickListener { importBackupLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) }
                },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = gap },
            )

            addView(TextView(context).apply {
                text = getString(R.string.backup_import_summary)
            })
        }
    }

    private fun exportBackup(uri: Uri) {
        runCatching {
            contentResolver.openOutputStream(uri)?.use { output ->
                BackupArchive.exportBackup(
                    passesDir = settings.getPassesDir(),
                    stateDir = settings.getStateDir(),
                    preferences = sharedPreferences.all,
                    appVersionName = BuildConfig.VERSION_NAME,
                    appVersionCode = BuildConfig.VERSION_CODE,
                    outputStream = output,
                )
            } ?: error("Could not open output document")
        }.onSuccess {
            Toast.makeText(this, R.string.backup_export_success, Toast.LENGTH_LONG).show()
        }.onFailure {
            Toast.makeText(this, getString(R.string.backup_export_error, it.localizedMessage), Toast.LENGTH_LONG).show()
        }
    }

    private fun importBackup(uri: Uri) {
        runCatching {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (ignored: SecurityException) {
                // Some providers grant one-shot access only. That is fine for an immediate restore.
            }
            contentResolver.openInputStream(uri)?.use { input ->
                BackupArchive.importBackup(
                    inputStream = input,
                    passesDir = settings.getPassesDir(),
                    stateDir = settings.getStateDir(),
                    restorePreferences = ::restorePreferences,
                )
            } ?: error("Could not open input document")
        }.onSuccess {
            (passStore.passMap as? MutableMap<*, *>)?.clear()
            passStore.syncPassStoreWithClassifier(TopicNames.NEW)
            passStore.notifyChange()
            Toast.makeText(this, R.string.backup_import_success, Toast.LENGTH_LONG).show()
            startActivity(Intent(this, PassListActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            finish()
        }.onFailure {
            Toast.makeText(this, getString(R.string.backup_import_error, it.localizedMessage), Toast.LENGTH_LONG).show()
        }
    }

    private fun restorePreferences(preferences: Map<String, Any>) {
        sharedPreferences.edit().apply {
            preferences.forEach { (key, value) ->
                when (value) {
                    is Boolean -> putBoolean(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Float -> putFloat(key, value)
                    is String -> putString(key, value)
                }
            }
        }.apply()
    }

    private fun defaultBackupFileName(): String {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return "PassWallet-backup-$date.zip"
    }
}
