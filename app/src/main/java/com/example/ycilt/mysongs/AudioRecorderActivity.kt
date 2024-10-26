package com.example.ycilt.mysongs

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints.*
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.arthenica.mobileffmpeg.FFmpeg
import com.example.ycilt.R
import com.example.ycilt.utils.AudioPlayerManager
import com.example.ycilt.utils.Constants
import com.example.ycilt.utils.Constants.NOT_UPLOADED
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AudioRecorderActivity : AppCompatActivity() {

    private var mediaRecorder: MediaRecorder? = null
    private var songFileName: String = ""
    private var latestTimestamp: String? = null

    private lateinit var btnRecord: Button
    private lateinit var btnStopRecord: Button
    private lateinit var btnSave: Button

    private lateinit var audioPlayerManager: AudioPlayerManager
    private val uploadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("AudioRecorderActivity", "Received broadcast: ${intent?.action}")
            val uploadedSongPath = intent?.getStringExtra("songPath")
            if (songFileName != uploadedSongPath) return
            Toast.makeText(this@AudioRecorderActivity, "Song uploaded", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_recorder)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 0)
        }

        val btnBack: ImageButton = findViewById(R.id.btnBack)
        btnRecord = findViewById(R.id.btnRecord)
        btnStopRecord = findViewById(R.id.btnStopRecord)
        btnSave = findViewById(R.id.btnSave)

        registerReceiver(uploadReceiver, IntentFilter(Constants.BROADCAST_UPLOAD),
            RECEIVER_NOT_EXPORTED)


        btnBack.setOnClickListener {
            if (mediaRecorder != null) {
                mediaRecorder?.release()
                mediaRecorder = null
                if (File(songFileName).exists()) {
                    File(songFileName).delete()
                }
            }
            finish()
        }

        btnRecord.setOnClickListener {
            startRecording()
            updateButtonStates(isRecording = true, hasRecorded = false)
        }

        btnStopRecord.setOnClickListener {
            stopRecording()
            updateButtonStates(isRecording = false, hasRecorded = true)
        }

        // Utilizza AudioPlayerManager per gestire la riproduzione audio
        audioPlayerManager = AudioPlayerManager(
            songFileName,
            findViewById(R.id.btnPlay),
            findViewById(R.id.audio_seekbar),
            findViewById(R.id.tv_current_time),
            findViewById(R.id.tv_total_time),
        )
        audioPlayerManager.startListener()

        btnSave.setOnClickListener {
            saveRecording()
        }

        // Disattiva i pulsanti inizialmente non utilizzabili
        btnStopRecord.isEnabled = false
        btnSave.isEnabled = false
        audioPlayerManager.updateStates(false)
    }

    private fun updateButtonStates(isRecording: Boolean, hasRecorded: Boolean) {
        btnRecord.isEnabled = !isRecording
        btnStopRecord.isEnabled = isRecording
        btnSave.isEnabled = !isRecording && hasRecorded
        audioPlayerManager.updateStates(!isRecording && hasRecorded)
    }

    private fun startRecording() {
        latestTimestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        songFileName = "${filesDir.absolutePath}/audio_record_$latestTimestamp.aac"

        mediaRecorder = MediaRecorder(this).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(songFileName)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

            try {
                prepare()
                start()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
        audioPlayerManager.updateAudioFilePath(songFileName)
        audioPlayerManager.canPlay = true
        audioPlayerManager.updateTotalTime()
    }

    private fun saveRecording() {
        val mp3FilePath = songFileName.replace(".aac", ".mp3")
        val command = arrayOf("-i", songFileName, mp3FilePath)
        FFmpeg.executeAsync(command) { _, returnCode ->
            if (returnCode == com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS) {
                saveMetaData(mp3FilePath)
                scheduleUpload(mp3FilePath)
                File(songFileName).delete()
            } else {
                Toast.makeText(this, "Failed to convert file", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveMetaData(songPath: String) {
        val metadataFilePath = songPath.replace(".mp3", "_metadata.json")
        val metadata = JSONObject().apply {
            put("file_name", songPath)
            put("latitude", intent.getDoubleExtra("latitude", 0.0))
            put("longitude", intent.getDoubleExtra("longitude", 0.0))
            put("id", NOT_UPLOADED)
            put("mood", (arrayOf<String>()))
            put("genre", (arrayOf<String>()))
            put("instruments", (arrayOf<String>()))
            put("bpm", -1)
            put("danceability", -1)
            put("loudness", -1)
            put("hidden", true)
        }
        File(metadataFilePath).writeText(metadata.toString())
    }

    private fun scheduleUpload(songPath: String) {
        val constraints = Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED) // Solo Wi-Fi
            .build()

        val data = Data.Builder()
            .putString("songPath", songPath)
            .putString("latitude", intent.getDoubleExtra("latitude", 0.0).toString())
            .putString("longitude", intent.getDoubleExtra("longitude", 0.0).toString())
            .build()

        val uploadRequest = OneTimeWorkRequestBuilder<UploadSongWorker>()
            .setConstraints(constraints)
            .setInputData(data)
            .addTag("upload_song_${songFileName.hashCode()}")
            .build()

        WorkManager.getInstance(this).enqueue(uploadRequest)
    }

    override fun onDestroy() {
        super.onDestroy()
        audioPlayerManager.release()
        mediaRecorder?.release()
        mediaRecorder = null
        unregisterReceiver(uploadReceiver)
    }
}
