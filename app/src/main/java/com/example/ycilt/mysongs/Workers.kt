package com.example.ycilt.mysongs

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.work.Constraints.Builder
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.ycilt.auth.LoginManager
import com.example.ycilt.utils.Constants
import com.example.ycilt.utils.Constants.NOT_UPLOADED
import com.example.ycilt.utils.NetworkUtils.deleteRequest
import com.example.ycilt.utils.NetworkUtils.getRequest
import com.example.ycilt.utils.NetworkUtils.postRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.io.File


abstract class AuthedWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    abstract suspend fun doAuthedWork(): Result

    override fun doWork(): Result {
        return runBlocking {
            val isTokenValid = verifyToken()
            if (isTokenValid) {
                doAuthedWork()
            } else {
                deferWorkUntilLogin()
                Result.failure()
            }
        }
    }
    private fun deferWorkUntilLogin() {
        saveWorkerState()
        waitForLogin()
    }

    abstract fun saveWorkerState()

    private fun waitForLogin() {
        LoginManager.registerOnLoginListener {
            WorkManager.getInstance(applicationContext)
                .enqueue(OneTimeWorkRequest.from(javaClass))
        }
    }

    private fun verifyToken(): Boolean {
        val (responseCode, _) = getRequest("audio/my", applicationContext)
        return responseCode == 200
    }
}

class UploadSongWorker(context: Context, params: WorkerParameters) : AuthedWorker(context, params) {
    private lateinit var songPath: String
    private lateinit var latitude: String
    private lateinit var longitude: String

    override suspend fun doAuthedWork(): Result {
        songPath = inputData.getString("songPath") ?: return Result.failure()
        latitude = inputData.getString("latitude") ?: return Result.failure()
        longitude = inputData.getString("longitude") ?: return Result.failure()
        val songFile = File(songPath)
        if (!songFile.exists()) return Result.failure()

        return runBlocking {
            val queryParams = mapOf(
                "longitude" to longitude,
                "latitude" to latitude
            )

            val (responseCode, responseBody) = postRequest(
                "upload",
                authRequest = applicationContext,
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
                return@runBlocking Result.retry()
            }
        }
    }

    // Salva lo stato del worker
    override fun saveWorkerState() {
        val data = Data.Builder()
            .putString("songPath", songPath)
            .putString("latitude", latitude)
            .putString("longitude", longitude)
            .build()
        val constraints = Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED) // Solo Wi-Fi
            .build()
        val uploadRequest = OneTimeWorkRequestBuilder<UploadSongWorker>()
            .setInputData(data)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(applicationContext).enqueue(uploadRequest)
    }
}

class EditPrivacyWorker(context: Context, params: WorkerParameters) : AuthedWorker(context, params) {
    private var songId: Int = NOT_UPLOADED
    private lateinit var newPrivacy: String

    override suspend fun doAuthedWork(): Result {
        songId = inputData.getInt("songId", -1)
        newPrivacy = inputData.getString("newPrivacy") ?: return Result.failure()

        return runBlocking {
            val (responseCode, _) = getRequest(
                "audio/my/$songId/$newPrivacy",
                authRequest = applicationContext,
            )

            if (responseCode == 200) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText( applicationContext, "Privacy changed successfully", Toast.LENGTH_SHORT ).show()
                }
                Result.success()
            } else {
                Result.failure()
            }
        }
    }

    override fun saveWorkerState() {
        val data = Data.Builder()
            .putInt("songId", inputData.getInt("songId", songId))
            .putString("newPrivacy", inputData.getString("newPrivacy"))
            .build()
        val constraints = Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // Qualsiasi rete
            .build()
        val editPrivacyRequest = OneTimeWorkRequestBuilder<EditPrivacyWorker>()
            .setInputData(data)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(applicationContext).enqueue(editPrivacyRequest)
    }
}

class DeleteSongWorker(context: Context, params: WorkerParameters) : AuthedWorker(context, params) {
    private var songId: Int = -1
    override suspend fun doAuthedWork(): Result {
        songId = inputData.getInt("songId", -1)

        return runBlocking {
            val (responseCode, _) = deleteRequest(
                "audio/$songId",
                authRequest = applicationContext,
            )

            if (responseCode == 200) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText( applicationContext, "Song deleted successfully", Toast.LENGTH_SHORT ).show()
                }
                Result.success()
            } else {
                Result.failure()
            }
        }
    }

    override fun saveWorkerState() {
        val data = Data.Builder()
            .putInt("songId", songId)
            .build()
        val constraints = Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // Qualsiasi rete
            .build()
        val deleteRequest = OneTimeWorkRequestBuilder<DeleteSongWorker>()
            .setInputData(data)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(applicationContext).enqueue(deleteRequest)
    }
}
