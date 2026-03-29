package com.naturewidget.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.naturewidget.app.data.SettingsManager
import com.naturewidget.app.data.SettingsManager.Companion.WidgetMode
import com.naturewidget.app.data.api.NetworkModule
import com.naturewidget.app.data.api.Observation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class NatureRepository(private val context: Context) {
    
    private val api = NetworkModule.api
    private val settings = SettingsManager.getInstance(context)
    
    companion object {
        private const val TAG = "NatureRepository"
        private const val CACHE_DIR = "nature_images"
        private const val CURRENT_IMAGE = "current_observation.jpg"
        private const val CURRENT_DATA = "current_observation.txt"
        private const val DISCOVER_RADIUS_KM = 50 // km radius for discover mode
        private const val DISCOVER_DAYS_BACK = 7 // days back for recent observations
    }
    
    /**
     * Fetch observation based on current widget mode
     */
    suspend fun getObservationForCurrentMode(
        currentLat: Double? = null,
        currentLng: Double? = null
    ): Result<Observation> {
        val mode = settings.getWidgetMode()
        val userLogin = settings.getUserLogin()
        
        Log.d(TAG, "Fetching observation for mode: $mode")
        
        return when (mode) {
            WidgetMode.PERSONAL -> {
                if (userLogin.isBlank()) {
                    Result.failure(Exception("Please set your iNaturalist username in settings for Personal mode"))
                } else {
                    getRandomObservation(userLogin = userLogin)
                }
            }
            WidgetMode.ALL -> {
                getRandomObservation()
            }
            WidgetMode.DISCOVER -> {
                if (currentLat == null || currentLng == null) {
                    Result.failure(Exception("Location required for Discover mode. Please enable location access."))
                } else {
                    getDiscoverObservation(currentLat, currentLng)
                }
            }
        }
    }
    
    /**
     * Fetch recent observations near the given location
     */
    private suspend fun getDiscoverObservation(
        lat: Double,
        lng: Double
    ): Result<Observation> = withContext(Dispatchers.IO) {
        try {
            val locale = settings.getEffectiveLocale()
            
            // Calculate date for "recent" observations (last 7 days)
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -DISCOVER_DAYS_BACK)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val createdAfter = dateFormat.format(calendar.time)
            
            Log.d(TAG, "Discover mode: lat=$lat, lng=$lng, radius=$DISCOVER_RADIUS_KM km, after=$createdAfter")
            
            val response = api.getObservations(
                qualityGrade = "research",
                lat = lat,
                lng = lng,
                radius = DISCOVER_RADIUS_KM,
                createdAfter = createdAfter,
                perPage = 30,
                orderBy = "created_at",
                order = "desc",
                locale = locale
            )
            
            Log.d(TAG, "Discover: Got ${response.results.size} results")
            
            val observation = response.results
                .filter { it.photos.isNotEmpty() }
                .randomOrNull()
            
            if (observation != null) {
                Result.success(observation)
            } else {
                Result.failure(Exception("No recent observations found within ${DISCOVER_RADIUS_KM}km. Try again later!"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in discover mode", e)
            Result.failure(e)
        }
    }
    
    /**
     * Fetch a random nature observation with photos
     */
    suspend fun getRandomObservation(
        taxonId: Int? = null,
        iconicTaxa: String? = null, // e.g., "Aves" for birds, "Mammalia" for mammals
        userLogin: String? = null, // iNaturalist username
        lat: Double? = null,
        lng: Double? = null,
        radius: Int? = null
    ): Result<Observation> = withContext(Dispatchers.IO) {
        try {
            // Only require research grade when not filtering by user
            val qualityGrade = if (userLogin.isNullOrBlank()) "research" else null
            
            Log.d(TAG, "Fetching observations - user: $userLogin, quality: $qualityGrade")
            
            val locale = settings.getEffectiveLocale()
            Log.d(TAG, "Using locale: $locale")
            
            val response = api.getObservations(
                qualityGrade = qualityGrade,
                taxonId = taxonId,
                iconicTaxa = iconicTaxa,
                userLogin = userLogin?.takeIf { it.isNotBlank() },
                lat = lat,
                lng = lng,
                radius = radius,
                perPage = 20,
                orderBy = "random",
                locale = locale
            )
            
            Log.d(TAG, "Got ${response.results.size} results, total: ${response.totalResults}")
            
            val observation = response.results
                .filter { it.photos.isNotEmpty() }
                .randomOrNull()
            
            if (observation != null) {
                Result.success(observation)
            } else {
                val msg = if (userLogin.isNullOrBlank()) {
                    "No observations found"
                } else {
                    "No observations with photos found for user '$userLogin'. Check the username?"
                }
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching observation", e)
            Result.failure(e)
        }
    }
    
    /**
     * Download and cache an image for widget use
     */
    suspend fun downloadAndCacheImage(observation: Observation): Result<File> = withContext(Dispatchers.IO) {
        try {
            val photo = observation.photos.firstOrNull()
                ?: return@withContext Result.failure(Exception("No photo available"))
            
            val imageUrl = photo.getMediumUrl()
            Log.d(TAG, "Downloading image: $imageUrl")
            
            // Download image
            val connection = URL(imageUrl).openConnection()
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            
            val inputStream = connection.getInputStream()
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            
            if (bitmap == null) {
                return@withContext Result.failure(Exception("Failed to decode image"))
            }
            
            // Save to cache
            val cacheDir = File(context.cacheDir, CACHE_DIR)
            if (!cacheDir.exists()) cacheDir.mkdirs()
            
            val imageFile = File(cacheDir, CURRENT_IMAGE)
            val outputStream = imageFile.outputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.close()
            
            // Save observation data
            val dataFile = File(cacheDir, CURRENT_DATA)
            val observationUrl = "https://www.inaturalist.org/observations/${observation.id}"
            val data = buildString {
                appendLine(observation.taxon?.preferredCommonName ?: observation.speciesGuess ?: "Unknown species")
                appendLine(observation.taxon?.name ?: "")
                appendLine(observation.placeGuess ?: "")
                appendLine(photo.attribution ?: "")
                appendLine(observationUrl)
            }
            dataFile.writeText(data)
            
            Log.d(TAG, "Image cached successfully: ${imageFile.absolutePath}")
            Result.success(imageFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading image", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get cached image file
     */
    fun getCachedImageFile(): File? {
        val cacheDir = File(context.cacheDir, CACHE_DIR)
        val imageFile = File(cacheDir, CURRENT_IMAGE)
        return if (imageFile.exists()) imageFile else null
    }
    
    /**
     * Get cached observation data
     */
    fun getCachedObservationData(): ObservationDisplayData? {
        val cacheDir = File(context.cacheDir, CACHE_DIR)
        val dataFile = File(cacheDir, CURRENT_DATA)
        
        if (!dataFile.exists()) return null
        
        val lines = dataFile.readLines()
        return ObservationDisplayData(
            commonName = lines.getOrNull(0) ?: "",
            scientificName = lines.getOrNull(1) ?: "",
            location = lines.getOrNull(2) ?: "",
            attribution = lines.getOrNull(3) ?: "",
            observationUrl = lines.getOrNull(4) ?: ""
        )
    }
}

data class ObservationDisplayData(
    val commonName: String,
    val scientificName: String,
    val location: String,
    val attribution: String,
    val observationUrl: String = ""
)
