package com.example.ycilt

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.ycilt.auth.LoginActivity
import com.example.ycilt.my_audio.AudioRecorderActivity
import com.example.ycilt.my_audio.MyAudioActivity
import com.example.ycilt.others_audio.AudioInfoActivity
import com.example.ycilt.utils.Constants.DEFAULT_FETCH_AUDIO_DELAY
import com.example.ycilt.utils.Constants.MAP_VIEW_BUNDLE_KEY
import com.example.ycilt.utils.Constants.MAX_FETCH_AUDIO_DELAY
import com.example.ycilt.utils.Keys.IS_LOGGED
import com.example.ycilt.utils.Keys.SHARED_PREFS
import com.example.ycilt.utils.Misc
import com.example.ycilt.utils.NetworkUtils.deleteRequest
import com.example.ycilt.utils.NetworkUtils.getRequest
import com.example.ycilt.utils.PermissionUtils
import com.example.ycilt.utils.ToastManager.displayToast
import com.example.ycilt.workers.WorkerManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
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
import kotlin.math.max

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
	private lateinit var mapView: MapView
	private lateinit var fusedLocationClient: FusedLocationProviderClient
	private lateinit var googleMap: GoogleMap
	private var recordIButton: ImageButton? = null
	private var userLocation: LatLng? = null
	private var loadAudioJob: Job? = null
	private var fetchAudioDelay: Long = DEFAULT_FETCH_AUDIO_DELAY
	private var markers: HashMap<Int, Marker> = HashMap()
	/*
	* TODO
	*  - Rimuovere pulsante sopra la ricerca di indirizzo
	* */

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		WorkerManager.initialize(this)

		if (getSharedPreferences(SHARED_PREFS, MODE_PRIVATE).getString(
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
			if (!PermissionUtils.hasLocationPermission((this))) {
				PermissionUtils.requestLocationPermission(this)
				displayToast(this, getString(R.string.gps_permission_needed)) //TODO edit string
				return@setOnClickListener
			}
			if (!PermissionUtils.isLocationEnabled(this)) {
				displayToast(this, getString(R.string.activate_gps))
				return@setOnClickListener
			}
			getUserLocationAndUpdateMap()
		}

		findViewById<ImageButton>(R.id.btn_record_audio).setOnClickListener {
			if (!PermissionUtils.hasLocationPermission((this))) {
				PermissionUtils.requestLocationPermission(this)
				displayToast(this, getString(R.string.gps_permission_needed))
				return@setOnClickListener
			}
			if (!PermissionUtils.isLocationEnabled(this)) {
				displayToast(this, getString(R.string.activate_gps))
				return@setOnClickListener
			}
			userLocation?.let { location ->
				val intent = Intent(this, AudioRecorderActivity::class.java).apply {
					putExtra("latitude", location.latitude)
					putExtra("longitude", location.longitude)
				}
				startActivity(intent)
			}
		}

		findViewById<ImageButton>(R.id.btn_menu).setOnClickListener {
			if (!drawerLayout.isDrawerOpen(GravityCompat.START))
				drawerLayout.openDrawer(GravityCompat.START)
			else
				drawerLayout.closeDrawer(GravityCompat.START)
		}

		findViewById<EditText>(R.id.searchAddress).setOnEditorActionListener { v, actionId, event ->
			if (!PermissionUtils.hasInternetPermission(this)) {
				PermissionUtils.requestInternetPermission(this)
				displayToast(this, getString(R.string.internet_permission_needed))
				return@setOnEditorActionListener false
			}
			if (!PermissionUtils.isInternetEnabled(this)) {
				displayToast(this, getString(R.string.activate_internet))
				return@setOnEditorActionListener false
			}
			if (actionId == EditorInfo.IME_ACTION_DONE ||
				(event != null && event.keyCode == KeyEvent.KEYCODE_ENTER
						&& event.action == KeyEvent.ACTION_DOWN)
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
					getSharedPreferences(SHARED_PREFS, MODE_PRIVATE).edit().clear().apply()
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

				R.id.drawer_my_audio -> {
					val intent = Intent(this, MyAudioActivity::class.java)
					intent.putExtra(IS_LOGGED, true)
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

		if (!PermissionUtils.hasInternetPermission(this)) {
			PermissionUtils.requestInternetPermission(this)
		}
		if (!PermissionUtils.hasLocationPermission(this)) {
			PermissionUtils.requestLocationPermission(this)
		}
		getUserLocationAndUpdateMap()
	}

	override fun onResume() {
		super.onResume()
		mapView.onResume()
		markers = HashMap()
		getUserLocationAndUpdateMap()
		if (::googleMap.isInitialized) {
			googleMap.clear()
			loadAudioJob = CoroutineScope(Dispatchers.Main).launch {
				while (isActive) {
					loadAudio()
					delay(fetchAudioDelay)
				}
			}
		} else {
			mapView.getMapAsync(this)
		}
	}

	override fun onPause() {
		super.onPause()
		mapView.onPause()
		loadAudioJob?.cancel() // Ferma la routine se la coroutine è attiva
	}

	override fun onDestroy() {
		super.onDestroy()
		mapView.onDestroy()
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

	private fun incrementFetchAudioDelay() {
		fetchAudioDelay = max((fetchAudioDelay * 1.5).toLong(), MAX_FETCH_AUDIO_DELAY)
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
					displayToast(this@MainActivity, getString(R.string.cant_resolve_address))
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

	private fun loadAudio() {
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
				incrementFetchAudioDelay()
				startActivity(intent)
				finish()
			}
			try {
				val audio = JSONArray(responseBody)
				val remoteIds: Array<Int> =
					Array(audio.length()) { index -> audio.getJSONObject(index).getInt("id") }

				fetchAudioDelay = DEFAULT_FETCH_AUDIO_DELAY
				for (i in 0 until audio.length()) {
					val audioJson = audio.getJSONObject(i)
					if (!markers.containsKey(audioJson.getInt("id"))) {
						runOnUiThread {
							val marker = googleMap.addMarker(
								MarkerOptions().position(
									LatLng(
										audioJson.getDouble("latitude"),
										audioJson.getDouble("longitude")
									)
								)
							)!!
							marker.tag = audioJson.getInt("id")
							markers[audioJson.getInt("id")] = marker
						}
					}
				}

				val keysToRemove = markers.filter { !remoteIds.contains(it.key) }.map { it.key }

				keysToRemove.forEach {
					runOnUiThread {
						markers[it]?.remove()
					}
					markers.remove(it)
				}

			} catch (e: JSONException) {
				runOnUiThread {
					incrementFetchAudioDelay()
					displayToast(
						this@MainActivity,
						getString(R.string.failed_to_load_audio_error, responseCode)
					)
				}
			}
		}
		googleMap.setOnMarkerClickListener { marker ->
			val audioId = marker.tag as Int
			val intent = Intent(this@MainActivity, AudioInfoActivity::class.java).apply {
				putExtra("audioId", audioId)
			}
			startActivity(intent)
			true
		}
	}

	override fun onMapReady(googleMap: GoogleMap) {
		this.googleMap = googleMap
		getUserLocationAndUpdateMap()
		loadAudio()
	}

	@SuppressLint("MissingPermission")
	private fun getUserLocationAndUpdateMap() {
		if (PermissionUtils.hasLocationPermission(this)) {
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

}