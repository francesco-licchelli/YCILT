package com.example.ycilt.mysongs

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.ycilt.utils.NetworkUtils.postRequest
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.io.File

class UploadSongWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val songPath = inputData.getString("songPath") ?: return Result.failure()
        val latitude = inputData.getString("latitude") ?: return Result.failure()
        val longitude = inputData.getString("longitude") ?: return Result.failure()
        val songFile = File(songPath)
        if (!songFile.exists()) return Result.failure()

        // Calcola il percorso del file di metadata
        val metaDataFilePath = songPath.replace(".mp3", "_metadata.json")
        val metaDataFile = File(metaDataFilePath)

        return runBlocking {
            val queryParams = mapOf(
                "longitude" to longitude,
                "latitude" to latitude
            )

            // Effettua l'upload
            val (responseCode, responseBody) = postRequest(
                "upload",
                authRequest = applicationContext,
                queryParams = queryParams,
                audio = songFile
            )

            if (responseCode == 200) {
                // Parse il JSON di risposta
                val jsonResponse = JSONObject(responseBody)
                Log.d("UploadSongWorker", "Response: $jsonResponse")
                for (key in jsonResponse.keys()) {
                    Log.d("UploadSongWorker", "$key: ${jsonResponse[key]}")
                }
                Result.success()
            } else {
                Result.retry()
            }
        }
    }
}
