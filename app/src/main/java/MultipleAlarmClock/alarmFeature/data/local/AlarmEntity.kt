package MultipleAlarmClock.alarmFeature.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(indices = [Index(value = ["startTime", "endTime"])], )
data class AlarmData(
	@PrimaryKey(autoGenerate = true) val id: Int = 0,
	@ColumnInfo(name = "startTime") val startTime: Long,
	@ColumnInfo(name = "endTime") val endTime: Long,
	@ColumnInfo(name = "message") val message: String,
	@ColumnInfo(name = "freq_used_to_skip_start_alarm") val frequencyInMin: Long,
	val sound: String?,
	@ColumnInfo(name = "is_ready_to_use") val isReadyToUse: Boolean
)