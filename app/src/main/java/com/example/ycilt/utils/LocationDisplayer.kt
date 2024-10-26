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
        if (address.isNullOrEmpty()) {
            // Mostra le coordinate se l'indirizzo non è disponibile
            coordinatesTextView.text = "Latitude: $latitude, Longitude: $longitude"
            coordinatesTextView.visibility = View.VISIBLE
            addressTextView.visibility = View.GONE
        } else {
            // Mostra l'indirizzo se è disponibile
            addressTextView.text = address
            addressTextView.visibility = View.VISIBLE
            coordinatesTextView.visibility = View.GONE
        }
    }
}
