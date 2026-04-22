package com.example.MultipleAlarmClock.Hilt

import android.app.AlarmManager
import android.content.Context
import androidx.room.Room
import com.coolApps.MultipleAlarmClock.analytics.Analytics
import com.coolApps.MultipleAlarmClock.dataBase.AlarmDao
import com.coolApps.MultipleAlarmClock.dataBase.AlarmDatabase
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
		).build()
	}

	@Provides
	fun provideAlarmDao(db: AlarmDatabase): AlarmDao = db.alarmDao()

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
