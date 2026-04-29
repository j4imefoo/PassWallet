package org.ligi.passandroid.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.ligi.passandroid.databinding.PassRecyclerBinding
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.PassStoreProjection
import org.ligi.passandroid.model.Settings

class PassListFragment : Fragment() {

    private lateinit var passStoreProjection: PassStoreProjection
    private lateinit var adapter: PassAdapter

    val passStore: PassStore by inject()
    val settings: Settings by inject()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val inflate = PassRecyclerBinding.inflate(layoutInflater, container, false)

        passStoreProjection = PassStoreProjection(passStore, arguments?.getString(BUNDLE_KEY_TOPIC)!!, settings.getSortOrder())
        adapter = PassAdapter(activity as AppCompatActivity, passStoreProjection)

        inflate.passRecyclerview.adapter = adapter
        inflate.passRecyclerview.layoutManager = LinearLayoutManager(activity)

        return inflate.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                passStore.updateChannel.collect {
                    refreshPassList()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPassList()
    }

    private fun refreshPassList() {
        passStoreProjection.refresh()
        adapter.notifyDataSetChanged()
    }

    companion object {
        private const val BUNDLE_KEY_TOPIC = "topic"

        fun newInstance(topic: String) = PassListFragment().apply {
            arguments = bundleOf(BUNDLE_KEY_TOPIC to topic)
        }
    }
}
