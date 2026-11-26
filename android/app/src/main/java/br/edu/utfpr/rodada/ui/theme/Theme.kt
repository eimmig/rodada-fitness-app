package br.edu.utfpr.rodada.ui.theme

import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// Visual world: modern social fitness app. Cool neutral ground, white rounded cards,
// electric violet for action, flame orange only for streaks, green only for a day achieved.
// Dynamic color is off on purpose so the brand stays recognizable.

private val Violet = Color(0xFF5B3DF5)
private val Flame = Color(0xFFFF6B2C)
private val Achieved = Color(0xFF12B76A)
private val Ink = Color(0xFF12131A)

/** Brand colors that have no Material role of their own. */
@Immutable
data class BrandColors(
    val streak: Color,
    val streakContainer: Color,
    val achieved: Color,
    val achievedContainer: Color,
    val live: Color,
    /** Unfilled part of bars and meters. */
    val track: Color,
    /** Highlight behind the signed-in user's own row. */
    val you: Color,
    val gold: Color,
    val silver: Color,
    val bronze: Color,
    /** Soft fills for initials avatars. */
    val avatarFills: List<Color>,
    val onAvatar: Color,
)

private val BrandLight = BrandColors(
    streak = Flame,
    streakContainer = Color(0xFFFFE9DF),
    achieved = Achieved,
    achievedContainer = Color(0xFFDDF7EA),
    live = Color(0xFFF0384F),
    track = Color(0xFFE8E9F1),
    you = Color(0xFFEEEAFF),
    gold = Color(0xFFF5B400),
    silver = Color(0xFFA7ADBD),
    bronze = Color(0xFFD08A4E),
    avatarFills = listOf(
        Color(0xFFDCD5FF), Color(0xFFFFDCCB), Color(0xFFCFF2E1),
        Color(0xFFD3E8FF), Color(0xFFFFE7A8), Color(0xFFFAD4E6),
    ),
    onAvatar = Ink,
)

private val BrandDark = BrandColors(
    streak = Color(0xFFFF8A57),
    streakContainer = Color(0xFF3A2116),
    achieved = Color(0xFF3DD68C),
    achievedContainer = Color(0xFF123526),
    live = Color(0xFFFF5A6E),
    track = Color(0xFF2A2C38),
    you = Color(0xFF2A2350),
    gold = Color(0xFFF5C33B),
    silver = Color(0xFFB9BECC),
    bronze = Color(0xFFE09A5E),
    avatarFills = listOf(
        Color(0xFF3B3270), Color(0xFF55301F), Color(0xFF1E4A37),
        Color(0xFF213F60), Color(0xFF5A4614), Color(0xFF53263C),
    ),
    onAvatar = Color(0xFFF1F1F6),
)

private val Light = lightColorScheme(
    primary = Violet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9E4FF),
    onPrimaryContainer = Color(0xFF1C0D6B),
    // Secondary is a quiet violet tonal; Flame is reserved for the streak (BrandColors.streak).
    secondary = Color(0xFF625B7A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFECE9F6),
    onSecondaryContainer = Color(0xFF1F1A33),
    tertiary = Achieved,
    onTertiary = Color.White,
    error = Color(0xFFD92D20),
    onError = Color.White,
    background = Color(0xFFF4F5F9),
    onBackground = Ink,
    surface = Color(0xFFF4F5F9),
    onSurface = Ink,
    surfaceVariant = Color(0xFFE8E9F1),
    onSurfaceVariant = Color(0xFF5B5E6E),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFF0F1F6),
    surfaceContainerHigh = Color(0xFFEBECF2),
    surfaceContainerHighest = Color(0xFFE4E5EC),
    outline = Color(0xFF8A8D9C),
    outlineVariant = Color(0xFFE2E3EA),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF8E7BFF),
    onPrimary = Color(0xFF14083F),
    primaryContainer = Color(0xFF2E2470),
    onPrimaryContainer = Color(0xFFE6E0FF),
    secondary = Color(0xFFCBC3E6),
    onSecondary = Color(0xFF2F2A45),
    secondaryContainer = Color(0xFF2C2840),
    onSecondaryContainer = Color(0xFFE8E2FA),
    tertiary = Color(0xFF3DD68C),
    onTertiary = Color(0xFF00391D),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF5C0600),
    background = Color(0xFF0E0F14),
    onBackground = Color(0xFFECEDF3),
    surface = Color(0xFF0E0F14),
    onSurface = Color(0xFFECEDF3),
    surfaceVariant = Color(0xFF2A2C38),
    onSurfaceVariant = Color(0xFFA9ACBC),
    surfaceContainerLowest = Color(0xFF17181F),
    surfaceContainerLow = Color(0xFF17181F),
    surfaceContainer = Color(0xFF1C1D26),
    surfaceContainerHigh = Color(0xFF22232D),
    surfaceContainerHighest = Color(0xFF2A2C38),
    outline = Color(0xFF80838F),
    outlineVariant = Color(0xFF2E303B),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

val LocalBrandColors = staticCompositionLocalOf { BrandLight }

/** true when the system "Remove animations" setting is on. */
val LocalReduceMotion = staticCompositionLocalOf { false }

object RodadaTheme {
    val colors: BrandColors
        @Composable get() = LocalBrandColors.current
}

@Composable
fun RodadaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val reduceMotion = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    CompositionLocalProvider(
        LocalBrandColors provides if (darkTheme) BrandDark else BrandLight,
        LocalReduceMotion provides reduceMotion,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) Dark else Light,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
