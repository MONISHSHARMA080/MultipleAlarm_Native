package MultipleAlarmClock.alarmFeature.ui.util

import java.text.SimpleDateFormat
import java.util.Locale

object AlarmFormatter {
	fun formatDateTime(timeMs: Long): String =
		SimpleDateFormat("hh:mm a dd/MM/yyyy", Locale.getDefault()).format(timeMs)

	fun formatDate(timeMs: Long): String =
		SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(timeMs).trim()
}