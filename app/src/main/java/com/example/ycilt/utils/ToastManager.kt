package com.example.ycilt.utils

import android.content.Context
import android.widget.Toast

object ToastManager {
	val activeToasts = mutableListOf<Toast>()
	private var areToastsActive: Boolean = true

	fun displayToast(context: Context, message: String) {
		if (!areToastsActive) return

		activeToasts.forEach { it.cancel() }
		activeToasts.clear()

		val toast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
		toast.show()

		activeToasts.add(toast)
		toast.addCallback(object : Toast.Callback() {
			override fun onToastHidden() {
				activeToasts.remove(toast)
			}
		})
	}

	fun disableToasts() {
		activeToasts.forEach { it.cancel() }
		activeToasts.clear()
		areToastsActive = false
	}

	fun enableToasts() {
		areToastsActive = true
	}

}