package org.ligi.passandroid.unittest

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.ligi.passandroid.model.PassClassifier
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.PassStoreProjection
import org.ligi.passandroid.model.PassStoreUpdateEvent
import org.ligi.passandroid.model.comparator.PassSortOrder
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.model.pass.PassImpl
import org.ligi.passandroid.model.pass.PassType
import org.threeten.bp.ZonedDateTime
import java.io.File
import java.util.*

class ThePassSorting {

    private val pass1 = PassImpl("ID1")
    private val pass2 = PassImpl("ID2")
    private val pass3 = PassImpl("ID3")
    private val pass4 = PassImpl("ID4")
    private val pass5 = PassImpl("ID5")

    private lateinit var passList: List<Pass>

    @Before
    fun init() {
        pass1.calendarTimespan = PassImpl.TimeSpan(ZonedDateTime.now().minusHours(5))
        pass3.calendarTimespan = PassImpl.TimeSpan(ZonedDateTime.now().plusHours(1))
        pass2.calendarTimespan = PassImpl.TimeSpan(ZonedDateTime.now().plusHours(2))
        pass5.validTimespans = listOf(PassImpl.TimeSpan(ZonedDateTime.now().plusHours(3)))

        pass1.type = PassType.GENERIC
        pass2.type = PassType.EVENT
        pass3.type = PassType.GENERIC
        pass4.type = PassType.GENERIC
        pass5.type = PassType.EVENT

        passList = mutableListOf(pass1, pass2, pass3, pass4, pass5)
    }

    @Test
    fun testDESC() {
        Collections.sort(passList, PassSortOrder.DATE_DESC.toComparator())

        assertThat(passList).containsExactly(pass4, pass5, pass2, pass3, pass1)
    }

    @Test
    fun testASC() {
        Collections.sort(passList, PassSortOrder.DATE_ASC.toComparator())

        assertThat(passList).containsExactly(pass1, pass3, pass2, pass5, pass4)
    }

    @Test
    fun testDIFF() {
        Collections.sort(passList, PassSortOrder.DATE_DIFF.toComparator())

        assertThat(passList).containsExactly(pass3, pass2, pass5, pass1, pass4)
    }

    @Test
    fun testTYPE() {
        Collections.sort(passList, PassSortOrder.TYPE.toComparator())

        assertThat(passList).containsExactly(pass1, pass3, pass4, pass2, pass5)
    }

    @Test
    fun testProjectionRefreshCanApplyChangedSortOrder() {
        val passStore = ProjectionTestPassStore(passList.associateBy { it.id })
        val projection = PassStoreProjection(passStore, ProjectionTestPassStore.TOPIC, PassSortOrder.DATE_ASC)

        assertThat(projection.passList).containsExactly(pass1, pass3, pass2, pass5, pass4)

        projection.refresh(PassSortOrder.DATE_DESC)

        assertThat(projection.passList).containsExactly(pass4, pass5, pass2, pass3, pass1)
    }

    private class ProjectionTestPassStore(private val passesById: Map<String, Pass>) : PassStore {
        override val updateChannel: SharedFlow<PassStoreUpdateEvent> = MutableSharedFlow()
        override val passMap: Map<String, Pass> = passesById
        override var currentPass: Pass? = null
        override val classifier = PassClassifier(
            passesById.keys.associateWith { TOPIC }.toMutableMap(),
            this,
        )

        override fun save(pass: Pass) = Unit
        override fun getPassbookForId(id: String): Pass? = passesById[id]
        override fun deletePassWithId(id: String): Boolean = false
        override fun getPathForID(id: String): File = File(id)
        override fun notifyChange() = Unit
        override fun syncPassStoreWithClassifier(defaultTopic: String) = Unit

        companion object {
            const val TOPIC = "topic"
        }
    }
}
