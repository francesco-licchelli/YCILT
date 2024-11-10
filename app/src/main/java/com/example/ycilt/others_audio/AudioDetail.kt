package com.example.ycilt.others_audio

import com.example.ycilt.utils.jsonToCat
import org.json.JSONObject

data class Category(val options: Map<String, Float>) {
	fun getTopN(n: Int = 5): List<Pair<String, Float>> {
		return options.entries
			.sortedByDescending { it.value }
			.take(n)
			.map { it.key to it.value * 100 }
	}
}

data class AudioDetails(
	val creator: String,
	val latitude: Double,
	val longitude: Double,
	val bpm: Int,
	val danceability: Double,
	val loudness: Double,
	val mood: Category,
	val genre: Category,
	val instrument: Category,
) {
	constructor(audioJson: JSONObject) : this(
		creator = audioJson.getString("creator_username"),
		latitude = audioJson.getDouble("latitude"),
		longitude = audioJson.getDouble("longitude"),
		bpm = audioJson.getJSONObject("tags").getInt("bpm"),
		/*
			danceability, secondo le specifiche, dovrebbe essere un valore compreso in [0, 1].
			Tuttavia, alcuni audio gia' presenti nel backend hanno valori maggiori di 1.
		*/
		danceability = audioJson.getJSONObject("tags").getDouble("danceability") * 100,
		loudness = audioJson.getJSONObject("tags").getDouble("loudness"),
		mood = jsonToCat(audioJson.getJSONObject("tags").getJSONObject("mood")),
		genre = jsonToCat(audioJson.getJSONObject("tags").getJSONObject("genre")),
		instrument = jsonToCat(audioJson.getJSONObject("tags").getJSONObject("instrument"))
	)

}
