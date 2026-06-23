package us.bensley.fieldrelay.location

import us.bensley.fieldrelay.data.Settings
import java.util.Locale
import kotlin.math.roundToInt

private const val APRS_DESTINATION = "APFDLD"

fun buildAprsIsLogin(settings: Settings, appVersion: String): String {
    val callsign = normalizedAprsCallsign(settings.aprsCallsign)
    val passcode = settings.aprsPasscode.trim()
    return "user $callsign pass $passcode vers FieldRelay $appVersion"
}

fun buildAprsPositionPacket(fix: LocationFix, settings: Settings): String {
    val callsign = normalizedAprsCallsign(settings.aprsCallsign)
    val symbolTable = settings.aprsSymbolTable.singleAprsCharOrDefault("/")
    val symbolCode = settings.aprsSymbolCode.singleAprsCharOrDefault(">")
    val course = fix.bearing.roundToInt().floorMod(360)
    val speedKnots = (fix.metersPerSecond * 1.94384).roundToInt().coerceAtLeast(0)
    val altitudeFeet = (fix.elev * 3.28084).roundToInt()
    val comment = buildAprsComment(settings)
    val body = buildString {
        append("!")
        append(formatAprsLatitude(fix.lat))
        append(symbolTable)
        append(formatAprsLongitude(fix.lon))
        append(symbolCode)
        append(String.format(Locale.US, "%03d/%03d", course, speedKnots.coerceAtMost(999)))
        append("/A=")
        append(String.format(Locale.US, "%06d", altitudeFeet.coerceIn(0, 999_999)))
        if (comment.isNotBlank()) {
            append(" ")
            append(comment)
        }
    }
    return "$callsign>$APRS_DESTINATION,TCPIP*:$body"
}

fun hasValidAprsConfiguration(settings: Settings): Boolean =
    settings.aprsEnabled &&
        settings.aprsCallsign.isNotBlank() &&
        settings.aprsPasscode.isNotBlank() &&
        settings.aprsServer.isNotBlank() &&
        settings.aprsPort in 1..65_535

fun buildAprsComment(settings: Settings): String =
    listOfNotNull(
        formatAprsFrequency(settings.aprsFrequencyMhz),
        formatAprsTone(settings.aprsToneMode, settings.aprsTone),
        formatAprsOffset(settings.aprsOffset),
        settings.aprsRange.sanitizeAprsToken().ifBlank { null },
        settings.aprsComment.sanitizeAprsComment().ifBlank { null },
    ).joinToString(" ").take(43).trim()

fun formatAprsFrequency(value: String): String? {
    val trimmed = value.trim()
    if (trimmed.isBlank()) return null
    val mhz = trimmed.toDoubleOrNull() ?: return null
    if (mhz <= 0.0 || mhz >= 1_000.0) return null
    return String.format(Locale.US, "%07.3fMHz", mhz)
}

fun isValidAprsOffset(value: String): Boolean =
    value.isBlank() || formatAprsOffset(value) != null

fun formatAprsOffset(value: String): String? {
    val trimmed = value.trim()
    if (trimmed.isBlank()) return null
    val offsetMhz = trimmed.toDoubleOrNull() ?: return null
    if (offsetMhz < -9.9 || offsetMhz > 9.9) return null
    val offsetUnits = (offsetMhz * 100).roundToInt()
    if (offsetUnits == 0) return "-000"
    val sign = if (offsetUnits > 0) "+" else "-"
    return sign + kotlin.math.abs(offsetUnits).toString().padStart(3, '0')
}

private fun normalizedAprsCallsign(value: String): String =
    value.trim().uppercase(Locale.US)

private fun String.singleAprsCharOrDefault(default: String): Char =
    trim().firstOrNull() ?: default.first()

private fun String.sanitizeAprsComment(): String =
    replace('\r', ' ')
        .replace('\n', ' ')
        .take(43)
        .trim()

private fun String.sanitizeAprsToken(): String =
    replace('\r', ' ')
        .replace('\n', ' ')
        .trim()
        .take(8)

private fun Int.floorMod(modulus: Int): Int = ((this % modulus) + modulus) % modulus
