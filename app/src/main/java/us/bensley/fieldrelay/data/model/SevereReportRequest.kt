package us.bensley.fieldrelay.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SevereReportRequest(
    val id: String,
    @SerialName("report_type") val reportType: String,
    val stamp: String,
    val tornado: Int,
    val funnelcloud: Int,
    val wallcloud: Int,
    val rotation: Int,
    val hail: Int,
    val wind: Int,
    val flood: Int,
    val flashflood: Int,
    val other: Int,
    val hailsize: Double,
    val windspeed: Int,
    val windmeasure: Int,
    @SerialName("stamp_exact") val stampExact: Int,
    val damage: Int,
    val injury: Int,
    val narrative: String,
    val lat: Double,
    val lon: Double,
    val gps: Int,
    val nwschat: Int,
    val twitter: Int,
)
