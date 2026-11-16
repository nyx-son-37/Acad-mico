package com.example.ui.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "oxford_theme_prefs")

enum class AcademiaMode(val key: String, val labelPt: String, val subtitlePt: String) {
    LIGHT("light", "Light Academia", "Pergaminho marfim ao sol da manhã"),
    DARK("dark", "Dark Academia", "Biblioteca noturna em mogno e latão"),
    SYSTEM("system", "Automático", "Segue o modo claro ou escuro do aparelho");

    companion object {
        fun fromKey(key: String?): AcademiaMode = entries.firstOrNull { it.key == key } ?: LIGHT
    }
}

enum class LibraryPalette(
    val key: String,
    val labelPt: String,
    val descriptionPt: String,
    val swatchLightHex: String,
    val swatchDarkHex: String
) {
    BODLEIAN(
        key = "bodleian",
        labelPt = "Bodleian Clássico",
        descriptionPt = "Mogno envelhecido, verde biblioteca e latão",
        swatchLightHex = "#5D3423",
        swatchDarkHex = "#D5A86A"
    ),
    RADCLIFFE(
        key = "radcliffe",
        labelPt = "Radcliffe Tinta & Fólio",
        descriptionPt = "Azul ferro-gálico, oliva antigo e ouro velho",
        swatchLightHex = "#273C54",
        swatchDarkHex = "#9EB6D0"
    ),
    BALLIOL(
        key = "balliol",
        labelPt = "Balliol Encadernado",
        descriptionPt = "Couro borgonha, musgo escovado e folha de ouro",
        swatchLightHex = "#6B282B",
        swatchDarkHex = "#D88A84"
    );

    companion object {
        fun fromKey(key: String?): LibraryPalette = entries.firstOrNull { it.key == key } ?: BODLEIAN
    }
}

enum class TypographyMood(val key: String, val labelPt: String, val subtitlePt: String) {
    CLASSIC_SERIF("classic_serif", "Literária Clássica", "Playfair Display & Lora serifada em textos"),
    EDITORIAL_MIXED("editorial_mixed", "Editorial Moderna", "Playfair Display nos títulos e leitura limpa");

    companion object {
        fun fromKey(key: String?): TypographyMood = entries.firstOrNull { it.key == key } ?: CLASSIC_SERIF
    }
}

data class OxfordThemeSettings(
    val academiaMode: AcademiaMode = AcademiaMode.LIGHT,
    val palette: LibraryPalette = LibraryPalette.BODLEIAN,
    val typographyMood: TypographyMood = TypographyMood.CLASSIC_SERIF,
    val paperTextureEnabled: Boolean = true,
)

class ThemePreferencesRepository(private val context: Context) {

    private object Keys {
        val ACADEMIA_MODE = stringPreferencesKey("academia_mode")
        val LIBRARY_PALETTE = stringPreferencesKey("library_palette")
        val TYPOGRAPHY_MOOD = stringPreferencesKey("typography_mood")
        val PAPER_TEXTURE = booleanPreferencesKey("paper_texture_enabled")
    }

    val settingsFlow: Flow<OxfordThemeSettings> = context.themeDataStore.data.map { prefs ->
        OxfordThemeSettings(
            academiaMode = AcademiaMode.fromKey(prefs[Keys.ACADEMIA_MODE]),
            palette = LibraryPalette.fromKey(prefs[Keys.LIBRARY_PALETTE]),
            typographyMood = TypographyMood.fromKey(prefs[Keys.TYPOGRAPHY_MOOD]),
            paperTextureEnabled = prefs[Keys.PAPER_TEXTURE] ?: true,
        )
    }

    suspend fun updateAcademiaMode(mode: AcademiaMode) {
        context.themeDataStore.edit { it[Keys.ACADEMIA_MODE] = mode.key }
    }

    suspend fun updatePalette(palette: LibraryPalette) {
        context.themeDataStore.edit { it[Keys.LIBRARY_PALETTE] = palette.key }
    }

    suspend fun updateTypographyMood(mood: TypographyMood) {
        context.themeDataStore.edit { it[Keys.TYPOGRAPHY_MOOD] = mood.key }
    }

    suspend fun updatePaperTexture(enabled: Boolean) {
        context.themeDataStore.edit { it[Keys.PAPER_TEXTURE] = enabled }
    }
}
