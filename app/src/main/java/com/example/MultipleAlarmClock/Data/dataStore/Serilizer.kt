package com.example.MultipleAlarmClock.Data.dataStore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

object SettingsSerializer : Serializer<Settings> {
	override val defaultValue: Settings = Settings(isFirstLaunch = true)

	/**
	 * @throws SerializationException in case of any decoding-specific error
 	*/
	override suspend fun readFrom(input: InputStream): Settings {
		return try {
			Json.decodeFromString<Settings>(
				input.readBytes().decodeToString()
			)
		} catch (serialization: SerializationException) {
			throw CorruptionException("Unable to read Settings", serialization)
		}
	}

	override suspend fun writeTo(t: Settings, output: OutputStream) {
		withContext(Dispatchers.IO) {
			output.write(
				Json.encodeToString(t)
					.encodeToByteArray()
			)
		}
	}
}