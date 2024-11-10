package com.example.ycilt

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.ycilt.auth.LoginActivity
import com.example.ycilt.auth.SignupActivity
import com.example.ycilt.my_audio.AudioRecorderActivity
import com.example.ycilt.my_audio.MyAudioActivity
import com.example.ycilt.others_audio.AudioInfoActivity
import com.example.ycilt.utils.Keys.IS_LOGGED
import com.example.ycilt.utils.Keys.SHARED_PREFS
import com.example.ycilt.utils.Misc.displayError
import com.example.ycilt.utils.NetworkUtils.deleteRequest
import com.example.ycilt.utils.NetworkUtils.getRequest
import com.example.ycilt.utils.ToastManager.displayToast
import com.example.ycilt.workers.WorkerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.HttpURLConnection

class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		WorkerManager.initialize(this)

		fun loadAudio(): JSONArray {
			var audioJsonArray = JSONArray()
			val (responseCode, responseBody) = getRequest(
				endpoint = "audio/all",
				context = this@MainActivity,
				loginRequired = true
			)
			if (responseCode != HttpURLConnection.HTTP_OK) {
				return audioJsonArray
			}
			try {
				audioJsonArray = JSONArray(responseBody)
			} catch (e: JSONException) {
				runOnUiThread {
					displayError(
						context = this@MainActivity,
						message = getString(R.string.failed_to_load_audio_error, responseCode)
					)
				}
			}
			return audioJsonArray
		}


		setContent {
			MaterialTheme {
				Surface {
					MainScreen(
						onNavigateToLogin = {
							getSharedPreferences(SHARED_PREFS, MODE_PRIVATE).edit().clear().apply()
							val intent = Intent(this, LoginActivity::class.java)
							intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
							startActivity(intent)
							finish()
						},
						onNavigateToAudioRecorder = { latitude, longitude ->
							startActivity(Intent(this, AudioRecorderActivity::class.java).apply {
								Log.d("MainActivity", "Latitude: $latitude, Longitude: $longitude")
								putExtra(IS_LOGGED, true)
								putExtra("latitude", latitude)
								putExtra("longitude", longitude)
							})
						},
						onNavigateToDeleteAccount = {
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
										val intent =
											Intent(this@MainActivity, SignupActivity::class.java)
										intent.flags =
											Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
										startActivity(intent)
									}
								}
							}
						},
						onNavigateToAudioInfo = { audioId ->
							startActivity(Intent(this, AudioInfoActivity::class.java).apply {
								putExtra("audioId", audioId)
							})
						},
						onNavigateToMyAudio = {
							startActivity(Intent(this, MyAudioActivity::class.java).apply {
								putExtra(IS_LOGGED, true)
							})
						},
						fetchAudios = { loadAudio() }
					)
				}
			}
		}
	}
}
