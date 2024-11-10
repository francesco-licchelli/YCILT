package com.example.ycilt.others_audio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.example.ycilt.utils.AudioInfoSaver.parseAudioData
import com.example.ycilt.utils.Constants.NOT_UPLOADED
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class AudioInfoActivity : ComponentActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val audioId = intent.getIntExtra("audioId", NOT_UPLOADED)
		val audioFilename = intent.getStringExtra("audioFilename")

		lifecycleScope.launch(Dispatchers.IO) {
			val audioDetails = parseAudioData(this@AudioInfoActivity, audioId, audioFilename)
			audioDetails?.let {
				runOnUiThread {
					setContent {
						AudioInfoScreen(audioDetails = it)
					}
				}
			}
		}
	}
}

