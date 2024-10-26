package com.example.ycilt.otherssongs

import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.widget.Toast
import com.example.ycilt.R
import com.example.ycilt.utils.Misc
import com.example.ycilt.utils.LocationDisplayer
import com.example.ycilt.utils.NetworkUtils.getRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class SongInfoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song_info)

        val songId = intent.getIntExtra("songId", -1)
        CoroutineScope(Dispatchers.IO).launch {
            val songDetails = getSongDataById(songId)
            runOnUiThread{
                updateUIWithSongDetails(songDetails!!)
            }
        }

        val backButton = findViewById<ImageButton>(R.id.btn_close)
        backButton.setOnClickListener {
            finish()
        }

    }


    fun updateUIWithSongDetails(songDetails: SongDetails) {
        // Set title, bpm, danceability, and loudness
        findViewById<TextView>(R.id.song_creator).text = songDetails.creator
        findViewById<TextView>(R.id.song_bpm).text = "BPM: ${songDetails.bpm}"
        findViewById<TextView>(R.id.song_danceability).text = "Danceability: ${String.format("%.1f", songDetails.danceability)}%"
        findViewById<TextView>(R.id.song_loudness).text = "Loudness: ${String.format("%.1f", songDetails.loudness)}"


        // Update Mood section
        val moodContainer = findViewById<LinearLayout>(R.id.song_mood_container)
        moodContainer.removeAllViews()  // Clear previous data
        songDetails.mood.getTopN(5).forEach { (k, v) ->
            val moodTextView = TextView(this)
            moodTextView.text = "$k: ${String.format("%.1f", v)}%"
            moodContainer.addView(moodTextView)
        }

        val genreContainer = findViewById<LinearLayout>(R.id.song_genre_container)
        genreContainer.removeAllViews()
        songDetails.genre.getTopN(5).forEach { (k, v) ->
            val genreTextView = TextView(this)
            genreTextView.text = "$k: ${String.format("%.1f", v)}%"
            genreContainer.addView(genreTextView)
        }

        // Update Instruments section
        val instrumentsContainer = findViewById<LinearLayout>(R.id.song_instruments_container)
        instrumentsContainer.removeAllViews()
        songDetails.instrument.getTopN(5).forEach { (k, v) ->
            val instrumentTextView = TextView(this)
            instrumentTextView.text = "$k: ${String.format("%.1f", v)}%"
            instrumentsContainer.addView(instrumentTextView)
        }
        val locationInfoView = findViewById<LinearLayout>(R.id.locationInfoView)

        Misc.coordToAddr(this, songDetails.latitude, songDetails.longitude) { address ->
            val ld = LocationDisplayer(locationInfoView, songDetails.latitude, songDetails.longitude, address)
        }

    }

    private fun getSongDataById(id: Int): SongDetails? {
        val (responseCode, responseBody) = getRequest(
            "audio/$id",
            authRequest = this
        )
        Log.d("SongInfoActivity", "Song data for ID $id:")
        Log.d("SongInfoActivity", "Response code: $responseCode")
        Log.d("SongInfoActivity", "Response body: $responseBody")

        if (responseCode != 200) {
            runOnUiThread {
                Toast.makeText(this, "Song not found", Toast.LENGTH_SHORT).show()
                finish() // Termina l'activity corrente
            }
            return null
        }

        return SongDetails(JSONObject(responseBody))
    }

}
