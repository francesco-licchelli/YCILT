package com.example.ycilt.utils

import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import com.example.ycilt.R
import java.util.Locale

class AudioPlayerManager(
	private var audioFilePath: String,
	private val playButton: ImageButton,
	private val seekBar: SeekBar,
	private val currentTimeView: TextView,
	private val totalTimeView: TextView,
	var canPlay: Boolean = true
) {
	private var mediaPlayer: MediaPlayer? = null
	private var isPlaying: Boolean = false
	private val handler = Handler(Looper.getMainLooper())
	private var runnable: Runnable? = null


	fun startListener() {
		playButton.setOnClickListener {
			if (canPlay) {
				togglePlayback()
			}
		}
	}

	private fun togglePlayback() {
		if (isPlaying) {
			pausePlayback()
		} else {
			startPlayback()
		}
		isPlaying = !isPlaying
	}

	private fun startPlayback() {
		if (mediaPlayer == null) {
			mediaPlayer = MediaPlayer().apply {
				setDataSource(audioFilePath)
				prepare()
				start()
			}
		} else {
			mediaPlayer?.start()
		}
		updateSeekBar()
		playButton.setImageResource(R.drawable.ic_pause)
	}

	private fun pausePlayback() {
		mediaPlayer?.pause()
		handler.removeCallbacks(runnable!!)
		playButton.setImageResource(R.drawable.ic_play)
	}

	fun updateAudioFilePath(newAudioFilePath: String) {
		mediaPlayer?.release()
		mediaPlayer = MediaPlayer().apply {
			setDataSource(newAudioFilePath)
			prepare()
		}
		audioFilePath = newAudioFilePath
		updateSeekBar()
		updateTotalTime()
	}

	private fun updateSeekBar() {
		seekBar.max = mediaPlayer?.duration ?: 0
		runnable = Runnable {
			seekBar.progress = mediaPlayer?.currentPosition ?: 0
			currentTimeView.text = formatTime(mediaPlayer?.currentPosition ?: 0)
			handler.postDelayed(runnable!!, 100)
		}
		handler.postDelayed(runnable!!, 0)

		mediaPlayer?.setOnCompletionListener {
			handler.removeCallbacks(runnable!!)
			seekBar.progress = 0
			currentTimeView.text = formatTime(0)
			playButton.setImageResource(R.drawable.ic_play)
			isPlaying = false
		}
	}

	fun updateStates(enabled: Boolean) {
		playButton.isEnabled = enabled
		playButton.alpha = if (!enabled) 0.5f else 1.0f
		seekBar.isEnabled = enabled
		currentTimeView.isEnabled = enabled
		totalTimeView.isEnabled = enabled
	}

	fun updateTotalTime() {
		val duration = mediaPlayer?.duration ?: 0
		totalTimeView.text = formatTime(duration)
	}

	private fun formatTime(milliseconds: Int): String {
		val minutes = (milliseconds / 1000) / 60
		val seconds = (milliseconds / 1000) % 60
		return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
	}

	fun release() {
		mediaPlayer?.release()
		mediaPlayer = null
	}
}
