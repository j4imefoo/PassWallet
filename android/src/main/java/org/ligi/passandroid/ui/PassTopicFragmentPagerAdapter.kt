package org.ligi.passandroid.ui

import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.ligi.passandroid.model.PassClassifier

class PassTopicFragmentPagerAdapter(private val passClassifier: PassClassifier, activity: FragmentActivity) : FragmentStateAdapter(activity) {

    private var topics: List<String> = passClassifier.getTopics().toList()

    override fun createFragment(position: Int) = PassListFragment.newInstance(topics[position])

    override fun getItemCount() = topics.size

    fun getPageTitle(position: Int) = topics[position]

    override fun getItemId(position: Int) = topics[position].hashCode().toLong()

    override fun containsItem(itemId: Long) = topics.any { it.hashCode().toLong() == itemId }

    fun refresh() {
        topics = passClassifier.getTopics().toList()
        notifyDataSetChanged()
    }

    fun getTopicPosition(topic: String?) = topics.indexOf(topic)

    fun getTopicAt(position: Int) = topics.getOrNull(position)
}
