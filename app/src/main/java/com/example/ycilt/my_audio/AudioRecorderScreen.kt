package com.example.ycilt.my_audio

import android.app.Activity
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ycilt.R
import com.example.ycilt.utils.AudioPlayer
import com.example.ycilt.utils.Constants.MIN_RECORDING_DURATION_MS
import com.example.ycilt.utils.ToastManager.displayToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioRecorderScreen(
	startRecording: (() -> Unit) -> Unit,
	stopRecording: () -> Unit,
	audioFilename: MutableState<String>,
	saveRecording: suspend () -> Unit
) {
	val context = LocalContext.current
	var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
	var aacFilename: String = ""
	val latestTimestamp = remember { mutableStateOf<String?>(null) }

	var isRecordEnabled by remember { mutableStateOf(true) }
	var isStopEnabled by remember { mutableStateOf(false) }
	val isPlayable = remember { mutableStateOf(false) }
	var isSaveEnabled by remember { mutableStateOf(false) }


	Scaffold(
		topBar = {
			CenterAlignedTopAppBar(
				title = { Text("YCILT") },
				navigationIcon = {
					IconButton(onClick = {
						if (mediaRecorder != null) {
							mediaRecorder?.release()
							mediaRecorder = null
							File(aacFilename).takeIf { it.exists() }?.delete()
						}
						(context as Activity).finish()
					}) {
						Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
					}
				}
			)
		},
		content = { paddingValues ->
			Column(
				modifier = Modifier
					.fillMaxSize()
					.padding(paddingValues)
					.padding(16.dp),
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				Text(
					text = stringResource(R.string.record_new_audio),
					fontSize = 24.sp,
					fontWeight = FontWeight.Bold,
					modifier = Modifier.padding(top = 24.dp)
				)

				// Bottone per iniziare la registrazione
				Button(
					onClick = {
						displayToast(context, context.getString(R.string.starting_recording))
						isPlayable.value = false
						isRecordEnabled = false
						isSaveEnabled = false
						startRecording {
							Handler(Looper.getMainLooper()).postDelayed({
								isStopEnabled = true
							}, MIN_RECORDING_DURATION_MS)
						}
					},
					modifier = Modifier
						.fillMaxWidth()
						.padding(top = 16.dp),
					enabled = isRecordEnabled
				) {
					Text(stringResource(R.string.start_recording))
				}

				// Bottone per fermare la registrazione
				Button(
					onClick = {
						stopRecording()
						isPlayable.value = true
						isRecordEnabled = true
						isStopEnabled = false
						isSaveEnabled = true
					},
					modifier = Modifier
						.fillMaxWidth()
						.padding(top = 8.dp),
					enabled = isStopEnabled
				) {
					Text(stringResource(R.string.stop_recording))
				}

				// Lettore Audio
				AudioPlayer(
					audioFilename = audioFilename,
					isButtonEnabled = isPlayable
				)

				// Bottone per caricare la registrazione
				Button(
					onClick = {
						CoroutineScope(Dispatchers.Main).launch {
							saveRecording()
							isSaveEnabled = false
							isRecordEnabled = true
							isStopEnabled = false
						}
					},
					modifier = Modifier
						.fillMaxWidth()
						.padding(top = 8.dp),
					enabled = isSaveEnabled
				) {
					Text(stringResource(R.string.upload_new_audio))
				}
			}
		}
	)

}

