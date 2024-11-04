package com.example.ycilt.mysongs

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ycilt.R
import org.json.JSONObject
import java.io.File
import java.util.Locale

class SongsAdapter(
	private val songs: List<Pair<File, JSONObject>>,
	private val isLoggedIn: Boolean,
) : RecyclerView.Adapter<SongsAdapter.SongViewHolder>() {

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
		val view = LayoutInflater.from(parent.context)
			.inflate(R.layout.item_song, parent, false)
		return SongViewHolder(view)
	}

	override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
		val (songFile, metadata) = songs[position]
		val durationMillis = metadata.getLong("duration")
		val date = metadata.getString("date")

		holder.songTitle.text = songFile.name
		holder.songDuration.text = formatDuration(durationMillis)
		holder.songTimestamp.text = formatDate(date)

		holder.itemView.setOnClickListener {
			val intent = Intent(holder.itemView.context, MySongInfoActivity::class.java)
			intent.putExtra("is_logged_in", isLoggedIn)
			intent.putExtra("songName", songFile.name)
			intent.putExtra("songMetadata", metadata.toString())
			holder.itemView.context.startActivity(intent)
		}
	}

	override fun getItemCount(): Int = songs.size


	class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
		val songTitle: TextView = itemView.findViewById(R.id.song_title)
		val songDuration: TextView = itemView.findViewById(R.id.song_duration)
		val songTimestamp: TextView = itemView.findViewById(R.id.song_timestamp)
	}

	private fun formatDuration(durationMillis: Long): String {
		val seconds = (durationMillis / 1000) % 60
		val minutes = (durationMillis / 1000) / 60
		return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
	}

	private fun formatDate(date: String): String {
		val year = date.substring(0, 4)
		val month = date.substring(4, 6)
		val day = date.substring(6, 8)
		val hour = date.substring(9, 11)
		val minute = date.substring(11, 13)
		return "$day/$month/$year $hour:$minute"
	}
}