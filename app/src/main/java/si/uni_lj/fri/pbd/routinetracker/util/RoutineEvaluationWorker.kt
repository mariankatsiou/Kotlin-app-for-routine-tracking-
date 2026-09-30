package si.uni_lj.fri.pbd.routinetracker.util

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import si.uni_lj.fri.pbd.routinetracker.data.RoutinesDatabase
import si.uni_lj.fri.pbd.routinetracker.data.entity.RoutineExecution
import java.util.Calendar

// Background worker that evaluates if routines were completed or missed
class RoutineEvaluationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // Retrieve app usage timestamps to check if the user was active
        val prefs = applicationContext.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val lastForeground = prefs.getLong("lastForegroundTime", 0L)
        var lastBackground = prefs.getLong("lastBackgroundTime", 0L)

        if (lastForeground > lastBackground) {
            lastBackground = System.currentTimeMillis()
        }

        val db = RoutinesDatabase.getDatabase(applicationContext)
        val dao = db.routineDao()
        val allRoutines = dao.getAllRoutines().first()

        val calendar = Calendar.getInstance()
        val todayDayName = getDayName(calendar.get(Calendar.DAY_OF_WEEK))

        // Define the time boundaries for the current day
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfToday = calendar.timeInMillis
        val endOfToday = startOfToday + (24 * 60 * 60 * 1000) - 1

        for (routine in allRoutines) {
            // Only evaluate routines scheduled for today
            if (!routine.days.contains(todayDayName)) continue

            val routineStartMillis = getMillisForTimeToday(routine.startTime)
            val routineEndMillis = getMillisForTimeToday(routine.endTime)

            // Check if this routine was already logged today
            val executions = dao.getExecutionsForRoutine(routine.id).first()
            val alreadyEvaluatedToday = executions.any { it.dateTimestamp in startOfToday..endOfToday }

            if (alreadyEvaluatedToday) continue

            // Determine completion based on app activity during the routine timeframe
            val appWasOpenDuringRoutine = (lastForeground <= routineEndMillis) && (lastBackground >= routineStartMillis)

            if (appWasOpenDuringRoutine) {
                val execution = RoutineExecution(
                    routineId = routine.id,
                    dateTimestamp = System.currentTimeMillis(),
                    completed = true
                )
                dao.insertRoutineExecution(execution)
                Log.d("Worker", "Routine ${routine.name} completed!")
            } else if (System.currentTimeMillis() > routineEndMillis) {
                // Mark as missed if the current time is past the routine's end time
                val execution = RoutineExecution(
                    routineId = routine.id,
                    dateTimestamp = System.currentTimeMillis(),
                    completed = false
                )
                dao.insertRoutineExecution(execution)
                Log.d("Worker", "Routine ${routine.name} missed.")
            }
        }

        return Result.success()
    }

    // Helper to map Calendar constants to short day strings
    private fun getDayName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.MONDAY -> "Mon"
            Calendar.TUESDAY -> "Tue"
            Calendar.WEDNESDAY -> "Wed"
            Calendar.THURSDAY -> "Thu"
            Calendar.FRIDAY -> "Fri"
            Calendar.SATURDAY -> "Sat"
            Calendar.SUNDAY -> "Sun"
            else -> ""
        }
    }

    // Convert "HH:mm" strings to milliseconds for the current day
    private fun getMillisForTimeToday(timeString: String): Long {
        val parts = timeString.split(":")
        if (parts.size != 2) return 0L
        val hour = parts[0].trim().toIntOrNull() ?: 0
        val minute = parts[1].trim().toIntOrNull() ?: 0

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        return calendar.timeInMillis
    }
}