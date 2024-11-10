package com.example.ycilt.my_audio

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.ycilt.R
import com.example.ycilt.others_audio.AudioInfoActivity
import com.example.ycilt.utils.Constants.NOT_UPLOADED
import com.example.ycilt.utils.Keys.IS_LOGGED
import com.example.ycilt.utils.Misc.audioToMetadataFilename
import com.example.ycilt.utils.Privacy.UNKNOWN_PRIVACY
import com.example.ycilt.utils.ToastManager.displayToast
import com.example.ycilt.utils.toBoolean
import com.example.ycilt.utils.toInt
import com.example.ycilt.workers.DeleteAudioWorker
import com.example.ycilt.workers.EditPrivacyWorker
import com.example.ycilt.workers.WorkerManager
import org.json.JSONObject
import java.io.File

/*class MyAudioInfoActivity : AppCompatActivity() {
	private lateinit var audioName: String
	private lateinit var audioFile: File
	private lateinit var metadataFile: File
	private lateinit var audioMetadata: JSONObject
	private var hiddenStatus: Int = NOT_UPLOADED
	private var audioId: Int = NOT_UPLOADED
	private var mediaPlayer: MediaPlayer? = null
//	private lateinit var audioPlayerManager: AudioPlayerManager

	private lateinit var privacyButton: Button
	private lateinit var deleteButton: Button
	private lateinit var showInfoButton: Button


	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_my_audio_info)

		audioName = intent.getStringExtra("audioName")!!
		audioMetadata = JSONObject(intent.getStringExtra("audioMetadata")!!)

		audioFile = File(filesDir, audioName)
		metadataFile = File(filesDir, audioToMetadataFilename(audioName))

		findViewById<TextView>(R.id.audio_info_name).text = audioName
		deleteButton = findViewById(R.id.delete_button)
		privacyButton = findViewById(R.id.privacy_button)
		showInfoButton = findViewById(R.id.btn_view_info)


		val locationInfoView = findViewById<LinearLayout>(R.id.locationInfoView)
		val latitude = audioMetadata.getDouble("latitude")
		val longitude = audioMetadata.getDouble("longitude")


		Misc.coordToAddr(this, latitude, longitude) { address ->
			LocationDisplayer(locationInfoView, latitude, longitude, address)
		}

		audioId = audioMetadata.getInt("id")

		Log.d("MyAudioInfoActivity", audioMetadata.toString())
		hiddenStatus = if (audioMetadata.has("hidden")) audioMetadata.getBoolean("hidden")
			.toInt() else UNKNOWN_PRIVACY

		deleteButton.setOnClickListener { deleteAudio() }

		privacyButton.setOnClickListener { changePrivacy() }

		showInfoButton.setOnClickListener {
			val intent = Intent(this, AudioInfoActivity::class.java)
			intent.putExtra("audioId", audioId)
			intent.putExtra("audioFilename", audioName)
			startActivity(intent)
		}


	}

	override fun onResume() {
		super.onResume()
		for (button in listOf(deleteButton, privacyButton)) {
			button.isEnabled = intent.getBooleanExtra(IS_LOGGED, false)
		}
		if (intent.getBooleanExtra(IS_LOGGED, false)) {
			updatePrivacyButton()
		}

		if (audioId == NOT_UPLOADED) {
			privacyButton.isEnabled = false
		}

		showInfoButton.isEnabled = true
		if (hiddenStatus != PUBLIC_AUDIO || audioId == NOT_UPLOADED) {
			showInfoButton.isEnabled = false
		}
	}

	private fun updatePrivacyButton() {
		privacyButton.text =
			when (hiddenStatus) {
				PRIVATE_AUDIO -> getString(R.string.make_public)
				PUBLIC_AUDIO -> getString(R.string.make_private)
				else -> getString(R.string.change_audio_s_privacy)
			}
		privacyButton.isEnabled = hiddenStatus != NOT_UPLOADED
	}


	private fun scheduleDelete(audioId: Int) {
		val inputData = Data.Builder()
			.putInt("audioId", audioId)
			.build()

		val constraints = Constraints.Builder()
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
		if (audioId != NOT_UPLOADED) {
			WorkManager.getInstance(this)
				.cancelAllWorkByTag("upload_audio_${audioFile.name.hashCode()}")
			scheduleDelete(audioId)
		}
		val intent = Intent().apply {
			putExtra("deletedAudioName", audioName)
		}
		setResult(RESULT_OK, intent)
		finish()
	}

	private fun changePrivacy() {
		if (hiddenStatus == NOT_UPLOADED) {
			displayToast(this, getString(R.string.audio_not_uploaded))
			return
		}
		*//*
		 * se hiddenstatus e'
		 * 0 -> il brano e' pubblico, quindi lo rendo nascosto, chiamo hide
		 * 1 -> il brano e' nascosto, quindi lo rendo pubblico, chiamo show
		 * *//*
		val newPrivacy = if (hiddenStatus.toBoolean()) "show" else "hide"
		val inputData = Data.Builder()
			.putInt("audioId", audioId)
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
				audioMetadata.put("hidden", hiddenStatus.toBoolean())
				metadataFile.writeText(audioMetadata.toString())
				updatePrivacyButton()
			}
		)
	}

	override fun onDestroy() {
		super.onDestroy()
		mediaPlayer?.release()
	}
}*/

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
				//TODO prendere i valori reali
				canDeleteAudio = intent.getBooleanExtra(IS_LOGGED, false),
				canChangePrivacy = intent.getBooleanExtra(IS_LOGGED, false),
				canShowInfo = true
			)
		}
	}


	private fun scheduleDelete(audioId: Int) {
		val inputData = Data.Builder()
			.putInt("audioId", audioId)
			.build()

		val constraints = Constraints.Builder()
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
		if (audioId != NOT_UPLOADED) {
			WorkManager.getInstance(this)
				.cancelAllWorkByTag("upload_audio_${audioFile.name.hashCode()}")
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
		val constraints = Constraints.Builder()
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
