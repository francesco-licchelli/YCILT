package com.example.ycilt.my_audio

import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.work.Constraints.Builder
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.arthenica.mobileffmpeg.FFmpeg
import com.example.ycilt.R
import com.example.ycilt.utils.AudioInfoSaver.updateMetadataFromBackend
import com.example.ycilt.utils.AudioPlayerManager
import com.example.ycilt.utils.Constants.MAX_FILE_SIZE
import com.example.ycilt.utils.Constants.NOT_UPLOADED
import com.example.ycilt.utils.Misc.audioToMetadataFilename
import com.example.ycilt.utils.Misc.displayToast
import com.example.ycilt.utils.PermissionUtils
import com.example.ycilt.utils.isConnectedToWifi
import com.example.ycilt.workers.NotificationWorker
import com.example.ycilt.workers.UploadAudioWorker
import com.example.ycilt.workers.WorkerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
	private var aacFilename: String = ""
	private var latestTimestamp: String? = null
	private var enqueuedAudio: Int = 0

	private lateinit var btnRecord: Button
	private lateinit var btnStopRecord: Button
	private lateinit var btnSave: Button

	private lateinit var audioPlayerManager: AudioPlayerManager

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_audio_recorder)

		if (!PermissionUtils.hasRecordPermission(this)) {
			PermissionUtils.requestRecordPermission(this)
		}

		val btnBack: ImageButton = findViewById(R.id.btnBack)
		btnRecord = findViewById(R.id.btnRecord)
		btnStopRecord = findViewById(R.id.btnStopRecord)
		btnSave = findViewById(R.id.btnSave)

		btnBack.setOnClickListener {
			if (mediaRecorder != null) {
				mediaRecorder?.release()
				mediaRecorder = null
				if (File(aacFilename).exists()) {
					File(aacFilename).delete()
				}
			}
			finish()
		}

		btnRecord.setOnClickListener {
			displayToast(this, getString(R.string.starting_recording))
			startRecording {
				btnRecord.isEnabled = false
				btnSave.isEnabled = false
				audioPlayerManager.updateStates(false)
				displayToast(this, getString(R.string.recording_started))
				Handler(Looper.getMainLooper()).postDelayed({
					btnStopRecord.isEnabled = true
				}, 2000)
			}
			/*
			* Il backend sembra rifiutare file audio dalla durata minore di 2 secondi,
			* quindi ho aggiunto il ritardo di 2 secondi prima di abilitare il pulsante di stop.
			* */
		}

		btnStopRecord.setOnClickListener {
			stopRecording()
			btnStopRecord.isEnabled = false
			btnRecord.isEnabled = true
			btnSave.isEnabled = true
			audioPlayerManager.updateStates(true)
		}

		// Utilizza AudioPlayerManager per gestire la riproduzione audio
		audioPlayerManager = AudioPlayerManager(
			aacFilename,
			findViewById(R.id.btnPlay),
			findViewById(R.id.audio_seekbar),
			findViewById(R.id.tv_current_time),
			findViewById(R.id.tv_total_time),
		)
		audioPlayerManager.startListener()

		btnSave.setOnClickListener {
			CoroutineScope(Dispatchers.Main).launch {
				btnSave.isEnabled = false
				saveRecording()
			}
		}

		// Disattivo i pulsanti inizialmente non utilizzabili
		btnStopRecord.isEnabled = false
		btnSave.isEnabled = false
		audioPlayerManager.updateStates(false)
	}

	private fun startRecording(afterRecordingStarted: () -> Unit = {}) {
		if (aacFilename.isNotEmpty())
			File(aacFilename).delete()

		latestTimestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
		aacFilename = "${filesDir.absolutePath}/audio_record_$latestTimestamp.aac"

		mediaRecorder = MediaRecorder(this).apply {
			setAudioSource(MediaRecorder.AudioSource.MIC)
			setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
			setOutputFile(aacFilename)
			setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
			try {
				prepare()
				start()
				//Provo ad attivare qui i pulsanti
				afterRecordingStarted()

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
		audioPlayerManager.updateAudioFilePath(aacFilename)
		audioPlayerManager.canPlay = true
		audioPlayerManager.updateTotalTime()
	}

	private suspend fun saveRecording() {
		val mp3Filename = aacFilename.replace(".aac", ".mp3")
		var trimStart = 0
		val trimIncrement = 5
		do {
			val command = arrayOf("-ss", trimStart.toString(), "-i", aacFilename, mp3Filename)
			val result = executeFFmpegCommandAsync(command)
			if (result != com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS) {
				displayToast(this, getString(R.string.failed_file_conversion))
				return
			}
			trimStart += trimIncrement
		} while (File(mp3Filename).length() > MAX_FILE_SIZE)
		saveMetaData(mp3Filename)
		scheduleUpload(mp3Filename)
		File(aacFilename).delete()
	}

	private suspend fun executeFFmpegCommandAsync(command: Array<String>): Int {
		return suspendCoroutine { continuation ->
			FFmpeg.executeAsync(command) { _, returnCode ->
				continuation.resume(returnCode)
			}
		}
	}

	private fun saveMetaData(audioFilename: String) {
		val metadataFilename = audioToMetadataFilename(audioFilename)
		val metadata = JSONObject().apply {
			put("file_name", audioFilename)
			put("latitude", intent.getDoubleExtra("latitude", 0.0))
			put("longitude", intent.getDoubleExtra("longitude", 0.0))
			put("id", NOT_UPLOADED)
			put("hidden", false)
		}
		File(metadataFilename).writeText(metadata.toString())
	}

	private fun scheduleUpload(audioPath: String) {
		val constraints = Builder()
			.setRequiredNetworkType(NetworkType.UNMETERED) // Solo Wi-Fi
			.build()

		val data = Data.Builder()
			.putString("audioPath", audioPath)
			.putString("latitude", intent.getDoubleExtra("latitude", 0.0).toString())
			.putString("longitude", intent.getDoubleExtra("longitude", 0.0).toString())
			.build()

		WorkerManager.enqueueWorker(
			this,
			OneTimeWorkRequestBuilder<UploadAudioWorker>(),
			data,
			constraints,
			listOf("audio_uploader", "upload_audio_${aacFilename.hashCode()}"),
			onSucceeded = {
				displayToast(this, getString(R.string.audio_uploaded))
				CoroutineScope(Dispatchers.Main).launch {
					//Quando finisco di caricare una canzone, salvo le informazioni del backend
					if (getPendingAudioUploaderWorkersCount() == 0) {
						updateMetadataFromBackend(this@AudioRecorderActivity, intent)
						Log.d("AudioRecorderActivity", "Metadata updated")
					}
				}
				finish()
			}
		)

		if (!isConnectedToWifi(this)) {

			if (!PermissionUtils.hasNotificationPermission(this))
				PermissionUtils.requestNotificationPermission(this)

			WorkerManager.enqueueWorker(
				this,
				OneTimeWorkRequestBuilder<NotificationWorker>(),
				Data.Builder().putInt("audioCount", ++enqueuedAudio).build(),
				Builder().setRequiredNetworkType(NetworkType.UNMETERED).build(),
				listOf("notification"),
				beforeWork = {
					WorkManager.getInstance(this).cancelAllWorkByTag("notification")
				},
				onSucceeded = {
					enqueuedAudio = 0
				}
			)
		}
	}

	override fun onDestroy() {
		super.onDestroy()
		audioPlayerManager.release()
		mediaRecorder?.release()
		mediaRecorder = null
	}

	private suspend fun getPendingAudioUploaderWorkersCount(): Int {
		return withContext(Dispatchers.IO) {
			val workManager = WorkManager.getInstance(this@AudioRecorderActivity)
			val workInfos = workManager.getWorkInfosByTag("audio_uploader").get()

			workInfos.count { it.state == WorkInfo.State.ENQUEUED }
		}
	}

}
