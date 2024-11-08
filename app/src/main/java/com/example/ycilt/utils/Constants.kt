package com.example.ycilt.utils

import android.content.Context
import android.location.Geocoder
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.example.ycilt.others_audio.Category
import com.example.ycilt.utils.NetworkUtils.getRequest
import com.example.ycilt.utils.Urls.ADDR_TO_COORD_API
import org.json.JSONException
import org.json.JSONObject

fun Boolean.toInt() = if (this) 1 else 0
fun Int.toBoolean() = this != 0

object Constants {
	const val MAP_VIEW_BUNDLE_KEY: String = "MapViewBundleKey"
	const val MAX_FILE_SIZE: Long = 5 * 1024 * 1024
	const val NOT_UPLOADED = -1
	const val DEFAULT_FETCH_AUDIO_DELAY = 5000L
	const val MAX_FETCH_AUDIO_DELAY = 10000L
}

object Workers {
	const val NOTIFICATION_CHANNEL_ID = "yclit_channel"
}

object Keys {
	const val TOKEN = "client_secret"
	const val IS_LOGGED = "is_logged_in"
	const val SHARED_PREFS = "shared_prefs"
}

object Urls {
	const val BASE_URL = "http://130.136.2.83/lam2024/"
	const val ADDR_TO_COORD_API = "https://maps.googleapis.com/maps/api/geocode/json"
}

object PermissionCodes {
	const val LOCATION_PERMISSION_REQUEST_CODE = 1000
	const val INTERNET_PERMISSION_REQUEST_CODE = 1001
	const val AUDIO_RECORD_PERMISSION_REQUEST_CODE = 1002
	const val SEND_NOTIFICATION_PERMISSION_REQUEST_CODE = 1003
}

object Privacy {
	const val UNKNOWN_PRIVACY = -1
	const val PUBLIC_AUDIO = 0
	const val PRIVATE_AUDIO = 1
}


object Misc {


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
			Log.e("GeocodingAPI", "Error: $responseCode")
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


	val activeToasts = mutableListOf<Toast>()
	fun displayToast(context: Context, message: String) {
		activeToasts.forEach { it.cancel() }
		activeToasts.clear()

		val toast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
		toast.show()

		activeToasts.add(toast)
		toast.addCallback(object : Toast.Callback() {
			override fun onToastHidden() {
				activeToasts.remove(toast)
			}
		})
	}

	fun audioToMetadataFilename(audioFilename: String): String {
		return audioFilename.replace(".mp3", "_metadata.json")
	}

	fun metadataToAudioFilename(metadataFilename: String): String {
		return metadataFilename.replace("_metadata.json", ".mp3")
	}

}