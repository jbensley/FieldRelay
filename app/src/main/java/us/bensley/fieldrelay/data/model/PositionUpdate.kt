package us.bensley.fieldrelay.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PositionUpdate(
    val id: String,
    @SerialName("report_at") val reportAt: String,
    val lat: Double,
    val lon: Double,
    val elev: Double,
    val mph: Double,
    val dir: Double,
    val active: Int,
    val gps: Int,
)

@Serializable
data class UpdateResponse(
    val success: Boolean = false,
    val message: String? = null,
)