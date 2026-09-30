package si.uni_lj.fri.pbd.routinetracker.repository

import android.content.Context
import androidx.preference.PreferenceManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import si.uni_lj.fri.pbd.routinetracker.data.RoutineContext
import si.uni_lj.fri.pbd.routinetracker.data.dao.RoutineDao
import si.uni_lj.fri.pbd.routinetracker.data.entity.Routine
import si.uni_lj.fri.pbd.routinetracker.data.entity.RoutineExecution
import si.uni_lj.fri.pbd.routinetracker.data.sensor.LightSensorReader
import si.uni_lj.fri.pbd.routinetracker.rest.RetrofitInstance
import si.uni_lj.fri.pbd.routinetracker.rest.WeatherData

class RoutineRepository(
    private val context: Context,
    private val routineDao: RoutineDao
) {

    val allRoutines: Flow<List<Routine>> = routineDao.getAllRoutines()

    val routinesWithStatus = routineDao.getRoutinesWithStatus()

    fun getRoutineById(id: Long): Flow<Routine> =
        routineDao.getRoutineById(id)

    suspend fun insertRoutine(routine: Routine): Long =
        routineDao.insertRoutine(routine)

    suspend fun updateRoutine(routine: Routine) =
        routineDao.updateRoutine(routine)

    suspend fun deleteRoutine(routine: Routine) =
        routineDao.deleteRoutine(routine)

    suspend fun insertRoutineExecution(execution: RoutineExecution) =
        routineDao.insertRoutineExecution(execution)

    fun getExecutionsForRoutine(routineId: Long): Flow<List<RoutineExecution>> =
        routineDao.getExecutionsForRoutine(routineId)


    suspend fun getWeather(location: String): WeatherData {

        return try {

            val apiKey = "edf2dabd5aaebfb6aeec898c8873956d"

            val response = RetrofitInstance
                .api
                .fetchWeather(
                    location,
                    apiKey
                )
            response.toWeatherData()

        } catch (e: Exception) {

            e.printStackTrace()

            WeatherData(
                temperature = 20f,
                conditions = "Clear"
            )
        }
    }

    /**
     * Builds environmental context for a routine
     */
    suspend fun buildRoutineContext(routineId: Long): RoutineContext {

        val routine = routineDao
            .getRoutineById(routineId)
            .first()

        // User city or default fallback
        val sharedPrefs =
            PreferenceManager.getDefaultSharedPreferences(context)


        val savedCity = sharedPrefs.getString("USER_CITY", "")

        val city =
            if (savedCity.isNullOrBlank())
                "Ljubljana, Slovenia"
            else
                savedCity

        // WEATHER
        val weatherData = getWeather(city)

        // LIGHT SENSOR
        val lightLux = try {

            val lightReader = LightSensorReader(context)

            lightReader.readOnce()

        } catch (e: Exception) {

            e.printStackTrace()

            300f
        }

        // SUGGESTION
        val suggestion = calculateSuggestion(
            type = routine.type,
            weather = weatherData,
            lightLux = lightLux
        )

        return RoutineContext(
            routine = routine,
            weatherData = weatherData,
            lightLux = lightLux,
            suggestion = suggestion
        )
    }

    /**
     * Environmental recommendation logic
     */
    private fun calculateSuggestion(
        type: String,
        weather: WeatherData?,
        lightLux: Float
    ): String {

        return when (type.lowercase()) {

            "study" -> {

                if (lightLux > 500f || lightLux < 50f) {
                    "Consider adjusting the light for better focus"
                } else {
                    "Get ready for some productive studying"
                }
            }

            "exercise" -> {

                if (
                    weather != null &&
                    (weather.temperature > 30f ||
                            weather.temperature < 5f)
                ) {
                    "Consider indoor exercise"
                } else {
                    "Consider outdoor exercise"
                }
            }

            "socialise" -> {

                val condition =
                    weather?.conditions?.lowercase() ?: ""

                if (
                    condition.contains("rain") ||
                    condition.contains("snow")
                ) {

                    "Consider attending an indoor event, for instance, going to a theatre"

                } else {

                    "Consider organising a picnic."
                }
            }

            else -> {
                "Have a great time!"
            }
        }
    }
}