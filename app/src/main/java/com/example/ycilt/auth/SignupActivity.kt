package com.example.ycilt.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.ycilt.MainActivity
import com.example.ycilt.R
import com.example.ycilt.utils.Keys.SHARED_PREFS
import com.example.ycilt.utils.Keys.TOKEN
import com.example.ycilt.utils.ToastManager.displayToast
import com.example.ycilt.workers.WorkerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class SignupActivity : Auther() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContent {
			MaterialTheme {
				Surface {
					SignupScreen(
						onSignupClick = { username, password -> performSignup(username, password) },
						onLoginClick = { navigateToLogin() }
					)
				}
			}
		}
	}

	private fun performSignup(username: String, password: String) {
		CoroutineScope(Dispatchers.IO).launch {
			val callback: (String) -> Unit = { _ ->
				runOnUiThread {
					displayToast(this@SignupActivity, getString(R.string.registration_ok))
				}
				val intent = Intent(this@SignupActivity, LoginActivity::class.java)
				startActivity(intent)
			}
			super.perform(username, password, callback)
		}
	}

	private fun navigateToLogin() {
		startActivity(Intent(this, LoginActivity::class.java))
		finish()
	}

}
