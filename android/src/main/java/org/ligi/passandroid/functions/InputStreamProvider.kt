package org.ligi.passandroid.functions

import android.content.Context
import android.net.Uri
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.ligi.passandroid.Tracker
import org.ligi.passandroid.model.InputStreamWithSource
import java.io.BufferedInputStream
import java.io.FilterInputStream
import java.io.InputStream
import java.net.URL
import java.util.concurrent.TimeUnit

const val IPHONE_USER_AGENT = "Mozilla/5.0 (iPhone; CPU iPhone OS 7_0 like Mac OS X; en-us) AppleWebKit/537.51.1 (KHTML, like Gecko) Version/7.0 Mobile/11A465 Safari/9537.53"

private val importHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(10, TimeUnit.SECONDS)
    .build()

fun fromURI(context: Context, uri: Uri, tracker: Tracker): InputStreamWithSource? {
    tracker.trackEvent("protocol", "to_inputstream", uri.scheme, null)
    return when (uri.scheme) {
        "content" -> fromContent(context, uri)
        "http", "https" -> fromOKHttp(uri, tracker)
        "file" -> getDefaultInputStreamForUri(uri)
        else -> {
            tracker.trackException("unknown scheme in ImportAsyncTask" + uri.scheme, false)
            getDefaultInputStreamForUri(uri)
        }
    }
}

private fun fromOKHttp(uri: Uri, tracker: Tracker): InputStreamWithSource? {
    val url = URL("$uri")
    val requestBuilder = Request.Builder().url(url)

    // Fake being an iPhone only for providers known to hide passbook downloads from Android user agents.
    val iPhoneFakeMap = mapOf(
        "air_canada" to "//m.aircanada.ca/ebp/",
        "air_canada2" to "//services.aircanada.com/ebp/",
        "air_canada3" to "//mci.aircanada.com/mci/bp/",
        "icelandair" to "//checkin.si.amadeus.net",
        "mbk" to "//mbk.thy.com/",
        "heathrow" to "//passbook.heathrow.com/",
        "eventbrite" to "//www.eventbrite.com/passes/order",
    )

    for ((key, value) in iPhoneFakeMap) {
        if ("$uri".contains(value)) {
            tracker.trackEvent("quirk_fix", "ua_fake", key, null)
            requestBuilder.header("User-Agent", IPHONE_USER_AGENT)
        }
    }

    val response = importHttpClient.newCall(requestBuilder.build()).execute()
    if (!response.isSuccessful) {
        response.close()
        tracker.trackException("import download failed with HTTP ${response.code()}", false)
        return null
    }

    val body = response.body()
    if (body == null) {
        response.close()
        return null
    }

    return InputStreamWithSource("$uri", ResponseClosingInputStream(body.byteStream(), response))
}

private fun fromContent(ctx: Context, uri: Uri): InputStreamWithSource? {
    return ctx.contentResolver.openInputStream(uri)?.let { InputStreamWithSource("$uri", it) }
}

private fun getDefaultInputStreamForUri(uri: Uri): InputStreamWithSource? {
    val connection = URL("$uri").openConnection().apply {
        connectTimeout = 10_000
        readTimeout = 30_000
    }
    return InputStreamWithSource("$uri", BufferedInputStream(connection.getInputStream(), 4096))
}

private class ResponseClosingInputStream(
    inputStream: InputStream,
    private val response: Response,
) : FilterInputStream(inputStream) {
    override fun close() {
        try {
            super.close()
        } finally {
            response.close()
        }
    }
}
