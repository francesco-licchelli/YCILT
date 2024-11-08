package com.example.ycilt.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import com.example.ycilt.MainActivity
import com.example.ycilt.R
import com.example.ycilt.my_audio.MyAudioActivity
import com.example.ycilt.utils.Keys.IS_LOGGED
import com.example.ycilt.utils.Keys.SHARED_PREFS
import com.example.ycilt.utils.Keys.TOKEN
import com.example.ycilt.utils.Misc.displayToast
import com.example.ycilt.workers.WorkerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class LoginActivity : Auther() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_login)

		val usernameEditText = findViewById<EditText>(R.id.username)
		val passwordEditText = findViewById<EditText>(R.id.password)

		findViewById<Button>(R.id.btn_login).setOnClickListener {
			val username = usernameEditText.text.toString()
			val password = passwordEditText.text.toString()

			if (username.isEmpty())
				displayToast(this, getString(R.string.missing_username))
			else if (password.isEmpty())
				displayToast(this, getString(R.string.missing_password))
			else
				performLogin(username, password)
		}

		findViewById<Button>(R.id.btn_login_to_reg).setOnClickListener {
			val intent = Intent(this, RegisterActivity::class.java)
			startActivity(intent)
			finish()
		}

		findViewById<Button>(R.id.btn_display_audio).setOnClickListener {
			val intent = Intent(this, MyAudioActivity::class.java)
			intent.putExtra(IS_LOGGED, false)
			startActivity(intent)
		}
	}

	private fun performLogin(username: String, password: String) {
		CoroutineScope(Dispatchers.IO).launch {
			val callback: (String) -> Unit = { responseBody ->
				val responseJson = JSONObject(responseBody)
				val clientSecret = responseJson.getString(TOKEN)
				val sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE)
				sharedPreferences.edit().putString(TOKEN, clientSecret).apply()
				WorkerManager.resumeAll()
				val intent = Intent(this@LoginActivity, MainActivity::class.java)
				startActivity(intent)
				finish()

			}
			super.perform(username, password, callback)
		}
	}


}
