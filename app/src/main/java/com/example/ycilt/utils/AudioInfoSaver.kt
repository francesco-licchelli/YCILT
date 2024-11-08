package com.example.ycilt.utils

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.util.Log
import com.example.ycilt.others_audio.AudioDetails
import com.example.ycilt.utils.Constants.NOT_UPLOADED
import com.example.ycilt.utils.Keys.IS_LOGGED
import com.example.ycilt.utils.Misc.audioToMetadataFilename
import com.example.ycilt.utils.NetworkUtils.getRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import kotlin.math.min

object AudioInfoSaver {

	private fun getAllJsonFiles(context: Context): List<File> {
		return context.filesDir.listFiles()?.filter { it.extension == "json" } ?: emptyList()
	}

	private fun readJsonFromFile(file: File): List<JSONObject> {
		val jsonList = mutableListOf<JSONObject>()
		try {
			val jsonString = file.readText() // Leggi il contenuto del file
			Log.d("MyAudioActivity", "Json string: $jsonString")
			jsonList.add(JSONObject(jsonString))
		} catch (e: Exception) {
			e.printStackTrace()
		}
		return jsonList
	}

	private suspend fun getOnlineAudio(context: Context): JSONArray? {
		return withContext(Dispatchers.IO) {
			val (responseCode, responseBody) = getRequest(
				endpoint = "audio/my",
				context = context,
				loginRequired = true,
			)
			Log.d("AudioInfoSaver", "Response code: $responseCode")
			Log.d("AudioInfoSaver", "Response body: $responseBody")
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

	suspend fun updateMetadataFromBackend(
		context: Context,
		intent: Intent,
	): List<Pair<File, JSONObject>> {
		val audioList = mutableListOf<Pair<File, JSONObject>>()
		var remoteAudio = JSONArray()
		if (intent.getBooleanExtra(IS_LOGGED, false)) {
			val remoteData = getOnlineAudio(context)
			if (remoteData != null) {
				remoteAudio = remoteData
			}
		}

		val jsonFiles = getAllJsonFiles(context)
		val localData = mutableListOf<JSONObject>()

		for (file in jsonFiles) {
			val jsonObjectsFromFile = readJsonFromFile(file)
			localData.addAll(jsonObjectsFromFile)
		}
		for (i in 0 until min(localData.size, remoteAudio.length())) {
			val audioFilename = localData[i].getString("file_name")
			val metadataFilename = File(audioToMetadataFilename(audioFilename))
			if (localData[i].getInt("id") == NOT_UPLOADED) {
				val remoteAudioDetails =
					getAudioDetails(
						context,
						remoteAudio.getJSONObject(i).getInt("id"),
						listOf("file_name" to audioFilename)
					)
				if (remoteAudioDetails != null) localData[i] = remoteAudioDetails
			}
			localData[i].put("hidden", remoteAudio.getJSONObject(i).getBoolean("hidden"))
			metadataFilename.writeText(localData[i].toString())
		}

		val audioDir = context.filesDir

		audioDir.listFiles { file ->
			file.extension == "mp3"
		}?.forEach { mp3File ->
			val metadataFile = File(audioToMetadataFilename(mp3File.absolutePath))
			if (metadataFile.exists()) {
				val metadata = JSONObject(metadataFile.readText())
				val duration = getAudioDuration(mp3File)
				val date = mp3File.nameWithoutExtension.split("record_").lastOrNull() ?: "Unknown"
				metadata.put("duration", duration)
				metadata.put("date", date)
				audioList.add(Pair(mp3File, metadata))
			}
		}
		return audioList
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

	private fun getAudioDetails(
		context: Context,
		audioId: Int,
		preserveData: List<Pair<String, String>> = emptyList(),
	): JSONObject? {
		val (responseCode, responseBody) = getRequest(
			endpoint = "audio/$audioId",
			context = context,
			loginRequired = true,
		)
		return if (responseCode == 200) {
			val res = JSONObject(responseBody)
			preserveData.forEach { (key, value) ->
				res.put(key, value)
			}
			res
		} else {
			null
		}

	}

	fun parseAudioData(
		context: Context,
		id: Int,
		audioFilename: String? = null
	): AudioDetails? {
		Log.d("AudioInfoSaver", "Parsing audio data from ${audioFilename ?: id}")
		val audioDetails: JSONObject? = if (audioFilename == null) {
			getAudioDetails(context, id)
		} else {
			getLocalAudioDetails(context, audioToMetadataFilename(audioFilename))
		}

		return if (audioDetails != null) {
			AudioDetails(audioDetails)
		} else {
			null
		}
	}

	private fun getLocalAudioDetails(context: Context, path: String): JSONObject {
		val metadataFile = File(context.filesDir, path)
		val metadata = JSONObject(metadataFile.readText())
		return metadata
	}



}