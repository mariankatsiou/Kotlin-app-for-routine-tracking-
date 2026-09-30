package si.uni_lj.fri.pbd.routinetracker.data

import si.uni_lj.fri.pbd.routinetracker.data.entity.Routine
import si.uni_lj.fri.pbd.routinetracker.rest.WeatherData


data class RoutineContext(
    val routine: Routine,
    val weatherData: WeatherData?,
    val lightLux: Float,
    val suggestion: String
)