package com.example.ycilt.my_audio

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.work.Constraints.Builder
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.ycilt.R
import com.example.ycilt.others_audio.AudioInfoActivity
import com.example.ycilt.utils.Constants.NOT_UPLOADED
import com.example.ycilt.utils.Keys.AUDIO_QUEUE
import com.example.ycilt.utils.Keys.AUDIO_QUEUE_LENGTH
import com.example.ycilt.utils.Keys.IS_LOGGED
import com.example.ycilt.utils.Privacy.UNKNOWN_PRIVACY
import com.example.ycilt.utils.ToastManager.displayToast
import com.example.ycilt.utils.audioToMetadataFilename
import com.example.ycilt.utils.toBoolean
import com.example.ycilt.utils.toInt
import com.example.ycilt.workers.DeleteAudioWorker
import com.example.ycilt.workers.EditPrivacyWorker
import com.example.ycilt.workers.NotificationWorker
import com.example.ycilt.workers.WorkerManager
import org.json.JSONObject
import java.io.File
import kotlin.math.max

class MyAudioInfoActivity : AppCompatActivity() {
	private lateinit var audioName: String
	private lateinit var audioFile: File
	private lateinit var metadataFile: File
	private lateinit var audioMetadata: JSONObject

	private var hiddenStatus: MutableIntState = mutableIntStateOf(NOT_UPLOADED)
	private var audioId: Int = NOT_UPLOADED

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		audioName = intent.getStringExtra("audioName")!!
		audioMetadata = JSONObject(intent.getStringExtra("audioMetadata")!!)

		audioFile = File(filesDir, audioName)
		metadataFile = File(filesDir, audioToMetadataFilename(audioName))

		audioId = audioMetadata.getInt("id")

		hiddenStatus.intValue = if (audioMetadata.has("hidden")) audioMetadata.getBoolean("hidden")
			.toInt() else UNKNOWN_PRIVACY

		setContent {
			MyAudioInfoScreen(
				hiddenStatus = hiddenStatus,
				audioFilename = intent.getStringExtra("audioName") ?: "",
				audioMetadata = JSONObject(intent.getStringExtra("audioMetadata") ?: ""),
				onDeleteAudio = { deleteAudio() },
				onChangePrivacy = { changePrivacy() },
				onShowInfo = { showAudioInfo() },
				canDeleteAudio = intent.getBooleanExtra(IS_LOGGED, false),
				canChangePrivacy = intent.getBooleanExtra(
					IS_LOGGED,
					false
				) && audioId != NOT_UPLOADED,
				canShowInfo = audioId != NOT_UPLOADED,
			)
		}
	}


	private fun scheduleDelete(audioId: Int) {
		val inputData = Data.Builder()
			.putInt("audioId", audioId)
			.build()

		val constraints = Builder()
			.setRequiredNetworkType(NetworkType.CONNECTED)
			.build()

		WorkerManager.enqueueWorker(
			owner = this,
			workerBuilderType = OneTimeWorkRequestBuilder<DeleteAudioWorker>(),
			inputData = inputData,
			constraints = constraints,
			onSucceeded = {
				displayToast(this, getString(R.string.audio_deleted_successfully))
			}
		)

	}

	private fun deleteAudio() {
		if (audioFile.exists()) audioFile.delete()
		if (metadataFile.exists()) metadataFile.delete()
		if (audioId == NOT_UPLOADED) {
			WorkManager.getInstance(this)
				.cancelAllWorkByTag("upload_audio_${audioFile.name.hashCode()}")
			val newQueueLength =
				max(
					getSharedPreferences(AUDIO_QUEUE, MODE_PRIVATE).getInt(
						AUDIO_QUEUE_LENGTH,
						0
					) - 1, 0
				)
			getSharedPreferences(AUDIO_QUEUE, MODE_PRIVATE).edit()
				.putInt(AUDIO_QUEUE_LENGTH, newQueueLength).apply()
			WorkerManager.enqueueWorker(
				this,
				OneTimeWorkRequestBuilder<NotificationWorker>(),
				Data.Builder().putInt("audioCount", newQueueLength).build(),
				Builder().setRequiredNetworkType(NetworkType.UNMETERED).build(),
				listOf("notification"),
				beforeWork = {
					WorkManager.getInstance(this).cancelAllWorkByTag("notification")
				},
				onSucceeded = {
					getSharedPreferences(AUDIO_QUEUE, MODE_PRIVATE).edit()
						.putInt(AUDIO_QUEUE_LENGTH, 0).apply()
				}
			)
		} else {
			scheduleDelete(audioId)
		}
		val intent = Intent().apply {
			putExtra("deletedAudioName", audioName)
		}
		setResult(RESULT_OK, intent)
		finish()
	}


	private fun changePrivacy() {
		if (hiddenStatus.intValue == NOT_UPLOADED) {
			displayToast(this, getString(R.string.audio_not_uploaded))
			return
		}
		/*
		 * se hiddenstatus e'
		 * 0 -> il brano e' pubblico, quindi lo rendo nascosto, chiamo hide
		 * 1 -> il brano e' nascosto, quindi lo rendo pubblico, chiamo show
		 * */
		val newPrivacy = if (hiddenStatus.intValue.toBoolean()) "show" else "hide"
		val inputData = Data.Builder()
			.putInt("audioId", audioId)
			.putString("newPrivacy", newPrivacy)
			.build()
		val constraints = Builder()
			.setRequiredNetworkType(NetworkType.CONNECTED)
			.build()

		WorkerManager.enqueueWorker(
			owner = this,
			workerBuilderType = OneTimeWorkRequestBuilder<EditPrivacyWorker>(),
			inputData = inputData,
			constraints = constraints,
			onSucceeded = {
				hiddenStatus.intValue = hiddenStatus.intValue.toBoolean().not().toInt()
				audioMetadata.put("hidden", hiddenStatus.intValue.toBoolean())
				metadataFile.writeText(audioMetadata.toString())
			}
		)
	}

	private fun showAudioInfo() {
		Intent(this, AudioInfoActivity::class.java).apply {
			putExtra("audioId", audioId)
			putExtra("audioFilename", audioName)
		}.also { startActivity(it) }
	}
}
