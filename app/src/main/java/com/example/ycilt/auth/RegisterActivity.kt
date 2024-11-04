package com.example.ycilt.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import com.example.ycilt.R
import com.example.ycilt.utils.Misc.displayToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RegisterActivity : Auther() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_register)

		val usernameEditText = findViewById<EditText>(R.id.username)
		val passwordEditText = findViewById<EditText>(R.id.password)

		val regButton = findViewById<Button>(R.id.btn_register)
		val switchToLoginButton = findViewById<Button>(R.id.btn_reg_to_login)

		regButton.setOnClickListener {
			val username = usernameEditText.text.toString()
			val password = passwordEditText.text.toString()

			if (username.isEmpty())
				displayToast(this, getString(R.string.missing_username))
			else if (password.isEmpty())
				displayToast(this, getString(R.string.missing_password))
			else
				performRegistration(username, password)
		}
		switchToLoginButton.setOnClickListener {
			val intent = Intent(this, LoginActivity::class.java)
			startActivity(intent)
			finish()
		}
	}

	private fun performRegistration(username: String, password: String) {
		CoroutineScope(Dispatchers.IO).launch {
			val callback: (String) -> Unit = { _ ->
				runOnUiThread {
					displayToast(this@RegisterActivity, getString(R.string.registration_ok))
				}
				val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
				startActivity(intent)
			}
			super.perform(username, password, callback)
		}
	}

}