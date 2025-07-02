package br.edu.utfpr.social_pm46s.service

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Power
import androidx.health.connect.client.units.Velocity
import br.edu.utfpr.social_pm46s.data.model.ActiveExerciseSession
import br.edu.utfpr.social_pm46s.data.model.AggregatedData
import br.edu.utfpr.social_pm46s.data.model.DistanceData
import br.edu.utfpr.social_pm46s.data.model.ExerciseData
import br.edu.utfpr.social_pm46s.data.model.HealthData
import br.edu.utfpr.social_pm46s.data.model.HeartRateData
import br.edu.utfpr.social_pm46s.data.model.SleepData
import br.edu.utfpr.social_pm46s.data.model.StepData
import br.edu.utfpr.social_pm46s.data.model.WeightData
import br.edu.utfpr.social_pm46s.data.model.workout.WorkoutSummary
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime

class HealthConnectService(private val context: Context) {

    private lateinit var healthConnectClient: HealthConnectClient
    private var isHealthConnectAvailable = false
    private var activeExerciseSession: ActiveExerciseSession? = null

    private lateinit var device: Device

    companion object {
        private const val TAG = "HealthConnectService"
        private const val APP_PACKAGE_NAME = "br.edu.utfpr.social_pm46s"

        const val EXERCISE_TYPE_WALKING = 79
        const val EXERCISE_TYPE_RUNNING = 56
        const val EXERCISE_TYPE_CYCLING = 8
        const val EXERCISE_TYPE_WEIGHTLIFTING = 96
        const val EXERCISE_TYPE_YOGA = 102
        const val EXERCISE_TYPE_SWIMMING = 71

    }

    init {
        initializeHealthConnect()
    }

    private fun initializeHealthConnect() {
        if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
            healthConnectClient = HealthConnectClient.getOrCreate(context)
            isHealthConnectAvailable = true
            device = Device(
                manufacturer = Build.MANUFACTURER,
                model = Build.MODEL,
                type = Device.TYPE_PHONE
            )

            Log.i(TAG, "Health Connect SDK inicializado e disponível.")
            Log.i(TAG, "Device configurado: ${device.manufacturer} ${device.model}")
        } else {
            isHealthConnectAvailable = false
            Log.w(TAG, "Health Connect não está disponível neste dispositivo.")
        }
    }

    suspend fun checkAllPermissions(): Boolean {
        if (!isHealthConnectAvailable || !::healthConnectClient.isInitialized) {
            return false
        }

        val requiredPermissions = setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getWritePermission(StepsRecord::class),
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getWritePermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(WeightRecord::class),
            HealthPermission.getWritePermission(WeightRecord::class),
            HealthPermission.getReadPermission(DistanceRecord::class),
            HealthPermission.getWritePermission(DistanceRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getWritePermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
            HealthPermission.getWritePermission(ActiveCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(SpeedRecord::class),
            HealthPermission.getWritePermission(SpeedRecord::class),
            HealthPermission.getReadPermission(PowerRecord::class),
            HealthPermission.getWritePermission(PowerRecord::class)
        )

        return try {
            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            granted.containsAll(requiredPermissions)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao verificar permissões do Health Connect", e)
            false
        }
    }

    fun getRequiredPermissions(): Set<String> {
        return setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getWritePermission(StepsRecord::class),
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getWritePermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(WeightRecord::class),
            HealthPermission.getWritePermission(WeightRecord::class),
            HealthPermission.getReadPermission(DistanceRecord::class),
            HealthPermission.getWritePermission(DistanceRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getWritePermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
            HealthPermission.getWritePermission(ActiveCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(SpeedRecord::class),
            HealthPermission.getWritePermission(SpeedRecord::class),
            HealthPermission.getReadPermission(PowerRecord::class),
            HealthPermission.getWritePermission(PowerRecord::class)
        )
    }

    suspend fun getAllHealthData(): HealthData? {
        if (!isHealthConnectAvailable || !checkAllPermissions()) {
            Log.w(TAG, "Não é possível obter dados: HC indisponível ou permissões ausentes.")
            return null
        }

        return try {
            HealthData(
                steps = readStepsData(),
                heartRate = readHeartRateData(),
                weight = readWeightData(),
                distance = readDistanceData(),
                sleep = readSleepData(),
                exercises = readExerciseData(),
                aggregated = getAggregatedData()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter todos os dados de saúde", e)
            null
        }
    }

    fun startExercise(exerciseType: Int, title: String): Boolean {
        if (!isHealthConnectAvailable) {
            Log.w(TAG, "Health Connect não disponível para iniciar exercício")
            return false
        }

        if (activeExerciseSession != null) {
            Log.w(TAG, "Já existe uma sessão de exercício ativa.")
            return false
        }

        activeExerciseSession = ActiveExerciseSession(
            sessionType = exerciseType,
            title = title,
            startTime = Instant.now()
        )

        Log.i(TAG, "Exercício iniciado: $title")
        return true
    }

    suspend fun finishExercise(caloriesBurned: Double? = null, notes: String? = null): Boolean {
        if (!isHealthConnectAvailable) return false

        val session = activeExerciseSession ?: run {
            Log.w(TAG, "Nenhuma sessão de exercício ativa para finalizar.")
            return false
        }

        return try {
            val endTime = Instant.now()
            val recordsToInsert = mutableListOf<Record>()

            caloriesBurned?.let { calories ->
                if (calories > 0) {
                    val caloriesRecord = ActiveCaloriesBurnedRecord(
                        startTime = session.startTime,
                        startZoneOffset = ZonedDateTime.now().offset,
                        endTime = endTime,
                        endZoneOffset = ZonedDateTime.now().offset,
                        energy = Energy.kilocalories(calories),
                        metadata = Metadata.autoRecorded(device),
                    )
                    recordsToInsert.add(caloriesRecord)
                }
            }

            if (recordsToInsert.isNotEmpty()) {
                healthConnectClient.insertRecords(recordsToInsert)
            }

            activeExerciseSession = null
            Log.i(TAG, "Exercício finalizado e salvo: ${session.title}")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao finalizar e salvar exercício '${session.title}'", e)
            false
        }
    }

    suspend fun finishExerciseWithMultipleRecords(
        caloriesBurned: Double? = null,
        distanceKm: Double? = null,
        steps: Long? = null,
        heartRateAvg: Long? = null
    ): Boolean {
        if (!isHealthConnectAvailable) return false

        val session = activeExerciseSession ?: return false

        return try {
            val endTime = Instant.now()
            val recordsToInsert = mutableListOf<Record>()

            caloriesBurned?.let { calories ->
                if (calories > 0) {
                    recordsToInsert.add(
                        ActiveCaloriesBurnedRecord(
                            startTime = session.startTime,
                            startZoneOffset = ZonedDateTime.now().offset,
                            endTime = endTime,
                            endZoneOffset = ZonedDateTime.now().offset,
                            energy = Energy.kilocalories(calories),
                            metadata = Metadata.autoRecorded(device),
                        )
                    )
                }
            }

            distanceKm?.let { distance ->
                if (distance > 0) {
                    recordsToInsert.add(
                        DistanceRecord(
                            distance = Length.kilometers(distance),
                            startTime = session.startTime,
                            startZoneOffset = ZonedDateTime.now().offset,
                            endTime = endTime,
                            endZoneOffset = ZonedDateTime.now().offset,
                            metadata = Metadata.autoRecorded(device),
                        )
                    )
                }
            }

            steps?.let { stepCount ->
                if (stepCount > 0) {
                    recordsToInsert.add(
                        StepsRecord(
                            count = stepCount,
                            startTime = session.startTime,
                            startZoneOffset = ZonedDateTime.now().offset,
                            endTime = endTime,
                            endZoneOffset = ZonedDateTime.now().offset,
                            metadata = Metadata.autoRecorded(device),
                        )
                    )
                }
            }

            heartRateAvg?.let { avgHr ->
                if (avgHr > 0) {
                    recordsToInsert.add(
                        HeartRateRecord(
                            startTime = session.startTime,
                            startZoneOffset = ZonedDateTime.now().offset,
                            endTime = endTime,
                            endZoneOffset = ZonedDateTime.now().offset,
                            samples = listOf(
                                HeartRateRecord.Sample(
                                    time = session.startTime,
                                    beatsPerMinute = avgHr
                                )
                            ),
                            metadata = Metadata.autoRecorded(device),
                        )
                    )
                }
            }

            if (recordsToInsert.isNotEmpty()) {
                healthConnectClient.insertRecords(recordsToInsert)
            }

            activeExerciseSession = null
            Log.i(TAG, "Exercício finalizado com ${recordsToInsert.size} records: ${session.title}")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao finalizar exercício '${session.title}'", e)
            false
        }
    }

    suspend fun finishExerciseComplete(
        caloriesBurned: Double? = null,
        distanceKm: Double? = null,
        steps: Long? = null,
        avgHeartRate: Long? = null,
        maxHeartRate: Long? = null,
        avgSpeedKmh: Double? = null
    ): WorkoutSummary? {
        val session = activeExerciseSession ?: return null
        val endTime = Instant.now()

        finishExerciseWithMultipleRecords(caloriesBurned, distanceKm, steps, avgHeartRate)

        return WorkoutSummary(
            exerciseType = session.sessionType,
            title = session.title,
            startTime = session.startTime,
            endTime = endTime,
            duration = Duration.between(session.startTime, endTime),
            caloriesBurned = caloriesBurned,
            distanceKm = distanceKm,
            steps = steps,
            averageHeartRate = avgHeartRate,
            maxHeartRate = maxHeartRate,
            averageSpeed = avgSpeedKmh
        )
    }

    suspend fun cancelExercise(): Boolean {
        if (activeExerciseSession == null) return false
        activeExerciseSession = null
        Log.i(TAG, "Sessão de exercício cancelada.")
        return true
    }

    fun getActiveExerciseSession(): ActiveExerciseSession? = activeExerciseSession

    suspend fun writeStepsData(steps: Long, startTime: Instant, endTime: Instant): Boolean {
        if (!isHealthConnectAvailable || !checkAllPermissions()) return false

        return try {
            val stepsRecord = StepsRecord(
                count = steps,
                startTime = startTime,
                startZoneOffset = ZonedDateTime.now().offset,
                endTime = endTime,
                endZoneOffset = ZonedDateTime.now().offset,
                metadata = Metadata.autoRecorded(device),
            )

            healthConnectClient.insertRecords(listOf(stepsRecord))
            Log.i(TAG, "Passos inseridos: $steps")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao inserir dados de passos", e)
            false
        }
    }

    suspend fun writeWeightData(weightKg: Double, time: Instant): Boolean {
        if (!isHealthConnectAvailable || !checkAllPermissions()) return false

        return try {
            val weightRecord = WeightRecord(
                weight = Mass.kilograms(weightKg),
                time = time,
                zoneOffset = ZonedDateTime.now().offset,
                metadata = Metadata.autoRecorded(device),
            )

            healthConnectClient.insertRecords(listOf(weightRecord))
            Log.i(TAG, "Peso inserido: ${weightKg}kg")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao inserir dados de peso", e)
            false
        }
    }

    suspend fun writeDistanceData(
        distanceKm: Double,
        startTime: Instant,
        endTime: Instant
    ): Boolean {
        if (!isHealthConnectAvailable || !checkAllPermissions()) return false

        return try {
            val distanceRecord = DistanceRecord(
                distance = Length.kilometers(distanceKm),
                startTime = startTime,
                startZoneOffset = ZonedDateTime.now().offset,
                endTime = endTime,
                endZoneOffset = ZonedDateTime.now().offset,
                metadata = Metadata.autoRecorded(device),
            )

            healthConnectClient.insertRecords(listOf(distanceRecord))
            Log.i(TAG, "Distância inserida: ${distanceKm}km")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao inserir dados de distância", e)
            false
        }
    }

    suspend fun writeCaloriesData(calories: Double, startTime: Instant, endTime: Instant): Boolean {
        if (!isHealthConnectAvailable || !checkAllPermissions()) return false

        return try {
            val caloriesRecord = ActiveCaloriesBurnedRecord(
                startTime = startTime,
                startZoneOffset = ZonedDateTime.now().offset,
                endTime = endTime,
                endZoneOffset = ZonedDateTime.now().offset,
                energy = Energy.kilocalories(calories),
                metadata = Metadata.autoRecorded(device),
            )

            healthConnectClient.insertRecords(listOf(caloriesRecord))
            Log.i(TAG, "Calorias inseridas: $calories kcal")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao inserir dados de calorias", e)
            false
        }
    }

    suspend fun writeSpeedData(speedKmh: Double, startTime: Instant, endTime: Instant): Boolean {
        if (!isHealthConnectAvailable || !checkAllPermissions()) return false

        return try {
            val speedRecord = SpeedRecord(
                startTime = startTime,
                startZoneOffset = ZonedDateTime.now().offset,
                endTime = endTime,
                endZoneOffset = ZonedDateTime.now().offset,
                samples = listOf(
                    SpeedRecord.Sample(
                        time = startTime,
                        speed = Velocity.kilometersPerHour(speedKmh)
                    )
                ),
                metadata = Metadata.autoRecorded(device),
            )

            healthConnectClient.insertRecords(listOf(speedRecord))
            Log.i(TAG, "Velocidade inserida: ${speedKmh}km/h")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao inserir dados de velocidade", e)
            false
        }
    }

    suspend fun writePowerData(watts: Double, startTime: Instant, endTime: Instant): Boolean {
        if (!isHealthConnectAvailable || !checkAllPermissions()) return false

        return try {
            val powerRecord = PowerRecord(
                startTime = startTime,
                startZoneOffset = ZonedDateTime.now().offset,
                endTime = endTime,
                endZoneOffset = ZonedDateTime.now().offset,
                samples = listOf(
                    PowerRecord.Sample(
                        time = startTime,
                        power = Power.watts(watts)
                    )
                ),
                metadata = Metadata.autoRecorded(device),
            )

            healthConnectClient.insertRecords(listOf(powerRecord))
            Log.i(TAG, "Potência inserida: ${watts}W")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao inserir dados de potência", e)
            false
        }
    }

    private suspend fun readStepsData(): List<StepData> {
        if (!isHealthConnectAvailable) return emptyList()

        return try {
            val request = ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(
                    Instant.now().minusSeconds(24 * 60 * 60),
                    Instant.now()
                )
            )

            val response = healthConnectClient.readRecords(request)
            response.records.map { record ->
                StepData(
                    count = record.count,
                    startTime = record.startTime,
                    endTime = record.endTime
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao ler dados de passos", e)
            emptyList()
        }
    }

    private suspend fun readHeartRateData(): List<HeartRateData> {
        if (!isHealthConnectAvailable) return emptyList()

        return try {
            val request = ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(
                    Instant.now().minusSeconds(24 * 60 * 60),
                    Instant.now()
                )
            )

            val response = healthConnectClient.readRecords(request)
            response.records.map { record ->
                HeartRateData(
                    beatsPerMinute = record.samples.firstOrNull()?.beatsPerMinute ?: 0L,
                    time = record.startTime
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao ler dados de frequência cardíaca", e)
            emptyList()
        }
    }

    private suspend fun readWeightData(): List<WeightData> {
        if (!isHealthConnectAvailable) return emptyList()

        return try {
            val request = ReadRecordsRequest(
                recordType = WeightRecord::class,
                timeRangeFilter = TimeRangeFilter.between(
                    Instant.now().minusSeconds(30 * 24 * 60 * 60),
                    Instant.now()
                )
            )

            val response = healthConnectClient.readRecords(request)
            response.records.map { record ->
                WeightData(
                    weightKg = record.weight.inKilograms,
                    time = record.time
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao ler dados de peso", e)
            emptyList()
        }
    }

    private suspend fun readDistanceData(): List<DistanceData> {
        if (!isHealthConnectAvailable) return emptyList()

        return try {
            val request = ReadRecordsRequest(
                recordType = DistanceRecord::class,
                timeRangeFilter = TimeRangeFilter.between(
                    Instant.now().minusSeconds(7 * 24 * 60 * 60),
                    Instant.now()
                )
            )

            val response = healthConnectClient.readRecords(request)
            response.records.map { record ->
                DistanceData(
                    distanceKm = record.distance.inKilometers,
                    startTime = record.startTime,
                    endTime = record.endTime
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao ler dados de distância", e)
            emptyList()
        }
    }

    private suspend fun readSleepData(): List<SleepData> {
        if (!isHealthConnectAvailable) return emptyList()

        return try {
            val request = ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(
                    Instant.now().minusSeconds(7 * 24 * 60 * 60),
                    Instant.now()
                )
            )

            val response = healthConnectClient.readRecords(request)
            response.records.map { record ->
                SleepData(
                    startTime = record.startTime,
                    endTime = record.endTime,
                    duration = Duration.between(record.startTime, record.endTime),
                    stages = emptyList()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao ler dados de sono", e)
            emptyList()
        }
    }

    private suspend fun readExerciseData(): List<ExerciseData> {
        if (!isHealthConnectAvailable) return emptyList()

        return try {
            val request = ReadRecordsRequest(
                recordType = ActiveCaloriesBurnedRecord::class,
                timeRangeFilter = TimeRangeFilter.between(
                    Instant.now().minusSeconds(7 * 24 * 60 * 60),
                    Instant.now()
                )
            )

            val response = healthConnectClient.readRecords(request)
            response.records.map { record ->
                ExerciseData(
                    sessionType = EXERCISE_TYPE_WALKING,
                    title = "Atividade",
                    startTime = record.startTime,
                    endTime = record.endTime,
                    duration = Duration.between(record.startTime, record.endTime),
                    caloriesBurned = record.energy.inKilocalories
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao ler dados de exercício", e)
            emptyList()
        }
    }

    private suspend fun getAggregatedData(): AggregatedData? {
        if (!isHealthConnectAvailable) return null

        return try {
            val aggregateRequest = AggregateRequest(
                metrics = setOf(
                    StepsRecord.COUNT_TOTAL,
                    DistanceRecord.DISTANCE_TOTAL,
                    ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL
                ),
                timeRangeFilter = TimeRangeFilter.between(
                    Instant.now().minusSeconds(24 * 60 * 60),
                    Instant.now()
                )
            )

            val response = healthConnectClient.aggregate(aggregateRequest)

            AggregatedData(
                totalSteps = response[StepsRecord.COUNT_TOTAL] ?: 0L,
                totalDistanceKm = response[DistanceRecord.DISTANCE_TOTAL]?.inKilometers ?: 0.0,
                totalActiveCalories = response[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories
                    ?: 0.0
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter dados agregados", e)
            null
        }
    }

    // Métodos de conveniência
    fun startWalking(title: String = "Caminhada") = startExercise(EXERCISE_TYPE_WALKING, title)
    fun startRunning(title: String = "Corrida") = startExercise(EXERCISE_TYPE_RUNNING, title)
    fun startCycling(title: String = "Ciclismo") = startExercise(EXERCISE_TYPE_CYCLING, title)
    fun startWeightlifting(title: String = "Musculação") =
        startExercise(EXERCISE_TYPE_WEIGHTLIFTING, title)

    fun startYoga(title: String = "Yoga") = startExercise(EXERCISE_TYPE_YOGA, title)
    fun startSwimming(title: String = "Natação") = startExercise(EXERCISE_TYPE_SWIMMING, title)

    fun isAvailable(): Boolean = isHealthConnectAvailable
}