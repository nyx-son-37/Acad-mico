package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// --- Oxford Library Light Academia Surfaces ---
val LightParchmentBackground = Color(0xFFF3ECE0)
val LightVellumSurface = Color(0xFFFAF5EC)
val LightLinenSurfaceVariant = Color(0xFFE8DEC9)
val LightWalnutInk = Color(0xFF231A14)
val LightUmberMuted = Color(0xFF5D4E41)
val LightSepiaOutline = Color(0xFFCBBBA2)

// --- Oxford Library Dark Academia Surfaces ---
val DarkEbonyBackground = Color(0xFF130F0C)
val DarkWalnutSurface = Color(0xFF1D1712)
val DarkMahoganySurfaceVariant = Color(0xFF2A211A)
val DarkIvoryInk = Color(0xFFECE2D0)
val DarkTaupeMuted = Color(0xFFB5A58E)
val DarkBronzeOutline = Color(0xFF4C3E31)

// --- Edition 1: Bodleian Clássico (Mahogany, Library Green & Brass) ---
val BodleianLightPrimary = Color(0xFF5D3423)
val BodleianLightPrimaryContainer = Color(0xFFEBD9CC)
val BodleianLightOnPrimaryContainer = Color(0xFF2E150B)
val BodleianLightSecondary = Color(0xFF2C4D3B)
val BodleianLightSecondaryContainer = Color(0xFFD6E5DB)
val BodleianLightOnSecondaryContainer = Color(0xFF11291C)
val BodleianLightTertiary = Color(0xFF8C6223)
val BodleianLightTertiaryContainer = Color(0xFFF1E2C4)
val BodleianLightOnTertiaryContainer = Color(0xFF3A2406)

val BodleianDarkPrimary = Color(0xFFD5A86A)
val BodleianDarkOnPrimary = Color(0xFF2C1B08)
val BodleianDarkPrimaryContainer = Color(0xFF4B3321)
val BodleianDarkOnPrimaryContainer = Color(0xFFF4DFBE)
val BodleianDarkSecondary = Color(0xFF7FA88E)
val BodleianDarkOnSecondary = Color(0xFF0E2518)
val BodleianDarkSecondaryContainer = Color(0xFF223B2D)
val BodleianDarkOnSecondaryContainer = Color(0xFFD0E5D7)
val BodleianDarkTertiary = Color(0xFFC9856C)
val BodleianDarkOnTertiary = Color(0xFF34140B)
val BodleianDarkTertiaryContainer = Color(0xFF4F2B20)
val BodleianDarkOnTertiaryContainer = Color(0xFFF4D8CE)

// --- Edition 2: Radcliffe Pergaminho (Iron-Gall Indigo, Olive Folio & Antique Gold) ---
val RadcliffeLightPrimary = Color(0xFF273C54)
val RadcliffeLightPrimaryContainer = Color(0xFFD6DFEA)
val RadcliffeLightOnPrimaryContainer = Color(0xFF101D2C)
val RadcliffeLightSecondary = Color(0xFF4C5B39)
val RadcliffeLightSecondaryContainer = Color(0xFFDFE5D3)
val RadcliffeLightOnSecondaryContainer = Color(0xFF1E2713)
val RadcliffeLightTertiary = Color(0xFF906526)
val RadcliffeLightTertiaryContainer = Color(0xFFF2E3C5)
val RadcliffeLightOnTertiaryContainer = Color(0xFF3A2507)

val RadcliffeDarkPrimary = Color(0xFF9EB6D0)
val RadcliffeDarkOnPrimary = Color(0xFF102030)
val RadcliffeDarkPrimaryContainer = Color(0xFF243547)
val RadcliffeDarkOnPrimaryContainer = Color(0xFFDCE7F2)
val RadcliffeDarkSecondary = Color(0xFFA3B489)
val RadcliffeDarkOnSecondary = Color(0xFF1A230E)
val RadcliffeDarkSecondaryContainer = Color(0xFF2E3821)
val RadcliffeDarkOnSecondaryContainer = Color(0xFFE0E9D1)
val RadcliffeDarkTertiary = Color(0xFFD9B26F)
val RadcliffeDarkOnTertiary = Color(0xFF332106)
val RadcliffeDarkTertiaryContainer = Color(0xFF4D3818)
val RadcliffeDarkOnTertiaryContainer = Color(0xFFF7E5C4)

// --- Edition 3: Balliol Borgonha (Oxblood Leather, Moss Slate & Gilded Leaf) ---
val BalliolLightPrimary = Color(0xFF6B282B)
val BalliolLightPrimaryContainer = Color(0xFFEFD6D4)
val BalliolLightOnPrimaryContainer = Color(0xFF320E10)
val BalliolLightSecondary = Color(0xFF395148)
val BalliolLightSecondaryContainer = Color(0xFFD7E4DE)
val BalliolLightOnSecondaryContainer = Color(0xFF13251F)
val BalliolLightTertiary = Color(0xFF8C6322)
val BalliolLightTertiaryContainer = Color(0xFFF1E2C3)
val BalliolLightOnTertiaryContainer = Color(0xFF382305)

val BalliolDarkPrimary = Color(0xFFD88A84)
val BalliolDarkOnPrimary = Color(0xFF331110)
val BalliolDarkPrimaryContainer = Color(0xFF4E2322)
val BalliolDarkOnPrimaryContainer = Color(0xFFF6DCD9)
val BalliolDarkSecondary = Color(0xFF87AA9B)
val BalliolDarkOnSecondary = Color(0xFF10251D)
val BalliolDarkSecondaryContainer = Color(0xFF223830)
val BalliolDarkOnSecondaryContainer = Color(0xFFD6E8E0)
val BalliolDarkTertiary = Color(0xFFDCB46E)
val BalliolDarkOnTertiary = Color(0xFF322105)
val BalliolDarkTertiaryContainer = Color(0xFF4D3818)
val BalliolDarkOnTertiaryContainer = Color(0xFFF8E7C6)

// --- Semantic Bookbinding Tokens (fallback constants + adaptive pairs) ---
val LightError = Color(0xFF992B24)
val LightErrorContainer = Color(0xFFF3D9D6)
val DarkError = Color(0xFFE0857D)
val DarkErrorContainer = Color(0xFF4D1F1B)

// Bookbinding & Discipline Natural Pigment Swatches
val AcademicPaletteHexes = listOf(
    "#5D3423", // Mogno
    "#2C4D3B", // Verde Bodleian
    "#273C54", // Tinta Ferro-Gálica
    "#6B282B", // Couro Borgonha
    "#8C6223", // Latão Envelhecido
    "#4C5B39", // Musgo Fólio
    "#573D52", // Ameixa Sépia
    "#365359", // Ardósia Antiga
)

fun parseHexColor(hex: String, fallback: Color = BodleianLightPrimary): Color {
    return runCatching {
        val clean = hex.trim().removePrefix("#")
        val colorInt = clean.toLong(16)
        when (clean.length) {
            6 -> Color(colorInt or 0xFF000000L)
            8 -> Color(colorInt)
            else -> fallback
        }
    }.getOrDefault(fallback)
}
