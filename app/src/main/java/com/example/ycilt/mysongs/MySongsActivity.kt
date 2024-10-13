package com.example.ycilt.mysongs

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ycilt.R
import com.example.ycilt.auth.LoginActivity
import com.example.ycilt.utils.Constants.MISSING_LOCATION
import com.example.ycilt.utils.Misc
import com.example.ycilt.utils.NetworkUtils.getRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException

class MySongsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SongsAdapter
    private val songsList = mutableListOf<Pair<File, JSONObject>>()
    private lateinit var songInfoLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_songs)

        recyclerView = findViewById(R.id.recyclerView_songs)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Inizializza e registra il launcher per ricevere i risultati da MySongInfoActivity
        songInfoLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val deletedSongName = result.data?.getStringExtra("deletedSongName")
                if (deletedSongName != null) {
                    removeSongFromList(deletedSongName)
                }
            }
        }

        //fetchSongsFromBackend() // Chiamata alla funzione per recuperare le canzoni
        Log.d("MySongsActivity", "Songs fetched from local storage: ${getSongs()}")
        displaySongs(getSongs())
        // idea: calcolare diff tra canzoni in backend e locali e caricare le canzoni non ancora caricate
    }

    private fun getAudioDuration(mp3File: File): Long {
        val mediaPlayer = MediaPlayer()
        return try {
            mediaPlayer.setDataSource(mp3File.absolutePath)
            mediaPlayer.prepare()
            val duration = mediaPlayer.duration.toLong()
            mediaPlayer.release()
            duration
        } catch (e: IOException) {
            0L
        }
    }

    fun getAllJsonFiles(): List<File> {
        return filesDir.listFiles()?.filter { it.extension == "json" } ?: emptyList()
    }

    fun readJsonFromFile(file: File): List<JSONObject> {
        val jsonList = mutableListOf<JSONObject>()
        try {
            val jsonString = file.readText() // Leggi il contenuto del file
            Log.d("MySongsActivity", "Json string: $jsonString")
            jsonList.add(JSONObject(jsonString))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return jsonList
    }

    private fun getSongs(): List<Pair<File, JSONObject>> {
        CoroutineScope(Dispatchers.IO).launch {
            val (responseCode, responseBody) = getRequest(
                "audio/my",
                authRequest = this@MySongsActivity,
            )
            if (responseCode != 200) {
                Log.d("MySongsActivity", "Failed to load songs: $responseCode")
                val intent = Intent(this@MySongsActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            //load all json from backend
            val jsonFiles = getAllJsonFiles()

            val localData = mutableListOf<JSONObject>()

            for (file in jsonFiles) {
                val jsonObjectsFromFile = readJsonFromFile(file)
                localData.addAll(jsonObjectsFromFile)
            }
            Log.d("MySongsActivity", "Local data: $localData")

            val remoteData = JSONArray(responseBody)
            Log.d("MySongsActivity", "Remote data: $remoteData")

            for (i in 0 until remoteData.length()) {
                if (
                    localData[i].getInt("id") !=
                    remoteData.getJSONObject(i).getInt("id")
                    ) {
                    val metadataFile = File(
                        localData[i].getString("file_name").replace(".mp3", "_metadata.json")
                    )
                    metadataFile.writeText(localData[i].toString())
                }
            }

            Log.d("MySongsActivity", "Response code: $responseCode")
            Log.d("MySongsActivity", "Response body: $responseBody")
        }
        val audioDir = filesDir
        songsList.clear()

        audioDir.listFiles { file ->
            file.extension == "mp3"
        }?.forEach { mp3File ->
            val metadataFile = File(mp3File.absolutePath.replace(".mp3", "_metadata.json"))
            if (metadataFile.exists()) {
                val metadata = JSONObject(metadataFile.readText())
                val duration = getAudioDuration(mp3File)
                val date = mp3File.nameWithoutExtension.split("record_").lastOrNull() ?: "Unknown"
                metadata.put("duration", duration)
                metadata.put("date", date)
                songsList.add(Pair(mp3File, metadata))
            }
        }

        return songsList
    }

    private fun displaySongs(songs: List<Pair<File, JSONObject>>) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                Log.d("MySongsActivity", "Displaying songs: $songs")
                if (songs.isEmpty()) {
                    findViewById<TextView>(R.id.no_songs_message).visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    findViewById<TextView>(R.id.no_songs_message).visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    adapter = SongsAdapter(songsList) { updatedSong ->
                        updateSongVisibility(updatedSong)
                    }
                    recyclerView.adapter = adapter
                }
            }
        }
    }

    /* private fun fetchSongsFromBackend() {
        CoroutineScope(Dispatchers.IO).launch {
            val (responseCode, responseBody) = getRequest(
                "audio/my",
                authRequest = this@MySongsActivity,
            )
            try {
                val responseArray = JSONArray(responseBody)
                val mySongList = mutableListOf<MySong>()

                for (i in 0 until responseArray.length()) {
                    val songJson = responseArray.getJSONObject(i)
                    val mySong = MySong(
                        id = songJson.getInt("id"),
                        latitude = songJson.getDouble("latitude"),
                        longitude = songJson.getDouble("longitude"),
                        hidden = songJson.getBoolean("hidden")
                    )
                    mySongList.add(mySong)
                }
            } catch (e: JSONException){
                val responseJson = JSONObject(responseBody)
                withContext(Dispatchers.Main) {
                    Log.d("MySongsActivity", "Failed to load songs: $responseCode")
                    Toast.makeText(
                        this@MySongsActivity,
                        responseJson.getString("detail"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: JSONException) {
                withContext(Dispatchers.Main) {
                    Log.d("MySongsActivity", "Failed to load songs: $responseCode")
                    Toast.makeText(
                        this@MySongsActivity,
                        "Failed to load songs: $responseCode",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }*/

    private fun updateSongVisibility(updatedMySong: MySong) {
        CoroutineScope(Dispatchers.IO).launch {
            val (responseCode, responseBody) = getRequest(
                "audio/my/${updatedMySong.id}" + if (updatedMySong.hidden) "/show" else "/hidden",
                authRequest = this@MySongsActivity,
            )
            Log.d("MySongsActivity", "Response body: $responseBody")
            if (responseCode != 200) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MySongsActivity,
                        "$responseCode, $responseBody",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                updatedMySong.hidden = !updatedMySong.hidden // Cambia lo stato
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MySongsActivity,
                        "Song updated successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun removeSongFromList(deletedSongName: String) {
        val songIndex = songsList.indexOfFirst { it.first.name == deletedSongName }
        if (songIndex != -1) {
            songsList.removeAt(songIndex)
            adapter.notifyItemRemoved(songIndex)
            if (songsList.isEmpty()) {
                findViewById<TextView>(R.id.no_songs_message).visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            }
        }
    }
}
