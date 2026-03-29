package com.naturewidget.app.widget

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.work.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Tasks
import com.naturewidget.app.data.SettingsManager
import com.naturewidget.app.data.SettingsManager.Companion.WidgetMode
import com.naturewidget.app.data.repository.NatureRepository
import java.util.concurrent.TimeUnit

class NatureWidgetWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        private const val TAG = "NatureWidgetWorker"
        private const val WORK_NAME_PERIODIC = "nature_widget_periodic"
        private const val WORK_NAME_ONETIME = "nature_widget_onetime"
        
        /**
         * Schedule periodic updates based on user settings
         */
        fun enqueuePeriodic(context: Context) {
            val settings = SettingsManager.getInstance(context)
            val intervalHours = settings.getRefreshInterval().toLong()
            
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val request = PeriodicWorkRequestBuilder<NatureWidgetWorker>(
                intervalHours, TimeUnit.HOURS,
                15, TimeUnit.MINUTES // Flex interval
            )
                .setConstraints(constraints)
                .setInitialDelay(1, TimeUnit.MINUTES) // Small delay on first run
                .build()
            
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME_PERIODIC,
                    ExistingPeriodicWorkPolicy.REPLACE,
                    request
                )
            
            Log.d(TAG, "Periodic work scheduled: every $intervalHours hours")
        }
        
        /**
         * Trigger an immediate refresh
         */
        fun enqueueOneTime(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val request = OneTimeWorkRequestBuilder<NatureWidgetWorker>()
                .setConstraints(constraints)
                .build()
            
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    WORK_NAME_ONETIME,
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            
            Log.d(TAG, "One-time refresh enqueued")
        }
        
        /**
         * Cancel all scheduled work
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_PERIODIC)
            Log.d(TAG, "Periodic work cancelled")
        }
    }
    
    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting widget update work")
        
        return try {
            val repository = NatureRepository(context)
            val settings = SettingsManager.getInstance(context)
            val mode = settings.getWidgetMode()
            
            // Get location if in Discover mode
            var currentLat: Double? = null
            var currentLng: Double? = null
            
            if (mode == WidgetMode.DISCOVER) {
                val location = getCurrentLocation()
                if (location != null) {
                    currentLat = location.latitude
                    currentLng = location.longitude
                    Log.d(TAG, "Got location: $currentLat, $currentLng")
                } else {
                    Log.w(TAG, "Could not get location for Discover mode")
                }
            }
            
            // Fetch observation based on current mode
            val observationResult = repository.getObservationForCurrentMode(
                currentLat = currentLat,
                currentLng = currentLng
            )
            
            if (observationResult.isFailure) {
                Log.e(TAG, "Failed to fetch observation", observationResult.exceptionOrNull())
                return Result.retry()
            }
            
            val observation = observationResult.getOrNull()!!
            Log.d(TAG, "Fetched observation: ${observation.taxon?.preferredCommonName}")
            
            // Download and cache the image
            val imageResult = repository.downloadAndCacheImage(observation)
            
            if (imageResult.isFailure) {
                Log.e(TAG, "Failed to download image", imageResult.exceptionOrNull())
                return Result.retry()
            }
            
            // Update all widget instances
            NatureWidget().updateAll(context)
            
            Log.d(TAG, "Widget updated successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Worker failed", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
    
    /**
     * Get current location for Discover mode
     */
    private fun getCurrentLocation(): Location? {
        // Check for location permission
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        if (!hasPermission) {
            Log.w(TAG, "No location permission")
            return null
        }
        
        return try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            val locationTask = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                null
            )
            // Wait for location with timeout
            Tasks.await(locationTask, 10, TimeUnit.SECONDS)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting location", e)
            null
        }
    }
}
