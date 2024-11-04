package com.example.ycilt.otherssongs

import com.example.ycilt.utils.Misc
import org.json.JSONObject

data class Category(val options: Map<String, Float>) {
	fun getTopN(n: Int = 5): List<Pair<String, Float>> {
		return options.entries
			.sortedByDescending { it.value }
			.take(n)
			.map { it.key to it.value * 100 }
	}
}

data class SongDetails(
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
	constructor(songJson: JSONObject) : this(
		creator = songJson.getString("creator_username"),
		latitude = songJson.getDouble("latitude"),
		longitude = songJson.getDouble("longitude"),
		bpm = songJson.getJSONObject("tags").getInt("bpm"),
		/*
			danceability, secondo le specifiche, dovrebbe essere un valore compreso in [0, 1].
			Tuttavia, alcuni audio gia' presenti nel backend hanno valori maggiori di 1.
			Ignoro la cosa per il momento
		*/
		danceability = songJson.getJSONObject("tags").getDouble("danceability") * 100,
		loudness = songJson.getJSONObject("tags").getDouble("loudness"),
		mood = Misc.jsonToCat(songJson.getJSONObject("tags").getJSONObject("mood")),
		genre = Misc.jsonToCat(songJson.getJSONObject("tags").getJSONObject("genre")),
		instrument = Misc.jsonToCat(songJson.getJSONObject("tags").getJSONObject("instrument"))
	)

}
