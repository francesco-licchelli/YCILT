package com.example.ycilt.mysongs

import android.app.Dialog
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ycilt.R
import com.example.ycilt.utils.NetworkUtils.getRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import kotlin.math.min

class MySongsActivity : AppCompatActivity() {

	private lateinit var recyclerView: RecyclerView
	private lateinit var adapter: SongsAdapter
	private val songsList = mutableListOf<Pair<File, JSONObject>>()
	private lateinit var songInfoLauncher: ActivityResultLauncher<Intent>
	private var loadingDialog: Dialog? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_my_songs)

		recyclerView = findViewById(R.id.recyclerView_songs)
		recyclerView.layoutManager = LinearLayoutManager(this)

		songInfoLauncher = registerForActivityResult(
			ActivityResultContracts.StartActivityForResult()
		) { result ->
			if (result.resultCode == RESULT_OK) {
				//TODO sistemare questo Edit: che commento e'???
				val deletedSongName = result.data?.getStringExtra("deletedSongName")
				if (deletedSongName != null) {
					removeSongFromList(deletedSongName)
				}
			}
		}

	}

	override fun onResume() {
		super.onResume()
		showLoadingScreen()

		CoroutineScope(Dispatchers.IO).launch {
			displaySongs(getSongs())
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

	private fun getAudioDuration(mp3File: File): Long {
		val mediaPlayer = MediaPlayer()
		return try {
			mediaPlayer.setDataSource(mp3File.absolutePath)
			mediaPlayer.prepare()
			val duration = mediaPlayer.duration.toLong()
			mediaPlayer.release()
			duration
		} catch (e: IOException) {
			0L
		}
	}

	private fun getAllJsonFiles(): List<File> {
		return filesDir.listFiles()?.filter { it.extension == "json" } ?: emptyList()
	}

	private fun readJsonFromFile(file: File): List<JSONObject> {
		val jsonList = mutableListOf<JSONObject>()
		try {
			val jsonString = file.readText() // Leggi il contenuto del file
			Log.d("MySongsActivity", "Json string: $jsonString")
			jsonList.add(JSONObject(jsonString))
		} catch (e: Exception) {
			e.printStackTrace()
		}
		return jsonList
	}

	private suspend fun getOnlineSongs(): JSONArray? {
		return withContext(Dispatchers.IO) {
			val (responseCode, responseBody) = getRequest(
				endpoint = "audio/my",
				context = this@MySongsActivity,
				loginRequired = true,
			)
			if (responseCode == 200) {
				try {
					JSONArray(responseBody)
				} catch (e: JSONException) {
					e.printStackTrace()
					null
				}
			} else {
				null
			}
		}
	}

	private suspend fun getSongs(): List<Pair<File, JSONObject>> {
		var remoteSongs = JSONArray()
		if (intent.getBooleanExtra("is_logged_in", false)) {
			val remoteData = getOnlineSongs()
			if (remoteData != null) {
				remoteSongs = remoteData
			}
		}

		val jsonFiles = getAllJsonFiles()
		val localData = mutableListOf<JSONObject>()

		for (file in jsonFiles) {
			val jsonObjectsFromFile = readJsonFromFile(file)
			localData.addAll(jsonObjectsFromFile)
		}

		for (i in 0 until min(localData.size, remoteSongs.length())) {
			val metadataFile = File(
				localData[i].getString("file_name").replace(".mp3", "_metadata.json")
			)
			localData[i].put("id", remoteSongs.getJSONObject(i).getInt("id"))
			localData[i].put("hidden", remoteSongs.getJSONObject(i).getBoolean("hidden"))
			metadataFile.writeText(localData[i].toString())
		}

		val audioDir = filesDir
		songsList.clear()

		audioDir.listFiles { file ->
			file.extension == "mp3"
		}?.forEach { mp3File ->
			val metadataFile = File(mp3File.absolutePath.replace(".mp3", "_metadata.json"))
			if (metadataFile.exists()) {
				val metadata = JSONObject(metadataFile.readText())
				val duration = getAudioDuration(mp3File)
				val date = mp3File.nameWithoutExtension.split("record_").lastOrNull() ?: "Unknown"
				metadata.put("duration", duration)
				metadata.put("date", date)
				songsList.add(Pair(mp3File, metadata))
			}
		}

		return songsList
	}


	private fun displaySongs(songs: List<Pair<File, JSONObject>>) {
		CoroutineScope(Dispatchers.IO).launch {
			runOnUiThread {
				findViewById<TextView>(R.id.loading_songs).visibility = View.GONE
				recyclerView.visibility = View.VISIBLE
			}
			withContext(Dispatchers.Main) {
				if (songs.isEmpty()) {
					findViewById<TextView>(R.id.no_songs_message).visibility = View.VISIBLE
					recyclerView.visibility = View.GONE
				} else {
					findViewById<TextView>(R.id.no_songs_message).visibility = View.GONE
					recyclerView.visibility = View.VISIBLE
					adapter = SongsAdapter(
						songsList,
						intent.getBooleanExtra("is_logged_in", false)
					)
					recyclerView.adapter = adapter
				}
			}
		}
	}

	private fun removeSongFromList(deletedSongName: String) {
		val songIndex = songsList.indexOfFirst { it.first.name == deletedSongName }
		if (songIndex != -1) {
			songsList.removeAt(songIndex)
			adapter.notifyItemRemoved(songIndex)
			if (songsList.isEmpty()) {
				findViewById<TextView>(R.id.no_songs_message).visibility = View.VISIBLE
				recyclerView.visibility = View.GONE
			}
		}
	}
}
