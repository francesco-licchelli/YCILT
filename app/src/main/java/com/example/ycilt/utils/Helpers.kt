package com.example.ycilt.utils

import android.content.Context
import android.location.Geocoder
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.ycilt.others_audio.Category
import com.example.ycilt.utils.NetworkUtils.getRequest
import com.example.ycilt.utils.ToastManager.displayToast
import com.example.ycilt.utils.Urls.ADDR_TO_COORD_API
import org.json.JSONException
import org.json.JSONObject

private fun jsonToMap(jsonObject: JSONObject): Map<String, Float> {
	val map = mutableMapOf<String, Float>()
	jsonObject.keys().forEach {
		map[it] = jsonObject.getDouble(it).toFloat()
	}
	return map
}

fun jsonToCat(jsonObject: JSONObject): Category {
	return Category(jsonToMap(jsonObject))
}

fun coordToAddr(
	context: Context,
	latitude: Double,
	longitude: Double,
	callback: (String?) -> Unit
) {
	val handler = Handler(Looper.getMainLooper())
	val geocoder = Geocoder(context)

	geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
		handler.post {
			val addressText = if (addresses.isEmpty()) {
				null
			} else {
				"${addresses[0].locality}, ${addresses[0].countryName}"
			}
			callback(addressText)
		}
	}
}


fun addrToCoord(address: String, apiKey: String, context: Context): Pair<Double, Double>? {
	val formattedAddress = address.replace(" ", "+")
	val (responseCode, responseBody) = getRequest(
		endpoint = ADDR_TO_COORD_API,
		context = context,
		loginRequired = false,
		queryParams = mapOf("address" to formattedAddress, "key" to apiKey),
		isFullEndpoint = true
	)
	return if (responseCode == 200) {
		try {
			val jsonResponse = JSONObject(responseBody)
			if (jsonResponse.getString("status") == "OK") {
				val results = jsonResponse.getJSONArray("results")
				if (results.length() == 0) {
					return null
				}
				val location = jsonResponse.getJSONArray("results")
					.getJSONObject(0)
					.getJSONObject("geometry")
					.getJSONObject("location")

				val lat = location.getDouble("lat")
				val lng = location.getDouble("lng")
				Pair(lat, lng)
			} else {
				null
			}
		} catch (e: JSONException) {
			e.printStackTrace()
			null
		}
	} else {
		Log.e("GeocodingAPI", "Error: $PermissionUtils.esponseCode")
		null
	}
}

fun displayError(context: Context, message: String) {
	displayToast(
		context, try {
			JSONObject(message).getString("detail")
		} catch (e: JSONException) {
			message
		}
	)
}

fun audioToMetadataFilename(audioFilename: String): String {
	return audioFilename.replace(".mp3", "_metadata.json")
}
