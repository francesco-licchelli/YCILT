package com.example.ycilt.my_audio

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ycilt.R
import com.example.ycilt.utils.Keys.IS_LOGGED
import org.json.JSONObject
import java.io.File
import java.util.Locale

class AudioAdapter(
	private val audio: List<Pair<File, JSONObject>>,
	private val isLoggedIn: Boolean,
) : RecyclerView.Adapter<AudioAdapter.AudioViewHolder>() {

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioViewHolder {
		val view = LayoutInflater.from(parent.context)
			.inflate(R.layout.item_audio, parent, false)
		return AudioViewHolder(view)
	}

	override fun onBindViewHolder(holder: AudioViewHolder, position: Int) {
		val (audioFile, metadata) = audio[position]
		val durationMillis = metadata.getLong("duration")
		val date = metadata.getString("date")

		holder.audioTitle.text = audioFile.name
		holder.audioDuration.text = formatDuration(durationMillis)
		holder.audioTimestamp.text = formatDate(date)

		holder.itemView.setOnClickListener {
			val intent = Intent(holder.itemView.context, MyAudioInfoActivity::class.java)
			intent.putExtra(IS_LOGGED, isLoggedIn)
			intent.putExtra("audioName", audioFile.name)
			intent.putExtra("audioMetadata", metadata.toString())
			holder.itemView.context.startActivity(intent)
		}
	}

	override fun getItemCount(): Int = audio.size


	class AudioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
		val audioTitle: TextView = itemView.findViewById(R.id.audio_title)
		val audioDuration: TextView = itemView.findViewById(R.id.audio_duration)
		val audioTimestamp: TextView = itemView.findViewById(R.id.audio_timestamp)
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