package com.example.ycilt.auth

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ycilt.utils.Misc
import com.example.ycilt.utils.NetworkUtils
import com.example.ycilt.utils.NetworkUtils.postRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection

abstract class Auther: AppCompatActivity() {
   var isLogin: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isLogin = this::class.simpleName == "LoginActivity"
    }

    fun auth(username: String, password: String): Pair<Int, String> {
        return postRequest(
            if (!isLogin) "auth" else "auth/token",
            this,
            mapOf("username" to username, "password" to password),
            encode={
                if (!isLogin) NetworkUtils.jsonEncoding(it)
                else NetworkUtils.wwwEncoding(it)
            }
        )
    }

    suspend fun perform(username: String, password: String, success: ((String) -> Unit)): Pair<Int, String>{
        return withContext(Dispatchers.IO) {
            val response = auth(username, password)
            val (responseStatus, responseBody) = response
            if (responseStatus != HttpURLConnection.HTTP_OK) {
                runOnUiThread{ Misc.displayError(this@Auther, responseBody) }
            } else {
                success(responseBody)
            }
            return@withContext response
        }
    }

}
