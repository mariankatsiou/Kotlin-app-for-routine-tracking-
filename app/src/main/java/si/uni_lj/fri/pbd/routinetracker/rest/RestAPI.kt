package si.uni_lj.fri.pbd.routinetracker.rest

import retrofit2.http.GET
import retrofit2.http.Query

interface RestAPI {
    @GET("data/2.5/weather")
    suspend fun fetchWeather(
        @Query("q") location: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): OpenWeatherResponse
}