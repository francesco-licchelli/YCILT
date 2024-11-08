package com.example.ycilt.workers

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.work.Data
import androidx.work.WorkerParameters
import com.example.ycilt.utils.Constants.NOT_UPLOADED
import com.example.ycilt.utils.NetworkUtils.getRequest
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

class EditPrivacyWorker(context: Context, params: WorkerParameters) :
	AuthedWorker(context, params) {
	private val audioId: Int = inputData.getInt("audioId", NOT_UPLOADED)
	private val newPrivacy: String = inputData.getString("newPrivacy") ?: ""

	override suspend fun doAuthedWork(): Result {
		return runBlocking {
			return@runBlocking parseResult(
				getRequest(
					endpoint = "audio/my/$audioId/$newPrivacy",
					context = applicationContext,
					loginRequired = true
				)
			)
		}
	}
}