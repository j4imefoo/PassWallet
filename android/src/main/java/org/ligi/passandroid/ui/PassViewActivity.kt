package org.ligi.passandroid.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import org.ligi.kaxt.disableRotation
import org.ligi.passandroid.R
import org.ligi.passandroid.model.PassStoreProjection
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.model.pass.PassType
import org.ligi.passandroid.model.pass.PassVisualClassifier
import org.ligi.passandroid.model.pass.PassVisualType

class PassViewActivity : PassViewActivityBase() {
    private lateinit var pagerAdapter: CollectionPagerAdapter
    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!hasCurrentPass()) {
            return
        }

        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            this.window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }

        disableRotation()
        setContentView(R.layout.activity_pass_view_base)
        applyMaterialInsets(
            root = findViewById(R.id.pass_view_root),
            appBar = findViewById(R.id.appbar),
            content = findViewById(R.id.pager),
        )

        pagerAdapter = CollectionPagerAdapter(this, PassStoreProjection(passStore,
                passStore.classifier.getTopic(currentPass, ""),
                settings.getSortOrder()))
        viewPager = findViewById(R.id.pager)
        viewPager.adapter = pagerAdapter
        val initialPosition = pagerAdapter.getPos(currentPass)
        if (initialPosition < 0) {
            tracker.trackException("pass ${currentPass.id} not present in pager projection", false)
            finish()
            return
        }
        viewPager.currentItem = initialPosition
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(pos: Int) {
                currentPass = pagerAdapter.getPass(pos)
                passStore.currentPass = currentPass
            }
        })
    }

    override fun refresh() {
        super.refresh()

        pagerAdapter.refresh()
    }

    inner class CollectionPagerAdapter(
            fa: FragmentActivity,
            private var passStoreProjection: PassStoreProjection
    ) : FragmentStateAdapter(fa) {

        override fun getItemCount(): Int = passStoreProjection.passList.size

        override fun createFragment(i: Int): Fragment {

            val pass = getPass(i)
            val visualType = PassVisualClassifier.classify(pass)
            if (visualType == PassVisualType.CREDENTIAL) {
                pass.visualType = PassVisualType.CREDENTIAL
            }

            val fragment =
                if (pass.visualType == PassVisualType.CREDENTIAL) {
                    PassViewPKFragment()
                } else {
                    when (pass.type) {
                        PassType.EVENT,
                        PassType.BOARDING,
                        PassType.PKBOARDING,
                        PassType.LOYALTY -> PassViewPKFragment()
                        else -> PassViewFragment()
                    }
                }

            fragment.arguments = bundleOf(EXTRA_KEY_UUID to pass.id)
            return fragment
        }

        fun getPass(i: Int): Pass {
            return passStoreProjection.passList[i]
        }

        fun getPos(pass: Pass): Int {
            return passStoreProjection.passList.indexOf(pass)
        }

        fun refresh() {
            passStoreProjection.refresh()
        }
    }

    override fun onResumeFragments() {
        super.onResumeFragments()

        setSupportActionBar(findViewById(R.id.toolbar))

        configureActionBar()
        refresh()
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.menu_update).isVisible = mightPassBeAbleToUpdate(currentPass)
        menu.findItem(R.id.install_shortcut).isVisible = ShortcutManagerCompat.isRequestPinShortcutSupported(this)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.update, menu)
        menuInflater.inflate(R.menu.shortcut, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            startActivity(Intent(this, PassListActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
            finish()
            true
        }

        else -> super.onOptionsItemSelected(item)
    }
}
