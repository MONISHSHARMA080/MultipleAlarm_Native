package com.coolApps.MultipleAlarmClock.presentation.picker.missions

import androidx.annotation.StringRes
import com.coolApps.MultipleAlarmClock.R

enum class AlarmMission(
	val id: String,
	val analyticsName: String,
	@param:StringRes val titleRes: Int,
	@param:StringRes val descriptionRes: Int
) {
	SHAKE(
		id = "shake",
		analyticsName = "Shake",
		titleRes = R.string.mission_shake_title,
		descriptionRes = R.string.mission_shake_desc
	),
	TYPING(
		id = "typing",
		analyticsName = "Typing",
		titleRes = R.string.mission_typing_title,
		descriptionRes = R.string.mission_typing_desc
	),
	MATH(
		id = "math",
		analyticsName = "Math",
		titleRes = R.string.mission_math_title,
		descriptionRes = R.string.mission_math_desc
	),
	ITEM_HUNT(
		id = "item_hunt",
		analyticsName = "Item hunt",
		titleRes = R.string.mission_item_hunt_title,
		descriptionRes = R.string.mission_item_hunt_desc
	),
	STEPS(
		id = "steps",
		analyticsName = "Steps",
		titleRes = R.string.mission_steps_title,
		descriptionRes = R.string.mission_steps_desc
	),
	PUSH_UPS(
		id = "push_ups",
		analyticsName = "Push-ups",
		titleRes = R.string.mission_push_ups_title,
		descriptionRes = R.string.mission_push_ups_desc
	)
}
