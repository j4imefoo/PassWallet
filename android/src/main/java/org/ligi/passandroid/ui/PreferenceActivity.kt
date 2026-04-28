package org.ligi.passandroid.ui

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.MenuItem
import androidx.fragment.app.commit
import org.ligi.passandroid.R
import org.ligi.passandroid.databinding.PreferencesBinding

class PreferenceActivity : AppCompatActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = PreferencesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        applyMaterialInsets(
            root = binding.preferencesRoot,
            appBar = binding.appbar,
            content = binding.container,
        )

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.container, PrefsFragment())
            }
        }

        supportActionBar?.title = getString(R.string.nav_settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    @SuppressLint("PrivateResource")
    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

}
