package org.ligi.passandroid.ui.edit.dialogs

import android.app.Activity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.ligi.kaxt.inflate
import org.ligi.passandroid.R
import org.ligi.passandroid.model.pass.PassImpl
import org.ligi.passandroid.model.pass.PassLocation
import java.io.IOException
import java.net.URLEncoder
import java.util.Locale

private data class LocationSearchResult(
    val displayName: String,
    val latitude: Double,
    val longitude: Double,
)

fun showLocationEditDialog(
    activity: Activity,
    pass: PassImpl,
    refreshCallback: () -> Unit,
) {
    val view = activity.inflate(R.layout.location_edit)
    val existingLocation = pass.locations.firstOrNull()

    val searchInput = view.findViewById<TextInputEditText>(R.id.locationSearchInput)
    val searchButton = view.findViewById<MaterialButton>(R.id.locationSearchButton)
    val searchProgress = view.findViewById<ProgressBar>(R.id.locationSearchProgress)
    val searchStatus = view.findViewById<TextView>(R.id.locationSearchStatus)
    val resultsContainer = view.findViewById<LinearLayout>(R.id.locationResultsContainer)

    val nameInput = view.findViewById<TextInputEditText>(R.id.locationNameInput)
    val latitudeInput = view.findViewById<TextInputEditText>(R.id.locationLatitudeInput)
    val longitudeInput = view.findViewById<TextInputEditText>(R.id.locationLongitudeInput)
    val latitudeLayout = view.findViewById<TextInputLayout>(R.id.locationLatitudeLayout)
    val longitudeLayout = view.findViewById<TextInputLayout>(R.id.locationLongitudeLayout)
    val saveButton = view.findViewById<MaterialButton>(R.id.locationSaveButton)
    val removeButton = view.findViewById<MaterialButton>(R.id.locationRemoveButton)
    val dialog = BottomSheetDialog(activity)

    fun setSearchLoading(isLoading: Boolean) {
        searchButton.isEnabled = !isLoading
        searchProgress.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    fun selectSearchResult(result: LocationSearchResult) {
        nameInput.setText(result.displayName)
        latitudeInput.setText(result.latitude.toString())
        longitudeInput.setText(result.longitude.toString())
        searchStatus.text = activity.getString(R.string.location_result_selected)
    }

    fun showSearchResults(results: List<LocationSearchResult>) {
        resultsContainer.removeAllViews()
        if (results.isEmpty()) {
            searchStatus.text = activity.getString(R.string.location_no_results)
            return
        }

        searchStatus.text = activity.getString(R.string.location_select_result)
        val rowMargin = activity.resources.getDimensionPixelSize(R.dimen.rhythm)
        results.forEach { result ->
            val row = activity.layoutInflater.inflate(
                R.layout.item_location_pick,
                resultsContainer,
                false,
            ) as MaterialCardView
            row.findViewById<TextView>(R.id.locationPickName).text = result.displayName
            row.findViewById<TextView>(R.id.locationPickCoordinates).text =
                activity.getString(R.string.location_coordinates_format, result.latitude, result.longitude)
            row.setOnClickListener { selectSearchResult(result) }
            resultsContainer.addView(
                row,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = rowMargin }
            )
        }
    }

    fun performLocationSearch() {
        val query = searchInput.text?.toString()?.trim().orEmpty()
        if (query.isBlank()) {
            searchStatus.text = activity.getString(R.string.location_search_empty)
            return
        }

        setSearchLoading(true)
        searchStatus.text = ""
        resultsContainer.removeAllViews()

        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val language = Locale.getDefault().toLanguageTag()
        val url = "https://nominatim.openstreetmap.org/search?format=jsonv2&limit=5&q=$encodedQuery"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "PassWallet Android (org.baumweg.passwallet)")
            .header("Accept-Language", language)
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity.runOnUiThread {
                    setSearchLoading(false)
                    searchStatus.text = activity.getString(R.string.location_search_failed)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val results = mutableListOf<LocationSearchResult>()
                var failed = !response.isSuccessful
                try {
                    val body = response.body()?.string().orEmpty()
                    if (!failed) {
                        val array = JSONArray(body)
                        for (index in 0 until array.length()) {
                            val obj = array.getJSONObject(index)
                            val latitude = obj.optString("lat").toDoubleOrNull()
                            val longitude = obj.optString("lon").toDoubleOrNull()
                            val displayName = obj.optString("display_name")
                            if (latitude != null && longitude != null && displayName.isNotBlank()) {
                                results.add(LocationSearchResult(displayName, latitude, longitude))
                            }
                        }
                    }
                } catch (_: Exception) {
                    failed = true
                } finally {
                    response.close()
                }

                activity.runOnUiThread {
                    setSearchLoading(false)
                    if (failed) {
                        searchStatus.text = activity.getString(R.string.location_search_failed)
                    } else {
                        showSearchResults(results)
                    }
                }
            }
        })
    }

    fun saveLocation(): Boolean {
        latitudeLayout.error = null
        longitudeLayout.error = null

        val latitude = latitudeInput.text?.toString()?.trim()?.replace(',', '.')?.toDoubleOrNull()
        val longitude = longitudeInput.text?.toString()?.trim()?.replace(',', '.')?.toDoubleOrNull()

        var hasError = false
        if (latitude == null || latitude !in -90.0..90.0) {
            latitudeLayout.error = activity.getString(R.string.invalid_location_coordinates)
            hasError = true
        }
        if (longitude == null || longitude !in -180.0..180.0) {
            longitudeLayout.error = activity.getString(R.string.invalid_location_coordinates)
            hasError = true
        }
        if (hasError) {
            return false
        }

        pass.locations = listOf(PassLocation().apply {
            name = nameInput.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            lat = latitude!!
            lon = longitude!!
        })
        refreshCallback.invoke()
        return true
    }

    nameInput.setText(existingLocation?.name.orEmpty())
    searchInput.setText(existingLocation?.name.orEmpty())
    existingLocation?.let {
        latitudeInput.setText(it.lat.toString())
        longitudeInput.setText(it.lon.toString())
    }

    searchButton.setOnClickListener { performLocationSearch() }
    searchInput.setOnEditorActionListener { _, actionId, _ ->
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            performLocationSearch()
            true
        } else {
            false
        }
    }
    saveButton.setOnClickListener {
        if (saveLocation()) {
            dialog.dismiss()
        }
    }
    removeButton.visibility = if (existingLocation != null) View.VISIBLE else View.GONE
    removeButton.setOnClickListener {
        pass.locations = emptyList()
        refreshCallback.invoke()
        dialog.dismiss()
    }

    dialog.setContentView(view)
    dialog.show()
}
