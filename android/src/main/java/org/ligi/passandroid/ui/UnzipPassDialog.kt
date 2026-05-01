package org.ligi.passandroid.ui

import android.app.Activity
import android.app.Dialog
import org.ligi.passandroid.R
import org.ligi.passandroid.model.InputStreamWithSource
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.ui.UnzipPassController.FailCallback
import org.ligi.passandroid.ui.UnzipPassController.InputStreamUnzipControllerSpec
import org.ligi.passandroid.ui.UnzipPassController.SuccessCallback

object UnzipPassDialog {

    private fun displayError(activity: Activity, title: String, err: String) {
        activity.showImportErrorDialog(
            title = title,
            message = err,
            finishOnOk = true,
        )
    }


    fun show(ins: InputStreamWithSource,
             activity: Activity,
             passStore: PassStore,
             callAfterFinishOnUIThread: (uuids: List<String>) -> Unit) {
        if (activity.isFinishing) {
            return  // no need to act any more ..
        }

        val dialog = activity.createProgressDialog(
            activity.getString(R.string.unzip_pass_dialog_message),
            R.string.unzip_pass_dialog_title,
        )
        dialog.show()
        dialog.setCancelable(false)

        class AlertDialogUpdater(private val call_after_finish: (uuids: List<String>) -> Unit) : Runnable {

            override fun run() {
                val spec = InputStreamUnzipControllerSpec(ins, activity, passStore, object : SuccessCallback {

                    override fun call(uuids: List<String>) {
                        activity.runOnUiThread(Runnable {
                            if (!prepareResult(activity, dialog)) {
                                return@Runnable
                            }

                            call_after_finish.invoke(uuids)
                        })
                    }
                }, object : FailCallback {
                    override fun fail(reason: String) {
                        activity.runOnUiThread(Runnable {
                            if (!prepareResult(activity, dialog)) {
                                return@Runnable
                            }

                            displayError(activity, activity.getString(R.string.invalid_passbook_title), reason)
                        })
                    }
                })
                UnzipPassController.processInputStream(spec)
            }
        }

        val alertDialogUpdater = AlertDialogUpdater(callAfterFinishOnUIThread)
        Thread(alertDialogUpdater).start()

    }

    private fun prepareResult(activity: Activity, dialog: Dialog): Boolean {
        if (activity.isFinishing) {
            return false
        }

        if (dialog.isShowing) {
            dialog.dismissSafely()
            return true
        }
        return false
    }

}
