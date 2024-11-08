package com.example.ycilt.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.ycilt.MainActivity
import com.example.ycilt.my_audio.MyAudioActivity
import com.example.ycilt.utils.Keys.IS_LOGGED
import com.example.ycilt.utils.Keys.SHARED_PREFS
import com.example.ycilt.utils.Keys.TOKEN
import com.example.ycilt.utils.ToastManager.disableToasts
import com.example.ycilt.utils.ToastManager.enableToasts
import com.example.ycilt.workers.WorkerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class LoginActivity : Auther() {
	companion object {
		private var firstLogin: Boolean = true
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContent {
			MaterialTheme {
				Surface {
					LoginScreen(
						onLoginClick = { username, password -> performLogin(username, password) },
						onSignupClick = { navigateToSignup() },
						onDisplayAudioClick = { navigateToMyAudio() }
					)
				}
			}
		}

	}

	override fun onStart() {
		super.onStart()
		if (!firstLogin) {
			disableToasts()
		}
	}

	private fun performLogin(username: String, password: String) {
		firstLogin = false
		enableToasts()
		CoroutineScope(Dispatchers.IO).launch {
			val callback: (String) -> Unit = { responseBody ->
				val responseJson = JSONObject(responseBody)
				val clientSecret = responseJson.getString(TOKEN)
				val sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE)
				sharedPreferences.edit().putString(TOKEN, clientSecret).apply()
				WorkerManager.resumeAll()
				startActivity(Intent(this@LoginActivity, MainActivity::class.java))
				finish()
			}
			super.perform(username, password, callback)
		}
	}

	private fun navigateToSignup() {
		startActivity(Intent(this, SignupActivity::class.java))
		finish()
	}

	private fun navigateToMyAudio() {
		val intent = Intent(this, MyAudioActivity::class.java)
		intent.putExtra(IS_LOGGED, false)
		startActivity(intent)
	}
}
