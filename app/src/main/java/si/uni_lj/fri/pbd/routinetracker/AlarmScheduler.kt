package si.uni_lj.fri.pbd.routinetracker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.preference.PreferenceManager
import java.util.Calendar
import si.uni_lj.fri.pbd.routinetracker.data.entity.Routine
class AlarmScheduler(private val context: Context) {

    fun scheduleAlarm(routine: Routine) {
        if (!routine.notificationsEnabled) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // Create an explicit intent to trigger the BroadcastReceiver when the scheduled time arrives
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("ROUTINE_ID", routine.id)
            putExtra("ROUTINE_NAME", routine.name)
        }
// Wrap the intent in a PendingIntent, granting the system AlarmManager permission to execute it on our behalf
        val pendingIntent = PendingIntent.getBroadcast(
            context, routine.id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
// Retrieve the user-defined notification offset (e.g., 10 minutes before) from SharedPreferences
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        val advanceMinutes = sharedPrefs.getInt("NOTIFICATION_OFFSET", 10)

        val timeParts = routine.startTime.split(":")
        val hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            add(Calendar.MINUTE, -advanceMinutes)
        }
// If the calculated alarm time has already passed for today, push the alarm to tomorrow
        if (calendar.before(Calendar.getInstance())) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } catch (e: SecurityException) {
        }
    }

    fun cancelAlarm(routineId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)
        // Recreate the exact same PendingIntent signature (using the routineId) in order to successfully cancel it
        val pendingIntent = PendingIntent.getBroadcast(
            context, routineId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}