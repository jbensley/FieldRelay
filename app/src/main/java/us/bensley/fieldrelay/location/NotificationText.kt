package us.bensley.fieldrelay.location

fun buildLocationNotificationText(
    expiresAt: Long?,
    providerStatuses: Map<String, ProviderTelemetry>,
    formatTime: (Long) -> String,
): String {
    val prefix = expiresAt?.let { "On until ${formatTime(it)}" } ?: "On (indefinite)"
    if (providerStatuses.isEmpty()) return prefix

    val failed = providerStatuses.entries.firstOrNull { !it.value.success }
    val lastReportAt = providerStatuses.values.maxOf { it.lastReportAt }
    val reportPart = if (failed != null) {
        val reason = failed.value.message?.let { ": $it" }.orEmpty()
        " - ${failed.key} failed$reason"
    } else {
        " - last beacon update ${formatTime(lastReportAt)} OK"
    }
    return prefix + reportPart
}
