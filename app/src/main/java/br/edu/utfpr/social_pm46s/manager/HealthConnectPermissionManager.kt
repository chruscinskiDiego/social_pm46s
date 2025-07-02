package br.edu.utfpr.social_pm46s.manager

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class HealthConnectPermissionManager private constructor(context: Context) {

    private val _permissionsGranted = MutableStateFlow(false)
    val permissionsGranted: StateFlow<Boolean> = _permissionsGranted

    private val _isHealthConnectAvailable = MutableStateFlow(false)
    val isHealthConnectAvailable: StateFlow<Boolean> = _isHealthConnectAvailable

    private lateinit var healthConnectClient: HealthConnectClient

    companion object {
        private const val TAG = "HealthConnectPermissions"

        @Volatile
        private var INSTANCE: HealthConnectPermissionManager? = null

        fun getInstance(context: Context): HealthConnectPermissionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: HealthConnectPermissionManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    init {
        checkAvailability(context.applicationContext)
    }

    private fun checkAvailability(context: Context) {
        val isAvailable =
            HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
        _isHealthConnectAvailable.value = isAvailable

        if (isAvailable) {
            healthConnectClient = HealthConnectClient.getOrCreate(context)
            Log.i(TAG, "Health Connect disponível e inicializado")
        } else {
            Log.w(TAG, "Health Connect não disponível neste dispositivo")
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
            HealthPermission.getWritePermission(PowerRecord::class),
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            HealthPermission.getWritePermission(ExerciseSessionRecord::class)
        )
    }

    suspend fun checkPermissions(): Boolean {
        if (!_isHealthConnectAvailable.value || !::healthConnectClient.isInitialized) {
            return false
        }

        return try {
            val requiredPermissions = getRequiredPermissions()
            val grantedPermissions =
                healthConnectClient.permissionController.getGrantedPermissions()
            val hasAllPermissions = grantedPermissions.containsAll(requiredPermissions)

            _permissionsGranted.value = hasAllPermissions
            hasAllPermissions
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao verificar permissões", e)
            _permissionsGranted.value = false
            false
        }
    }

    fun updatePermissionStatus(granted: Boolean) {
        _permissionsGranted.value = granted
    }

    fun reset() {
        _permissionsGranted.value = false
    }
}