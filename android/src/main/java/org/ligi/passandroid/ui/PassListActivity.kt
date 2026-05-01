package org.ligi.passandroid.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import org.ligi.kaxt.startActivityFromClass
import org.ligi.passandroid.R
import org.ligi.passandroid.databinding.PassListBinding
import org.ligi.passandroid.functions.createAndAddEmptyPass
import org.ligi.passandroid.model.PassStoreProjection
import org.ligi.passandroid.model.State
import org.ligi.tracedroid.TraceDroid
import org.ligi.tracedroid.sending.sendTraceDroidStackTracesIfExist

class PassListActivity : PassAndroidActivity() {

    private lateinit var binding: PassListBinding
    private var tabLayoutMediator: TabLayoutMediator? = null
    private var fabMenuExpanded = false
    private val importPassLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            startActivity(Intent(this, PassImportActivity::class.java).setData(it))
        }
    }

    private val adapter by lazy { PassTopicFragmentPagerAdapter(passStore.classifier, this) }


    internal fun onImportPassClick() {
        try {
            importPassLauncher.launch(arrayOf("*/*"))
        } catch (e: ActivityNotFoundException) {
            Snackbar.make(binding.fam, "Unavailable", Snackbar.LENGTH_LONG).show()
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = PassListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.emptyView.text = createEmptyText()
        applyMaterialInsets(
            root = binding.root,
            appBar = binding.appbar,
            content = binding.viewPager,
            floatingView = binding.fam,
        )


        // don't want too many windows in worst case - so check for errors first
        if (TraceDroid.stackTraceFiles.isNotEmpty()) {
            tracker.trackEvent("ui_event", "send", "stacktraces", null)

            if (settings.doTraceDroidEmailSend()) {
                sendTraceDroidStackTracesIfExist("ligi+passandroid@ligi.de", this)
            }
        } else {
            tracker.trackEvent("ui_event", "processFile", "updatenotice", null)
        }

        onBackPressedDispatcher.addCallback(this) {
            when {
                fabMenuExpanded -> setFabMenuExpanded(false)
                else -> isEnabled = false
            }

            if (!isEnabled) {
                onBackPressedDispatcher.onBackPressed()
            }
        }

        binding.viewPager.adapter = adapter

        if (adapter.itemCount > 0) {
            binding.viewPager.currentItem = State.lastSelectedTab.coerceIn(0, adapter.itemCount - 1)
        }
        syncTabs()

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                State.lastSelectedTab = position
                invalidateOptionsMenu()
            }
        })
        passStore.syncPassStoreWithClassifier(TopicNames.NEW)
        migrateRemovedArchiveTopicToNew()

        refresh()

        binding.fabActionCreatePass.setOnClickListener {
            val pass = createAndAddEmptyPass(passStore, resources)

            setFabMenuExpanded(false)
            startActivityFromClass(PassEditActivity::class.java)

            val newTitle = if (binding.tabLayout.selectedTabPosition < 0) {
                TopicNames.NEW
            } else {
                adapter.getTopicAt(binding.tabLayout.selectedTabPosition) ?: TopicNames.NEW
            }

            passStore.classifier.moveToTopic(pass, newTitle)
        }

        binding.fabActionImport.setOnClickListener {
            setFabMenuExpanded(false)
            onImportPassClick()
        }
        binding.fabMenuToggle.setOnClickListener {
            setFabMenuExpanded(!fabMenuExpanded)
        }
        setFabMenuExpanded(false)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                passStore.updateChannel.collect {
                    refresh()
                }
            }
        }
    }

    fun refresh() {
        val selectedTopic = adapter.getTopicAt(binding.viewPager.currentItem)
        adapter.refresh()
        syncTabs()

        if (adapter.itemCount > 0) {
            val selectedPosition = adapter.getTopicPosition(selectedTopic)
                .takeIf { it >= 0 }
                ?: State.lastSelectedTab.coerceIn(0, adapter.itemCount - 1)
            binding.viewPager.setCurrentItem(selectedPosition, false)
        }

        invalidateOptionsMenu()
        val empty = passStore.classifier.topicByIdMap.isEmpty()
        binding.emptyView.visibility = if (empty) VISIBLE else GONE
        val onlyDefaultTopicExists = passStore.classifier.getTopics().all { it == TopicNames.NEW }
        binding.tabLayout.visibility = if (onlyDefaultTopicExists) GONE else VISIBLE
    }
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.menu_settings -> {
            startActivityFromClass(PreferenceActivity::class.java)
            true
        }

        R.id.menu_emptytrash -> {
            val passStoreProjection = PassStoreProjection(
                passStore,
                TopicNames.TRASH,
                null
            )
            val trashPasses = passStoreProjection.passList
            showDestructiveConfirmationDialog(
                titleRes = R.string.empty_trash_dialog_title,
                message = getString(R.string.empty_trash_dialog_message, trashPasses.size),
                confirmTextRes = R.string.emtytrash_label
            ) {
                for (pass in trashPasses) {
                    passStore.deletePassWithId(pass.id)
                }
            }
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()

        adapter.refresh()
        refresh()
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.menu_emptytrash).isVisible = adapter.itemCount > 0 && adapter.getTopicAt(binding.viewPager.currentItem) == TopicNames.TRASH
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_pass_list_view, menu)
        return super.onCreateOptionsMenu(menu)
    }

    private fun syncTabs() {
        tabLayoutMediator?.detach()
        tabLayoutMediator = TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = adapter.getPageTitle(position)
        }.apply { attach() }
    }

    private fun setFabMenuExpanded(expanded: Boolean) {
        fabMenuExpanded = expanded
        val optionVisibility = if (expanded) VISIBLE else GONE
        binding.fabActionImport.visibility = optionVisibility
        binding.fabActionCreatePass.visibility = optionVisibility
        binding.fabMenuToggle.setImageResource(if (expanded) R.drawable.ic_close_24 else R.drawable.ic_add_24)
        binding.fabMenuToggle.backgroundTintList = getColorStateList(
            if (expanded) R.color.secondary else R.color.accent,
        )
    }

    private fun createEmptyText(): SpannableString {
        val title = getString(R.string.empty_text_title)
        val text = "$title\n\n${getString(R.string.empty_text_body)}"
        return SpannableString(text).apply {
            setSpan(StyleSpan(Typeface.BOLD), 0, title.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(RelativeSizeSpan(1.15f), 0, title.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun migrateRemovedArchiveTopicToNew() {
        var migrated = false
        passStore.classifier.topicByIdMap.toList().forEach { (id, topic) ->
            if (TopicNames.isRemovedArchive(topic)) {
                passStore.classifier.topicByIdMap[id] = TopicNames.NEW
                migrated = true
            }
        }

        if (migrated) {
            passStore.classifier.processDataChange()
        }
    }

}
