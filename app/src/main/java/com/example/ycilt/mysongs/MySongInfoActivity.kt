package com.example.ycilt.mysongs

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.ycilt.R
import com.example.ycilt.otherssongs.SongInfoActivity
import com.example.ycilt.utils.AudioPlayerManager
import com.example.ycilt.utils.Constants.NOT_UPLOADED
import com.example.ycilt.utils.LocationDisplayer
import com.example.ycilt.utils.Misc
import org.json.JSONObject
import java.io.File

class MySongInfoActivity : AppCompatActivity() {

    private lateinit var songName: String
    private lateinit var songFile: File
    private lateinit var metadataFile: File
    private lateinit var songMetadata: JSONObject
    private var songId: Int = NOT_UPLOADED
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var audioPlayerManager: AudioPlayerManager

    private lateinit var deleteButton: Button
    private lateinit var privateButton: Button
    private lateinit var publicButton: Button
    private lateinit var showInfoButton: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_song_info)

        songName = intent.getStringExtra("songName")!!
        songMetadata = JSONObject(intent.getStringExtra("songMetadata")!!)

        // Recupera il file della canzone dalla memoria interna
        songFile = File(filesDir, songName)
        metadataFile = File(filesDir, songName.replace(".mp3", "_metadata.json"))

        // Imposta i campi UI
        findViewById<TextView>(R.id.song_info_name).text = songName
        deleteButton = findViewById(R.id.delete_button)
        privateButton = findViewById(R.id.privacy_button_public)
        publicButton = findViewById(R.id.privacy_button_private)
        showInfoButton = findViewById(R.id.btn_view_info)


        val locationInfoView = findViewById<LinearLayout>(R.id.locationInfoView)
        val latitude = songMetadata.getDouble("latitude")
        val longitude = songMetadata.getDouble("longitude")


        Misc.coordToAddr(this, latitude, longitude) { address ->
            LocationDisplayer(locationInfoView, latitude, longitude, address)
        }

        songId = songMetadata.getInt("id")

        deleteButton.setOnClickListener { deleteSong() }
        publicButton.setOnClickListener {
            changePrivacy(true)
        }
        privateButton.setOnClickListener {
            changePrivacy(false)
        }

        showInfoButton.setOnClickListener {
            val intent = Intent(this, SongInfoActivity::class.java)
            intent.putExtra("songId", songId)
            startActivity(intent)
        }

        audioPlayerManager = AudioPlayerManager(
            songFile.absolutePath,
            findViewById(R.id.btnPlay),
            findViewById(R.id.audio_seekbar),
            findViewById(R.id.tv_current_time),
            findViewById(R.id.tv_total_time)
        )


        audioPlayerManager.updateAudioFilePath(songFile.absolutePath)
        audioPlayerManager.startListener()

    }

    override fun onResume() {
        super.onResume()
        for (button in listOf(deleteButton, privateButton, publicButton, showInfoButton)) {
            button.isEnabled = intent.getBooleanExtra("is_logged_in", false)
        }
        if (intent.getBooleanExtra("is_logged_in", false)){
            updatePrivacyButtons(songId, songMetadata.getBoolean("hidden"))
            showInfoButton.isEnabled = songId != NOT_UPLOADED
        }
        audioPlayerManager.updateAudioFilePath(songFile.absolutePath)
        audioPlayerManager.startListener()
    }

    private fun updatePrivacyButtons(id: Int, hidden: Boolean) {
        val layout = findViewById<LinearLayout>(R.id.privacy_buttons_lay)
        val publicBtn = findViewById<Button>(R.id.privacy_button_public)
        val privateBtn = findViewById<Button>(R.id.privacy_button_private)
        layout.visibility = if (id != NOT_UPLOADED) View.VISIBLE else View.GONE
        publicBtn.isEnabled = hidden
        privateBtn.isEnabled = !hidden
    }


    private fun scheduleDelete(songId: Int) {
        val inputData = Data.Builder()
            .putInt("songId", songId)
            .build()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val deleteRequest = OneTimeWorkRequestBuilder<DeleteSongWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueue(deleteRequest)
    }

    private fun deleteSong() {
        if (songFile.exists()) songFile.delete()
        if (metadataFile.exists()) metadataFile.delete()
        if (songId != NOT_UPLOADED){
            WorkManager.getInstance(this).cancelAllWorkByTag("upload_song_${songFile.name.hashCode()}")
            scheduleDelete(songId)
        }
        val intent = Intent().apply {
            putExtra("deletedSongName", songName)
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun changePrivacy(hidden: Boolean) {
        val newPrivacy = if (hidden) "show" else "hide"
        val inputData = Data.Builder()
            .putInt("songId", songId)
            .putString("newPrivacy", newPrivacy)
            .build()
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val editPrivacyRequest = OneTimeWorkRequestBuilder<EditPrivacyWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueue(editPrivacyRequest)
        songMetadata.put("hidden", hidden)
        metadataFile.writeText(songMetadata.toString())
        updatePrivacyButtons(songId, hidden)
    }

    override fun onDestroy() {
        super.onDestroy()
        audioPlayerManager.release()
        mediaPlayer?.release()
    }
}
