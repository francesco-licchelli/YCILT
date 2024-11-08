package com.example.ycilt.my_audio

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ycilt.R
import com.example.ycilt.utils.AudioInfoSaver.updateMetadataFromBackend
import com.example.ycilt.utils.Keys.IS_LOGGED
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

class MyAudioActivity : AppCompatActivity() {

	private lateinit var recyclerView: RecyclerView
	private lateinit var adapter: AudioAdapter
	private val audioList = mutableListOf<Pair<File, JSONObject>>()
	private lateinit var audioInfoLauncher: ActivityResultLauncher<Intent>
	private var loadingDialog: Dialog? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_my_audio)

		recyclerView = findViewById(R.id.recyclerView_audio)
		recyclerView.layoutManager = LinearLayoutManager(this)

		audioInfoLauncher = registerForActivityResult(
			ActivityResultContracts.StartActivityForResult()
		) { result ->
			if (result.resultCode == RESULT_OK) {
				val deletedAudioName = result.data?.getStringExtra("deletedAudioName")
				if (deletedAudioName != null) {
					removeAudioFromList(deletedAudioName)
				}
			}
		}

	}

	override fun onResume() {
		super.onResume()
		showLoadingScreen()

		CoroutineScope(Dispatchers.IO).launch {
			audioList.clear()
			val audio = updateMetadataFromBackend(this@MyAudioActivity, intent)
			audioList.addAll(audio)
			displayAudio(audioList)
			withContext(Dispatchers.Main) {
				hideLoadingScreen()
			}
		}
	}

	private fun showLoadingScreen() {
		if (loadingDialog == null) {
			loadingDialog = Dialog(this).apply {
				setContentView(R.layout.loading_screen)
				setCancelable(false) // Impedisce di chiuderlo cliccando fuori
				window?.setBackgroundDrawableResource(android.R.color.transparent)
			}
		}
		loadingDialog?.show()
	}

	private fun hideLoadingScreen() {
		loadingDialog?.dismiss()
	}

	private fun displayAudio(audio: List<Pair<File, JSONObject>>) {
		CoroutineScope(Dispatchers.IO).launch {
			runOnUiThread {
				findViewById<TextView>(R.id.loading_audio).visibility = View.GONE
				recyclerView.visibility = View.VISIBLE
			}
			withContext(Dispatchers.Main) {
				if (audio.isEmpty()) {
					findViewById<TextView>(R.id.no_audio_message).visibility = View.VISIBLE
					recyclerView.visibility = View.GONE
				} else {
					findViewById<TextView>(R.id.no_audio_message).visibility = View.GONE
					recyclerView.visibility = View.VISIBLE
					adapter = AudioAdapter(
						audioList,
						intent.getBooleanExtra(IS_LOGGED, false)
					)
					recyclerView.adapter = adapter
				}
			}
		}
	}

	private fun removeAudioFromList(deletedAudioName: String) {
		val audioIndex = audioList.indexOfFirst { it.first.name == deletedAudioName }
		if (audioIndex != -1) {
			audioList.removeAt(audioIndex)
			adapter.notifyItemRemoved(audioIndex)
			if (audioList.isEmpty()) {
				findViewById<TextView>(R.id.no_audio_message).visibility = View.VISIBLE
				recyclerView.visibility = View.GONE
			}
		}
	}
}