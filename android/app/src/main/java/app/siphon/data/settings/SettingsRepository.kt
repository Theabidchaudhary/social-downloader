package app.siphon.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, DARK, LIGHT }

/** Standard MediaStore collections a download can land in. */
enum class TargetDir { DOWNLOADS, MOVIES, MUSIC, CUSTOM }

data class SiphonSettings(
    val theme: ThemeMode = ThemeMode.DARK,
    val targetDir: TargetDir = TargetDir.DOWNLOADS,
    val customTreeUri: String? = null,
    val maxParallelDownloads: Int = 3,
    val clipboardDetection: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val languageTag: String = "", // empty = system default
    /** Empty = use the build's default (BuildConfig.SIPHON_API_BASE_URL). */
    val apiBaseUrl: String = "",
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "siphon_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val theme = stringPreferencesKey("theme")
        val targetDir = stringPreferencesKey("target_dir")
        val customTreeUri = stringPreferencesKey("custom_tree_uri")
        val maxParallel = intPreferencesKey("max_parallel")
        val clipboard = booleanPreferencesKey("clipboard_detection")
        val notifications = booleanPreferencesKey("notifications_enabled")
        val language = stringPreferencesKey("language_tag")
        val apiBaseUrl = stringPreferencesKey("api_base_url")
    }

    val settings: Flow<SiphonSettings> = context.dataStore.data.map { prefs ->
        SiphonSettings(
            theme = prefs[Keys.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.DARK,
            targetDir = prefs[Keys.targetDir]?.let { runCatching { TargetDir.valueOf(it) }.getOrNull() }
                ?: TargetDir.DOWNLOADS,
            customTreeUri = prefs[Keys.customTreeUri],
            maxParallelDownloads = (prefs[Keys.maxParallel] ?: 3).coerceIn(1, 5),
            clipboardDetection = prefs[Keys.clipboard] ?: true,
            notificationsEnabled = prefs[Keys.notifications] ?: true,
            languageTag = prefs[Keys.language] ?: "",
            apiBaseUrl = prefs[Keys.apiBaseUrl] ?: "",
        )
    }

    suspend fun current(): SiphonSettings = settings.first()

    suspend fun setTheme(mode: ThemeMode) = context.dataStore.edit { it[Keys.theme] = mode.name }

    suspend fun setTargetDir(dir: TargetDir, customTreeUri: String? = null) =
        context.dataStore.edit {
            it[Keys.targetDir] = dir.name
            if (dir == TargetDir.CUSTOM && customTreeUri != null) {
                it[Keys.customTreeUri] = customTreeUri
            }
        }

    suspend fun setMaxParallel(count: Int) =
        context.dataStore.edit { it[Keys.maxParallel] = count.coerceIn(1, 5) }

    suspend fun setClipboardDetection(enabled: Boolean) =
        context.dataStore.edit { it[Keys.clipboard] = enabled }

    suspend fun setNotificationsEnabled(enabled: Boolean) =
        context.dataStore.edit { it[Keys.notifications] = enabled }

    suspend fun setLanguageTag(tag: String) = context.dataStore.edit { it[Keys.language] = tag }

    /** Pass a blank string to clear the override and fall back to the build default. */
    suspend fun setApiBaseUrl(url: String) =
        context.dataStore.edit { it[Keys.apiBaseUrl] = url.trim().trimEnd('/') }
}
