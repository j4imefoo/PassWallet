package org.ligi.passandroid.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.content.SharedPreferences
import android.net.Uri
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.setPadding
import androidx.core.widget.NestedScrollView
import androidx.preference.PreferenceManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
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
        val screen = createContentView()
        setContentView(screen.root)
        setSupportActionBar(screen.toolbar)
        applyMaterialInsets(
            root = screen.root,
            appBar = screen.appBar,
            content = screen.content,
            statusBarSpacer = screen.statusBarSpacer,
        )
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.backup_title)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private data class BackupScreen(
        val root: LinearLayout,
        val appBar: LinearLayout,
        val statusBarSpacer: View,
        val toolbar: MaterialToolbar,
        val content: View,
    )

    private fun createContentView(): BackupScreen {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.color.surface)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }

        val appBar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.color.top_app_bar)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }

        val statusBarSpacer = View(this).apply {
            setBackgroundResource(R.color.top_app_bar)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
            )
        }

        val toolbar = MaterialToolbar(this).apply {
            setBackgroundResource(R.color.top_app_bar)
            setTitleTextColor(getColor(android.R.color.white))
            setNavigationIconTint(getColor(android.R.color.white))
            popupTheme = R.style.PassWallet_ToolbarPopup
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(androidx.appcompat.R.dimen.abc_action_bar_default_height_material),
            )
        }
        appBar.addView(statusBarSpacer)
        appBar.addView(toolbar)
        root.addView(appBar)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )

            addView(
                createActionCard(
                    iconRes = R.drawable.ic_backup_export_24,
                    title = getString(R.string.backup_export_title),
                    summary = getString(R.string.backup_export_summary),
                    buttonText = getString(R.string.backup_export_action),
                    buttonContentDescription = getString(R.string.backup_export_title),
                    filled = true,
                ) { exportBackupLauncher.launch(defaultBackupFileName()) },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = dp(16) },
            )
            addView(
                createActionCard(
                    iconRes = R.drawable.ic_backup_import_24,
                    title = getString(R.string.backup_import_title),
                    summary = getString(R.string.backup_import_summary),
                    buttonText = getString(R.string.backup_import_action),
                    buttonContentDescription = getString(R.string.backup_import_title),
                    filled = true,
                ) { importBackupLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = dp(16) },
            )
        }

        val scroll = NestedScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f,
            )
            addView(content)
        }
        root.addView(scroll)

        return BackupScreen(root, appBar, statusBarSpacer, toolbar, scroll)
    }

    private fun createActionCard(
        iconRes: Int,
        title: String,
        summary: String,
        buttonText: String,
        buttonContentDescription: String,
        filled: Boolean,
        onClick: () -> Unit,
    ): MaterialCardView {
        return MaterialCardView(this).apply {
            radius = dp(22).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(getColor(R.color.surface_container))
            strokeColor = getColor(R.color.outline_variant)
            strokeWidth = dp(1)
            addView(
                LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(20))
                    addView(createIconBadge(iconRes))
                    addView(createText(title, 19f, true).apply {
                        setPadding(0, dp(14), 0, 0)
                    })
                    addView(createText(summary, 14f, false).apply {
                        setTextColor(themeColor(android.R.attr.textColorSecondary))
                        setPadding(0, dp(6), 0, 0)
                    })
                    addView(
                        createActionButton(buttonText, buttonContentDescription, filled, onClick),
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                        ).apply { topMargin = dp(18) },
                    )
                },
            )
        }
    }

    private fun createIconBadge(iconRes: Int): ImageView {
        return ImageView(this).apply {
            setImageResource(iconRes)
            imageTintList = ColorStateList.valueOf(getColor(R.color.edit_action_text))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(18).toFloat()
                setColor(getColor(R.color.edit_action_background))
                setStroke(dp(1), getColor(R.color.edit_action_stroke))
            }
            setPadding(dp(12))
            layoutParams = LinearLayout.LayoutParams(dp(52), dp(52))
        }
    }

    private fun createText(textValue: String, sizeSp: Float, medium: Boolean): TextView {
        return TextView(this).apply {
            text = textValue
            textSize = sizeSp
            setTextColor(themeColor(android.R.attr.textColorPrimary))
            if (medium) {
                typeface = Typeface.DEFAULT_BOLD
            }
        }
    }

    private fun createActionButton(
        textValue: String,
        contentDescriptionValue: String,
        filled: Boolean,
        onClick: () -> Unit,
    ): MaterialButton {
        return MaterialButton(this).apply {
            text = textValue
            contentDescription = contentDescriptionValue
            gravity = Gravity.CENTER
            isAllCaps = false
            minimumHeight = dp(52)
            minWidth = 0
            setPadding(dp(18), 0, dp(18), 0)
            typeface = Typeface.DEFAULT_BOLD
            textSize = 15f
            cornerRadius = dp(18)
            rippleColor = ColorStateList.valueOf(getColor(R.color.accent))

            if (filled) {
                setTextColor(getColor(android.R.color.white))
                backgroundTintList = ColorStateList.valueOf(getColor(R.color.secondary))
                strokeWidth = 0
            } else {
                setTextColor(getColor(R.color.edit_action_text))
                backgroundTintList = ColorStateList.valueOf(getColor(R.color.edit_action_background))
                strokeColor = ColorStateList.valueOf(getColor(R.color.edit_action_text))
                strokeWidth = dp(1)
            }

            setOnClickListener { onClick() }
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun themeColor(attr: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return if (typedValue.resourceId != 0) getColor(typedValue.resourceId) else typedValue.data
    }

    private fun exportBackup(uri: Uri) {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
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
                }
            }
            result.onSuccess {
                Toast.makeText(this@BackupActivity, R.string.backup_export_success, Toast.LENGTH_LONG).show()
            }.onFailure {
                Toast.makeText(this@BackupActivity, getString(R.string.backup_export_error, it.localizedMessage), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun importBackup(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (ignored: SecurityException) {
            // Some providers grant one-shot access only. That is fine for an immediate restore.
        }

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    contentResolver.openInputStream(uri)?.use { input ->
                        BackupArchive.importBackup(
                            inputStream = input,
                            passesDir = settings.getPassesDir(),
                            stateDir = settings.getStateDir(),
                            restorePreferences = ::restorePreferences,
                        )
                    } ?: error("Could not open input document")
                }
            }
            result.onSuccess {
                (passStore.passMap as? MutableMap<*, *>)?.clear()
                passStore.syncPassStoreWithClassifier(TopicNames.NEW)
                passStore.notifyChange()
                Toast.makeText(this@BackupActivity, R.string.backup_import_success, Toast.LENGTH_LONG).show()
                startActivity(Intent(this@BackupActivity, PassListActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                finish()
            }.onFailure {
                Toast.makeText(this@BackupActivity, getString(R.string.backup_import_error, it.localizedMessage), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun restorePreferences(preferences: Map<String, Any>) {
        sharedPreferences.edit {
            preferences.forEach { (key, value) ->
                when (value) {
                    is Boolean -> putBoolean(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Float -> putFloat(key, value)
                    is String -> putString(key, value)
                }
            }
        }
    }

    private fun defaultBackupFileName(): String {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return "PassWallet-backup-$date.zip"
    }
}
