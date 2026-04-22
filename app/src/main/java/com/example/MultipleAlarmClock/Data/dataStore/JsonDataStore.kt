package com.example.MultipleAlarmClock.Data.dataStore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore

val Context.dataStore: DataStore<Settings> by dataStore(
	fileName = "settings.json",
	serializer = SettingsSerializer,
)
