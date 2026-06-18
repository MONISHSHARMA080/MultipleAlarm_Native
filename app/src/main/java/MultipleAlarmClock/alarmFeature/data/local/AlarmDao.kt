package MultipleAlarmClock.alarmFeature.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {

	@Insert
	suspend fun insert(alarmData: AlarmData): Long

	@Query("DELETE FROM AlarmData WHERE startTime= :firstValue AND endTime = :secondValue")
	suspend fun deleteAlarm(firstValue: Long, secondValue: Long): Int

	@Query("DELETE FROM AlarmData WHERE id = :id")
	suspend fun deleteAlarm(id: Int): Int

	@Delete
	suspend fun deleteAlarm(alarm: AlarmData): Int

	@Query("SELECT * FROM AlarmData")
	fun getAll(): List<AlarmData>

	@Query("SELECT * FROM AlarmData")
	suspend fun getAllAlarms(): List<AlarmData>

	//    @Query("SELECT * FROM AlarmData ORDER BY date_in_long ASC, first_value ASC")
	@Query("SELECT * FROM AlarmData ORDER BY startTime ASC, endTime ASC")
	fun getAllAlarmsFlow(): Flow<List<AlarmData>>

	// New function to retrieve an alarm by first_value and second_value
	@Query("SELECT * FROM AlarmData WHERE startTime = :firstValue AND endTime = :secondValue LIMIT 1")
	suspend fun getAlarmByValues(firstValue: Long, secondValue: Long): AlarmData?

	/**-1L when update occurs and rest is the row ID*/
	@Upsert
	suspend fun updateOrInsert(alarmData: AlarmData): Long

	// New function to update the is_ready_to_use field of an alarm
	@Query("UPDATE AlarmData SET is_ready_to_use = :isReadyToUse WHERE startTime = :startTime AND endTime = :endTime")
	suspend fun updateReadyToUseInAlarm(startTime: Long, endTime: Long, isReadyToUse: Boolean)

	@Update
	suspend fun updateAlarm(alarmData: AlarmData): Int  // Returns number of rows updated

	@Query("SELECT * FROM AlarmData WHERE id = :id")
	suspend fun getAlarmById(id: Int): AlarmData?

	@Query(" UPDATE AlarmData SET is_ready_to_use = :isReadyToUse , startTime = :firstValue, endTime = :second_value  WHERE id = :id")
	suspend fun updateAlarmForReset(id: Int, firstValue: Long, second_value: Long, isReadyToUse: Boolean)

	@Update
	suspend fun updateAlarmForReset(alarmData: AlarmData): Int

	@Query("DELETE  FROM AlarmData")
	suspend fun deleteAllAlarmsFromDb()
}
