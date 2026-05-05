package org.ligi.passandroid.model

import org.ligi.passandroid.model.comparator.PassSortOrder
import org.ligi.passandroid.model.pass.Pass
import java.util.*

class PassStoreProjection(private val passStore: PassStore, private val topic: String, private var passSortOrder: PassSortOrder? = null) {

    var passList: List<Pass> = ArrayList()
        private set

    init {
        refresh()
    }

    fun refresh(passSortOrder: PassSortOrder? = this.passSortOrder) {
        this.passSortOrder = passSortOrder
        val newPassList = passStore.classifier.getPassListByTopic(topic)
        if (passSortOrder != null) {
            Collections.sort(newPassList, passSortOrder.toComparator())
        }

        passList = newPassList
    }
}
