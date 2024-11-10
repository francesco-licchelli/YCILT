package com.example.ycilt.others_audio

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ycilt.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioInfoScreen(audioDetails: AudioDetails) {
	val context: Context = LocalContext.current
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
			Column(
				modifier = Modifier
					.fillMaxSize()
					.padding(paddingValues)
					.verticalScroll(rememberScrollState())
					.padding(16.dp)
			) {
				// Audio details
				Text(
					text = audioDetails.creator,
					textAlign = TextAlign.Center,
					modifier = Modifier.fillMaxWidth(),
					style = MaterialTheme.typography.titleLarge
				)
				Text(
					text = stringResource(R.string.bpm, audioDetails.bpm),
					textAlign = TextAlign.Center,
					modifier = Modifier.fillMaxWidth(),
					style = MaterialTheme.typography.bodyLarge
				)
				Text(
					text = stringResource(R.string.danceability, audioDetails.danceability),
					textAlign = TextAlign.Center,
					modifier = Modifier.fillMaxWidth(),
					style = MaterialTheme.typography.bodyLarge
				)
				Text(
					text = stringResource(R.string.loudness, audioDetails.loudness),
					textAlign = TextAlign.Center,
					modifier = Modifier.fillMaxWidth(),
					style = MaterialTheme.typography.bodyLarge
				)
				Spacer(modifier = Modifier.padding(16.dp))
				Column(
					modifier = Modifier.fillMaxWidth(),
				) {
					SectionContent(
						items = audioDetails.mood.getTopN(5),
						label = R.string.mood
					)
					SectionContent(
						items = audioDetails.genre.getTopN(5),
						label = R.string.genre
					)
					SectionContent(
						items = audioDetails.instrument.getTopN(5),
						label = R.string.instruments
					)
				}
			}
		})
}


@Composable
fun SectionContent(items: List<Pair<String, Float>>, label: Int) {
	Column(
		modifier = Modifier
			.height(140.dp)  // You can adjust the height as needed
			.padding(8.dp),
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Text(
			text = stringResource(label),
			textAlign = TextAlign.Center,
			modifier = Modifier.fillMaxWidth(),
			style = MaterialTheme.typography.titleLarge
		)
		LazyColumn {
			items(items) { (item, value) ->
				Text(
					text = stringResource(R.string.cat_displayer, item, value),
					modifier = Modifier.fillMaxWidth(),
					textAlign = TextAlign.Center,
				)
			}
		}
	}
}