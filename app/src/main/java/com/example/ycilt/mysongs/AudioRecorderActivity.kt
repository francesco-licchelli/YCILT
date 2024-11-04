package com.example.ycilt.mysongs

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints.Builder
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import com.arthenica.mobileffmpeg.FFmpeg
import com.example.ycilt.R
import com.example.ycilt.utils.AudioPlayerManager
import com.example.ycilt.utils.Constants
import com.example.ycilt.utils.Constants.NOT_UPLOADED
import com.example.ycilt.utils.Misc.displayToast
import com.example.ycilt.workers.UploadSongWorker
import com.example.ycilt.workers.WorkerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AudioRecorderActivity : AppCompatActivity() {

	private var mediaRecorder: MediaRecorder? = null
	private var songFileName: String = ""
	private var latestTimestamp: String? = null

	private lateinit var btnRecord: Button
	private lateinit var btnStopRecord: Button
	private lateinit var btnSave: Button

	private lateinit var audioPlayerManager: AudioPlayerManager
	private val uploadReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context?, intent: Intent?) {
			Log.d("AudioRecorderActivity", "Received broadcast: ${intent?.action}")
			val uploadedSongPath = intent?.getStringExtra("songPath")
			if (songFileName != uploadedSongPath) return
			displayToast(this@AudioRecorderActivity, getString(R.string.song_uploaded))
			finish()
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_audio_recorder)

		if (ContextCompat.checkSelfPermission(
				this,
				Manifest.permission.RECORD_AUDIO
			) != PackageManager.PERMISSION_GRANTED
		) {
			ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 0)
		}

		val btnBack: ImageButton = findViewById(R.id.btnBack)
		btnRecord = findViewById(R.id.btnRecord)
		btnStopRecord = findViewById(R.id.btnStopRecord)
		btnSave = findViewById(R.id.btnSave)

		registerReceiver(
			uploadReceiver, IntentFilter(Constants.BROADCAST_UPLOAD),
			RECEIVER_NOT_EXPORTED
		)


		btnBack.setOnClickListener {
			if (mediaRecorder != null) {
				mediaRecorder?.release()
				mediaRecorder = null
				if (File(songFileName).exists()) {
					File(songFileName).delete()
				}
			}
			finish()
		}

		btnRecord.setOnClickListener {
			startRecording()
			updateButtonStates(isRecording = true, hasRecorded = false)
		}

		btnStopRecord.setOnClickListener {
			stopRecording()
			updateButtonStates(isRecording = false, hasRecorded = true)
		}

		// Utilizza AudioPlayerManager per gestire la riproduzione audio
		audioPlayerManager = AudioPlayerManager(
			songFileName,
			findViewById(R.id.btnPlay),
			findViewById(R.id.audio_seekbar),
			findViewById(R.id.tv_current_time),
			findViewById(R.id.tv_total_time),
		)
		audioPlayerManager.startListener()

		btnSave.setOnClickListener {
			CoroutineScope(Dispatchers.Main).launch {
				saveRecording()
			}
		}

		// Disattiva i pulsanti inizialmente non utilizzabili
		btnStopRecord.isEnabled = false
		btnSave.isEnabled = false
		audioPlayerManager.updateStates(false)
	}

	private fun updateButtonStates(isRecording: Boolean, hasRecorded: Boolean) {
		btnRecord.isEnabled = !isRecording
		btnStopRecord.isEnabled = isRecording
		btnSave.isEnabled = !isRecording && hasRecorded
		audioPlayerManager.updateStates(!isRecording && hasRecorded)
	}

	private fun startRecording() {
		latestTimestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
		songFileName = "${filesDir.absolutePath}/audio_record_$latestTimestamp.aac"

		mediaRecorder = MediaRecorder(this).apply {
			setAudioSource(MediaRecorder.AudioSource.MIC)
			setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
			setOutputFile(songFileName)
			setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

			try {
				prepare()
				start()
			} catch (e: IOException) {
				e.printStackTrace()
			}
		}
	}

	private fun stopRecording() {
		mediaRecorder?.apply {
			stop()
			release()
		}
		mediaRecorder = null
		audioPlayerManager.updateAudioFilePath(songFileName)
		audioPlayerManager.canPlay = true
		audioPlayerManager.updateTotalTime()
	}

	private suspend fun saveRecording() {
		val mp3FilePath = songFileName.replace(".aac", ".mp3")
		var trimStart = 0
		val trimIncrement = 5
		do {
			val command = arrayOf("-ss", trimStart.toString(), "-i", songFileName, mp3FilePath)
			val result = executeFFmpegCommandAsync(command)
			if (result != com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS) {
				displayToast(this, getString(R.string.failed_file_conversion))
				return
			}
			trimStart += trimIncrement
		} while (File(mp3FilePath).length() > Constants.MAX_FILE_SIZE)
		saveMetaData(mp3FilePath)
		scheduleUpload(mp3FilePath)
		File(songFileName).delete()
	}

	private suspend fun executeFFmpegCommandAsync(command: Array<String>): Int {
		return suspendCoroutine { continuation ->
			FFmpeg.executeAsync(command) { _, returnCode ->
				continuation.resume(returnCode)
			}
		}
	}

	private fun saveMetaData(songPath: String) {
		val metadataFilePath = songPath.replace(".mp3", "_metadata.json")
		val metadata = JSONObject().apply {
			put("file_name", songPath)
			put("latitude", intent.getDoubleExtra("latitude", 0.0))
			put("longitude", intent.getDoubleExtra("longitude", 0.0))
			put("id", NOT_UPLOADED)
			put("mood", (arrayOf<String>()))
			put("genre", (arrayOf<String>()))
			put("instruments", (arrayOf<String>()))
			put("bpm", -1)
			put("danceability", -1)
			put("loudness", -1)
			put("hidden", true)
		}
		File(metadataFilePath).writeText(metadata.toString())
	}

	private fun scheduleUpload(songPath: String) {
		//TODO capire perche' ora quando carico la registrazione mi da errore
		//
		/*
			//TEST ONLY
			getSharedPreferences(Constants.SHARED_PREFS, Context.MODE_PRIVATE)
				.edit()
				.putString(Constants.TOKEN, "INVALID")
				.apply()
		*/

		val constraints = Builder()
			.setRequiredNetworkType(NetworkType.UNMETERED) // Solo Wi-Fi
			.build()

		val data = Data.Builder()
			.putString("songPath", songPath)
			.putString("latitude", intent.getDoubleExtra("latitude", 0.0).toString())
			.putString("longitude", intent.getDoubleExtra("longitude", 0.0).toString())
			.build()

		WorkerManager.enqueueWorker(
			this,
			OneTimeWorkRequestBuilder<UploadSongWorker>(),
			data,
			constraints,
			listOf("upload_song_${songFileName.hashCode()}"),
			onSucceeded = {
				displayToast(this, getString(R.string.song_uploaded))
				finish()
			}
		)
	}

	override fun onDestroy() {
		super.onDestroy()
		audioPlayerManager.release()
		mediaRecorder?.release()
		mediaRecorder = null
		unregisterReceiver(uploadReceiver)
	}
}
