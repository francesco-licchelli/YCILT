package com.example.ycilt.mysongs

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Looper
import android.widget.Button
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
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
import com.example.ycilt.utils.Constants.NOT_UPLOADED
import com.example.ycilt.utils.Misc
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AudioRecorderActivity : AppCompatActivity() {

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var audioFilePath: String = ""
    private var latestTimestamp: String? = null

    private lateinit var btnRecord: Button
    private lateinit var btnStopRecord: Button
    private lateinit var btnPlay: ImageButton
    private lateinit var btnSave: Button

    private var isPlaying = false
    private var isPaused = false // Aggiunta variabile per tracciare la pausa

    private val handler = android.os.Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_recorder)

        // Controllo dei permessi per la registrazione audio
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
        btnPlay = findViewById(R.id.btnPlay)
        btnSave = findViewById(R.id.btnSave)
        val seekBar = findViewById<SeekBar>(R.id.audio_seekbar)

        // Disattiva i pulsanti inizialmente non utilizzabili
        btnStopRecord.isEnabled = false
        btnPlay.isEnabled = false
        btnSave.isEnabled = false

        btnBack.setOnClickListener {
            if (mediaRecorder != null) {
                mediaRecorder?.release()
                mediaRecorder = null
                if(File(audioFilePath).exists()) {
                    File(audioFilePath).delete()
                }
            }
            finish()
        }

        btnRecord.setOnClickListener {
            startRecording()
            updateButtonStates(isRecording = true)
        }

        btnStopRecord.setOnClickListener {
            stopRecording()
            updateButtonStates(isRecording = false)
        }

        btnPlay.setOnClickListener {
            if (isPlaying) {
                pauseRecording()
                btnPlay.setImageResource(R.drawable.ic_play)  // Cambia l'icona in "play"
            } else {
                playRecording()
                btnPlay.setImageResource(R.drawable.ic_pause)  // Cambia l'icona in "pause"
            }
            isPlaying = !isPlaying
        }

        btnSave.setOnClickListener {
            saveRecording()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                mediaPlayer?.pause()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                mediaPlayer?.start()
            }
        })
    }

    private fun updateButtonStates(isRecording: Boolean) {
        btnRecord.isEnabled = !isRecording
        btnStopRecord.isEnabled = isRecording
        btnPlay.isEnabled = !isRecording && audioFilePath.isNotEmpty()
        btnSave.isEnabled = !isRecording && audioFilePath.isNotEmpty()
    }

    private fun startRecording() {
        latestTimestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        audioFilePath = "${filesDir.absolutePath}/audio_record_$latestTimestamp.aac"

        mediaRecorder = MediaRecorder(this).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(audioFilePath)
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
        updateTotalTime(audioFilePath)
    }

    private fun playRecording() {
        if (mediaPlayer != null && isPaused) {
            // Riprendi da dove era stata messa in pausa
            mediaPlayer?.start()
            updateSeekBar()
            isPaused = false
        } else {
            // Crea un nuovo MediaPlayer e inizia da capo
            mediaPlayer = MediaPlayer().apply {
                try {
                    setDataSource(audioFilePath)
                    prepare()
                    start()

                    val seekBar = findViewById<SeekBar>(R.id.audio_seekbar)
                    seekBar.max = duration
                    updateSeekBar()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun pauseRecording() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
            handler.removeCallbacks(runnable!!)
            isPaused = true // Indica che la riproduzione è stata messa in pausa
        }
    }

    private fun updateSeekBar() {
        val seekBar = findViewById<SeekBar>(R.id.audio_seekbar)
        val currentTimeTextView = findViewById<TextView>(R.id.tv_current_time)

        runnable = Runnable {
            seekBar.progress = mediaPlayer?.currentPosition ?: 0
            val currentTime = formatTime(mediaPlayer?.currentPosition ?: 0)
            currentTimeTextView.text = currentTime

            // Esegui il Runnable ogni 500ms per aggiornare la SeekBar
            handler.postDelayed(runnable!!, 500)
        }

        // Avvia l'aggiornamento
        handler.postDelayed(runnable!!, 0)

        // Ferma l'aggiornamento quando l'audio termina
        mediaPlayer?.setOnCompletionListener {
            handler.removeCallbacks(runnable!!)
            seekBar.progress = seekBar.max
            currentTimeTextView.text = formatTime(seekBar.max)
            isPlaying = false
            isPaused = false
            btnPlay.setImageResource(R.drawable.ic_play)
        }
    }

    private fun saveRecording() {
        val mp3FilePath = audioFilePath.replace(".aac", ".mp3")
        val command = arrayOf("-i", audioFilePath, mp3FilePath)
        FFmpeg.executeAsync(command) { _, returnCode ->
            if (returnCode == com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS) {
                saveMetaData(mp3FilePath)
                scheduleUpload(mp3FilePath)
                File(audioFilePath).delete()
            } else {
                Toast.makeText(this, "Failed to convert file", Toast.LENGTH_LONG).show()
            }
        }
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
            .build()

        WorkManager.getInstance(this).enqueue(uploadRequest)
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
            Misc.coordToAddr(
                this@AudioRecorderActivity,
                intent.getDoubleExtra("latitude", 0.0),
                intent.getDoubleExtra("longitude", 0.0)
            ) { location -> put("location", location) }
            put("bpm", -1)
            put("danceability", -1)
            put("loudness", -1)
            put("hidden", true)
        }
        File(metadataFilePath).writeText(metadata.toString())
    }

    private fun formatTime(milliseconds: Int): String {
        val minutes = (milliseconds / 1000) / 60
        val seconds = (milliseconds / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun updateTotalTime(path: String) {
        val totalTimeTextView = findViewById<TextView>(R.id.tv_total_time)
        val player = MediaPlayer()
        try {
            player.setDataSource(path)
            player.prepare()
            val duration = player.duration
            totalTimeTextView.text = formatTime(duration)
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            player.release()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        mediaRecorder?.release()
        mediaRecorder = null
    }
}
