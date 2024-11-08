package com.example.ycilt.others_audio

import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.ycilt.R
import com.example.ycilt.utils.AudioInfoSaver.parseAudioData
import com.example.ycilt.utils.Constants.NOT_UPLOADED
import com.example.ycilt.utils.LocationDisplayer
import com.example.ycilt.utils.Misc
import com.example.ycilt.utils.Misc.displayToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AudioInfoActivity : AppCompatActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_audio_info)

		val audioId = intent.getIntExtra("audioId", NOT_UPLOADED)
		val audioFilename = intent.getStringExtra("audioFilename")
		CoroutineScope(Dispatchers.IO).launch {
			val audioDetails = parseAudioData(
				this@AudioInfoActivity,
				audioId,
				audioFilename
			)
			runOnUiThread {
				updateUIWithAudioDetails(audioDetails!!)
			}
		}

		val backButton = findViewById<ImageButton>(R.id.btn_close)
		backButton.setOnClickListener {
			finish()
		}

	}

	private fun updateUIWithAudioDetails(audioDetails: AudioDetails) =
		try {
			findViewById<TextView>(R.id.audio_creator).text = audioDetails.creator
			findViewById<TextView>(R.id.audio_bpm).text = getString(R.string.bpm, audioDetails.bpm)
			findViewById<TextView>(R.id.audio_danceability).text =
				getString(R.string.danceability, audioDetails.danceability)
			findViewById<TextView>(R.id.audio_loudness).text =
				getString(R.string.loudness, audioDetails.loudness)


			// Update Mood section
			val moodContainer = findViewById<LinearLayout>(R.id.audio_mood_container)
			moodContainer.removeAllViews()  // Clear previous data
			audioDetails.mood.getTopN(5).forEach { (k, v) ->
				val moodTextView = TextView(this)
				moodTextView.text = getString(R.string.cat_displayer, k, v)
				moodContainer.addView(moodTextView)
			}

			val genreContainer = findViewById<LinearLayout>(R.id.audio_genre_container)
			genreContainer.removeAllViews()
			audioDetails.genre.getTopN(5).forEach { (k, v) ->
				val genreTextView = TextView(this)
				genreTextView.text = getString(R.string.cat_displayer, k, v)
				genreContainer.addView(genreTextView)
			}

			val instrumentsContainer = findViewById<LinearLayout>(R.id.audio_instruments_container)
			instrumentsContainer.removeAllViews()
			audioDetails.instrument.getTopN(5).forEach { (k, v) ->
				val instrumentTextView = TextView(this)
				instrumentTextView.text = getString(R.string.cat_displayer, k, v)
				instrumentsContainer.addView(instrumentTextView)
			}
			val locationInfoView = findViewById<LinearLayout>(R.id.locationInfoView)

			Misc.coordToAddr(this, audioDetails.latitude, audioDetails.longitude) { address ->
				LocationDisplayer(
					locationInfoView,
					audioDetails.latitude,
					audioDetails.longitude,
					address
				)
			}
		} catch (e: Exception) {
			e.printStackTrace()
			displayToast(this, getString(R.string.error_loading_audio_data))
			finish()
		}


}
