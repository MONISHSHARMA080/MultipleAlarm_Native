package com.coolApps.MultipleAlarmClock.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.coolApps.MultipleAlarmClock.data.billing.EntitlementManager
import com.coolApps.MultipleAlarmClock.data.paywall.PaywallEvent
import com.coolApps.MultipleAlarmClock.presentation.logD
import com.coolApps.MultipleAlarmClock.receiver.ScheduledNotificationReceiver
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PeriodType

/**
 * Utility to schedule a local notification one period-unit before the user's
 * free trial expires, using AlarmManager.
 *
 * One period-unit means: 1 day for multi-day trials, 1 hour for
 * sub-day trials, 1 minute for sub-hour trials. E.g. a 7-day trial
 * notifies on the 6th day, a 5-minute trial at the 4th minute.
 *
 * This is a plain object (not Hilt-managed) — call methods directly
 * with a Context, same pattern as [com.coolApps.MultipleAlarmClock.util.OfflineNotificationScheduler].
 */
object TrialReminderScheduler {

	private const val TRIAL_REMINDER_REQUEST_CODE = 99882
	private const val ONE_DAY_MS = 24 * 60 * 60 * 1000L
	private const val ONE_HOUR_MS = 60 * 60 * 1000L
	private const val ONE_MINUTE_MS = 60 * 1000L
	private const val PREMIUM_ENTITLEMENT = EntitlementManager.RevenueCatEntitlements.PREMIUM

	/**
	 * Checks if the user is currently on a free trial via [PaywallEvent.PurchaseCompleted]
	 * (falling back to [customerInfo] for callers without a purchase event,
	 * e.g. rescheduling after boot).
	 * If so, schedules a reminder notification one period-unit before the trial expires.
	 * If the user is not on a trial, or the reminder time has already passed,
	 * this is a no-op.
	 */
	fun scheduleIfOnTrial(
		context: Context,
		customerInfo: CustomerInfo,
	) {
		val appContext = context.applicationContext
		val entitlement = customerInfo.entitlements.active[PREMIUM_ENTITLEMENT]

		// Not on a trial, or the user already turned off auto-renew: no "you'll be charged" reminder.
		if (entitlement == null ||
			entitlement.periodType != PeriodType.TRIAL ||
			!entitlement.willRenew
		) {
			cancelReminder(appContext)
			return
		}

		val expiresAt = entitlement.expirationDate?.time ?: return
		val trialDurationMs = expiresAt - entitlement.latestPurchaseDate.time
		val reminderAt = expiresAt - reminderOffsetMs(trialDurationMs)

		if (reminderAt <= System.currentTimeMillis()) return
		scheduleAlarm(appContext, reminderAt)

	}
	private fun reminderOffsetMs(trialDurationMs: Long): Long = when {
		trialDurationMs >= ONE_DAY_MS -> ONE_DAY_MS      // 3d+ trial -> 1 day before
		trialDurationMs >= ONE_HOUR_MS -> ONE_HOUR_MS    // sub-day   -> 1 hour before
		else -> ONE_MINUTE_MS                            // sub-hour (Play test mode) -> 1 min before
	}

	fun cancelReminder(context: Context) {
		logD("in cancel reminder")
		val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
		val pendingIntent = buildPendingIntent(context, PendingIntent.FLAG_NO_CREATE) ?: return
		alarmManager.cancel(pendingIntent)
		pendingIntent.cancel()
	}

	private fun scheduleAlarm(context: Context, triggerAtMillis: Long) {
		val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
		cancelReminder(context) // cancel any existing one first
		logD("in TrialReminderScheduler.scheduleAlarm, and trigger time is $triggerAtMillis")
		val pendingIntent = buildPendingIntent(context, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
		alarmManager.setAndAllowWhileIdle(
			AlarmManager.RTC_WAKEUP,
			triggerAtMillis,
			pendingIntent,
		)
	}

	private fun buildPendingIntent(context: Context, extraFlags: Int): PendingIntent? {
		val intent = Intent(context, ScheduledNotificationReceiver::class.java).apply {
			action = ScheduledNotificationReceiver.ACTION_TRIAL_REMINDER
		}
		return PendingIntent.getBroadcast(
			context,
			TRIAL_REMINDER_REQUEST_CODE,
			intent,
			extraFlags or PendingIntent.FLAG_IMMUTABLE,
		)
	}
}
