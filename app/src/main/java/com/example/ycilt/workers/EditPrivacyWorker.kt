package com.example.ycilt.workers

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.work.WorkerParameters
import com.example.ycilt.utils.Constants.NOT_UPLOADED
import com.example.ycilt.utils.Misc.displayToast
import com.example.ycilt.utils.NetworkUtils.getRequest
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

class EditPrivacyWorker(context: Context, params: WorkerParameters) :
	AuthedWorker(context, params) {
	private val songId: Int = inputData.getInt("songId", NOT_UPLOADED)
	private val newPrivacy: String = inputData.getString("newPrivacy") ?: ""

	override suspend fun doAuthedWork(): Result {
		return runBlocking {
			val (responseCode, responseBody) = getRequest(
				endpoint = "audio/my/$songId/$newPrivacy",
				context = applicationContext,
				loginRequired = true
			)

			if (responseCode == 200) {
				Handler(Looper.getMainLooper()).post {
					displayToast(
						applicationContext,
						JSONObject(responseBody).getString("detail")
					)
				}
				Result.success()
			} else {
				Result.failure()
			}
		}
	}

}