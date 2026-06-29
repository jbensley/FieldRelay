package us.bensley.fieldrelay.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OverlandUpdate(
    val locations: List<OverlandLocationFeature>,
)

@Serializable
data class OverlandLocationFeature(
    val type: String = "Feature",
    val geometry: OverlandPointGeometry,
    val properties: OverlandLocationProperties,
)

@Serializable
data class OverlandPointGeometry(
    val type: String = "Point",
    val coordinates: List<Double>,
)

@Serializable
data class OverlandLocationProperties(
    val timestamp: String,
    val altitude: Double,
    val speed: Double,
    val course: Double,
    @SerialName("device_id") val deviceId: String,
)

@Serializable
data class OverlandResponse(
    val result: String? = null,
    val error: String? = null,
)
