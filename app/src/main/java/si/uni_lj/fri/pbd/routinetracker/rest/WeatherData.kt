package si.uni_lj.fri.pbd.routinetracker.rest

import com.google.gson.annotations.SerializedName


data class WeatherData(
    val temperature: Float,
    val conditions: String
)



data class OpenWeatherResponse(
    @SerializedName("weather") val weather: List<WeatherItem>,
    @SerializedName("main") val main: MainData
) {

    fun toWeatherData(): WeatherData {
        val temp = main.temp
        val cond = weather.firstOrNull()?.main ?: "Unknown"
        return WeatherData(temperature = temp, conditions = cond)
    }
}

data class WeatherItem(
    @SerializedName("main") val main: String
)

data class MainData(
    @SerializedName("temp") val temp: Float
)