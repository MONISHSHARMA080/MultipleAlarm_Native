package com.coolApps.MultipleAlarmClock.Components_for_ui_compose.alarmListScreen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coolApps.MultipleAlarmClock.dataBase.AlarmData

@Composable fun AlarmCard(
	alarmData: AlarmData,
	onEdit: (AlarmData) -> Unit ,
	onStop: (AlarmData) -> Unit ,
	onReset: (AlarmData) -> Unit ,
	onDelete: (AlarmData) -> Unit,
    modifier: Modifier,
    onLongPress: (AlarmData) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val buttonColor by animateColorAsState(
        targetValue = if (alarmData.isReadyToUse) Color(0xFF00E5FF) else MaterialTheme.colorScheme.secondary,
        animationSpec = tween(durationMillis = 300),
        label = "buttonColor"
    )
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(32.dp))
            .combinedClickable(
                onClick = { isExpanded = !isExpanded },
                onLongClick = { onLongPress(alarmData) }
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text =formatDate(alarmData.startTime) ,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
						fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Every ${alarmData.frequencyInMin} mins",
                        style = MaterialTheme.typography.bodySmall,
						fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { onEdit(alarmData) },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .size(43.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Alarm",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val timeSize = 27.sp
                    Text(
                        text = formatTime12h(alarmData.startTime),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = timeSize
                        )
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.padding(horizontal = 12.dp).size(30.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Text(
                        text = formatTime12h(alarmData.endTime),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = timeSize
                        ),
                        maxLines = 1,
                        softWrap = false
                    )

                }
            }
            // The Hidden Message (Revealed on Tap)
            AnimatedVisibility(visible = isExpanded) {
                Text(
                    text = alarmData.message.ifEmpty { "No message added" },
                    modifier = Modifier.padding(bottom = 16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    fontStyle = if(alarmData.message.isEmpty()) FontStyle.Italic else FontStyle.Normal
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { if (alarmData.isReadyToUse) onStop(alarmData) else onReset(alarmData) },
                    modifier = Modifier.weight(1f).height(64.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonColor,
                        contentColor = Color.Black
                    )
                ) {
                    AnimatedContent(
                        targetState = if (alarmData.isReadyToUse) "Stop" else "Reset",
                        transitionSpec = {
                            fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                        },
                        label = "buttonText"
                    ) { text ->
                        Text(
                            text = text,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                        )
                    }
                }
                FilledIconButton(
                    onClick = { onDelete(alarmData) },
                    modifier = Modifier.size(64.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.93f),
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}

@Composable
fun TimeRow(label: String, time: String, isDimmed: Boolean = false) {
	Row(verticalAlignment = Alignment.CenterVertically) {
		Text(
			text = label,
			style = MaterialTheme.typography.labelSmall,
			modifier = Modifier.width(45.dp),
			color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
		)
		Text(
			text = time,
			style = MaterialTheme.typography.displaySmall.copy(
				fontWeight = FontWeight.Black,
				fontSize = 32.sp
			),
			color = if (isDimmed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
		)
	}
}

fun formatTime12h(millis: Long): String {
	val formatter = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
	return formatter.format(java.util.Date(millis))
}

fun formatDate(millis: Long): String =
	java.text.SimpleDateFormat("EEEE, MMM d", java.util.Locale.getDefault()).format(java.util.Date(millis))
