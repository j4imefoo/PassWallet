package org.ligi.passandroid.printing

import android.content.Context
import android.print.PrintManager
import androidx.core.content.getSystemService
import org.ligi.passandroid.R
import org.ligi.passandroid.model.pass.Pass

fun doPrint(context: Context, pass: Pass) {
    val printManager = context.getSystemService<PrintManager>()!!
    val jobName = context.getString(R.string.app_name) + " print of " + pass.description
    printManager.print(jobName, PassPrintDocumentAdapter(context, pass, jobName), null)
}
