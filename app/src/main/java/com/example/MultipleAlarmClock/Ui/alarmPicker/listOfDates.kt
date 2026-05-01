package com.coolApps.MultipleAlarmClock.Components_for_ui_compose.alarmPicker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coolApps.MultipleAlarmClock.logD
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.Calendar

data class DayInWeek(
	val dayOfWeek: DayOfWeek,
	val date: LocalDate,
	val isToday: Boolean
){
	fun getCalendar(): Calendar {
		return Calendar.getInstance().apply {
			timeInMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
		}

	}
}
@Composable fun DateList(onSelect: (Calendar) -> Unit, startDateIndex:Long?, weGood: Boolean, allowSelectingPastDate:Boolean= false ) {
	val today = remember { LocalDate.now() }
	val alarmDate = remember(startDateIndex) {
		startDateIndex
			?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
			?.takeIf { !it.isBefore(today) }  // only use if not in the past
			?: today
	}

	var weekDates: List<DayInWeek> by remember { mutableStateOf(getListOfDatesInThisWeek(alarmDate)) }
	var currentDateIndex by remember { mutableIntStateOf(weekDates.indexOfFirst { it.isToday  }) }
	val initialIndex = remember (startDateIndex){
		val foundIndex = weekDates.indexOfFirst {
			it.date == startDateIndex?.let {dateInstance -> Instant.ofEpochMilli(dateInstance).atZone(ZoneId.systemDefault()).toLocalDate() }
		}
		if (foundIndex == -1) currentDateIndex else foundIndex
	}
	var selectedDateIndex by remember { mutableIntStateOf(initialIndex) }
	var showCalendar by remember { mutableStateOf(false) }
	logD("selected date is $selectedDateIndex and current date index is $currentDateIndex")
	// ** get the date that is set by the user or get the date and if not null then check if it is in the list and if not then default to the current date **
	val listState = rememberLazyListState()
	val coroutineScope = rememberCoroutineScope()
	LaunchedEffect(initialIndex ) { listState.animateScrollToItem(initialIndex) }
		LazyRow(
			state = listState,
			contentPadding = PaddingValues(horizontal = 16.dp),
			horizontalArrangement = Arrangement.spacedBy(10.dp),
			modifier = Modifier.fillMaxWidth()
		) { itemsIndexed(weekDates) { index, date ->
			val isPast = date.date.isBefore(today)
			val isSelectable = allowSelectingPastDate || !isPast
			DateCard(
				date = date.date, isSelected = index == selectedDateIndex, weGood = weGood, isSelectable,
				onClick = {
					logD("clicked on a date component and index:$index and isSelectable:$isSelectable")
					if (isSelectable){
						selectedDateIndex = index
						val dateSelected = weekDates[index]
						val calVersion = dateSelected.getCalendar()
						onSelect(calVersion)
					}
				}
			)
		}
			item{
				AddMoreDatesCard { showCalendar = true }
			}
	}
	if (showCalendar){
		DatePickerModal(onDateSelected = { date ->
			if (date == null) {
				showCalendar = false
				return@DatePickerModal
			}
			val selectedDate = Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault()).toLocalDate()
			weekDates = getListOfDatesInThisWeek(selectedDate)
			currentDateIndex = weekDates.indexOfFirst { it.date == selectedDate }
			selectedDateIndex = currentDateIndex
			logD("selected date is $selectedDate and current date index is $currentDateIndex")
			if (selectedDateIndex != -1) {
				val dateSelected = weekDates[selectedDateIndex]
				val calVersion = dateSelected.getCalendar()
				onSelect(calVersion)
				coroutineScope.launch {
					listState.animateScrollToItem(selectedDateIndex)
				}
			}
			showCalendar = false
		}, onDismiss = {showCalendar = false})
	}

}
@Composable
fun AddMoreDatesCard(onClick: () -> Unit) {
	val colorScheme = MaterialTheme.colorScheme
	Surface(
		onClick = onClick,
		shape = RoundedCornerShape(12.dp),
		color = Color(0xFF1C1F26),
		border = BorderStroke(2.dp, colorScheme.primary),
		modifier = Modifier
			.width(64.dp)
			.height(80.dp)
	) {
		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.Center
		) {
			Icon(
				imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
				contentDescription = "Load more dates",
				tint = colorScheme.primary,
				modifier = Modifier.size(32.dp)
			)
		}
	}
}

@Composable fun DateCard(date: LocalDate, isSelected: Boolean, weGood: Boolean, isEnabled: Boolean , onClick: () -> Unit) {
//	val backgroundColor = if (isSelected) Color(0xFF152A46) else Color(0xFF1C1F26)
//	val backgroundColorIfErrorState = Color( 0xFFde0707)
//	val borderColor = if (isSelected) Color(0xFF1E88E5) else Color(0xFF2C313A)
//	val textColor = if (isSelected) Color.White else Color(0xFF7D8592)
	val colorScheme = MaterialTheme.colorScheme
	val containerColor = when {
		!weGood && isSelected -> colorScheme.error
		isSelected -> colorScheme.primary
		else -> colorScheme.surfaceContainer
	}
	val baseContentColor = if (isEnabled) colorScheme.onSurface else colorScheme.onSurface.copy(alpha = 0.38f)
	val contentColor = when {
		!weGood && isSelected -> colorScheme.onError
		isSelected -> colorScheme.onPrimary
		else -> baseContentColor
	}
	val dayName =  date.dayOfWeek.name.take(3)
	val borderColor = if (isSelected) colorScheme.outline else colorScheme.outlineVariant
	Surface(
		onClick = onClick,
		shape = RoundedCornerShape(12.dp),
		color = containerColor,
		border = BorderStroke(2.dp, borderColor),
		modifier = Modifier
			.width(64.dp)
			.height(80.dp)
	) {
		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.Center,
			modifier = Modifier.padding(8.dp)
		) {
			Text(
				text = dayName,
				style = TextStyle(
					fontSize = 10.sp,
					fontWeight = FontWeight.Bold,
					color = if (isSelected) colorScheme.onPrimary else contentColor
				)
			)
			Spacer(modifier = Modifier.height(4.dp))
			Text(
				text = date.dayOfMonth.toString(),
				style = TextStyle(
					fontSize = 20.sp,
					fontWeight = FontWeight.Bold,
					color = if (isSelected) colorScheme.onPrimary else contentColor
				)
			)
			Text(
				text = date.month.name.take(3),
				style = TextStyle(
					fontSize = 10.sp,
					fontWeight = FontWeight.Medium,
					color = contentColor
				)
			)
		}
	}
}

fun getListOfDatesInThisWeek(startDate: LocalDate = LocalDate.now()): List<DayInWeek> {
	val today = LocalDate.now() // Capture actual today
	val sunday = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
	val saturday = startDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))

	require(saturday.toEpochDay() - sunday.toEpochDay() == 6L) {
		"Week span is incorrect, expected the distance from saturday - sunday to be 6 but got ${saturday.toEpochDay() - sunday.toEpochDay()}"
	}

	val list = (0..6).map { offset ->
		val date = sunday.plusDays(offset.toLong())
		DayInWeek(
			dayOfWeek = date.dayOfWeek,
			date = date,
			isToday = date == today // Compare against ACTUAL today
		)
	}

	require(list.size == 7) { "expected the list size to be 7 but got ${list.size}" }
	require(list.first().dayOfWeek == DayOfWeek.SUNDAY) { "expected the first day of week to be sunday but got ${list.first().dayOfWeek}" }
	require(list.last().dayOfWeek == DayOfWeek.SATURDAY) { "expected the last day of week to be saturday but got ${list.last().dayOfWeek}" }

	return list
}

@Composable fun DatePickerModal(onDateSelected: (Long?) -> Unit, onDismiss: () -> Unit) {
	val datePickerState = rememberDatePickerState(
		selectableDates = object : SelectableDates {
			override fun isSelectableDate(utcTimeMillis: Long): Boolean {
				val today = Calendar.getInstance().apply {
					set(Calendar.HOUR_OF_DAY, 0)
					set(Calendar.MINUTE, 0)
					set(Calendar.SECOND, 0)
					set(Calendar.MILLISECOND, 0)
				}.timeInMillis
				return utcTimeMillis >= today
			}
		}
)
	DatePickerDialog(
		onDismissRequest = onDismiss,
		confirmButton = {
			TextButton(onClick = {
				onDateSelected(datePickerState.selectedDateMillis)
				onDismiss()
			}) {
				Text("OK")
			}
		},
		dismissButton = {
			TextButton(onClick = onDismiss) {
				Text("Cancel")
			}
		}
	) {
		DatePicker(state = datePickerState)
	}
}
