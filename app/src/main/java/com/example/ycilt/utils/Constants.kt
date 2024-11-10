package com.example.ycilt.utils

import org.json.JSONArray
import org.json.JSONObject

fun Boolean.toInt() = if (this) 1 else 0
fun Int.toBoolean() = this != 0

fun JSONArray.toList(): List<JSONObject> {
	val list = mutableListOf<JSONObject>()
	for (i in 0 until length()) {
		list.add(getJSONObject(i))
	}
	return list
}


object Constants {
	const val MAX_FILE_SIZE: Long = 5 * 1024 * 1024
	const val NOT_UPLOADED = -1
	const val MIN_RECORDING_DURATION_MS = 2000L
	const val FETCH_AUDIO_INTERVAL = 5000L
}

object Workers {
	const val NOTIFICATION_CHANNEL_ID = "yclit_channel"
}

object Keys {
	const val TOKEN = "client_secret"
	const val IS_LOGGED = "is_logged_in"
	const val SHARED_PREFS = "shared_prefs"
	const val AUDIO_QUEUE = "audio_queue"
	const val AUDIO_QUEUE_LENGTH = "audio_queue_length"
}

object Urls {
	const val BASE_URL = "http://130.136.2.83/lam2024/"
	const val ADDR_TO_COORD_API = "https://maps.googleapis.com/maps/api/geocode/json"
}

object Coords {
	const val DEFAULT_LATITUDE = 44.497994
	const val DEFAULT_LONGITUDE = 11.355701
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

