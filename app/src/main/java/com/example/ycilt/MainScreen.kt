package com.example.ycilt

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ycilt.utils.Constants.FETCH_AUDIO_INTERVAL
import com.example.ycilt.utils.Coords.DEFAULT_LATITUDE
import com.example.ycilt.utils.Coords.DEFAULT_LONGITUDE
import com.example.ycilt.utils.ToastManager.displayToast
import com.example.ycilt.utils.addrToCoord
import com.example.ycilt.utils.toList
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
	onNavigateToLogin: () -> Unit,
	onNavigateToAudioRecorder: (Double, Double) -> Unit,
	onNavigateToDeleteAccount: () -> Unit,
	onNavigateToAudioInfo: (Int) -> Unit,
	onNavigateToMyAudio: () -> Unit,
	fetchAudios: () -> JSONArray,
	locationPermittedState: MutableState<Boolean>
) {
	val context: Context = LocalContext.current
	val activity: Activity = context as Activity

	val markerStates = remember { mutableStateListOf<Pair<MarkerState, Int>>() }
	val drawerState = rememberDrawerState(DrawerValue.Closed)
	val coroutineScope = rememberCoroutineScope()
	val focusRequester = remember { FocusRequester() }

	var isSearching by remember { mutableStateOf(false) }
	var searchText by remember { mutableStateOf(TextFieldValue("")) }

	var userLocation by remember { mutableStateOf<LatLng?>(null) }

	val cameraPositionState = rememberCameraPositionState {
		position = CameraPosition.fromLatLngZoom(LatLng(45.0, 12.0), 15f)
	}

	fun updateMarkers(audios: JSONArray) {
		markerStates.removeAll { (_, id) ->
			audios.toList().none { it.getInt("id") == id }
		}.toString()
		audios.toList().filter { newLocation ->
			markerStates.none { (_, id) -> id == newLocation.getInt("id") }
		}.forEach { newLocation ->
			val lat = newLocation.getDouble("latitude")
			val lng = newLocation.getDouble("longitude")
			val id = newLocation.getInt("id")
			markerStates.add(MarkerState(position = LatLng(lat, lng)) to id)
		}
	}

	LaunchedEffect(Unit) {
		while (true) {
			withContext(Dispatchers.IO) {
				updateMarkers(fetchAudios())
				delay(FETCH_AUDIO_INTERVAL)
			}
		}
	}

	userLocation = getUserLocationAndUpdateMap(
		activity,
		cameraPositionState
	)

	ModalNavigationDrawer(
		drawerState = drawerState,
		gesturesEnabled = drawerState.isOpen,
		drawerContent = {
			DrawerContent(
				onLogout = onNavigateToLogin,
				onMyAudio = onNavigateToMyAudio,
				onDeleteAccount = onNavigateToDeleteAccount
			)
		},
	) {
		Scaffold(
			topBar = {
				CenterAlignedTopAppBar(
					title = { Text("YCILT") },
					navigationIcon = {
						IconButton(onClick = {
							coroutineScope.launch { drawerState.open() }
						}) {
							Icon(Icons.Filled.Menu, contentDescription = "Open Menu")
						}
					},
					actions = {
						if (!isSearching) {
							IconButton(onClick = {
								isSearching = true
							}) {
								Icon(Icons.Filled.Search, contentDescription = "Search")
							}
						} else {
							IconButton(onClick = { isSearching = false }) {
								Icon(Icons.Filled.Close, contentDescription = "Close Search")
							}
						}
					}
				)
			},
			floatingActionButton = {
				FloatingActionButton(
					onClick = {
						onNavigateToAudioRecorder(
							userLocation?.latitude ?: 0.0,
							userLocation?.longitude ?: 0.0
						)
					}
				) {
					Icon(
						Icons.Filled.Add,
						contentDescription = stringResource(R.string.add_audio)
					)
				}
			}
		) { innerPadding ->
			Column(
				modifier = Modifier
					.padding(innerPadding)
					.fillMaxSize()
			) {
				if (isSearching) {
					LaunchedEffect(Unit) {
						focusRequester.requestFocus()
					}
					SearchBar(
						searchText = searchText,
						onTextChange = { searchText = it },
						focusRequester = focusRequester,
						cameraPositionState = cameraPositionState
					)
				}
				Box(
					modifier = Modifier
						.fillMaxSize()
				) {
					GoogleMap(
						cameraPositionState = cameraPositionState,
						modifier = Modifier.fillMaxSize(),
						properties = MapProperties(isMyLocationEnabled = locationPermittedState.value),
						uiSettings = MapUiSettings(zoomControlsEnabled = false),
						onMapLoaded = {
							CoroutineScope(Dispatchers.IO).launch {
								updateMarkers(fetchAudios())
							}
						}
					) {
						markerStates.forEach { (state, id) ->
							Marker(
								state = state,
								tag = id,
								onClick = {
									onNavigateToAudioInfo(id)
									true
								}
							)
						}
					}
				}
			}
		}
	}
}

@Composable
fun DrawerItem(
	stringId: Int,
	iconId: Int,
	onClick: () -> Unit,
) {
	Spacer(modifier = Modifier.height(4.dp))
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.clickable(onClick = onClick)
			.padding(16.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		Icon(
			painter = painterResource(id = iconId),
			contentDescription = stringResource(id = R.string.my_uploads),
			tint = MaterialTheme.colorScheme.onSurface
		)
		Spacer(modifier = Modifier.width(16.dp))
		Text(
			text = stringResource(id = stringId),
			style = MaterialTheme.typography.bodyMedium
		)
	}
}

@Composable
fun DrawerContent(
	onMyAudio: () -> Unit,
	onLogout: () -> Unit,
	onDeleteAccount: () -> Unit,
) {
	Column(
		modifier = Modifier
			.fillMaxHeight()
			.width(if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) 420.dp else 280.dp)
			.background(MaterialTheme.colorScheme.surface)
	) {
		DrawerItem(
			stringId = R.string.my_uploads,
			iconId = R.drawable.ic_note,
			onClick = onMyAudio
		)
		Spacer(modifier = Modifier.height(8.dp))
		DrawerItem(
			stringId = R.string.logout,
			iconId = R.drawable.ic_logout,
			onClick = onLogout
		)
		Spacer(modifier = Modifier.height(8.dp))
		DrawerItem(
			stringId = R.string.delete_account,
			iconId = R.drawable.ic_delete,
			onClick = onDeleteAccount
		)
	}
}

@SuppressLint("MissingPermission")
@Composable
fun getUserLocationAndUpdateMap(
	activity: Activity,
	cameraPositionState: CameraPositionState
): LatLng? {
	var userLocation by remember { mutableStateOf<LatLng?>(null) }

	LaunchedEffect(Unit) {

		val fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)

		fusedLocationClient.lastLocation
			.addOnSuccessListener { location: Location? ->
				location?.let {
					val result = LatLng(it.latitude, it.longitude)
					userLocation = result
					cameraPositionState.position = CameraPosition.fromLatLngZoom(result, 15f)
				}
			}.addOnFailureListener {
				userLocation = LatLng(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
				cameraPositionState.position = CameraPosition.fromLatLngZoom(userLocation!!, 15f)
			}
	}

	return userLocation
}


@Composable
fun SearchBar(
	searchText: TextFieldValue,
	onTextChange: (TextFieldValue) -> Unit,
	focusRequester: FocusRequester,
	cameraPositionState: CameraPositionState
) {
	val keyboardController = LocalSoftwareKeyboardController.current
	val context = LocalContext.current

	fun getApiKeyFromManifest(): String? {
		return try {
			val applicationInfo =
				context.packageManager.getApplicationInfo(
					context.packageName,
					PackageManager.GET_META_DATA
				)
			val bundle = applicationInfo.metaData
			bundle.getString("com.google.android.geo.API_KEY")
		} catch (e: PackageManager.NameNotFoundException) {
			e.printStackTrace()
			null
		}
	}


	OutlinedTextField(
		value = searchText,
		onValueChange = onTextChange,
		modifier = Modifier
			.fillMaxWidth()
			.focusRequester(focusRequester),
		keyboardOptions = KeyboardOptions.Default.copy(
			imeAction = ImeAction.Done // Imposta il tasto "Fatto" sulla tastiera
		),
		keyboardActions = KeyboardActions(
			onDone = {
				CoroutineScope(Dispatchers.Main).launch {
					withContext(Dispatchers.IO) {
						val apiKey = getApiKeyFromManifest() ?: ""
						val result = addrToCoord(searchText.text, apiKey, context)

						if (result == null) {
							withContext(Dispatchers.Main) {
								displayToast(context, "Location not found")
							}
							return@withContext
						}

						val (lat, lng) = result
						withContext(Dispatchers.Main) {
							cameraPositionState.position =
								CameraPosition.fromLatLngZoom(LatLng(lat, lng), 15f)
						}

						keyboardController?.hide()
					}
				}
			}
		),
		leadingIcon = {
			Icon(
				imageVector = Icons.Filled.Search,
				contentDescription = "Search Icon"
			)
		},
		placeholder = {
			Text(text = stringResource(id = R.string.search))
		},
		singleLine = true,
		shape = MaterialTheme.shapes.medium
	)
}


@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
	MaterialTheme {
		Surface {
			MainScreen(
				onNavigateToLogin = {},
				onNavigateToAudioRecorder = { _, _ -> },
				onNavigateToDeleteAccount = {},
				onNavigateToAudioInfo = {},
				onNavigateToMyAudio = {},
				fetchAudios = { JSONArray() },
				locationPermittedState = remember { mutableStateOf(false) }
			)
		}
	}
}

