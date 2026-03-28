package com.naturewidget.app.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

interface INaturalistApi {
    
    @GET("observations")
    suspend fun getObservations(
        @Query("quality_grade") qualityGrade: String = "research",
        @Query("photos") photos: Boolean = true,
        @Query("per_page") perPage: Int = 20,
        @Query("order_by") orderBy: String = "random",
        @Query("taxon_id") taxonId: Int? = null,
        @Query("iconic_taxa") iconicTaxa: String? = null,
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null,
        @Query("radius") radius: Int? = null,
        @Query("locale") locale: String = "en"
    ): ObservationsResponse
    
    companion object {
        const val BASE_URL = "https://api.inaturalist.org/v1/"
    }
}

@Serializable
data class ObservationsResponse(
    @SerialName("total_results") val totalResults: Int,
    val results: List<Observation>
)

@Serializable
data class Observation(
    val id: Long,
    val uuid: String,
    @SerialName("species_guess") val speciesGuess: String? = null,
    @SerialName("place_guess") val placeGuess: String? = null,
    @SerialName("observed_on") val observedOn: String? = null,
    val photos: List<Photo> = emptyList(),
    val taxon: Taxon? = null,
    val user: User? = null
)

@Serializable
data class Photo(
    val id: Long,
    val url: String,
    @SerialName("license_code") val licenseCode: String? = null,
    val attribution: String? = null,
    @SerialName("original_dimensions") val originalDimensions: Dimensions? = null
) {
    // Get different size URLs by replacing "square" in the URL
    fun getMediumUrl(): String = url.replace("/square.", "/medium.")
    fun getLargeUrl(): String = url.replace("/square.", "/large.")
    fun getOriginalUrl(): String = url.replace("/square.", "/original.")
}

@Serializable
data class Dimensions(
    val width: Int,
    val height: Int
)

@Serializable
data class Taxon(
    val id: Long,
    val name: String,
    val rank: String? = null,
    @SerialName("preferred_common_name") val preferredCommonName: String? = null,
    @SerialName("iconic_taxon_name") val iconicTaxonName: String? = null,
    @SerialName("wikipedia_url") val wikipediaUrl: String? = null
)

@Serializable
data class User(
    val id: Long,
    val login: String,
    val name: String? = null,
    @SerialName("icon_url") val iconUrl: String? = null
)
