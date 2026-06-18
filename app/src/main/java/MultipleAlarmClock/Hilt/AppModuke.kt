package com.example.MultipleAlarmClock.Hilt

import MultipleAlarmClock.alarmFeature.data.local.AlarmDao
import MultipleAlarmClock.alarmFeature.data.local.AlarmDatabase
import MultipleAlarmClock.alarmFeature.data.local.repository.AlarmRepositoryImpl
import MultipleAlarmClock.alarmFeature.domain.AlarmRepository
import android.app.AlarmManager
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import androidx.room.Room
import com.coolApps.MultipleAlarmClock.analytics.Analytics
import com.example.MultipleAlarmClock.Data.MIGRATION_1_2
import com.example.MultipleAlarmClock.Data.dataStore.Settings
import com.example.MultipleAlarmClock.Data.dataStore.SettingsSerializer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object AppModule {

	@Provides
	@Singleton
	fun provideDatabase(@ApplicationContext context: Context): AlarmDatabase {
		return Room.databaseBuilder(
			context,
			AlarmDatabase::class.java,
			"alarm-database"
		).addMigrations(MIGRATION_1_2).build()
	}

	@Provides
	@Singleton
	fun provideProtoDataStore(@ApplicationContext context: Context): DataStore<Settings> {
		return DataStoreFactory.create(
			serializer = SettingsSerializer,
			produceFile = { context.dataStoreFile("user_settings.pb") },
		)
	}

	@Provides
	fun provideAlarmDao(db: AlarmDatabase): AlarmDao = db.alarmDao()

	@Provides
	@Singleton
	fun provideAlarmRepository(repository: AlarmRepositoryImpl): AlarmRepository = repository

	@Provides
	fun provideTimeProvider(): com.coolApps.MultipleAlarmClock.AlarmLogic.TimeProvider = com.coolApps.MultipleAlarmClock.AlarmLogic.TimeProviderImpl()


	@Provides
	fun provideAlarmManager(@ApplicationContext context: Context): AlarmManager {
		return context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
	}

	@Provides
	@Singleton
	fun provideAnalytics(@ApplicationContext context: Context): Analytics {
		return Analytics(context)
	}
}
