package com.naturewidget.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.naturewidget.app.data.api.NetworkModule
import com.naturewidget.app.data.api.Observation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

class NatureRepository(private val context: Context) {
    
    private val api = NetworkModule.api
    
    companion object {
        private const val TAG = "NatureRepository"
        private const val CACHE_DIR = "nature_images"
        private const val CURRENT_IMAGE = "current_observation.jpg"
        private const val CURRENT_DATA = "current_observation.txt"
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
            val response = api.getObservations(
                taxonId = taxonId,
                iconicTaxa = iconicTaxa,
                userLogin = userLogin?.takeIf { it.isNotBlank() },
                lat = lat,
                lng = lng,
                radius = radius,
                perPage = 20,
                orderBy = "random"
            )
            
            val observation = response.results
                .filter { it.photos.isNotEmpty() }
                .randomOrNull()
            
            if (observation != null) {
                Result.success(observation)
            } else {
                Result.failure(Exception("No observations found"))
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
            val data = buildString {
                appendLine(observation.taxon?.preferredCommonName ?: observation.speciesGuess ?: "Unknown species")
                appendLine(observation.taxon?.name ?: "")
                appendLine(observation.placeGuess ?: "")
                appendLine(photo.attribution ?: "")
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
            attribution = lines.getOrNull(3) ?: ""
        )
    }
}

data class ObservationDisplayData(
    val commonName: String,
    val scientificName: String,
    val location: String,
    val attribution: String
)
