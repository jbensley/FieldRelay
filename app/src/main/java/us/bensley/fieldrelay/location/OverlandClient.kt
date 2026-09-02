package us.bensley.fieldrelay.location

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import us.bensley.fieldrelay.data.model.OverlandResponse
import us.bensley.fieldrelay.data.model.OverlandUpdate

class OverlandClient(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
) {
    suspend fun publish(endpoint: String, token: String, update: OverlandUpdate): OverlandResponse =
        withContext(Dispatchers.IO) {
            val requestBuilder = Request.Builder()
                .url(endpoint)
                .post(json.encodeToString(update).toRequestBody("application/json".toMediaType()))
            if (token.isNotBlank()) {
                requestBuilder.header("Authorization", "Bearer ${token.trim()}")
            }
            okHttpClient.newCall(requestBuilder.build()).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext OverlandResponse(error = body.ifBlank { "HTTP ${response.code}" })
                }
                body.takeIf { it.isNotBlank() }
                    ?.let { runCatching { json.decodeFromString<OverlandResponse>(it) }.getOrNull() }
                    ?: OverlandResponse(result = "ok")
            }
        }
}
