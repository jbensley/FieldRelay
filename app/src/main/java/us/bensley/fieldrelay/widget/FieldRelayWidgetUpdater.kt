package us.bensley.fieldrelay.widget

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

object FieldRelayWidgetUpdater {
    suspend fun updateAll(context: Context) {
        FieldRelayWidgetReceiver.updateAll(context)
    }

    fun updateAllBlocking(context: Context, timeoutMs: Long = 1_500L) {
        runBlocking(Dispatchers.IO) {
            withTimeoutOrNull(timeoutMs) {
                updateAll(context)
            }
        }
    }
}
