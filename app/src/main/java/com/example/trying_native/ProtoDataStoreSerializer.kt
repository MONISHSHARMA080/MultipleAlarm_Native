package com.example.trying_native.data

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import java.io.InputStream
import java.io.OutputStream
import com.example.trying_native.PremissionDataStore

import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.datastore.preferences.protobuf.InvalidProtocolBufferException

object ProtoDataStoreSerializer : Serializer<PremissionDataStore> {
    override val defaultValue: PremissionDataStore  = PremissionDataStore.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): PremissionDataStore {
        try {
            return PremissionDataStore.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }
    override suspend fun writeTo(
        t: PremissionDataStore,
        output: OutputStream) = t.writeTo(output)

}
val Context.settingsDataStore: DataStore<PremissionDataStore> by dataStore(
    fileName = " dataStore.proto",
    serializer = ProtoDataStoreSerializer,
