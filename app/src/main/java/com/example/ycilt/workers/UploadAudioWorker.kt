package com.example.ycilt.workers

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.WorkerParameters
import com.example.ycilt.utils.NetworkUtils.postRequest
import kotlinx.coroutines.runBlocking
import java.io.File

class UploadAudioWorker(context: Context, params: WorkerParameters) :
	AuthedWorker(context, params) {
	private val audioPath: String = inputData.getString("audioPath") ?: ""
	private val latitude: String = inputData.getString("latitude") ?: ""
	private val longitude: String = inputData.getString("longitude") ?: ""

	override suspend fun doAuthedWork(): Result {
		val audioFile = File(audioPath)
		if (!audioFile.exists()) return Result.failure()

		return runBlocking {
			val queryParams = mapOf(
				"longitude" to longitude,
				"latitude" to latitude
			)
			return@runBlocking parseResult(
				postRequest(
					endpoint = "upload",
					context = applicationContext,
					loginRequired = true,
					queryParams = queryParams,
					audio = audioFile

				)
			)

		}
	}

}