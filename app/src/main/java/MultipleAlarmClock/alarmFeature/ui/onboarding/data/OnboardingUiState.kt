package MultipleAlarmClock.alarmFeature.ui.onboarding.data

import MultipleAlarmClock.alarmFeature.data.local.AlarmData

enum class DisplaySate() {
	Greeting,
	Problem,
	CreateFirstAlarm,
	Permission,
	AlarmResult;

	override fun toString(): String {
		return "DisplayState: " + when(this){
			Greeting -> "Greeting"
			Problem -> "Problem"
			CreateFirstAlarm -> "CreateFirstAlarm"
			Permission -> "Permission"
			AlarmResult -> "AlarmResult"
		} + " "
	}
}


data class OnboardingUiState(
	val displaySate: DisplaySate = DisplaySate.Greeting,
	val askForNotificationPermission: Boolean = false,
	val alarmData: AlarmData? = null
)