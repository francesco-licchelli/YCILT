package com.example.ycilt.mysongs

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ycilt.R
import com.example.ycilt.utils.Constants.NOT_UPLOADED
import com.example.ycilt.utils.NetworkUtils
import com.example.ycilt.utils.NetworkUtils.deleteRequest
import com.example.ycilt.utils.NetworkUtils.postRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream

class MySongInfoActivity : AppCompatActivity() {

    private lateinit var songName: String
    private lateinit var songFile: File
    private lateinit var metadataFile: File
    private lateinit var songMetadata: JSONObject
    private var songId: Int = -1
    private var mediaPlayer: MediaPlayer? = null

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
        findViewById<TextView>(R.id.song_info_metadata).text =
            "Latitude: ${songMetadata.getDouble("latitude")
            }, Longitude: ${songMetadata.getDouble("longitude")}"

        songId = songMetadata.getInt("id")
        if (songId != NOT_UPLOADED) {
            findViewById<Button>(R.id.privacy_button).visibility = View.VISIBLE
        }

        findViewById<Button>(R.id.play_button).setOnClickListener { playSong() }
        findViewById<Button>(R.id.delete_button).setOnClickListener { deleteSong() }
        findViewById<Button>(R.id.privacy_button).setOnClickListener { changePrivacy() }
    }

    private fun playSong() {
        mediaPlayer = MediaPlayer()
        try {
            val fis = FileInputStream(songFile)
            mediaPlayer?.apply {
                setDataSource(fis.fd)
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to play song", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteSong() {
        if (songFile.exists()) songFile.delete()
        if (metadataFile.exists()) metadataFile.delete()
        if (songId != NOT_UPLOADED) {
            CoroutineScope(Dispatchers.IO).launch {
                val (responseCode, _) = deleteRequest(
                    "audio/$songId",
                    authRequest = this@MySongInfoActivity
                )
                withContext(Dispatchers.Main) {
                    if (responseCode != 200) {
                        Toast.makeText(this@MySongInfoActivity, "Failed to delete song", Toast.LENGTH_SHORT).show()
                        return@withContext
                    }
                    // Invia il nome della canzone eliminata come risultato solo se l'eliminazione è riuscita
                    val intent = Intent().apply {
                        putExtra("deletedSongName", songName)
                    }
                    setResult(RESULT_OK, intent)

                    Toast.makeText(this@MySongInfoActivity, "Song deleted", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        } else {
            // Se la canzone non è stata caricata sul backend, restituisci comunque il risultato
            val intent = Intent().apply {
                putExtra("deletedSongName", songName)
            }
            setResult(RESULT_OK, intent)

            Toast.makeText(this, "Song deleted", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun changePrivacy() {
        val newPrivacy = if (songMetadata.getBoolean("hidden")) "public" else "hidden"
        CoroutineScope(Dispatchers.IO).launch {
            val (responseCode, responseBody) = NetworkUtils.getRequest(
                "audio/my/$songId/$newPrivacy",
                authRequest = this@MySongInfoActivity
            )
            withContext(Dispatchers.Main) {
                if (responseCode == 200) {
                    // Aggiorna il metadata
                    songMetadata.put("hidden", !songMetadata.getBoolean("hidden"))
                    metadataFile.writeText(songMetadata.toString())
                    Toast.makeText(this@MySongInfoActivity, "Privacy changed", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MySongInfoActivity, "Failed to change privacy", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }
}
