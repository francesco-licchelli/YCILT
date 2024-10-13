package com.example.ycilt

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewAnimationUtils
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
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
import com.example.ycilt.utils.NetworkUtils.deleteRequest
import com.example.ycilt.utils.NetworkUtils.getRequest
import com.example.ycilt.otherssongs.SongPreview
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
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import kotlin.math.hypot

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var googleMap: GoogleMap
    private val MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey"
    private var recordIButton: ImageButton? = null
    private var userLocation: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (getSharedPreferences(Constants.SHARED_PREFS, MODE_PRIVATE).getString("client_secret", null) == null) {
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

        val menuIButton = findViewById<ImageButton>(R.id.btn_menu)
        val addressEditText = findViewById<EditText>(R.id.searchAddress)
        val filterIButton = findViewById<ImageButton>(R.id.btn_filter)
        val resyncGPSIButton = findViewById<ImageButton>(R.id.btn_resync_gps)
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val navigationView = findViewById<NavigationView>(R.id.navigation_view)
        recordIButton = findViewById(R.id.btn_record_audio)


        filterIButton.setOnClickListener{
            CoroutineScope(Dispatchers.IO).launch {
                val (responseCode, responseBody) = getRequest(
                    "audio/my",
                    authRequest = this@MainActivity,
                )
                Log.d("MainActivity", "Response code: $responseCode")
                Log.d("MainActivity", "Response body: $responseBody")
            }.start()
        }

        resyncGPSIButton.setOnClickListener {
            getUserLocationAndUpdateMap()
        }

        recordIButton?.setOnClickListener {
            userLocation?.let { location ->
                val intent = Intent(this, AudioRecorderActivity::class.java).apply{
                    putExtra("latitude", location.latitude)
                    putExtra("longitude", location.longitude)
                }
                startActivity(intent)
            } ?: run {
                Toast.makeText(this, "Location not available. Cannot start recording.", Toast.LENGTH_SHORT).show()
            }

        }

        menuIButton.setOnClickListener {
            if (!drawerLayout.isDrawerOpen(GravityCompat.START))
                drawerLayout.openDrawer(GravityCompat.START)
            else
                drawerLayout.closeDrawer(GravityCompat.START)
        }
        navigationView.setNavigationItemSelectedListener { menuItem ->
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
                            "auth/unsubscribe",
                            authRequest = this@MainActivity,
                        )
                        val responseJson = JSONObject(responseBody)
                        runOnUiThread {
                            Toast.makeText(
                                this@MainActivity,
                                responseJson.getString("detail"),
                                Toast.LENGTH_SHORT
                            ).show()
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
                    startActivity(intent)
                }
            }
            true
        }

        checkLocationPermission()
    }


    private fun loadSongs() {
        CoroutineScope(Dispatchers.IO).launch {
            val (responseCode, responseBody) = getRequest(
                "audio/all",
                authRequest = this@MainActivity,
            )
            Log.d("MainActivity", "Response code: $responseCode")
            Log.d("MainActivity", "Response body: $responseBody")
            try{ //oppure, come anche per MySongsActivity.fetchSongsFromBackend, dovrei usare un if sul code?
                val songs = JSONArray(responseBody)
                val songsList = mutableListOf<SongPreview>()
                for (i in 0 until songs.length()) {
                    val songJson = songs.getJSONObject(i)
                    songsList.add(
                        SongPreview(
                            id = songJson.getInt("id"),
                            latitude = songJson.getDouble("latitude"),
                            longitude = songJson.getDouble("longitude"),
                        )
                    )
                    runOnUiThread {
                        val marker = this@MainActivity.googleMap.addMarker(
                            MarkerOptions().position(
                                LatLng(
                                    songJson.getDouble("latitude"),
                                    songJson.getDouble("longitude")
                                )
                            ))
                        marker?.tag = songJson.getInt("id")
                    }
                }

            }
            catch (e: JSONException) {
                val responseJson = JSONObject(responseBody)
                runOnUiThread {
                    Log.d("MainActivity", "Failed to load songs: $responseCode")
                    Toast.makeText(
                        this@MainActivity,
                        responseJson.getString("detail"),
                        Toast.LENGTH_SHORT
                    ).show()
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
            == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    userLocation = LatLng(it.latitude, it.longitude)
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getUserLocationAndUpdateMap()
            }
            else{
                recordIButton?.isEnabled = false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        checkLocationPermission()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
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

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}