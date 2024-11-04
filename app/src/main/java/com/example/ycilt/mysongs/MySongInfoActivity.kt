package com.example.ycilt.mysongs

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.ycilt.R
import com.example.ycilt.otherssongs.SongInfoActivity
import com.example.ycilt.utils.AudioPlayerManager
import com.example.ycilt.utils.Constants.NOT_UPLOADED
import com.example.ycilt.utils.Constants.PRIVATE_SONG
import com.example.ycilt.utils.Constants.PUBLIC_SONG
import com.example.ycilt.utils.LocationDisplayer
import com.example.ycilt.utils.Misc
import com.example.ycilt.utils.Misc.displayToast
import com.example.ycilt.utils.toBoolean
import com.example.ycilt.utils.toInt
import com.example.ycilt.workers.DeleteSongWorker
import com.example.ycilt.workers.EditPrivacyWorker
import com.example.ycilt.workers.WorkerManager
import org.json.JSONObject
import java.io.File

class MySongInfoActivity : AppCompatActivity() {

	private lateinit var songName: String
	private lateinit var songFile: File
	private lateinit var metadataFile: File
	private lateinit var songMetadata: JSONObject
	private var hiddenStatus: Int = NOT_UPLOADED
	private var songId: Int = NOT_UPLOADED
	private var mediaPlayer: MediaPlayer? = null
	private lateinit var audioPlayerManager: AudioPlayerManager

	private lateinit var privacyButton: Button
	private lateinit var deleteButton: Button
	private lateinit var showInfoButton: Button


	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_my_song_info)

		songName = intent.getStringExtra("songName")!!
		songMetadata = JSONObject(intent.getStringExtra("songMetadata")!!)

		songFile = File(filesDir, songName)
		metadataFile = File(filesDir, songName.replace(".mp3", "_metadata.json"))

		findViewById<TextView>(R.id.song_info_name).text = songName
		deleteButton = findViewById(R.id.delete_button)
		privacyButton = findViewById(R.id.privacy_button)
		showInfoButton = findViewById(R.id.btn_view_info)


		val locationInfoView = findViewById<LinearLayout>(R.id.locationInfoView)
		val latitude = songMetadata.getDouble("latitude")
		val longitude = songMetadata.getDouble("longitude")


		Misc.coordToAddr(this, latitude, longitude) { address ->
			LocationDisplayer(locationInfoView, latitude, longitude, address)
		}

		songId = songMetadata.getInt("id")

		hiddenStatus = if (songMetadata.has("hidden")) songMetadata.getBoolean("hidden")
			.toInt() else NOT_UPLOADED

		deleteButton.setOnClickListener { deleteSong() }

		privacyButton.setOnClickListener { changePrivacy() }

		showInfoButton.setOnClickListener {
			val intent = Intent(this, SongInfoActivity::class.java)
			intent.putExtra("songId", songId)
			startActivity(intent)
		}

		audioPlayerManager = AudioPlayerManager(
			songFile.absolutePath,
			findViewById(R.id.btnPlay),
			findViewById(R.id.audio_seekbar),
			findViewById(R.id.tv_current_time),
			findViewById(R.id.tv_total_time)
		)


		audioPlayerManager.updateAudioFilePath(songFile.absolutePath)
		audioPlayerManager.startListener()

	}

	override fun onResume() {
		super.onResume()
		audioPlayerManager.updateAudioFilePath(songFile.absolutePath)
		audioPlayerManager.startListener()
		for (button in listOf(deleteButton, privacyButton, showInfoButton)) {
			button.isEnabled = intent.getBooleanExtra("is_logged_in", false)
		}
		Log.d("MySongInfoActivity", "Hidden status: $hiddenStatus")
		Log.d(
			"MySongInfoActivity",
			"is_logged_in: ${intent.getBooleanExtra("is_logged_in", false)}"
		)
		if (intent.getBooleanExtra("is_logged_in", false)) {
			updatePrivacyButton()
			showInfoButton.isEnabled = songId != NOT_UPLOADED
		}
		if (hiddenStatus != PUBLIC_SONG) {
			showInfoButton.isEnabled = false
		}
		if (songId == NOT_UPLOADED) {
			privacyButton.isEnabled = false
			showInfoButton.isEnabled = false
		}
	}

	private fun updatePrivacyButton() {
		privacyButton.text =
			when (hiddenStatus) {
				PRIVATE_SONG -> getString(R.string.make_public)
				PUBLIC_SONG -> getString(R.string.make_private)
				else -> getString(R.string.change_song_s_privacy)
			}
		privacyButton.isEnabled = hiddenStatus != NOT_UPLOADED
		showInfoButton.isEnabled = hiddenStatus == PUBLIC_SONG
	}


	private fun scheduleDelete(songId: Int) {
		val inputData = Data.Builder()
			.putInt("songId", songId)
			.build()

		val constraints = Constraints.Builder()
			.setRequiredNetworkType(NetworkType.CONNECTED)
			.build()

		/*
				val deleteRequest = OneTimeWorkRequestBuilder<DeleteSongWorker>()
					.setInputData(inputData)
					.setConstraints(constraints)
					.build()

				WorkManager.getInstance(this).enqueue(deleteRequest)
		*/

		WorkerManager.enqueueWorker(
			owner = this,
			workerBuilderType = OneTimeWorkRequestBuilder<DeleteSongWorker>(),
			inputData = inputData,
			constraints = constraints,
			onSucceeded = {
				displayToast(this, getString(R.string.song_deleted_successfully))
			}
		)

		/*
				WorkManager.getInstance(this)
					.getWorkInfoByIdLiveData(deleteRequest.id)
					.observe(this) { workInfo ->
						if (workInfo != null && workInfo.state == WorkInfo.State.SUCCEEDED) {
							Log.d("MySongInfoActivity", "Song deleted from server")
						}
					}
		*/
	}

	private fun deleteSong() {
		if (songFile.exists()) songFile.delete()
		if (metadataFile.exists()) metadataFile.delete()
		if (songId != NOT_UPLOADED) {
			WorkManager.getInstance(this)
				.cancelAllWorkByTag("upload_song_${songFile.name.hashCode()}")
			scheduleDelete(songId)
		}
		val intent = Intent().apply {
			putExtra("deletedSongName", songName)
		}
		setResult(RESULT_OK, intent)
		finish()
	}

	private fun changePrivacy() {
		if (hiddenStatus == NOT_UPLOADED) {
			displayToast(this, getString(R.string.song_not_uploaded))
			return
		}
		/*
		 * se hiddenstatus e'
		 * 0 -> il brano e' pubblico, quindi lo rendo nascosto, chiamo hide
		 * 1 -> il brano e' nascosto, quindi lo rendo pubblico, chiamo show
		 * */
		val newPrivacy = if (hiddenStatus.toBoolean()) "show" else "hide"
		val inputData = Data.Builder()
			.putInt("songId", songId)
			.putString("newPrivacy", newPrivacy)
			.build()
		val constraints = Constraints.Builder()
			.setRequiredNetworkType(NetworkType.CONNECTED)
			.build()

		WorkerManager.enqueueWorker(
			owner = this,
			workerBuilderType = OneTimeWorkRequestBuilder<EditPrivacyWorker>(),
			inputData = inputData,
			constraints = constraints,
			onSucceeded = {
				hiddenStatus = hiddenStatus.toBoolean().not().toInt()
				songMetadata.put("hidden", hiddenStatus.toBoolean())
				metadataFile.writeText(songMetadata.toString())
				updatePrivacyButton()
			}
		)
	}

	override fun onDestroy() {
		super.onDestroy()
		audioPlayerManager.release()
		mediaPlayer?.release()
	}
}
