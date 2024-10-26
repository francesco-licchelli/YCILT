package com.example.ycilt.utils

import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import com.example.ycilt.R

class AudioPlayerManager(
    private var audioFilePath: String,
    private val playButton: ImageButton,
    private val seekBar: SeekBar,
    private val currentTimeView: TextView,
    private val totalTimeView: TextView,
    var canPlay: Boolean = true
) {
    var mediaPlayer: MediaPlayer? = null
    var isPlaying: Boolean = false
    private val handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null


    fun startListener() {
        playButton.setOnClickListener {
            Log.d("AudioPlayerManager", "Play button clicked")
            if (canPlay) {
                togglePlayback()
            }
        }
    }

    fun togglePlayback() {
        if (isPlaying) {
            pausePlayback()
        } else {
            startPlayback()
        }
        isPlaying = !isPlaying
    }

    fun setPlayability(playability: Boolean) {
        canPlay = playability
        playButton.setAlpha(if (playability) 1.0f else 0.5f)
        updateStates(playability)
    }

    fun startPlayback() {
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

    fun pausePlayback() {
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

    fun updateSeekBar() {
        seekBar.max = mediaPlayer?.duration ?: 0
        runnable = Runnable {
            seekBar.progress = mediaPlayer?.currentPosition ?: 0
            currentTimeView.text = formatTime(mediaPlayer?.currentPosition ?: 0)
            handler.postDelayed(runnable!!, 100)
        }
        handler.postDelayed(runnable!!, 0)

        mediaPlayer?.setOnCompletionListener {
            handler.removeCallbacks(runnable!!)
            seekBar.progress = seekBar.max
            currentTimeView.text = formatTime(seekBar.max)
        }
    }

    fun updateStates(enabled: Boolean) {
        playButton.isEnabled = enabled
        playButton.setAlpha(if (!enabled) 0.5f else 1.0f)
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
        return String.format("%02d:%02d", minutes, seconds)
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
