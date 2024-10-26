package com.example.ycilt.auth

object LoginManager {
    private var loginListener: (() -> Unit)? = null

    fun registerOnLoginListener(listener: () -> Unit) {
        loginListener = listener
    }

    fun onLoginSuccess() {
        loginListener?.invoke()
    }

}
