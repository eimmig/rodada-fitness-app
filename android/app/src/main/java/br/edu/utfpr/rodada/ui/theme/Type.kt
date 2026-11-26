package br.edu.utfpr.rodada.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import br.edu.utfpr.rodada.R

private fun figtree(weight: Int) = Font(
    R.font.figtree,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Figtree (variable): friendly geometric sans, heavy weights for stats. */
val Figtree = FontFamily(
    figtree(400),
    figtree(500),
    figtree(600),
    figtree(700),
    figtree(800),
)

private fun style(size: Int, line: Int, weight: Int, tracking: Double = 0.0) = TextStyle(
    fontFamily = Figtree,
    fontWeight = FontWeight(weight),
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.em,
    fontFeatureSettings = "tnum",
)

val AppTypography = Typography(
    // Big stat on the activity card.
    displayLarge = style(72, 76, 800, -0.04),
    displayMedium = style(56, 60, 800, -0.035),
    displaySmall = style(36, 42, 800, -0.02),
    headlineLarge = style(32, 38, 800, -0.02),
    headlineMedium = style(26, 32, 700, -0.015),
    headlineSmall = style(22, 28, 700, -0.01),
    titleLarge = style(20, 26, 700, -0.005),
    titleMedium = style(16, 22, 700),
    titleSmall = style(14, 20, 700),
    bodyLarge = style(16, 24, 400),
    bodyMedium = style(14, 20, 400),
    bodySmall = style(12, 16, 500),
    labelLarge = style(15, 20, 700),
    labelMedium = style(13, 16, 600),
    labelSmall = style(11, 14, 600, 0.02),
)
