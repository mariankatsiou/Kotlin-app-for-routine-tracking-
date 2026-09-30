package si.uni_lj.fri.pbd.routinetracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import si.uni_lj.fri.pbd.routinetracker.data.RoutinesDatabase
import si.uni_lj.fri.pbd.routinetracker.repository.RoutineRepository

// Receiver that handles routine alarms and shows notifications with suggestions
class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        val globalNotifications = sharedPrefs.getBoolean("notifications_enabled", true)
        if (!globalNotifications) return

        val routineId = intent.getLongExtra("ROUTINE_ID", -1L)
        val routineName = intent.getStringExtra("ROUTINE_NAME") ?: "Routine"

        if (routineId == -1L) return

        // Keeps the receiver alive during asynchronous background tasks
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = RoutinesDatabase.getDatabase(context.applicationContext).routineDao()
                val repository = RoutineRepository(context.applicationContext, dao)

                // Fetch weather and light data to generate the suggestion
                val routineContext = repository.buildRoutineContext(routineId)
                val suggestion = routineContext.suggestion

                val notificationText = "It's time for: $routineName\n💡 $suggestion"
                showNotification(context, routineId, notificationText)
            } catch (e: Exception) {
                e.printStackTrace()
                showNotification(context, routineId, "It's time for: $routineName")
            } finally {
                // Must call finish so the system can reclaim the receiver
                pendingResult.finish()
            }
        }
    }

    // Helper to create and display the Android notification
    private fun showNotification(context: Context, routineId: Long, text: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "routine_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Routines", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("ROUTINE_ID", routineId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context, routineId.toInt(), mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Routine Reminder")
            .setContentText(text)
            // Uses BigTextStyle to ensure the suggestion text isn't truncated
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(routineId.toInt(), notification)
    }
}