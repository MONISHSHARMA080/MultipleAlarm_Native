package com.coolApps.MultipleAlarmClock.di

import android.app.AlarmManager
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import androidx.room.Room
import com.coolApps.MultipleAlarmClock.Data.dataStore.Settings
import com.coolApps.MultipleAlarmClock.Data.dataStore.SettingsSerializer
import com.coolApps.MultipleAlarmClock.ErrorHandling.ErrorHandler
import com.coolApps.MultipleAlarmClock.Hilt.AppModule
import com.coolApps.MultipleAlarmClock.alarmFeature.data.local.AlarmDao
import com.coolApps.MultipleAlarmClock.alarmFeature.data.local.AlarmDatabase
import com.coolApps.MultipleAlarmClock.analytics.Analytics
import com.coolApps.MultipleAlarmClock.notification.NotificationHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import jakarta.inject.Singleton

@Module
@TestInstallIn(
	components = [SingletonComponent::class],
	replaces = [AppModule::class]
)
object TestAppModule {

	@Provides
	@Singleton
	fun provideDatabase(
			@ApplicationContext context: Context
	): AlarmDatabase =
		Room.inMemoryDatabaseBuilder(
			context,
			AlarmDatabase::class.java
		)
			.allowMainThreadQueries()
			.build()

	@Provides
	fun provideAlarmDao(
			db: AlarmDatabase
	): AlarmDao =
		db.alarmDao()

	@Provides
	@Singleton
	fun provideProtoDataStore(
			@ApplicationContext context: Context
	): DataStore<Settings> =
		DataStoreFactory.create(
			serializer = SettingsSerializer,
			produceFile = {
				context.dataStoreFile("test_user_settings.pb")
			}
		)

	@Provides
	fun provideAlarmManager(
			@ApplicationContext context: Context
	): AlarmManager =
		context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

	@Provides
	@Singleton
	fun provideAnalytics(
			@ApplicationContext context: Context
	): Analytics =
		Analytics(context)

	@Provides
	fun provideNotificationHandler(
			@ApplicationContext context: Context
	): NotificationHandler =
		NotificationHandler(context)

	@Provides
	fun provideErrorHandler(
			notificationHandler: NotificationHandler,
			analytics: Analytics
	): ErrorHandler =
		ErrorHandler(
			notificationHandler,
			analytics
		)
}

///**
// * Re-provides everything AppModule provided except AlarmRepository,
// * which is replaced by the @BindValue fake below.
// */
//
//@Module
//@InstallIn(SingletonComponent::class)
//object TestAppModule {
//
//	@Provides
//	@Singleton
//	fun provideDatabase(@ApplicationContext context: Context): AlarmDatabase =
//		Room.inMemoryDatabaseBuilder(context, AlarmDatabase::class.java)
//			.allowMainThreadQueries()
//			.build()
//
//	@Provides
//	fun provideAlarmDao(db: AlarmDatabase): AlarmDao = db.alarmDao()
//
//
////	@AppModule.IoDispatcher
////	@Provides
////	fun provideIoDispatcher(): CoroutineDispatcher = UnconfinedTestDispatcher()
//
//
//	@Provides
//	@Singleton
//	fun provideProtoDataStore(@ApplicationContext context: Context): DataStore<Settings> =
//		DataStoreFactory.create(
//			serializer = SettingsSerializer,
//			produceFile = { context.dataStoreFile("test_user_settings.pb") },
//		)
//
//	@Provides
//	fun provideAlarmManager(@ApplicationContext context: Context): AlarmManager =
//		context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
//
//	@Provides
//	@Singleton
//	fun provideAnalytics(@ApplicationContext context: Context): Analytics =
//		Analytics(context)
//
//	@Provides
//	fun provideNotificationHandler(@ApplicationContext context: Context): NotificationHandler =
//		NotificationHandler(context)
//
//	@Provides
//	fun provideErrorHandler(
//			notificationHandler: NotificationHandler,
//			analytics: Analytics,
//	): ErrorHandler = ErrorHandler(notificationHandler, analytics)
//}
