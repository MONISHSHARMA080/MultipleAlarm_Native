package MultipleAlarmClock.alarmFeature.ui.onboarding.data

import MultipleAlarmClock.alarmFeature.data.local.AlarmData
import com.example.MultipleAlarmClock.Ui.Permissions.PermissionStep

enum class DisplaySate() {
	Greeting,
	Problem,
	CreateFirstAlarm,
	Permission,
	AlarmResult;

	override fun toString(): String {
		return  when(this){
			Greeting -> "Greeting"
			Problem -> "Problem"
			CreateFirstAlarm -> "CreateFirstAlarm"
			Permission -> "Permission"
			AlarmResult -> "AlarmResult"
		}
	}
}


data class OnboardingUiState(
	val displaySate: DisplaySate = DisplaySate.Greeting,
	val askForNotificationPermission: Boolean = false,
	val missingSteps: List<PermissionStep> = emptyList(),
	val alarmData: AlarmData? = null,
	val	allCriticalGranted: Boolean = false
){
	override fun toString(): String {
		return "OnboardingUiState: DisplayState: $displaySate , askForNotificationPermission: $askForNotificationPermission, alarmData: $alarmData, missingSteps: $missingSteps, allCriticalGranted: $allCriticalGranted   "
	}
}