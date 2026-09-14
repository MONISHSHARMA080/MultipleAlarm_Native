package com.coolApps.MultipleAlarmClock.notification.trial

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.coolApps.MultipleAlarmClock.alarmFeature.data.billing.EntitlementManager
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PeriodType

/**
 * Utility to schedule a local notification 1 day before the user's
 * free trial expires, using AlarmManager.
 *
 * This is a plain object (not Hilt-managed) — call methods directly
 * with a Context, same pattern as [com.coolApps.MultipleAlarmClock.notification.offline.OfflineNotificationScheduler].
 */
object TrialReminderScheduler {

	private const val TRIAL_REMINDER_REQUEST_CODE = 99882
	private const val ONE_DAY_MS = 24 * 60 * 60 * 1000L
	private const val PREMIUM_ENTITLEMENT = EntitlementManager.RevenueCatEntitlements.PREMIUM

	/**
	 * Checks if the user is currently on a free trial via [CustomerInfo].
	 * If so, schedules a reminder notification 1 day before the trial expires.
	 * If the user is not on a trial, or the reminder time has already passed,
	 * this is a no-op.
	 */
	fun scheduleIfOnTrial(context: Context, customerInfo: CustomerInfo) {
		val entitlement = customerInfo.entitlements[PREMIUM_ENTITLEMENT] ?: return
		if (entitlement.periodType != PeriodType.TRIAL) return
		val trialExpirationDate = entitlement.expirationDate ?: return

		val reminderTimeMs = trialExpirationDate.time - ONE_DAY_MS
		val now = System.currentTimeMillis()

		// Already past the reminder window — don't schedule
		if (reminderTimeMs <= now) return

		scheduleAlarm(context, reminderTimeMs)
	}

	/**
	 * Cancels any previously scheduled trial reminder alarm.
	 */
	fun cancelReminder(context: Context) {
		val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
		val pendingIntent = buildPendingIntent(context, PendingIntent.FLAG_NO_CREATE) ?: return
		alarmManager.cancel(pendingIntent)
		pendingIntent.cancel()
	}

	private fun scheduleAlarm(context: Context, triggerAtMillis: Long) {
		val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
		cancelReminder(context) // cancel any existing one first
		val pendingIntent = buildPendingIntent(context, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
		alarmManager.setAndAllowWhileIdle(
			AlarmManager.RTC_WAKEUP,
			triggerAtMillis,
			pendingIntent,
		)
	}

	private fun buildPendingIntent(context: Context, extraFlags: Int): PendingIntent? {
		val intent = Intent(context, TrialReminderReceiver::class.java).apply {
			action = TrialReminderReceiver.ACTION_TRIAL_REMINDER
		}
		return PendingIntent.getBroadcast(
			context,
			TRIAL_REMINDER_REQUEST_CODE,
			intent,
			extraFlags or PendingIntent.FLAG_IMMUTABLE,
		)
	}
}
