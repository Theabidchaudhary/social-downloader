package app.siphon.di

import android.content.Context
import app.siphon.BuildConfig
import app.siphon.data.db.AppDatabase
import app.siphon.data.remote.SiphonApi
import app.siphon.data.repo.DownloadRepository
import app.siphon.data.repo.MediaRepository
import app.siphon.data.settings.SettingsRepository
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Composition root. Everything app-scoped is built exactly once, lazily,
 * in dependency order. ViewModels receive what they need through factories
 * (see ui/ViewModelFactories.kt).
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    val database: AppDatabase by lazy { AppDatabase.build(appContext) }

    val settings: SettingsRepository by lazy { SettingsRepository(appContext) }

    /**
     * Base URL resolves fresh on every request: a user-entered override in
     * Settings (Render/Railway free tier, self-hosted, etc.) wins, otherwise
     * the build's baked-in default.
     */
    val api: SiphonApi by lazy {
        SiphonApi(okHttpClient) {
            settings.current().apiBaseUrl.ifBlank { BuildConfig.SIPHON_API_BASE_URL }
        }
    }

    val mediaRepository: MediaRepository by lazy { MediaRepository(api) }

    val downloadRepository: DownloadRepository by lazy {
        DownloadRepository(appContext, database.downloadDao(), mediaRepository)
    }
}
