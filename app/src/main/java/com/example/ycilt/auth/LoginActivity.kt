package com.example.ycilt.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ycilt.MainActivity
import com.example.ycilt.R
import com.example.ycilt.utils.Constants
import com.example.ycilt.utils.NetworkUtils
import com.example.ycilt.utils.NetworkUtils.postRequest
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.util.Date

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val usernameEditText = findViewById<EditText>(R.id.username)
        val passwordEditText = findViewById<EditText>(R.id.password)

        val loginButton = findViewById<Button>(R.id.btn_login)
        val switchToRegisterButton = findViewById<Button>(R.id.btn_login_to_reg)

        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (username.isEmpty())
                Toast.makeText(this, "Insert username", Toast.LENGTH_SHORT).show()
            else if (password.isEmpty())
                Toast.makeText(this, "Insert password", Toast.LENGTH_SHORT).show()
            else
                performLogin(username, password)
        }

        switchToRegisterButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun performLogin(username: String, password: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val (responseStatus, responseBody) = loginRequest(username, password)

            withContext(Dispatchers.Main) {
                if (responseStatus != HttpURLConnection.HTTP_OK) {
                    Toast.makeText(this@LoginActivity, responseBody.getString("detail"), Toast.LENGTH_SHORT).show()
                    return@withContext
                }
                else {
                    val clientSecret = responseBody.getString("client_secret")
                    val sharedPreferences = getSharedPreferences(Constants.SHARED_PREFS, MODE_PRIVATE)
                    sharedPreferences.edit().putString("client_secret", clientSecret).apply()
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    private fun loginRequest(username: String, password: String): Pair<Int, JSONObject> {
        val (responseCode, responseBody) = postRequest(
            "auth/token",
            this,
            postData=mapOf("username" to username, "password" to password),
            encode={NetworkUtils.wwwEncoding(it)}
        )
        Log.d("LoginActivity", "Login request response code: $responseCode")
        Log.d("LoginActivity", "Login request response body: $responseBody")
        return Pair(responseCode, JSONObject(responseBody))
    }

}
