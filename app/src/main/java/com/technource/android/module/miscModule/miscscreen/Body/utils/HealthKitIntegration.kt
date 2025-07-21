//package com.technource.android.module.miscModule.miscscreen.Body.utils
//
//import android.content.Context
//import android.hardware.Sensor
//import android.hardware.SensorEvent
//import android.hardware.SensorEventListener
//import android.hardware.SensorManager
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import javax.inject.Inject
//import javax.inject.Singleton
//
//@Singleton
//class HealthKitIntegration @Inject constructor(
//    private val context: Context
//) : SensorEventListener {
//
//    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
//
//    private val _heartRate = MutableStateFlow(0)
//    val heartRate: Flow<Int> = _heartRate.asStateFlow()
//
//    private val _stepCount = MutableStateFlow(0)
//    val stepCount: Flow<Int> = _stepCount.asStateFlow()
//
//    private val _isMonitoring = MutableStateFlow(false)
//    val isMonitoring: Flow<Boolean> = _isMonitoring.asStateFlow()
//
//    private var heartRateSensor: Sensor? = null
//    private var stepCounterSensor: Sensor? = null
//    private var accelerometerSensor: Sensor? = null
//
//    init {
//        initializeSensors()
//    }
//
//    private fun initializeSensors() {
//        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
//        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
//        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
//    }
//
//    suspend fun startMonitoring() {
//        if (_isMonitoring.value) return
//
//        try {
//            // Start heart rate monitoring
//            heartRateSensor?.let { sensor ->
//                sensorManager.registerListener(
//                    this,
//                    sensor,
//                    SensorManager.SENSOR_DELAY_NORMAL
//                )
//            }
//
//            // Start step counting
//            stepCounterSensor?.let { sensor ->
//                sensorManager.registerListener(
//                    this,
//                    sensor,
//                    SensorManager.SENSOR_DELAY_NORMAL
//                )
//            }
//
//            // Start accelerometer for activity detection
//            accelerometerSensor?.let { sensor ->
//                sensorManager.registerListener(
//                    this,
//                    sensor,
//                    SensorManager.SENSOR_DELAY_NORMAL
//                )
//            }
//
//            _isMonitoring.value = true
//
//        } catch (e: Exception) {
//            // Handle monitoring start error
//        }
//    }
//
//    fun stopMonitoring() {
//        sensorManager.unregisterListener(this)
//        _isMonitoring.value = false
//    }
//
//    override fun onSensorChanged(event: SensorEvent?) {
//        event?.let { sensorEvent ->
//            when (sensorEvent.sensor.type) {
//                Sensor.TYPE_HEART_RATE -> {
//                    val heartRateValue = sensorEvent.values[0].toInt()
//                    if (heartRateValue > 0) {
//                        _heartRate.value = heartRateValue
//                    }
//                }
//
//                Sensor.TYPE_STEP_COUNTER -> {
//                    val stepCountValue = sensorEvent.values[0].toInt()
//                    _stepCount.value = stepCountValue
//                }
//
//                Sensor.TYPE_ACCELEROMETER -> {
//                    // Process accelerometer data for activity detection
//                    processAccelerometerData(sensorEvent.values)
//                }
//            }
//        }
//    }
//
//    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
//        // Handle sensor accuracy changes
//    }
//
//    private fun processAccelerometerData(values: FloatArray) {
//        // Calculate activity level from accelerometer data
//        val x = values[0]
//        val y = values[1]
//        val z = values[2]
//
//        val magnitude = kotlin.math.sqrt((x * x + y * y + z * z).toDouble())
//
//        // Simple activity detection based on acceleration magnitude
//        when {
//            magnitude > 15 -> {
//                onActivityDetected(ActivityLevel.HIGH)
//            }
//            magnitude > 10 -> {
//                onActivityDetected(ActivityLevel.MODERATE)
//            }
//            magnitude > 5 -> {
//                onActivityDetected(ActivityLevel.LIGHT)
//            }
//            else -> {
//                onActivityDetected(ActivityLevel.SEDENTARY)
//            }
//        }
//    }
//
//    private fun onActivityDetected(level: ActivityLevel) {
//        // Handle activity level detection
//    }
//
//    fun getHealthSummary(): HealthSummary {
//        return HealthSummary(
//            currentHeartRate = _heartRate.value,
//            todaySteps = _stepCount.value,
//            isMonitoringActive = _isMonitoring.value,
//            lastUpdated = System.currentTimeMillis()
//        )
//    }
//
//    fun isHeartRateAvailable(): Boolean {
//        return heartRateSensor != null
//    }
//
//    fun isStepCounterAvailable(): Boolean {
//        return stepCounterSensor != null
//    }
//
//    fun isAccelerometerAvailable(): Boolean {
//        return accelerometerSensor != null
//    }
//}
//
//enum class ActivityLevel {
//    SEDENTARY, LIGHT, MODERATE, HIGH
//}
//
//data class HealthSummary(
//    val currentHeartRate: Int,
//    val todaySteps: Int,
//    val isMonitoringActive: Boolean,
//    val lastUpdated: Long
//)
