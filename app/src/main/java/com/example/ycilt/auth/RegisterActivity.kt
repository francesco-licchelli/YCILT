package com.example.ycilt.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ycilt.R
import com.example.ycilt.utils.Constants
import com.example.ycilt.utils.NetworkUtils
import com.example.ycilt.utils.NetworkUtils.postRequest
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection

class RegisterActivity : AppCompatActivity() {

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
                Toast.makeText(this, "Insert username", Toast.LENGTH_SHORT).show()
            else if (password.isEmpty())
                Toast.makeText(this, "Insert password", Toast.LENGTH_SHORT).show()
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
            val (responseStatus, responseBody) = registerRequest(username, password)

            withContext(Dispatchers.Main) {
                if (responseStatus != HttpURLConnection.HTTP_OK) {
                    Toast.makeText(this@RegisterActivity, responseBody.getString("detail"), Toast.LENGTH_SHORT).show()
                    return@withContext
                }
                else {
                    Toast.makeText(this@RegisterActivity, "Registration successful! Now you can login", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                    startActivity(intent)
                }
            }
        }
    }

    private fun registerRequest(username: String, password: String): Pair<Int, JSONObject> {
        val (responseCode, responseBody) = postRequest(
            "auth",
            this,
            mapOf("username" to username, "password" to password),
            encode={NetworkUtils.jsonEncoding(it)}
        )
        return Pair(responseCode, JSONObject(responseBody))
    }

}