package com.example.ycilt.utils

import android.content.Context
import android.location.Geocoder
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.example.ycilt.otherssongs.Category
import com.example.ycilt.utils.Constants.ADDR_TO_COORD_API
import com.example.ycilt.utils.NetworkUtils.getRequest
import org.json.JSONException
import org.json.JSONObject

object Constants {
    const val BASE_URL = "http://130.136.2.83/lam2024/"
    const val MAX_SONG_BYTES = 5*1024*1024
    const val SHARED_PREFS = "shared_prefs"
    const val RECORDING_PERMISSION_REQUEST_CODE = 1001
    const val LOCATION_PERMISSION_REQUEST_CODE = 1000
    const val NOT_UPLOADED = -1
    const val ADDR_TO_COORD_API = "https://maps.googleapis.com/maps/api/geocode/json"
    const val BROADCAST_UPLOAD = "com.example.ycilt.SONG_UPLOADED"
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

    fun coordToAddr(context: Context, latitude: Double, longitude: Double, callback: (String?) -> Unit) {
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


    fun addrToCoord(address: String, apiKey: String): Pair<Double, Double>? {
        val formattedAddress = address.replace(" ", "+")
        val (responseCode, responseBody) = getRequest(
            ADDR_TO_COORD_API,
            null,
            mapOf("address" to formattedAddress, "key" to apiKey),
            true
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
                    Pair(lat, lng) // Restituisce le coordinate come Pair
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
        Toast.makeText(
            context,
            try {
                JSONObject(message).getString("detail")
            } catch (e: JSONException) {
                message
            },
            Toast.LENGTH_LONG).show()
    }

}