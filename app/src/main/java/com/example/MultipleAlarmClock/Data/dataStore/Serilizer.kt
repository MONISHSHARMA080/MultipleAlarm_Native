package com.example.MultipleAlarmClock.Data.dataStore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

//object SettingsSerializer : Serializer<Settings> {
//	override val defaultValue: Settings = Settings(isFirstLaunch = true, allPermissionsGranted = false)
//
//	/**
//	 * @throws SerializationException in case of any decoding-specific error
// 	*/
//	override suspend fun readFrom(input: InputStream): Settings {
//		return try {
//			Json.decodeFromString<Settings>(
//				input.readBytes().decodeToString()
//			)
//		} catch (serialization: SerializationException) {
//			throw CorruptionException("Unable to read Settings", serialization)
//		}
//	}
//
//	override suspend fun writeTo(t: Settings, output: OutputStream) {
//		withContext(Dispatchers.IO) {
//			output.write(
//				Json.encodeToString(t)
//					.encodeToByteArray()
//			)
//		}
//	}
//}

object SettingsSerializer : Serializer<Settings> {
	// this will run on the first run, like default values
//	override val defaultValue: Settings = Settings.newBuilder().setFirstAlarmSet(false).build()
	override val defaultValue: Settings = settings {
		isFirstLaunch = true
		allPermissionsGranted = false
		firstAlarmSet = false
		firstAlarmNotificationReceived = false
	}
	override suspend fun readFrom(input: InputStream): Settings {
		try {
			return Settings.parseFrom(input)
		} catch (exception: InvalidProtocolBufferException) {
			throw CorruptionException("Cannot read proto.", exception)
		}
	}

	override suspend fun writeTo(t: Settings, output: OutputStream) {
		return t.writeTo(output)
	}
}
