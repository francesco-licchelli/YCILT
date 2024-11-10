package com.example.ycilt.my_audio

import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.work.Constraints.Builder
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.arthenica.mobileffmpeg.FFmpeg
import com.example.ycilt.R
import com.example.ycilt.utils.AudioInfoSaver.updateMetadataFromBackend
import com.example.ycilt.utils.Constants.MAX_FILE_SIZE
import com.example.ycilt.utils.Constants.NOT_UPLOADED
import com.example.ycilt.utils.Misc.audioToMetadataFilename
import com.example.ycilt.utils.PermissionUtils
import com.example.ycilt.utils.ToastManager.displayToast
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
	private var aacFilename: MutableState<String> = mutableStateOf("")
	private var latestTimestamp: String? = null
	private var enqueuedAudio: Int = 0


	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContent {
			AudioRecorderScreen(
				startRecording = { afterRecordingStarted ->
					startRecording(afterRecordingStarted)
					Log.d("AudioRecorder", "File path 1: $aacFilename")
				},
				stopRecording = {
					stopRecording()
					Log.d("AudioRecorder", "File path 2: $aacFilename")
				},
				audioFilename = aacFilename,
				saveRecording = { saveRecording() },
			)
		}

		if (!PermissionUtils.hasRecordPermission(this)) {
			PermissionUtils.requestRecordPermission(this)
		}
	}

	private fun startRecording(afterRecordingStarted: () -> Unit = {}) {
		if (aacFilename.value.isNotEmpty())
			File(aacFilename.value).delete()

		latestTimestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
		aacFilename.value = "${filesDir.absolutePath}/audio_record_$latestTimestamp.aac"

		mediaRecorder = MediaRecorder(this).apply {
			setAudioSource(MediaRecorder.AudioSource.MIC)
			setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
			setOutputFile(aacFilename.value)
			setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
			try {
				prepare()
				start()
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
	}

	private suspend fun saveRecording() {
		val mp3Filename = aacFilename.value.replace(".aac", ".mp3")
		var trimStart = 0
		val trimIncrement = 5
		do {
			val command = arrayOf("-ss", trimStart.toString(), "-i", aacFilename.value, mp3Filename)
			val result = executeFFmpegCommandAsync(command)
			if (result != com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS) {
				displayToast(this, getString(R.string.failed_file_conversion))
			}
			trimStart += trimIncrement
		} while (File(mp3Filename).length() > MAX_FILE_SIZE)
		saveMetaData(mp3Filename)
		scheduleUpload(mp3Filename)
		File(aacFilename.value).delete()
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
