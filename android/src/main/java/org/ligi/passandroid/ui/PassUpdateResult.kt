package org.ligi.passandroid.ui

import okhttp3.Response
import okhttp3.ResponseBody

sealed class PassUpdateResult {
    data class Available(val body: ResponseBody) : PassUpdateResult()
    data class Unavailable(val statusCode: Int) : PassUpdateResult()
}

fun interpretPassUpdateResponse(response: Response): PassUpdateResult {
    val body = response.body()
    return if (response.isSuccessful && body != null && response.code() != 204) {
        PassUpdateResult.Available(body)
    } else {
        PassUpdateResult.Unavailable(response.code())
    }
}
