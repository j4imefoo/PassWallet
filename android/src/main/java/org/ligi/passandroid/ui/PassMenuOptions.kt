package org.ligi.passandroid.ui

import android.app.Activity
import android.content.Intent
import android.view.MenuItem
import android.widget.Toast
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.ligi.kaxt.startActivityFromClass
import org.ligi.passandroid.R
import org.ligi.passandroid.Tracker
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.Settings
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.printing.doPrint

class PassMenuOptions(val activity: Activity, val pass: Pass) : KoinComponent {

    val passStore: PassStore by inject()
    val tracker: Tracker by inject()
    val settings: Settings by inject()

    fun process(item: MenuItem): Boolean {
        when (item.itemId) {

            R.id.menu_delete -> {
                tracker.trackEvent("ui_action", "move_to_trash", "move_to_trash", null)

                passStore.classifier.moveToTopic(pass, TopicNames.TRASH)
                Toast.makeText(activity, R.string.pass_moved_to_trash, Toast.LENGTH_SHORT).show()

                if (activity is PassViewActivityBase) {
                    val passListIntent = Intent(activity, PassListActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    activity.startActivity(passListIntent)
                    activity.finish()
                }

                return true
            }

            R.id.menu_share -> {
                tracker.trackEvent("ui_action", "share", "shared", null)
                PassExportTaskAndShare(activity, passStore.getPathForID(pass.id)).execute()
                return true
            }

            R.id.menu_edit -> {
                tracker.trackEvent("ui_action", "share", "shared", null)
                passStore.currentPass = pass
                activity.startActivityFromClass(PassEditActivity::class.java)
                return true
            }

            R.id.menu_print -> {
                doPrint(activity, pass)
                return true
            }
        }
        return false
    }

}
