package com.naturewidget.app.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.work.*
import com.naturewidget.app.data.SettingsManager
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
         * Schedule periodic updates (every 4 hours by default)
         */
        fun enqueuePeriodic(context: Context, intervalHours: Long = 4) {
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
                    ExistingPeriodicWorkPolicy.KEEP,
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
            
            // Get user settings
            val userLogin = settings.getUserLogin()
            
            // Fetch a random observation
            val observationResult = repository.getRandomObservation(
                userLogin = userLogin
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
}
