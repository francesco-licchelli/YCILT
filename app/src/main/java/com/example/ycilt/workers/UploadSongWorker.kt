package com.example.ycilt.workers

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.WorkerParameters
import com.example.ycilt.utils.Constants
import com.example.ycilt.utils.NetworkUtils.postRequest
import kotlinx.coroutines.runBlocking
import java.io.File

class UploadSongWorker(context: Context, params: WorkerParameters) : AuthedWorker(context, params) {
	private val songPath: String = inputData.getString("songPath") ?: ""
	private val latitude: String = inputData.getString("latitude") ?: ""
	private val longitude: String = inputData.getString("longitude") ?: ""

	override suspend fun doAuthedWork(): Result {
		val songFile = File(songPath)
		if (!songFile.exists()) return Result.failure()

		return runBlocking {
			val queryParams = mapOf(
				"longitude" to longitude,
				"latitude" to latitude
			)

			val (responseCode, _) = postRequest(
				endpoint = "upload",
				context = applicationContext,
				loginRequired = true,
				queryParams = queryParams,
				audio = songFile
			)

			if (responseCode == 200) {
				val intent = Intent(Constants.BROADCAST_UPLOAD)
				intent.putExtra("songPath", songPath)
				applicationContext.sendBroadcast(intent)
				Log.d("UploadSongWorker", "Song uploaded successfully")
				return@runBlocking Result.success()
			} else {
				return@runBlocking Result.failure()
			}
		}
	}

}