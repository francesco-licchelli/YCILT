package com.example.ycilt

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.ycilt.auth.LoginActivity
import com.example.ycilt.mysongs.AudioRecorderActivity
import com.example.ycilt.mysongs.MySongsActivity
import com.example.ycilt.otherssongs.SongInfoActivity
import com.example.ycilt.utils.Constants
import com.example.ycilt.utils.Constants.LOCATION_PERMISSION_REQUEST_CODE
import com.example.ycilt.utils.Constants.MAP_VIEW_BUNDLE_KEY
import com.example.ycilt.utils.Misc
import com.example.ycilt.utils.Misc.displayToast
import com.example.ycilt.utils.NetworkUtils.deleteRequest
import com.example.ycilt.utils.NetworkUtils.getRequest
import com.example.ycilt.workers.WorkerManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.HttpURLConnection

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

	private lateinit var mapView: MapView
	private lateinit var fusedLocationClient: FusedLocationProviderClient
	private lateinit var googleMap: GoogleMap
	private var recordIButton: ImageButton? = null
	private var userLocation: LatLng? = null
	private var loadSongsJob: Job? = null
	private var fetchSongDelay: Long = Constants.DEFAULT_FETCH_SONG_DELAY

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		WorkerManager.initialize(this)

		if (getSharedPreferences(Constants.SHARED_PREFS, MODE_PRIVATE).getString(
				"client_secret",
				null
			) == null
		) {
			val intent = Intent(this, LoginActivity::class.java)
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
			startActivity(intent)
			finish()
		}

		fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

		var mapViewBundle: Bundle? = null
		if (savedInstanceState != null) {
			mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY)
		}

		mapView = findViewById(R.id.mapView)
		mapView.onCreate(mapViewBundle)
		mapView.getMapAsync(this)

		val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)

		findViewById<ImageButton>(R.id.btn_resync_gps).setOnClickListener {
			userLocation?.let { _ ->
				getUserLocationAndUpdateMap()
			} ?: run {
				displayToast(this, getString(R.string.cant_resync))
			}
		}

		findViewById<ImageButton>(R.id.btn_record_audio).setOnClickListener {
			userLocation?.let { location ->
				val intent = Intent(this, AudioRecorderActivity::class.java).apply {
					putExtra("latitude", location.latitude)
					putExtra("longitude", location.longitude)
				}
				startActivity(intent)
			} ?: run {
				displayToast(this, getString(R.string.cant_record))
			}
		}

		findViewById<ImageButton>(R.id.btn_menu).setOnClickListener {
			if (!drawerLayout.isDrawerOpen(GravityCompat.START))
				drawerLayout.openDrawer(GravityCompat.START)
			else
				drawerLayout.closeDrawer(GravityCompat.START)
		}

		findViewById<EditText>(R.id.searchAddress).setOnEditorActionListener { v, actionId, event ->
			if (actionId == EditorInfo.IME_ACTION_DONE ||
				(event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
			) {
				CoroutineScope(Dispatchers.Main).launch {
					if (showLocation(v.text.toString())) {
						v.clearFocus()
						v.text = null
					}
				}
				true
			} else {
				false
			}
		}

		findViewById<NavigationView>(R.id.navigation_view).setNavigationItemSelectedListener { menuItem ->
			when (menuItem.itemId) {
				R.id.drawer_logout -> {
					val intent = Intent(this, LoginActivity::class.java)
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
					startActivity(intent)
					finish()
				}

				R.id.drawer_delete_account -> {
					CoroutineScope(Dispatchers.IO).launch {
						val (responseCode, responseBody) = deleteRequest(
							endpoint = "auth/unsubscribe",
							context = this@MainActivity,
							loginRequired = true
						)
						val responseJson = JSONObject(responseBody)
						runOnUiThread {
							displayToast(
								this@MainActivity,
								responseJson.getString("detail")
							)
						}
						if (responseCode == 200) {
							filesDir.listFiles()?.forEach { it.delete() }
							Handler(Looper.getMainLooper()).post {
								val intent = Intent(this@MainActivity, LoginActivity::class.java)
								intent.flags =
									Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
								startActivity(intent)
							}
						}
					}
				}

				R.id.drawer_my_songs -> {
					val intent = Intent(this, MySongsActivity::class.java)
					intent.putExtra("is_logged_in", true)
					startActivity(intent)
				}
			}
			true
		}

		onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
			override fun handleOnBackPressed() {
				val navigationView = findViewById<NavigationView>(R.id.navigation_view)

				if (drawerLayout != null && navigationView != null && drawerLayout.isDrawerOpen(
						navigationView
					)
				) {
					drawerLayout.closeDrawer(navigationView)
				} else {
					isEnabled = false // Disabilita il callback temporaneamente
					onBackPressedDispatcher.onBackPressed() // Comportamento di default
					isEnabled = true // Riabilita il callback
				}
			}
		})

		checkLocationPermission()
	}

	override fun onResume() {
		super.onResume()
		mapView.onResume()
		getUserLocationAndUpdateMap()

		//TODO if token invalid, then go to login activity

		if (::googleMap.isInitialized) {
			loadSongsJob = CoroutineScope(Dispatchers.Main).launch {
				while (isActive) {
					loadSongs()
					delay(fetchSongDelay)
				}
			}
		} else {
			mapView.getMapAsync(this)
		}
	}

	override fun onPause() {
		super.onPause()
		mapView.onPause()
		loadSongsJob?.cancel() // Ferma la routine se la coroutine è attiva
	}

	override fun onDestroy() {
		super.onDestroy()
		mapView.onDestroy()
	}

	override fun onRequestPermissionsResult(
		requestCode: Int,
		permissions: Array<out String>,
		grantResults: IntArray
	) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)
		if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
			if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				getUserLocationAndUpdateMap()
			} else {
				recordIButton?.isEnabled = false
			}
		}
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)

		var mapViewBundle = outState.getBundle(MAP_VIEW_BUNDLE_KEY)
		if (mapViewBundle == null) {
			mapViewBundle = Bundle()
			outState.putBundle(MAP_VIEW_BUNDLE_KEY, mapViewBundle)
		}

		mapView.onSaveInstanceState(mapViewBundle)
	}

	@Deprecated("Deprecated in Java")
	override fun onLowMemory() {
		super.onLowMemory()
		mapView.onLowMemory()
	}


	private fun incrementFetchSongDelay() {
		fetchSongDelay = (fetchSongDelay * 1.5).toLong()
	}

	private fun getApiKeyFromManifest(): String? {
		return try {
			val applicationInfo =
				packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
			val bundle = applicationInfo.metaData
			bundle.getString("com.google.android.geo.API_KEY")
		} catch (e: PackageManager.NameNotFoundException) {
			e.printStackTrace()
			null
		}
	}

	private suspend fun showLocation(address: String): Boolean {
		return withContext(Dispatchers.IO) {
			val apiKey = getApiKeyFromManifest() ?: ""
			val result = Misc.addrToCoord(address, apiKey, this@MainActivity)

			if (result == null) {
				withContext(Dispatchers.Main) {
					displayToast(this@MainActivity, "Error while resolving the address")
				}
				return@withContext false
			}

			val (lat, lng) = result
			withContext(Dispatchers.Main) {
				userLocation = LatLng(lat, lng)
				googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation!!, 16f))
			}

			return@withContext true
		}
	}

	private fun loadSongs() {
		googleMap.clear()
		CoroutineScope(Dispatchers.IO).launch {
			val (responseCode, responseBody) = getRequest(
				endpoint = "audio/all",
				context = this@MainActivity,
				loginRequired = true
			)
			if (responseCode != HttpURLConnection.HTTP_OK) {
				runOnUiThread { Misc.displayError(this@MainActivity, responseBody) }
				val intent = Intent(this@MainActivity, LoginActivity::class.java)
				intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
				incrementFetchSongDelay()
				startActivity(intent)
				finish()
			}
			try {
				val songs = JSONArray(responseBody)
				fetchSongDelay = Constants.DEFAULT_FETCH_SONG_DELAY
				for (i in 0 until songs.length()) {
					val songJson = songs.getJSONObject(i)
					runOnUiThread {
						val marker = this@MainActivity.googleMap.addMarker(
							MarkerOptions().position(
								LatLng(
									songJson.getDouble("latitude"),
									songJson.getDouble("longitude")
								)
							)
						)
						marker?.tag = songJson.getInt("id")
					}
				}
			} catch (e: JSONException) {
				runOnUiThread {
					incrementFetchSongDelay()
					Log.d("MainActivity", "Failed to load songs: $responseCode")
					displayToast(this@MainActivity, "Failed to load songs: error $responseCode")
				}
			}
		}
		googleMap.setOnMarkerClickListener { marker ->
			val songId = marker.tag as Int
			val intent = Intent(this@MainActivity, SongInfoActivity::class.java).apply {
				putExtra("songId", songId)
			}
			startActivity(intent)
			true
		}
	}

	override fun onMapReady(googleMap: GoogleMap) {
		this.googleMap = googleMap
		getUserLocationAndUpdateMap()
		loadSongs()
	}

	private fun getUserLocationAndUpdateMap() {
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
			== PackageManager.PERMISSION_GRANTED
		) {
			fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
				location?.let {
					userLocation = LatLng(it.latitude, it.longitude)
					googleMap.isMyLocationEnabled = true
					googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation!!, 16f))
					recordIButton?.isEnabled = true
				}
			}
		}
	}

	private fun checkLocationPermission() {
		val fineLocationPermissionCheck = ContextCompat.checkSelfPermission(
			this, Manifest.permission.ACCESS_FINE_LOCATION
		)
		val coarseLocationPermissionCheck = ContextCompat.checkSelfPermission(
			this,
			Manifest.permission.ACCESS_COARSE_LOCATION
		)

		if (fineLocationPermissionCheck != PackageManager.PERMISSION_GRANTED ||
			coarseLocationPermissionCheck != PackageManager.PERMISSION_GRANTED
		) {
			val permissionsToRequest = mutableListOf<String>()
			if (fineLocationPermissionCheck != PackageManager.PERMISSION_GRANTED) {
				permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
			}
			if (coarseLocationPermissionCheck != PackageManager.PERMISSION_GRANTED) {
				permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
			}

			ActivityCompat.requestPermissions(
				this,
				permissionsToRequest.toTypedArray(),
				LOCATION_PERMISSION_REQUEST_CODE
			)
		} else {
			getUserLocationAndUpdateMap()
		}
	}

}