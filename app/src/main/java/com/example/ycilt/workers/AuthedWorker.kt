package com.example.ycilt.workers

import android.content.Context
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.ycilt.utils.NetworkUtils.getRequest
import kotlinx.coroutines.runBlocking

abstract class AuthedWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

	abstract suspend fun doAuthedWork(): Result

	companion object {
		fun verifyToken(context: Context): Boolean {
			val (responseCode, _) = getRequest(endpoint = "audio/my", context = context)
			return responseCode == 200
		}
	}

	override fun doWork(): Result {
		return runBlocking {
			val isTokenValid = verifyToken(applicationContext)
			if (isTokenValid) {
				doAuthedWork()
			} else {
				val data = Data.Builder()
					.putBoolean("retry", true)
					.build()
				Result.failure(data)
			}
		}
	}

	fun parseResult(result: Pair<Int, String>): Result {
		val (responseCode, responseBody) = result
		return if (responseCode == 200) {
			val data = Data.Builder()
				.putInt("responseCode", responseCode)
				.putString("responseBody", responseBody)
				.build()
			Result.success(data)
		} else {
			Result.failure()
		}
	}

}