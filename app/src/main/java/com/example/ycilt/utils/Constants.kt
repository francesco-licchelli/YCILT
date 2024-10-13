package com.example.ycilt.utils

import android.content.Context
import android.location.Geocoder
import android.os.Handler
import android.os.Looper
import com.example.ycilt.otherssongs.Category
import org.json.JSONObject

object Constants {
    const val BASE_URL = "http://130.136.2.83/lam2024/"
    const val MAX_SONG_BYTES = 5*1024*1024
    const val SHARED_PREFS = "shared_prefs"
    const val RECORDING_PERMISSION_REQUEST_CODE = 1001
    const val LOCATION_PERMISSION_REQUEST_CODE = 1000
    const val NOT_UPLOADED = -1
    const val MISSING_LOCATION = "Missing location"

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

    fun coordToAddr(context: Context, latitude: Double, longitude: Double, callback: (String) -> Unit) {
        val handler = Handler(Looper.getMainLooper())
        val geocoder = Geocoder(context)

        geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
            handler.post {
                val addressText = if (addresses.isEmpty()) {
                    Constants.MISSING_LOCATION
                } else {
                    "${addresses[0].locality}, ${addresses[0].countryName}"
                }
                callback(addressText)
            }
        }
    }

}
