package com.example.ycilt.utils

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.example.ycilt.R

class LocationDisplayer(
	private val layout: LinearLayout,
	private val latitude: Double,
	private val longitude: Double,
	private val address: String?
) {

	private val coordinatesTextView: TextView = layout.findViewById(R.id.coordinatesTextView)
	private val addressTextView: TextView = layout.findViewById(R.id.addressTextView)

	init {
		setLocationDetails()
	}

	private fun setLocationDetails() {
		// Se l'indirizzo non e' disponibile, mostra le coordinate
		if (address.isNullOrEmpty()) {
			coordinatesTextView.text =
				this.layout.context.getString(R.string.latitude_longitude, latitude, longitude)
			coordinatesTextView.visibility = View.VISIBLE
			addressTextView.visibility = View.GONE
		} else {
			addressTextView.text = address
			addressTextView.visibility = View.VISIBLE
			coordinatesTextView.visibility = View.GONE
		}
	}
}
