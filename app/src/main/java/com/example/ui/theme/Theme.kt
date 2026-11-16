package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class AcademicColors(
    val isDark: Boolean,
    val paperTextureEnabled: Boolean,
    val priorityA: Color,
    val priorityASoft: Color,
    val priorityB: Color,
    val priorityBSoft: Color,
    val approved: Color,
    val approvedSoft: Color,
    val revision: Color,
    val revisionSoft: Color,
    val brassAccent: Color,
    val paperGrain: Color,
    val folioBorder: Color,
)

private val LightAcademicColors = AcademicColors(
    isDark = false,
    paperTextureEnabled = true,
    priorityA = Color(0xFF946116),
    priorityASoft = Color(0xFFF0E1C2),
    priorityB = Color(0xFF9B332A),
    priorityBSoft = Color(0xFFF2DAD6),
    approved = Color(0xFF2B6144),
    approvedSoft = Color(0xFFD9E6DC),
    revision = Color(0xFF945D18),
    revisionSoft = Color(0xFFF1E3CA),
    brassAccent = Color(0xFF8C6223),
    paperGrain = Color(0xFF6B533B),
    folioBorder = Color(0xFFC9B89D),
)

private val DarkAcademicColors = AcademicColors(
    isDark = true,
    paperTextureEnabled = true,
    priorityA = Color(0xFFE0B465),
    priorityASoft = Color(0xFF332713),
    priorityB = Color(0xFFDF7D73),
    priorityBSoft = Color(0xFF381C19),
    approved = Color(0xFF7CB693),
    approvedSoft = Color(0xFF1A2D22),
    revision = Color(0xFFDFAD63),
    revisionSoft = Color(0xFF342513),
    brassAccent = Color(0xFFD5A86A),
    paperGrain = Color(0xFFD8C5A6),
    folioBorder = Color(0xFF4C3E31),
)

val LocalAcademicColors = staticCompositionLocalOf { LightAcademicColors }

val MaterialTheme.academicColors: AcademicColors
    @Composable
    @ReadOnlyComposable
    get() = LocalAcademicColors.current

private fun oxfordColorScheme(palette: LibraryPalette, darkTheme: Boolean): ColorScheme {
    return if (!darkTheme) {
        when (palette) {
            LibraryPalette.BODLEIAN -> lightColorScheme(
                primary = BodleianLightPrimary,
                onPrimary = Color(0xFFFAF5EC),
                primaryContainer = BodleianLightPrimaryContainer,
                onPrimaryContainer = BodleianLightOnPrimaryContainer,
                secondary = BodleianLightSecondary,
                onSecondary = Color(0xFFFAF5EC),
                secondaryContainer = BodleianLightSecondaryContainer,
                onSecondaryContainer = BodleianLightOnSecondaryContainer,
                tertiary = BodleianLightTertiary,
                onTertiary = Color(0xFFFAF5EC),
                tertiaryContainer = BodleianLightTertiaryContainer,
                onTertiaryContainer = BodleianLightOnTertiaryContainer,
                background = LightParchmentBackground,
                onBackground = LightWalnutInk,
                surface = LightVellumSurface,
                onSurface = LightWalnutInk,
                surfaceVariant = LightLinenSurfaceVariant,
                onSurfaceVariant = LightUmberMuted,
                outline = LightSepiaOutline,
                error = LightError,
                onError = Color.White,
                errorContainer = LightErrorContainer,
            )
            LibraryPalette.RADCLIFFE -> lightColorScheme(
                primary = RadcliffeLightPrimary,
                onPrimary = Color(0xFFFAF5EC),
                primaryContainer = RadcliffeLightPrimaryContainer,
                onPrimaryContainer = RadcliffeLightOnPrimaryContainer,
                secondary = RadcliffeLightSecondary,
                onSecondary = Color(0xFFFAF5EC),
                secondaryContainer = RadcliffeLightSecondaryContainer,
                onSecondaryContainer = RadcliffeLightOnSecondaryContainer,
                tertiary = RadcliffeLightTertiary,
                onTertiary = Color(0xFFFAF5EC),
                tertiaryContainer = RadcliffeLightTertiaryContainer,
                onTertiaryContainer = RadcliffeLightOnTertiaryContainer,
                background = LightParchmentBackground,
                onBackground = LightWalnutInk,
                surface = LightVellumSurface,
                onSurface = LightWalnutInk,
                surfaceVariant = LightLinenSurfaceVariant,
                onSurfaceVariant = LightUmberMuted,
                outline = LightSepiaOutline,
                error = LightError,
                onError = Color.White,
                errorContainer = LightErrorContainer,
            )
            LibraryPalette.BALLIOL -> lightColorScheme(
                primary = BalliolLightPrimary,
                onPrimary = Color(0xFFFAF5EC),
                primaryContainer = BalliolLightPrimaryContainer,
                onPrimaryContainer = BalliolLightOnPrimaryContainer,
                secondary = BalliolLightSecondary,
                onSecondary = Color(0xFFFAF5EC),
                secondaryContainer = BalliolLightSecondaryContainer,
                onSecondaryContainer = BalliolLightOnSecondaryContainer,
                tertiary = BalliolLightTertiary,
                onTertiary = Color(0xFFFAF5EC),
                tertiaryContainer = BalliolLightTertiaryContainer,
                onTertiaryContainer = BalliolLightOnTertiaryContainer,
                background = LightParchmentBackground,
                onBackground = LightWalnutInk,
                surface = LightVellumSurface,
                onSurface = LightWalnutInk,
                surfaceVariant = LightLinenSurfaceVariant,
                onSurfaceVariant = LightUmberMuted,
                outline = LightSepiaOutline,
                error = LightError,
                onError = Color.White,
                errorContainer = LightErrorContainer,
            )
        }
    } else {
        when (palette) {
            LibraryPalette.BODLEIAN -> darkColorScheme(
                primary = BodleianDarkPrimary,
                onPrimary = BodleianDarkOnPrimary,
                primaryContainer = BodleianDarkPrimaryContainer,
                onPrimaryContainer = BodleianDarkOnPrimaryContainer,
                secondary = BodleianDarkSecondary,
                onSecondary = BodleianDarkOnSecondary,
                secondaryContainer = BodleianDarkSecondaryContainer,
                onSecondaryContainer = BodleianDarkOnSecondaryContainer,
                tertiary = BodleianDarkTertiary,
                onTertiary = BodleianDarkOnTertiary,
                tertiaryContainer = BodleianDarkTertiaryContainer,
                onTertiaryContainer = BodleianDarkOnTertiaryContainer,
                background = DarkEbonyBackground,
                onBackground = DarkIvoryInk,
                surface = DarkWalnutSurface,
                onSurface = DarkIvoryInk,
                surfaceVariant = DarkMahoganySurfaceVariant,
                onSurfaceVariant = DarkTaupeMuted,
                outline = DarkBronzeOutline,
                error = DarkError,
                onError = Color(0xFF3A120E),
                errorContainer = DarkErrorContainer,
            )
            LibraryPalette.RADCLIFFE -> darkColorScheme(
                primary = RadcliffeDarkPrimary,
                onPrimary = RadcliffeDarkOnPrimary,
                primaryContainer = RadcliffeDarkPrimaryContainer,
                onPrimaryContainer = RadcliffeDarkOnPrimaryContainer,
                secondary = RadcliffeDarkSecondary,
                onSecondary = RadcliffeDarkOnSecondary,
                secondaryContainer = RadcliffeDarkSecondaryContainer,
                onSecondaryContainer = RadcliffeDarkOnSecondaryContainer,
                tertiary = RadcliffeDarkTertiary,
                onTertiary = RadcliffeDarkOnTertiary,
                tertiaryContainer = RadcliffeDarkTertiaryContainer,
                onTertiaryContainer = RadcliffeDarkOnTertiaryContainer,
                background = DarkEbonyBackground,
                onBackground = DarkIvoryInk,
                surface = DarkWalnutSurface,
                onSurface = DarkIvoryInk,
                surfaceVariant = DarkMahoganySurfaceVariant,
                onSurfaceVariant = DarkTaupeMuted,
                outline = DarkBronzeOutline,
                error = DarkError,
                onError = Color(0xFF3A120E),
                errorContainer = DarkErrorContainer,
            )
            LibraryPalette.BALLIOL -> darkColorScheme(
                primary = BalliolDarkPrimary,
                onPrimary = BalliolDarkOnPrimary,
                primaryContainer = BalliolDarkPrimaryContainer,
                onPrimaryContainer = BalliolDarkOnPrimaryContainer,
                secondary = BalliolDarkSecondary,
                onSecondary = BalliolDarkOnSecondary,
                secondaryContainer = BalliolDarkSecondaryContainer,
                onSecondaryContainer = BalliolDarkOnSecondaryContainer,
                tertiary = BalliolDarkTertiary,
                onTertiary = BalliolDarkOnTertiary,
                tertiaryContainer = BalliolDarkTertiaryContainer,
                onTertiaryContainer = BalliolDarkOnTertiaryContainer,
                background = DarkEbonyBackground,
                onBackground = DarkIvoryInk,
                surface = DarkWalnutSurface,
                onSurface = DarkIvoryInk,
                surfaceVariant = DarkMahoganySurfaceVariant,
                onSurfaceVariant = DarkTaupeMuted,
                outline = DarkBronzeOutline,
                error = DarkError,
                onError = Color(0xFF3A120E),
                errorContainer = DarkErrorContainer,
            )
        }
    }
}

private val OxfordBookShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

@Composable
fun AcademicoTheme(
    settings: OxfordThemeSettings = OxfordThemeSettings(),
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (settings.academiaMode) {
        AcademiaMode.LIGHT -> false
        AcademiaMode.DARK -> true
        AcademiaMode.SYSTEM -> systemDark
    }

    val targetScheme = oxfordColorScheme(settings.palette, useDark)
    val animSpec = tween<Color>(durationMillis = 280)

    val animatedPrimary by animateColorAsState(targetScheme.primary, animSpec, label = "primary")
    val animatedBackground by animateColorAsState(targetScheme.background, animSpec, label = "bg")
    val animatedSurface by animateColorAsState(targetScheme.surface, animSpec, label = "surface")
    val animatedSecondary by animateColorAsState(targetScheme.secondary, animSpec, label = "secondary")

    val colorScheme = targetScheme.copy(
        primary = animatedPrimary,
        background = animatedBackground,
        surface = animatedSurface,
        secondary = animatedSecondary,
    )

    val academicColors = (if (useDark) DarkAcademicColors else LightAcademicColors).copy(
        paperTextureEnabled = settings.paperTextureEnabled,
        brassAccent = targetScheme.tertiary,
        folioBorder = targetScheme.outline,
    )

    CompositionLocalProvider(LocalAcademicColors provides academicColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = buildOxfordTypography(settings.typographyMood),
            shapes = OxfordBookShapes,
            content = content,
        )
    }
}
