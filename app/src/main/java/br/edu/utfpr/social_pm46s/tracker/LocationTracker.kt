package br.edu.utfpr.social_pm46s.tracker

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LocationTracker(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
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
                    if (distance > 5) {
                        _totalDistance.value += distance / 1000.0

                        val timeDiff = (newLocation.time - lastLoc.time) / 1000.0 / 60.0
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

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun startTracking(): Boolean {
        return if (hasLocationPermission() && !isTracking) {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            isTracking = true
            _totalDistance.value = 0.0
            _currentPace.value = null
            lastLocation = null
            true
        } else {
            false
        }
    }

    fun stopTracking(): Double {
        if (isTracking) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            isTracking = false
            lastLocation = null
        }
        return _totalDistance.value
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    fun getCurrentDistance(): Double = _totalDistance.value

    fun isCurrentlyTracking(): Boolean = isTracking
}