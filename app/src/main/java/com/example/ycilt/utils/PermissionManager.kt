package com.example.ycilt.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.ycilt.utils.PermissionCodes.AUDIO_RECORD_PERMISSION_REQUEST_CODE
import com.example.ycilt.utils.PermissionCodes.INTERNET_PERMISSION_REQUEST_CODE
import com.example.ycilt.utils.PermissionCodes.LOCATION_PERMISSION_REQUEST_CODE
import com.example.ycilt.utils.PermissionCodes.SEND_NOTIFICATION_PERMISSION_REQUEST_CODE

object PermissionUtils {

	fun hasLocationPermission(context: Context): Boolean {
		return ContextCompat.checkSelfPermission(
			context,
			Manifest.permission.ACCESS_FINE_LOCATION
		) == PackageManager.PERMISSION_GRANTED &&
				ContextCompat.checkSelfPermission(
					context,
					Manifest.permission.ACCESS_COARSE_LOCATION
				) == PackageManager.PERMISSION_GRANTED
	}

	fun requestLocationPermission(activity: Activity) {
		val permissionsToRequest = mutableListOf<String>()

		if (ContextCompat.checkSelfPermission(
				activity,
				Manifest.permission.ACCESS_FINE_LOCATION
			) != PackageManager.PERMISSION_GRANTED
		) {
			permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
		}
		if (ContextCompat.checkSelfPermission(
				activity,
				Manifest.permission.ACCESS_COARSE_LOCATION
			) != PackageManager.PERMISSION_GRANTED
		) {
			permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
		}

		if (permissionsToRequest.isNotEmpty()) {
			ActivityCompat.requestPermissions(
				activity,
				permissionsToRequest.toTypedArray(),
				LOCATION_PERMISSION_REQUEST_CODE
			)
		}

	}

	fun isLocationEnabled(context: Context): Boolean {
		val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
		return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
				locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
	}

	fun hasInternetPermission(context: Context): Boolean {
		return ContextCompat.checkSelfPermission(
			context,
			Manifest.permission.INTERNET
		) == PackageManager.PERMISSION_GRANTED
	}

	fun requestInternetPermission(activity: Activity) {
		if (!hasInternetPermission(activity)) {
			ActivityCompat.requestPermissions(
				activity,
				arrayOf(Manifest.permission.INTERNET),
				INTERNET_PERMISSION_REQUEST_CODE
			)
		}
	}

	fun isInternetEnabled(context: Context): Boolean {
		val connectivityManager =
			context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
		val network = connectivityManager.activeNetwork
		val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
		return networkCapabilities != null &&
				(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
						networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
						networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
	}

	fun hasRecordPermission(context: Context): Boolean {
		return ActivityCompat.checkSelfPermission(
			context,
			Manifest.permission.RECORD_AUDIO
		) == PackageManager.PERMISSION_GRANTED
	}

	fun requestRecordPermission(activity: Activity) {
		ActivityCompat.requestPermissions(
			activity,
			arrayOf(Manifest.permission.RECORD_AUDIO),
			AUDIO_RECORD_PERMISSION_REQUEST_CODE
		)
	}

	fun hasNotificationPermission(context: Context): Boolean {
		return ContextCompat.checkSelfPermission(
			context,
			Manifest.permission.POST_NOTIFICATIONS
		) == PackageManager.PERMISSION_GRANTED
	}

	fun requestNotificationPermission(activity: FragmentActivity) {
		ActivityCompat.requestPermissions(
			activity,
			arrayOf(Manifest.permission.POST_NOTIFICATIONS),
			SEND_NOTIFICATION_PERMISSION_REQUEST_CODE
		)
	}

}
