@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ycilt.my_audio

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ycilt.utils.formatDuration
import org.json.JSONObject
import java.io.File

@Composable
fun AudioList(
	audio: List<Pair<File, JSONObject>>,
	onAudioClicked: (File, JSONObject) -> Unit
) {
	val context = LocalContext.current
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
			LazyColumn(
				modifier = Modifier
					.fillMaxSize()
					.padding(paddingValues)
			) {
				items(audio.size) { index ->
					AudioItem(
						audioFile = audio[index].first,
						metadata = audio[index].second,
						onClick = { onAudioClicked(audio[index].first, audio[index].second) }
					)
				}
			}
		})
}

@Composable
fun AudioItem(
	audioFile: File,
	metadata: JSONObject,
	onClick: () -> Unit
) {
	val durationMillis = metadata.getLong("duration")
	val date = metadata.getString("date")

	Column(
		modifier = Modifier
			.fillMaxWidth()
			.clickable(onClick = onClick)
	) {
		Text(
			text = audioFile.name,
			style = MaterialTheme.typography.titleMedium,
			modifier = Modifier.padding(bottom = 8.dp, start = 8.dp, end = 8.dp)
		)

		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(bottom = 8.dp, start = 16.dp, end = 16.dp)
		) {
			Text(
				text = formatDate(date),
				modifier = Modifier.weight(1f),
				style = MaterialTheme.typography.bodyMedium
			)

			Text(
				text = formatDuration(durationMillis),
				style = MaterialTheme.typography.bodyMedium
			)
		}

		HorizontalDivider(color = Color.Gray, thickness = 1.dp)
	}
}


@Composable
fun LoadingScreen() {
	Box(
		modifier = Modifier.fillMaxSize(),
		contentAlignment = Alignment.Center
	) {
		CircularProgressIndicator()
	}
}


fun formatDate(date: String): String {
	val year = date.substring(0, 4)
	val month = date.substring(4, 6)
	val day = date.substring(6, 8)
	val hour = date.substring(9, 11)
	val minute = date.substring(11, 13)
	return "$day/$month/$year $hour:$minute"
}
