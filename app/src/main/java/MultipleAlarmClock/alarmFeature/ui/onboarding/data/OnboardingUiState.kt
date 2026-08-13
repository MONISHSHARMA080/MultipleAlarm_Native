package MultipleAlarmClock.alarmFeature.ui.onboarding.data

enum class DisplaySate {
	Greeting,
	Problem,
	CreateFirstAlarm,
	Permission,
	AlarmResult
}

data class OnboardingUiState(
	val displaySate: DisplaySate = DisplaySate.Greeting,
	val askForNotificationPermission: Boolean = false
)