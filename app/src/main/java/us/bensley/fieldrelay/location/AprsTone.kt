package us.bensley.fieldrelay.location

import us.bensley.fieldrelay.data.AprsToneMode
import java.util.Locale
import kotlin.math.roundToInt

data class AprsToneOption(
    val value: String,
    val label: String,
)

val ctcssToneOptions: List<AprsToneOption> = listOf(
    "67.0", "69.3", "71.9", "74.4", "77.0", "79.7", "82.5", "85.4", "88.5",
    "91.5", "94.8", "97.4", "100.0", "103.5", "107.2", "110.9", "114.8",
    "118.8", "123.0", "127.3", "131.8", "136.5", "141.3", "146.2", "150.0",
    "151.4", "156.7", "159.8", "162.2", "165.5", "167.9", "171.3", "173.8",
    "177.3", "179.9", "183.5", "186.2", "189.9", "192.8", "196.6", "199.5",
    "203.5", "206.5", "210.7", "218.1", "225.7", "229.1", "233.6", "241.8",
    "250.3", "254.1",
).map { AprsToneOption(value = it, label = "$it Hz") }

val dcsToneOptions: List<AprsToneOption> = listOf(
    "023", "025", "026", "031", "032", "043", "047", "051", "054", "065",
    "071", "072", "073", "074", "114", "115", "116", "125", "131", "132",
    "134", "143", "152", "155", "156", "162", "165", "172", "174", "205",
    "223", "226", "243", "244", "245", "251", "261", "263", "265", "271",
    "306", "311", "315", "331", "343", "346", "351", "364", "365", "371",
    "411", "412", "413", "423", "431", "432", "445", "464", "465", "466",
    "503", "506", "516", "532", "546", "565", "606", "612", "624", "627",
    "631", "632", "654", "662", "664", "703", "712", "723", "731", "732",
    "734", "743", "754",
).map { AprsToneOption(value = it, label = "DCS $it") }

fun aprsToneOptions(mode: AprsToneMode): List<AprsToneOption> = when (mode) {
    AprsToneMode.NONE -> emptyList()
    AprsToneMode.CTCSS -> ctcssToneOptions
    AprsToneMode.DCS -> dcsToneOptions
}

fun formatAprsTone(mode: AprsToneMode, value: String): String? {
    val trimmed = value.trim()
    return when (mode) {
        AprsToneMode.NONE -> null
        AprsToneMode.CTCSS -> {
            val toneHz = trimmed.toDoubleOrNull() ?: return null
            if (ctcssToneOptions.none { it.value == String.format(Locale.US, "%.1f", toneHz) }) return null
            "C${toneHz.roundToInt().toString().padStart(3, '0')}"
        }
        AprsToneMode.DCS -> {
            val code = trimmed.padStart(3, '0')
            if (dcsToneOptions.none { it.value == code }) return null
            "D$code"
        }
    }
}
