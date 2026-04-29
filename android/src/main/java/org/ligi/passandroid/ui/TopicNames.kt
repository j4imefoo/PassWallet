package org.ligi.passandroid.ui

import android.content.Context
import org.ligi.passandroid.R

object TopicNames {
    const val NEW = "new"
    const val TRASH = "trash"
    private const val REMOVED_ARCHIVE = "archive"

    val builtInTopics = listOf(TRASH, NEW)

    fun displayName(context: Context, topic: String): String = when (topic.lowercase()) {
        NEW -> context.getString(R.string.topic_new)
        TRASH -> context.getString(R.string.topic_trash)
        else -> topic
    }

    fun isRemovedArchive(topic: String) = topic.equals(REMOVED_ARCHIVE, ignoreCase = true)

    fun sortedTopics(topics: Set<String>): List<String> {
        val builtInOrder = builtInTopics.withIndex().associate { it.value to it.index }
        return topics.sortedWith(
            compareBy<String> { builtInOrder[it] ?: builtInTopics.size }
                .thenBy { it.lowercase() }
        )
    }
}
