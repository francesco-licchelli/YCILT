package com.example.ycilt.workers

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.work.WorkerParameters
import com.example.ycilt.R
import com.example.ycilt.utils.Constants.NOT_UPLOADED
import com.example.ycilt.utils.Misc.displayToast
import com.example.ycilt.utils.NetworkUtils.deleteRequest
import kotlinx.coroutines.runBlocking

class DeleteSongWorker(context: Context, params: WorkerParameters) : AuthedWorker(context, params) {
	private val songId: Int = inputData.getInt("songId", NOT_UPLOADED)

	override suspend fun doAuthedWork(): Result {
		return runBlocking {
			val (responseCode, _) = deleteRequest(
				endpoint = "audio/$songId",
				context = applicationContext,
				loginRequired = true
			)

			if (responseCode == 200) {
				Handler(Looper.getMainLooper()).post {
					displayToast(
						applicationContext,
						applicationContext.getString(R.string.song_deleted_successfully)
					)
				}
				Result.success()
			} else {
				Result.failure()
			}
		}
	}

}