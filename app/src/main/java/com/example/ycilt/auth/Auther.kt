package com.example.ycilt.auth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.ycilt.utils.NetworkUtils
import com.example.ycilt.utils.NetworkUtils.postRequest
import com.example.ycilt.utils.displayError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection

abstract class Auther : AppCompatActivity() {
	private var isLogin: Boolean = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		isLogin = this::class.simpleName == "LoginActivity"
	}

	private fun auth(username: String, password: String): Pair<Int, String> {
		return postRequest(
			endpoint = if (!isLogin) "auth" else "auth/token",
			context = this,
			loginRequired = false,
			body = mapOf("username" to username, "password" to password),
			encode = {
				if (!isLogin) NetworkUtils.jsonEncoding(it)
				else NetworkUtils.wwwEncoding(it)
			}
		)
	}

	suspend fun perform(
		username: String,
		password: String,
		success: ((String) -> Unit)
	): Pair<Int, String> {
		return withContext(Dispatchers.IO) {
			val response = auth(username, password)
			val (responseStatus, responseBody) = response
			if (responseStatus != HttpURLConnection.HTTP_OK) {
				runOnUiThread { displayError(this@Auther, responseBody) }
			} else {
				success(responseBody)
			}
			return@withContext response
		}
	}

}
