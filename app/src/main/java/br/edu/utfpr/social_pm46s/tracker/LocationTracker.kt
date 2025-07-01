package br.edu.utfpr.social_pm46s.tracker

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LocationTracker(private val context: Context) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private var lastLocation: Location? = null
    private var isTracking = false

    private val _totalDistance = MutableStateFlow(0.0)
    val totalDistance: StateFlow<Double> = _totalDistance

    private val _currentPace = MutableStateFlow<Double?>(null)
    val currentPace: StateFlow<Double?> = _currentPace

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let { newLocation ->
                lastLocation?.let { lastLoc ->
                    val distance = lastLoc.distanceTo(newLocation)
                    if (distance > 5) { // Filtrar ruído GPS
                        _totalDistance.value += distance / 1000.0 // Converter para km

                        // Calcular pace (min/km)
                        val timeDiff = (newLocation.time - lastLoc.time) / 1000.0 / 60.0 // minutos
                        val distanceKm = distance / 1000.0
                        if (distanceKm > 0) {
                            _currentPace.value = timeDiff / distanceKm
                        }
                    }
                }
                lastLocation = newLocation
            }
        }
    }

    fun startTracking(): Boolean {
        return if (hasLocationPermission() && !isTracking) {
            val locationRequest = LocationRequest.create().apply {
                interval = 5000
                fastestInterval = 2000
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            isTracking = true
            _totalDistance.value = 0.0
            _currentPace.value = null
            true
        } else {
            false
        }
    }

    fun stopTracking(): Double {
        if (isTracking) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            isTracking = false
        }
        return _totalDistance.value
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}