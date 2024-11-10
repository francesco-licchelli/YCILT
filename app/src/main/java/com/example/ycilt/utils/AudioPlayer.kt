package com.example.ycilt.utils

import android.media.MediaPlayer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.ycilt.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun AudioPlayer(
	audioFilename: MutableState<String>,
	isButtonEnabled: State<Boolean>,
	modifier: Modifier = Modifier
) {
	var isPlaying by remember { mutableStateOf(false) }
	var playbackPosition by remember { mutableFloatStateOf(0f) }
	var audioDuration by remember { mutableLongStateOf(0L) }
	val coroutineScope = rememberCoroutineScope()
	var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

	// Gestione Play/Pause
	fun onPlayPauseClick() {
		mediaPlayer?.let {
			if (isPlaying) {
				it.pause()
			} else {
				it.start()
				coroutineScope.launch {
					while (it.isPlaying) {
						playbackPosition = it.currentPosition / audioDuration.toFloat()
						delay(100)
					}
				}
			}
			isPlaying = !isPlaying
		}
	}

	LaunchedEffect(isButtonEnabled.value) {
		if (isButtonEnabled.value) {
			mediaPlayer = MediaPlayer().apply {
				setDataSource(audioFilename.value)
				prepare()
				audioDuration = duration.toLong()

				// Listener per riportare a 0 il playbackPosition e impostare il pulsante su "Play" alla fine della riproduzione
				setOnCompletionListener {
					playbackPosition = 0f
					isPlaying = false
				}
			}
		}
	}

	Column(
		modifier = modifier.padding(16.dp),
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		// Pulsante Play/Pause
		IconButton(
			onClick = { onPlayPauseClick() },
			enabled = isButtonEnabled.value && mediaPlayer != null
		) {
			Icon(
				painter = painterResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
				contentDescription = if (isPlaying) "Pause" else "Play",
				tint = Color.Black
			)
		}

		// SeekBar per la posizione
		Slider(
			value = playbackPosition,
			onValueChange = {
				playbackPosition = it
				mediaPlayer?.seekTo((it * audioDuration).toInt())
			},
			modifier = Modifier.fillMaxWidth(),
			enabled = mediaPlayer != null
		)

		// Durata in basso
		Text(
			text = formatDuration((playbackPosition * audioDuration).toLong()) + " / " + formatDuration(
				audioDuration
			),
			style = MaterialTheme.typography.bodyMedium
		)
	}

	DisposableEffect(Unit) {
		onDispose {
			mediaPlayer?.release()
		}
	}
}

// Funzione di formattazione della durata in minuti:secondi
@Composable
fun formatDuration(durationMs: Long): String {
	val totalSeconds = durationMs / 1000
	val minutes = totalSeconds / 60
	val seconds = totalSeconds % 60
	return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
