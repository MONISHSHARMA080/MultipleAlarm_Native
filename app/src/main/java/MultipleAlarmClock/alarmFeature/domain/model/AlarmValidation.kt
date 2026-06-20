package MultipleAlarmClock.alarmFeature.domain.model

enum class AlarmErrorField {
	Time,
	DATE,
	FREQUENCY,
	AlarmIsNotDiff,
}

sealed class ValidationResult {
	object Success : ValidationResult()
	data class Failure(val field: AlarmErrorField, val messageResId: Int) : ValidationResult()

	override fun toString(): String {
		return when(this){
			is Success -> "Success"
			is Failure -> "Failure: field:$field messageResId: $messageResId"
		}
	}
}
