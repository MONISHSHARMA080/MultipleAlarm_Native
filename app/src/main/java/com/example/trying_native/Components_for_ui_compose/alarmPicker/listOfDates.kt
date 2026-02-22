package com.example.trying_native.Components_for_ui_compose.alarmPicker

import android.icu.util.LocaleData
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trying_native.logD
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.Instant
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
			timeInMillis = date
				.atStartOfDay(ZoneId.systemDefault())
				.toInstant()
				.toEpochMilli()
		}

	}
}
//@Composable fun DisplayDatesOfWeekDays(){}

@Composable fun DateList(weekDates: List<DayInWeek>,  onSelect: (Int) -> Unit, startDateIndex:Long?, allowSelectingPastDate:Boolean= false ) {
	// handling past dates and selecting today's date by default and future date is my problem
	val currentDateIndex = weekDates.indexOfFirst { it.isToday  }
	require(currentDateIndex != -1,{"expected to find a current date in the list returned but got it to be false "} )
// 1. Calculate the initial index based on the timestamp or fallback to today
	val initialIndex = remember (startDateIndex){
		val foundIndex = weekDates.indexOfFirst {
			it.date == startDateIndex?.let {dateInstance -> Instant.ofEpochMilli(dateInstance).atZone(ZoneId.systemDefault()).toLocalDate() }
		}
		if (foundIndex == -1) currentDateIndex else foundIndex
	}
	var selectedDate by remember { mutableIntStateOf(initialIndex) }

	// ** get the date that is set by the user or get the date and if not null then check if it is in the list and if not then default to the current date **
	val listState = rememberLazyListState()
	LaunchedEffect(Unit) { listState.animateScrollToItem(currentDateIndex) }
	LazyRow(
		state = listState,
		contentPadding = PaddingValues(horizontal = 16.dp),
		horizontalArrangement = Arrangement.spacedBy(10.dp),
		modifier = Modifier.fillMaxWidth()
	) { itemsIndexed(weekDates) { index, date ->
		val isSelectable = if (allowSelectingPastDate) true else index >= currentDateIndex

			DateCard(
				date = date.date,
				isSelected = index == selectedDate,
				onClick = {
					logD("clicked on a date component and index:$index and isSelectable:$isSelectable")
					if (isSelectable){
						selectedDate = index
						onSelect(index)
					}
				}
			)
		}
	}
}

@Composable fun DateCard(date: LocalDate, isSelected: Boolean, onClick: () -> Unit) {
	val backgroundColor = if (isSelected) Color(0xFF152A46) else Color(0xFF1C1F26)
	val borderColor = if (isSelected) Color(0xFF1E88E5) else Color(0xFF2C313A)
	val textColor = if (isSelected) Color.White else Color(0xFF7D8592)
	val dayName =  date.dayOfWeek.name.take(3)

	Surface(
		onClick = onClick,
		shape = RoundedCornerShape(12.dp),
		color = backgroundColor,
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
					color = textColor
				)
			)
			Spacer(modifier = Modifier.height(4.dp))
			Text(
				text = date.dayOfMonth.toString(),
				style = TextStyle(
					fontSize = 20.sp,
					fontWeight = FontWeight.Bold,
					color = Color.White
				)
			)
			Text(
				text = date.month.name.take(3),
				style = TextStyle(
					fontSize = 10.sp,
					fontWeight = FontWeight.Medium,
					color = textColor
				)
			)
		}
	}
}
fun getListOfDatesInThisWeek(): List<DayInWeek> {
	val today = LocalDate.now()
	val sunday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
	val saturday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
	require(saturday.toEpochDay() - sunday.toEpochDay() == 6L) {
		"Week span is incorrect, expected the distance from saturday - sunday to be 6 but got ${saturday.toEpochDay() - sunday.toEpochDay()}"
	}
	val list = (0..6).map { offset ->
		val date = sunday.plusDays(offset.toLong())
		DayInWeek(
			dayOfWeek = date.dayOfWeek,
			date = date, isToday = date == today
		)
	}
	require(list.size == 7, { "expected the  list size to be 7 but got ${list.size} " })
	require(list.first().dayOfWeek == DayOfWeek.SUNDAY, {"expected the first day of week to be sunday but got ${list.first().dayOfWeek}"})
	require(list.last().dayOfWeek == DayOfWeek.SATURDAY, {"expected the last day of week to be saturday but got ${list.last().dayOfWeek}"})
	return  list
}
