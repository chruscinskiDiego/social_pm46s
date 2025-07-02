package br.edu.utfpr.social_pm46s.service

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
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
import java.time.ZoneOffset
import kotlin.reflect.KClass

class HealthConnectService(private val context: Context) {

    private lateinit var healthConnectClient: HealthConnectClient
    private var isHealthConnectAvailable = false
    private var activeExerciseSession: ActiveExerciseSession? = null

    private lateinit var device: Device

    companion object {
        private const val TAG = "HealthConnectService"
        private val TOLERANCE_DURATION = Duration.ofSeconds(30)

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
        val isAvailable =
            HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
        isHealthConnectAvailable = isAvailable
        device = Device(
            type = Device.TYPE_PHONE,
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
        )

        if (isAvailable) {
            healthConnectClient = HealthConnectClient.getOrCreate(context)
            Log.i(TAG, "Health Connect disponível e inicializado")
        } else {
            Log.w(TAG, "Health Connect não disponível neste dispositivo")
        }
    }

    suspend fun checkAllPermissions(): Boolean {
        if (!isHealthConnectAvailable) return false

        return try {
            val requiredPermissions = getRequiredPermissions()
            val grantedPermissions =
                healthConnectClient.permissionController.getGrantedPermissions()
            grantedPermissions.containsAll(requiredPermissions)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao verificar permissões", e)
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
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            HealthPermission.getWritePermission(ExerciseSessionRecord::class)
        )
    }

    suspend fun <T : Record> validateDataBeforeInsertion(
        startTime: Instant,
        endTime: Instant,
        recordType: KClass<T>
    ): Boolean {
        if (!isHealthConnectAvailable) return true

        return try {
            val request = ReadRecordsRequest(
                recordType = recordType,
                timeRangeFilter = TimeRangeFilter.between(
                    startTime.minus(TOLERANCE_DURATION),
                    endTime.plus(TOLERANCE_DURATION)
                )
            )

            val response = healthConnectClient.readRecords(request)

            val hasDuplicates = response.records.any { record ->
                val recordStart = when (record) {
                    is StepsRecord -> record.startTime
                    is HeartRateRecord -> record.startTime
                    is WeightRecord -> record.time
                    is DistanceRecord -> record.startTime
                    is ActiveCaloriesBurnedRecord -> record.startTime
                    else -> null
                }

                val recordEnd = when (record) {
                    is StepsRecord -> record.endTime
                    is HeartRateRecord -> record.endTime
                    is WeightRecord -> record.time
                    is DistanceRecord -> record.endTime
                    is ActiveCaloriesBurnedRecord -> record.endTime
                    else -> null
                }

                if (recordStart != null && recordEnd != null) {
                    val overlap = !(recordEnd.isBefore(startTime) || recordStart.isAfter(endTime))
                    if (overlap) {
                        Log.w(TAG, "Sobreposição detectada para ${recordType.simpleName}")
                    }
                    overlap
                } else false
            }

            if (hasDuplicates) {
                Log.w(
                    TAG,
                    "Dados duplicados detectados para ${recordType.simpleName} - inserção cancelada"
                )
            }

            !hasDuplicates
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao validar duplicação para ${recordType.simpleName}", e)
            true
        }
    }

    suspend fun getAllHealthData(): HealthData? {
        if (!isHealthConnectAvailable) return null

        return try {
            val steps = readStepsData()
            val heartRate = readHeartRateData()
            val weight = readWeightData()
            val distance = readDistanceData()
            val sleep = readSleepData()
            val exercises = readExerciseData()
            val aggregated = getAggregatedData()

            HealthData(
                steps = steps,
                heartRate = heartRate,
                weight = weight,
                distance = distance,
                sleep = sleep,
                exercises = exercises,
                aggregated = aggregated
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar dados do Health Connect", e)
            null
        }
    }

    fun startExercise(sessionType: Int, title: String): Boolean {
        if (!isHealthConnectAvailable) {
            Log.w(TAG, "Health Connect não disponível para iniciar exercício")
            return false
        }

        if (activeExerciseSession != null) {
            Log.w(TAG, "Já existe uma sessão de exercício ativa")
            return false
        }

        activeExerciseSession = ActiveExerciseSession(
            sessionType = sessionType,
            title = title,
            startTime = Instant.now()
        )

        Log.i(TAG, "Exercício iniciado: $title (tipo: $sessionType)")
        return true
    }

    suspend fun finishExerciseComplete(workoutSummary: WorkoutSummary): Boolean {
        val session = activeExerciseSession ?: return false

        return finishExerciseWithValidation(
            caloriesBurned = workoutSummary.caloriesBurned,
            distanceKm = workoutSummary.distanceKm,
            steps = workoutSummary.steps,
            avgHeartRate = workoutSummary.averageHeartRate,
            notes = "Exercício registrado via app - Duração: ${workoutSummary.duration}"
        )
    }

    suspend fun finishExerciseWithValidation(
        caloriesBurned: Double? = null,
        distanceKm: Double? = null,
        steps: Long? = null,
        avgHeartRate: Long? = null,
        notes: String? = null
    ): Boolean {
        val session = activeExerciseSession ?: return false
        val endTime = Instant.now()

        return try {
            var recordsSaved = 0

            caloriesBurned?.let { calories ->
                if (calories > 0 && validateDataBeforeInsertion(
                        session.startTime,
                        endTime,
                        ActiveCaloriesBurnedRecord::class
                    )
                ) {
                    if (writeCaloriesData(calories, session.startTime, endTime)) {
                        recordsSaved++
                    }
                }
            }

            distanceKm?.let { distance ->
                if (distance > 0 && validateDataBeforeInsertion(
                        session.startTime,
                        endTime,
                        DistanceRecord::class
                    )
                ) {
                    if (writeDistanceData(distance, session.startTime, endTime)) {
                        recordsSaved++
                    }
                }
            }

            steps?.let { stepCount ->
                if (stepCount > 0 && validateDataBeforeInsertion(
                        session.startTime,
                        endTime,
                        StepsRecord::class
                    )
                ) {
                    if (writeStepsData(stepCount, session.startTime, endTime)) {
                        recordsSaved++
                    }
                }
            }

            avgHeartRate?.let { heartRate ->
                if (heartRate > 0) {
                    if (writeHeartRateData(heartRate, session.startTime, endTime)) {
                        recordsSaved++
                    }
                }
            }

            if (recordsSaved > 0) {
                Log.i(TAG, "Exercício completo salvo com $recordsSaved registros: ${session.title}")
            } else {
                Log.w(TAG, "Nenhum dado foi salvo para o exercício: ${session.title}")
            }

            activeExerciseSession = null
            true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao finalizar exercício com validação", e)
            false
        }
    }

    suspend fun cancelExercise(): Boolean {
        return if (activeExerciseSession != null) {
            Log.i(TAG, "Exercício cancelado: ${activeExerciseSession?.title}")
            activeExerciseSession = null
            true
        } else {
            false
        }
    }

    fun getActiveExerciseSession(): ActiveExerciseSession? = activeExerciseSession

    suspend fun writeStepsData(steps: Long, startTime: Instant, endTime: Instant): Boolean {
        if (!isHealthConnectAvailable || !validateDataBeforeInsertion(
                startTime,
                endTime,
                StepsRecord::class
            )
        ) {
            return false
        }

        return try {
            val stepsRecord = StepsRecord(
                startTime = startTime,
                startZoneOffset = ZoneOffset.systemDefault().rules.getOffset(startTime),
                endTime = endTime,
                endZoneOffset = ZoneOffset.systemDefault().rules.getOffset(endTime),
                count = steps,
                metadata = Metadata.autoRecorded(device)
            )

            healthConnectClient.insertRecords(listOf(stepsRecord))
            Log.i(TAG, "Dados de passos salvos: $steps passos")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao salvar dados de passos", e)
            false
        }
    }

    suspend fun writeWeightData(weightKg: Double, time: Instant): Boolean {
        if (!isHealthConnectAvailable || !validateDataBeforeInsertion(
                time,
                time,
                WeightRecord::class
            )
        ) {
            return false
        }

        return try {
            val weightRecord = WeightRecord(
                time = time,
                zoneOffset = ZoneOffset.systemDefault().rules.getOffset(time),
                weight = Mass.kilograms(weightKg),
                metadata = Metadata.autoRecorded(device)
            )

            healthConnectClient.insertRecords(listOf(weightRecord))
            Log.i(TAG, "Dados de peso salvos: ${weightKg}kg")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao salvar dados de peso", e)
            false
        }
    }

    suspend fun writeDistanceData(
        distanceKm: Double,
        startTime: Instant,
        endTime: Instant
    ): Boolean {
        if (!isHealthConnectAvailable || !validateDataBeforeInsertion(
                startTime,
                endTime,
                DistanceRecord::class
            )
        ) {
            return false
        }

        return try {
            val distanceRecord = DistanceRecord(
                startTime = startTime,
                startZoneOffset = ZoneOffset.systemDefault().rules.getOffset(startTime),
                endTime = endTime,
                endZoneOffset = ZoneOffset.systemDefault().rules.getOffset(endTime),
                distance = Length.kilometers(distanceKm),
                metadata = Metadata.autoRecorded(device)
            )

            healthConnectClient.insertRecords(listOf(distanceRecord))
            Log.i(TAG, "Dados de distância salvos: ${distanceKm}km")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao salvar dados de distância", e)
            false
        }
    }

    suspend fun writeCaloriesData(calories: Double, startTime: Instant, endTime: Instant): Boolean {
        if (!isHealthConnectAvailable || !validateDataBeforeInsertion(
                startTime,
                endTime,
                ActiveCaloriesBurnedRecord::class
            )
        ) {
            return false
        }

        return try {
            val caloriesRecord = ActiveCaloriesBurnedRecord(
                startTime = startTime,
                startZoneOffset = ZoneOffset.systemDefault().rules.getOffset(startTime),
                endTime = endTime,
                endZoneOffset = ZoneOffset.systemDefault().rules.getOffset(endTime),
                energy = Energy.kilocalories(calories),
                metadata = Metadata.autoRecorded(device)
            )

            healthConnectClient.insertRecords(listOf(caloriesRecord))
            Log.i(TAG, "Dados de calorias salvos: ${calories}kcal")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao salvar dados de calorias", e)
            false
        }
    }

    suspend fun writeHeartRateData(
        beatsPerMinute: Long,
        startTime: Instant,
        endTime: Instant
    ): Boolean {
        if (!isHealthConnectAvailable || !validateDataBeforeInsertion(
                startTime,
                endTime,
                HeartRateRecord::class
            )
        ) {
            return false
        }

        return try {
            val heartRateRecord = HeartRateRecord(
                startTime = startTime,
                startZoneOffset = ZoneOffset.systemDefault().rules.getOffset(startTime),
                endTime = endTime,
                endZoneOffset = ZoneOffset.systemDefault().rules.getOffset(endTime),
                samples = listOf(
                    HeartRateRecord.Sample(
                        time = startTime,
                        beatsPerMinute = beatsPerMinute
                    )
                ),
                metadata = Metadata.autoRecorded(device)
            )

            healthConnectClient.insertRecords(listOf(heartRateRecord))
            Log.i(TAG, "Dados de frequência cardíaca salvos: ${beatsPerMinute}bpm")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao salvar dados de frequência cardíaca", e)
            false
        }
    }

    private suspend fun readStepsData(): List<StepData> {
        if (!isHealthConnectAvailable) return emptyList()

        return try {
            val request = ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(
                    Instant.now().minus(Duration.ofDays(7)),
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
                    Instant.now().minus(Duration.ofDays(7)),
                    Instant.now()
                )
            )

            val response = healthConnectClient.readRecords(request)
            response.records.flatMap { record ->
                record.samples.map { sample ->
                    HeartRateData(
                        beatsPerMinute = sample.beatsPerMinute,
                        time = sample.time
                    )
                }
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
                    Instant.now().minus(Duration.ofDays(30)),
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
                    Instant.now().minus(Duration.ofDays(7)),
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
                    Instant.now().minus(Duration.ofDays(7)),
                    Instant.now()
                )
            )

            val response = healthConnectClient.readRecords(request)
            response.records.map { record ->
                SleepData(
                    startTime = record.startTime,
                    endTime = record.endTime,
                    duration = Duration.between(record.startTime, record.endTime)
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
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(
                    Instant.now().minus(Duration.ofDays(7)),
                    Instant.now()
                )
            )

            val response = healthConnectClient.readRecords(request)
            response.records.map { record ->
                ExerciseData(
                    sessionType = record.exerciseType,
                    title = record.title ?: "Exercício",
                    startTime = record.startTime,
                    endTime = record.endTime,
                    duration = Duration.between(record.startTime, record.endTime),
                    caloriesBurned = 0.0
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
            val today = Instant.now()
            val startOfDay = today.minus(Duration.ofDays(1))

            val request = AggregateRequest(
                metrics = setOf(
                    StepsRecord.COUNT_TOTAL,
                    ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL,
                    DistanceRecord.DISTANCE_TOTAL
                ),
                timeRangeFilter = TimeRangeFilter.between(startOfDay, today)
            )

            val response = healthConnectClient.aggregate(request)

            AggregatedData(
                totalSteps = response[StepsRecord.COUNT_TOTAL] ?: 0L,
                totalDistanceKm = response[DistanceRecord.DISTANCE_TOTAL]?.inKilometers ?: 0.0,
                totalActiveCalories = response[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories
                    ?: 0.0
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar dados agregados", e)
            null
        }
    }

    fun startWalking(title: String = "Caminhada") = startExercise(EXERCISE_TYPE_WALKING, title)
    fun startRunning(title: String = "Corrida") = startExercise(EXERCISE_TYPE_RUNNING, title)
    fun startCycling(title: String = "Ciclismo") = startExercise(EXERCISE_TYPE_CYCLING, title)
    fun startWeightlifting(title: String = "Musculação") =
        startExercise(EXERCISE_TYPE_WEIGHTLIFTING, title)

    fun startYoga(title: String = "Yoga") = startExercise(EXERCISE_TYPE_YOGA, title)
    fun startSwimming(title: String = "Natação") = startExercise(EXERCISE_TYPE_SWIMMING, title)

    fun isAvailable(): Boolean = isHealthConnectAvailable
}