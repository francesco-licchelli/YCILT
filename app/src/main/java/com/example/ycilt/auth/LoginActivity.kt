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
import com.example.ycilt.mysongs.MySongsActivity
import com.example.ycilt.utils.Constants
import com.example.ycilt.utils.NetworkUtils
import com.example.ycilt.utils.NetworkUtils.postRequest
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.util.Date

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
                Toast.makeText(this, "Insert username", Toast.LENGTH_SHORT).show()
            else if (password.isEmpty())
                Toast.makeText(this, "Insert password", Toast.LENGTH_SHORT).show()
            else
                performLogin(username, password)
        }

        findViewById<Button>(R.id.btn_login_to_reg).setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            finish()
        }

        findViewById<Button>(R.id.btn_display_songs).setOnClickListener{
            val intent = Intent(this, MySongsActivity::class.java)
            intent.putExtra("is_logged_in", false)
            startActivity(intent)
        }
    }

    private fun performLogin(username: String, password: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val callback: (String) -> Unit = { responseBody ->
                val responseJson = JSONObject(responseBody)
                val clientSecret = responseJson.getString("client_secret")
                val sharedPreferences = getSharedPreferences(Constants.SHARED_PREFS, MODE_PRIVATE)
                sharedPreferences.edit().putString("client_secret", clientSecret).apply()
                LoginManager.onLoginSuccess()
                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                startActivity(intent)
                finish()

            }
            super.perform(username, password, callback)
        }
    }


}
