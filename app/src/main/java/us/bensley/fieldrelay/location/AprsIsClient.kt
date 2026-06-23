package us.bensley.fieldrelay.location

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket

class AprsIsClient(
    private val connectTimeoutMs: Int = 10_000,
    private val readTimeoutMs: Int = 10_000,
) {
    suspend fun transmit(
        server: String,
        port: Int,
        loginLine: String,
        packetLine: String,
    ) {
        withContext(Dispatchers.IO) {
            Socket().use { socket ->
                socket.soTimeout = readTimeoutMs
                socket.connect(InetSocketAddress(server, port), connectTimeoutMs)
                val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.US_ASCII))
                val writer = OutputStreamWriter(socket.getOutputStream(), Charsets.US_ASCII)

                reader.readLine()
                writer.write("$loginLine\r\n")
                writer.flush()
                val loginResponse = reader.readLine().orEmpty()
                if (loginResponse.contains("unverified", ignoreCase = true) ||
                    loginResponse.contains("invalid", ignoreCase = true)
                ) {
                    error(loginResponse.ifBlank { "APRS-IS login rejected." })
                }

                writer.write("$packetLine\r\n")
                writer.flush()
            }
        }
    }
}
