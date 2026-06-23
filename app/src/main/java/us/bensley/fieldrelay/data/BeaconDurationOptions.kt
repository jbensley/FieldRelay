package us.bensley.fieldrelay.data

data class RuntimeDurationOption(val label: String, val durationMs: Long)

fun runtimeDurationOptions(): List<RuntimeDurationOption> = listOf(
    RuntimeDurationOption("1 hour", BeaconDurationMode.HOURS_1.toDurationMs()),
    RuntimeDurationOption("2 hours", BeaconDurationMode.HOURS_2.toDurationMs()),
    RuntimeDurationOption("4 hours", BeaconDurationMode.HOURS_4.toDurationMs()),
    RuntimeDurationOption("8 hours", BeaconDurationMode.HOURS_8.toDurationMs()),
    RuntimeDurationOption("24 hours", BeaconDurationMode.HOURS_24.toDurationMs()),
    RuntimeDurationOption("Indefinitely", BeaconDurationMode.INDEFINITE.toDurationMs()),
)

fun BeaconDurationMode.settingsLabel(): String = when (this) {
    BeaconDurationMode.HOURS_1 -> "1 hour"
    BeaconDurationMode.HOURS_2 -> "2 hours"
    BeaconDurationMode.HOURS_4 -> "4 hours"
    BeaconDurationMode.HOURS_8 -> "8 hours"
    BeaconDurationMode.HOURS_24 -> "24 hours"
    BeaconDurationMode.INDEFINITE -> "Indefinite"
    BeaconDurationMode.ASK -> "Ask me each time"
}
