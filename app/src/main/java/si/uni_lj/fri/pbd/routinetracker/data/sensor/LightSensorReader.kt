package si.uni_lj.fri.pbd.routinetracker.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LightSensorReader(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val lightSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    suspend fun readOnce(): Float = suspendCancellableCoroutine { continuation ->

        if (lightSensor == null) {
            continuation.resume(300f)
            return@suspendCancellableCoroutine
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    val lux = it.values[0]
                    sensorManager.unregisterListener(this)

                    if (continuation.isActive) {
                        continuation.resume(lux)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

            }
        }


        sensorManager.registerListener(listener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)


        continuation.invokeOnCancellation {
            sensorManager.unregisterListener(listener)
        }
    }
}