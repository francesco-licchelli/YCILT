package com.example.ycilt.my_audio

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ycilt.R
import com.example.ycilt.utils.AudioPlayer
import com.example.ycilt.utils.Misc
import com.example.ycilt.utils.Privacy.PRIVATE_AUDIO
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAudioInfoScreen(
	hiddenStatus: MutableIntState,
	audioFilename: String,
	audioMetadata: JSONObject,
	canDeleteAudio: Boolean,
	canChangePrivacy: Boolean,
	canShowInfo: Boolean,
	onDeleteAudio: () -> Unit,
	onChangePrivacy: () -> Unit,
	onShowInfo: () -> Unit,
) {
	val context = LocalContext.current
	val audioFilenameState =
		remember { mutableStateOf("${context.filesDir.absolutePath}/$audioFilename") }
	val latitude = audioMetadata.getDouble("latitude")
	val longitude = audioMetadata.getDouble("longitude")

	val locationInfo = remember { mutableStateOf<String?>(null) }

	LaunchedEffect(latitude, longitude) {
		Misc.coordToAddr(context, latitude, longitude) { address ->
			locationInfo.value = address
		}
	}

	Scaffold(
		topBar = {
			CenterAlignedTopAppBar(
				title = { Text("YCILT") },
				navigationIcon = {
					IconButton(onClick = {
						(context as Activity).finish()
					}) {
						Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
					}
				}
			)
		},
		content = { paddingValues ->
			Column(modifier = Modifier.padding(16.dp)) {
				Text(
					text = audioFilename,
					modifier = Modifier
						.fillMaxWidth()
						.padding(paddingValues),
					style = MaterialTheme.typography.titleMedium
				)
				Spacer(modifier = Modifier.height(16.dp))

				AudioPlayer(
					audioFilename = audioFilenameState,
					isButtonEnabled = remember { mutableStateOf(true) }
				)
				Spacer(modifier = Modifier.height(16.dp))

				LocationInfoView(
					latitude = latitude,
					longitude = longitude,
					address = locationInfo.value
				)
				Spacer(modifier = Modifier.height(16.dp))

				Button(
					modifier = Modifier.fillMaxWidth(),
					enabled = canDeleteAudio,
					onClick = { onDeleteAudio() }) {
					Text(
						text = stringResource(id = R.string.delete_audio),
					)
				}
				Button(
					modifier = Modifier.fillMaxWidth(),
					enabled = canChangePrivacy,
					onClick = { onChangePrivacy() }) {
					Text(
						text =
						if (!canChangePrivacy) stringResource(id = R.string.change_audio_s_privacy)
						else if (hiddenStatus.intValue == PRIVATE_AUDIO) stringResource(id = R.string.make_public)
						else stringResource(id = R.string.make_private)
					)
				}
				Button(
					onClick = { onShowInfo() },
					enabled = canShowInfo,
					modifier = Modifier.fillMaxWidth(),
				) {
					Text(text = stringResource(id = R.string.view_recording_info))
				}

				Spacer(modifier = Modifier.height(16.dp))


			}
		})
}

@Composable
fun LocationInfoView(latitude: Double, longitude: Double, address: String?) {
	val content = address ?: stringResource(id = R.string.latitude_longitude, latitude, longitude)
	Text(
		text = content,
		modifier = Modifier.fillMaxWidth(),
		fontSize = MaterialTheme.typography.titleMedium.fontSize,
		textAlign = TextAlign.Center
	)
}
