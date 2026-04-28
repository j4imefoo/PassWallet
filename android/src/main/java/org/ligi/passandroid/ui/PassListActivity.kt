package org.ligi.passandroid.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import org.ligi.kaxt.startActivityFromClass
import org.ligi.kaxt.startActivityFromURL
import org.ligi.passandroid.R
import org.ligi.passandroid.databinding.PassListBinding
import org.ligi.passandroid.functions.createAndAddEmptyPass
import org.ligi.passandroid.model.PassStoreProjection
import org.ligi.passandroid.model.State
import org.ligi.tracedroid.TraceDroid
import org.ligi.tracedroid.sending.sendTraceDroidStackTracesIfExist

class PassListActivity : PassAndroidActivity() {

    private lateinit var binding: PassListBinding
    private val drawerToggle by lazy { ActionBarDrawerToggle(this, binding.drawerLayout, R.string.drawer_open, R.string.drawer_close) }
    private var tabLayoutMediator: TabLayoutMediator? = null
    private val openFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            startActivity(Intent(this, PassImportActivity::class.java).setData(it))
        }
    }

    private val adapter by lazy { PassTopicFragmentPagerAdapter(passStore.classifier, this) }


    internal fun onAddOpenFileClick() {
        try {
            openFileLauncher.launch(arrayOf("*/*"))
        } catch (e: ActivityNotFoundException) {
            Snackbar.make(binding.fam, "Unavailable", Snackbar.LENGTH_LONG).show()
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = PassListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        applyMaterialInsets(
            root = binding.drawerLayout,
            appBar = binding.appbar,
            drawerContent = binding.navigationView,
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

        binding.drawerLayout.addDrawerListener(drawerToggle)

        onBackPressedDispatcher.addCallback(this) {
            when {
                binding.drawerLayout.isDrawerOpen(GravityCompat.START) -> binding.drawerLayout.closeDrawer(GravityCompat.START)
                binding.fam.isExpanded -> binding.fam.collapse()
                else -> isEnabled = false
            }

            if (!isEnabled) {
                onBackPressedDispatcher.onBackPressed()
            }
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

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
        passStore.syncPassStoreWithClassifier(getString(R.string.topic_new))

        refresh()

        binding.fabActionCreatePass.setOnClickListener {
            val pass = createAndAddEmptyPass(passStore, resources)

            binding.fam.collapse()
            startActivityFromClass(PassEditActivity::class.java)

            val newTitle = if (binding.tabLayout.selectedTabPosition < 0) {
                getString(R.string.topic_new)
            } else {
                adapter.getPageTitle(binding.tabLayout.selectedTabPosition)
            }

            passStore.classifier.moveToTopic(pass, newTitle)
        }

        binding.fabActionDemoPass.setOnClickListener {
            startActivityFromURL("http://espass.it/examples")
            binding.fam.collapse()
        }

        binding.fabActionOpenFile.setOnClickListener {
            onAddOpenFileClick()
        }
        binding.fabActionOpenFile.visibility = VISIBLE

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                passStore.updateChannel.collect {
                    binding.navigationView.passStoreUpdate()
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
        val onlyDefaultTopicExists = passStore.classifier.getTopics().all { it == getString(R.string.topic_new) }
        binding.tabLayout.visibility = if (onlyDefaultTopicExists) GONE else VISIBLE
    }
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.menu_help -> {
            startActivityFromClass(HelpActivity::class.java)
            true
        }

        R.id.menu_emptytrash -> {
            AlertDialog.Builder(this)
                    .setMessage(getString(R.string.empty_trash_dialog_message))
                    .setIcon(R.drawable.ic_alert_warning)
                    .setTitle(getString(R.string.empty_trash_dialog_title))
                    .setPositiveButton(R.string.emtytrash_label) { _, _ ->
                        val passStoreProjection = PassStoreProjection(passStore,
                                getString(R.string.topic_trash),
                                null)

                        for (pass in passStoreProjection.passList) {
                            passStore.deletePassWithId(pass.id)
                        }
                    }.setNegativeButton(android.R.string.cancel, null).show()
            true
        }
        else -> drawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()

        adapter.refresh()
        refresh()
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.menu_emptytrash).isVisible = adapter.itemCount > 0 && adapter.getPageTitle(binding.viewPager.currentItem) == getString(R.string.topic_trash)
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_pass_list_view, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        drawerToggle.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        drawerToggle.onConfigurationChanged(newConfig)
    }

    private fun syncTabs() {
        tabLayoutMediator?.detach()
        tabLayoutMediator = TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = adapter.getPageTitle(position)
        }.apply { attach() }
    }

}
