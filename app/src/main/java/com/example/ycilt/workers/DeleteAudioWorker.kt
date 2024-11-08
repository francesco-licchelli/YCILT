package com.example.ycilt.workers

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.work.Data
import androidx.work.WorkerParameters
import com.example.ycilt.R
import com.example.ycilt.utils.Constants.NOT_UPLOADED
import com.example.ycilt.utils.NetworkUtils.deleteRequest
import kotlinx.coroutines.runBlocking

class DeleteAudioWorker(context: Context, params: WorkerParameters) : AuthedWorker(context, params) {
	private val audioId: Int = inputData.getInt("audioId", NOT_UPLOADED)

	override suspend fun doAuthedWork(): Result {
		return runBlocking {
			return@runBlocking parseResult(
				deleteRequest(
					endpoint = "audio/$audioId",
					context = applicationContext,
					loginRequired = true
				)
			)
		}
	}

}