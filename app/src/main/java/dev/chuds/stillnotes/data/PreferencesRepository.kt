package dev.chuds.stillnotes.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.preferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "still_notes_settings",
)

private val FONT_PRESET_KEY = stringPreferencesKey("font_preset")
private val PREVIEW_BY_DEFAULT_KEY = booleanPreferencesKey("preview_by_default")
private val HAPTICS_ENABLED_KEY = booleanPreferencesKey("haptics_enabled")

enum class FontPreset { System, Editorial, Terminal, Grotesk }

data class NotesSettings(
    val fontPreset: FontPreset = FontPreset.System,
    val previewByDefault: Boolean = false,
    val hapticsEnabled: Boolean = true,
)

class PreferencesRepository(private val context: Context) {

    val settings: Flow<NotesSettings> = context.preferencesDataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs ->
            NotesSettings(
                fontPreset = prefs[FONT_PRESET_KEY]
                    ?.let { runCatching { FontPreset.valueOf(it) }.getOrNull() }
                    ?: FontPreset.System,
                previewByDefault = prefs[PREVIEW_BY_DEFAULT_KEY] ?: false,
                hapticsEnabled = prefs[HAPTICS_ENABLED_KEY] ?: true,
            )
        }

    suspend fun setFontPreset(preset: FontPreset) {
        context.preferencesDataStore.edit { it[FONT_PRESET_KEY] = preset.name }
    }

    suspend fun setPreviewByDefault(value: Boolean) {
        context.preferencesDataStore.edit { it[PREVIEW_BY_DEFAULT_KEY] = value }
    }

    suspend fun setHapticsEnabled(value: Boolean) {
        context.preferencesDataStore.edit { it[HAPTICS_ENABLED_KEY] = value }
    }
}
