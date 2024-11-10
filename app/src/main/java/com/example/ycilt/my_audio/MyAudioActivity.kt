package com.example.ycilt.my_audio

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ycilt.R
import com.example.ycilt.utils.AudioInfoSaver.updateMetadataFromBackend
import com.example.ycilt.utils.Keys.IS_LOGGED
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File

class MyAudioActivity : ComponentActivity() {

	private val isFetchingSongs = mutableStateOf(true)
	private var isLoggedIn = false
	private val audios = mutableStateOf(emptyList<Pair<File, JSONObject>>())

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		isLoggedIn = intent.getBooleanExtra(IS_LOGGED, false)

		setContent {
			MaterialTheme {
				Surface {
					if (isFetchingSongs.value) {
						LoadingScreen()
					} else if (audios.value.isEmpty()) {
						Text(
							stringResource(R.string.no_uploaded_audio),
							modifier = Modifier.fillMaxSize().padding(8.dp),
							fontSize = 20.sp,
							textAlign = androidx.compose.ui.text.style.TextAlign.Center
						)
					} else {
						AudioList(
							audio = audios.value,
							onAudioClicked = { audio, data ->
								val intent = Intent(this, MyAudioInfoActivity::class.java)
								intent.putExtra(IS_LOGGED, isLoggedIn)
								intent.putExtra("audioName", audio.name)
								intent.putExtra("audioMetadata", data.toString())
								startActivity(intent)
							}
						)
					}
				}
			}
		}
	}

	override fun onResume() {
		super.onResume()
		CoroutineScope(Dispatchers.IO).launch {
			isFetchingSongs.value = true
			audios.value = updateMetadataFromBackend(this@MyAudioActivity, intent)
			isFetchingSongs.value = false
		}
	}
}