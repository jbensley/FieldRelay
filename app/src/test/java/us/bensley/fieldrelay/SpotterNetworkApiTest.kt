package us.bensley.fieldrelay

import us.bensley.fieldrelay.data.model.PositionUpdate
import us.bensley.fieldrelay.di.ServiceLocator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Live API harness. Exercises the same ServiceLocator.api / Retrofit / OkHttp / kotlinx.serialization
 * stack that LocationService will use in production.
 *
 * Run:
 *   ./gradlew :app:testDebugUnitTest --tests us.bensley.fieldrelay.SpotterNetworkApiTest \
 *       -PappId=YOUR_APP_ID -Plat=33.669 -Plon=-96.926
 *
 * Optional: -Pelev=<m> -Pmph=<speed> -Pdir=<deg>
 * Skips cleanly if appId/lat/lon are missing.
 */
class SpotterNetworkApiTest {

    private val appId = sysProp("appId")
    private val lat = sysProp("lat").toDoubleOrNull()
    private val lon = sysProp("lon").toDoubleOrNull()
    private val elev = sysProp("elev").toDoubleOrNull() ?: 0.0
    private val mph = sysProp("mph").toDoubleOrNull() ?: 0.0
    private val dir = sysProp("dir").toDoubleOrNull() ?: 0.0

    @Test
    fun submitsPositionUpdate() = runBlocking {
        Assume.assumeTrue(
            "Requires -PappId=<id> -Plat=<deg> -Plon=<deg> (or -D equivalents)",
            appId.isNotBlank() && lat != null && lon != null,
        )

        val body = PositionUpdate(
            id = appId,
            reportAt = utcTimestamp(),
            lat = lat!!,
            lon = lon!!,
            elev = elev,
            mph = mph,
            dir = dir,
            active = 1,
            gps = 1,
        )

        println("Request body: $body")
        val response = ServiceLocator.api.updatePosition(body)
        println("Response: $response")

        assertTrue("Expected success=true, got $response", response.success)
    }

    private fun sysProp(name: String): String = (System.getProperty(name) ?: "").trim()

    private fun utcTimestamp(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date())
}
