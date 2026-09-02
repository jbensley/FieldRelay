package us.bensley.fieldrelay.di

import android.app.Application
import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import us.bensley.fieldrelay.data.api.SpotterNetworkApi
import us.bensley.fieldrelay.location.AprsIsClient
import us.bensley.fieldrelay.location.AprsIsPositionProvider
import us.bensley.fieldrelay.location.OverlandClient
import us.bensley.fieldrelay.location.OverlandPositionProvider
import us.bensley.fieldrelay.location.PositionReportingCoordinator
import us.bensley.fieldrelay.location.SpotterNetworkPositionProvider
import java.util.concurrent.TimeUnit

object ServiceLocator {
    private lateinit var app: Application
    
    fun init(app: Application) {
        this.app = app
    }

    val appContext: Context
        get() = app

    val settings: us.bensley.fieldrelay.data.SettingsRepository by lazy {
        us.bensley.fieldrelay.data.SettingsRepository(
            app
        )
    }

    private val loggingInterceptor: okhttp3.logging.HttpLoggingInterceptor by lazy {
        okhttp3.logging.HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val json = Json {
        ignoreUnknownKeys = true
    }

    val api: SpotterNetworkApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://www.spotternetwork.org/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SpotterNetworkApi::class.java)
    }

    val positionReportingCoordinator: PositionReportingCoordinator by lazy {
        PositionReportingCoordinator(
            providers = listOf(
                SpotterNetworkPositionProvider(api),
                OverlandPositionProvider(OverlandClient(okHttpClient, json)),
                AprsIsPositionProvider(AprsIsClient()),
            ),
        )
    }
}
