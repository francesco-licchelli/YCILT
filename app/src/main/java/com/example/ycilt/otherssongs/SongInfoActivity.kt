package com.example.ycilt.otherssongs

import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.ycilt.R
import com.example.ycilt.utils.Constants.NOT_UPLOADED
import com.example.ycilt.utils.LocationDisplayer
import com.example.ycilt.utils.Misc
import com.example.ycilt.utils.Misc.displayToast
import com.example.ycilt.utils.NetworkUtils.getRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class SongInfoActivity : AppCompatActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_song_info)

		val songId = intent.getIntExtra("songId", NOT_UPLOADED)
		CoroutineScope(Dispatchers.IO).launch {
			val songDetails = getSongDataById(songId)
			runOnUiThread {
				updateUIWithSongDetails(songDetails!!)
			}
		}

		val backButton = findViewById<ImageButton>(R.id.btn_close)
		backButton.setOnClickListener {
			finish()
		}

	}

	private fun updateUIWithSongDetails(songDetails: SongDetails) =
		try {
			findViewById<TextView>(R.id.song_creator).text = songDetails.creator
			findViewById<TextView>(R.id.song_bpm).text = getString(R.string.bpm, songDetails.bpm)
			findViewById<TextView>(R.id.song_danceability).text =
				getString(R.string.danceability, songDetails.danceability)
			findViewById<TextView>(R.id.song_loudness).text =
				getString(R.string.loudness, songDetails.loudness)


			// Update Mood section
			val moodContainer = findViewById<LinearLayout>(R.id.song_mood_container)
			moodContainer.removeAllViews()  // Clear previous data
			songDetails.mood.getTopN(5).forEach { (k, v) ->
				val moodTextView = TextView(this)
				moodTextView.text = getString(R.string.cat_displayer, k, v)
				moodContainer.addView(moodTextView)
			}

			val genreContainer = findViewById<LinearLayout>(R.id.song_genre_container)
			genreContainer.removeAllViews()
			songDetails.genre.getTopN(5).forEach { (k, v) ->
				val genreTextView = TextView(this)
				genreTextView.text = getString(R.string.cat_displayer, k, v)
				genreContainer.addView(genreTextView)
			}

			val instrumentsContainer = findViewById<LinearLayout>(R.id.song_instruments_container)
			instrumentsContainer.removeAllViews()
			songDetails.instrument.getTopN(5).forEach { (k, v) ->
				val instrumentTextView = TextView(this)
				instrumentTextView.text = getString(R.string.cat_displayer, k, v)
				instrumentsContainer.addView(instrumentTextView)
			}
			val locationInfoView = findViewById<LinearLayout>(R.id.locationInfoView)

			Misc.coordToAddr(this, songDetails.latitude, songDetails.longitude) { address ->
				LocationDisplayer(
					locationInfoView,
					songDetails.latitude,
					songDetails.longitude,
					address
				)
			}
		} catch (e: Exception) {
			e.printStackTrace()
			displayToast(this, getString(R.string.error_loading_song_data))
			finish()
		}

	private fun getSongDataById(id: Int): SongDetails? {
		val (responseCode, responseBody) = getRequest(
			endpoint = "audio/$id",
			context = this,
			loginRequired = true,
		)

		if (responseCode != 200) {
			runOnUiThread {
				displayToast(this, getString(R.string.song_not_found))
				finish()
			}
			return null
		}

		return SongDetails(JSONObject(responseBody))
	}

}
