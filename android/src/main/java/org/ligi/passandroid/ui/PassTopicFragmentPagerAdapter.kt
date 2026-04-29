package org.ligi.passandroid.ui

import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.ligi.passandroid.model.PassClassifier

class PassTopicFragmentPagerAdapter(private val passClassifier: PassClassifier, private val activity: FragmentActivity) : FragmentStateAdapter(activity) {

    private var topics: List<String> = TopicNames.sortedTopics(passClassifier.getTopics())

    override fun createFragment(position: Int) = PassListFragment.newInstance(topics[position])

    override fun getItemCount() = topics.size

    fun getPageTitle(position: Int) = topics.getOrNull(position)?.let { TopicNames.displayName(activity, it) }.orEmpty()

    override fun getItemId(position: Int) = topics[position].hashCode().toLong()

    override fun containsItem(itemId: Long) = topics.any { it.hashCode().toLong() == itemId }

    fun refresh() {
        topics = TopicNames.sortedTopics(passClassifier.getTopics())
        notifyDataSetChanged()
    }

    fun getTopicPosition(topic: String?) = topics.indexOf(topic)

    fun getTopicAt(position: Int) = topics.getOrNull(position)
}
