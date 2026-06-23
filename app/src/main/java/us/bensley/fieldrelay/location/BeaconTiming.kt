package us.bensley.fieldrelay.location

fun expiresAtForDuration(nowMs: Long, durationMs: Long): Long? =
    if (durationMs > 0L) nowMs + durationMs else null
