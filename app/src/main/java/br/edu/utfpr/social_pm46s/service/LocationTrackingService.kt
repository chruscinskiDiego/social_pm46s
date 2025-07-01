package br.edu.utfpr.social_pm46s.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import br.edu.utfpr.social_pm46s.tracker.LocationTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class LocationTrackingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var locationTracker: LocationTracker

    override fun onCreate() {
        super.onCreate()
        locationTracker = LocationTracker(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_LOCATION_TRACKING" -> {
                locationTracker.startTracking()
            }
            "STOP_LOCATION_TRACKING" -> {
                locationTracker.stopTracking()
                stopSelf()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        locationTracker.stopTracking()
    }
}