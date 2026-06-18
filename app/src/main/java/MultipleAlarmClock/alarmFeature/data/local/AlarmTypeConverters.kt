package MultipleAlarmClock.alarmFeature.data.local

import android.net.Uri
import androidx.core.net.toUri
import androidx.room.TypeConverter

class AlarmTypeConverters {
	@TypeConverter
	fun uriToString(uri: Uri?): String? = uri?.toString()
	@TypeConverter
	fun stringToUri(value: String?): Uri? = value?.toUri()
}
