package org.ligi.passandroid.ui

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.ligi.passandroid.R
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.PassStoreProjection
import org.ligi.passandroid.ui.pass_view_holder.PassViewHolder
import org.ligi.passandroid.ui.pass_view_holder.VerbosePassViewHolder

class PassAdapter(
        private val passListActivity: AppCompatActivity,
        private val passStoreProjection: PassStoreProjection
) : RecyclerView.Adapter<PassViewHolder>(), KoinComponent {

    private val passStore: PassStore by inject ()

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): PassViewHolder {
        val inflater = LayoutInflater.from(viewGroup.context)

        val res = inflater.inflate(R.layout.pass_list_item, viewGroup, false) as CardView
        return VerbosePassViewHolder(res)
    }

    override fun onBindViewHolder(viewHolder: PassViewHolder, position: Int) {
        val pass = passStoreProjection.passList[position]

        viewHolder.apply(pass, passStore, passListActivity)

        val root = viewHolder.view

        root.setOnClickListener {
            passStore.currentPass = pass
            val intent = Intent(passListActivity, PassViewActivity::class.java)
                .putExtra(PassViewActivityBase.EXTRA_KEY_UUID, pass.id)
            passListActivity.startActivity(intent)
        }

        root.setOnLongClickListener {
            MoveToNewTopicUI(passListActivity, passStore, pass).show()
            true
        }
    }

    override fun getItemId(position: Int) = position.toLong()
    override fun getItemCount() = passStoreProjection.passList.size

}
