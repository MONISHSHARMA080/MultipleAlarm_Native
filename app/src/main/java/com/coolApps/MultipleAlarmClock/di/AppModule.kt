package com.coolApps.MultipleAlarmClock.di

import android.app.AlarmManager
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import androidx.room.Room
import com.coolApps.MultipleAlarmClock.data.preferences.Settings
import com.coolApps.MultipleAlarmClock.data.preferences.SettingsSerializer
import com.coolApps.MultipleAlarmClock.ErrorHandling.ErrorHandler
import com.coolApps.MultipleAlarmClock.data.local.AlarmDao
import com.coolApps.MultipleAlarmClock.data.local.AlarmDatabase
import com.coolApps.MultipleAlarmClock.data.local.MIGRATION_1_2
import com.coolApps.MultipleAlarmClock.data.local.MIGRATION_2_3
import com.coolApps.MultipleAlarmClock.data.local.MIGRATION_3_4
import com.coolApps.MultipleAlarmClock.data.repository.AlarmRepositoryImpl
import com.coolApps.MultipleAlarmClock.domain.repository.AlarmRepository
import com.coolApps.MultipleAlarmClock.util.Analytics
import com.coolApps.MultipleAlarmClock.util.NotificationHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Qualifier
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers


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
		).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()
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
	fun provideAlarmManager(@ApplicationContext context: Context): AlarmManager {
		return context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
	}

	@Provides
	@Singleton
	fun provideAnalytics(@ApplicationContext context: Context): Analytics {
		return Analytics(context)
	}

	@Provides
	fun provideNotificationHandler(@ApplicationContext context: Context): NotificationHandler {
		return NotificationHandler(context)
	}

	@Provides
	fun provideErrorHandler(notificationHandler: NotificationHandler, analytics: Analytics): ErrorHandler {
		return ErrorHandler(notificationHandler, analytics)
	}
}
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {

	@Provides
	@IoDispatcher
	fun provideIoDispatcher(): CoroutineDispatcher =
		Dispatchers.IO
}