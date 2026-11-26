package br.edu.utfpr.rodada.ui.components

import android.app.Activity
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import br.edu.utfpr.rodada.ui.theme.LocalReduceMotion
import br.edu.utfpr.rodada.ui.theme.RodadaTheme
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

private val ptBr = NumberFormat.getIntegerInstance(Locale.forLanguageTag("pt-BR"))

/** Formats with Brazilian thousands separator: 12.397. */
fun Int.fmt(): String = ptBr.format(this)

fun initialsOf(name: String): String =
    name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "?" }

/** Circular avatar with initials; the fill is stable per uid. */
@Composable
fun Avatar(uid: String, name: String, size: Dp = 40.dp, ring: Color? = null, modifier: Modifier = Modifier) {
    val brand = RodadaTheme.colors
    val fill = brand.avatarFills[abs(uid.hashCode()) % brand.avatarFills.size]
    Box(
        modifier
            .size(size)
            .then(if (ring != null) Modifier.border(3.dp, ring, CircleShape) .padding(4.dp) else Modifier)
            .background(fill, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initialsOf(name),
            style = TextStyle(
                fontFamily = MaterialTheme.typography.titleMedium.fontFamily,
                fontWeight = MaterialTheme.typography.titleMedium.fontWeight,
                fontSize = (size.value * 0.36f).sp,
            ),
            color = brand.onAvatar,
        )
    }
}

/** White rounded container used by every section. */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 0.dp,
        shadowElevation = 1.dp,
    ) {
        Column(Modifier.padding(padding), content = content)
    }
}

/** Flame chip with the current streak. */
@Composable
fun StreakChip(days: Int, modifier: Modifier = Modifier) {
    val brand = RodadaTheme.colors
    Row(
        modifier
            .background(brand.streakContainer, CircleShape)
            .padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp)
            .clearAndSetSemantics { contentDescription = "Sequência de $days ${if (days == 1) "dia" else "dias"}" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = brand.streak, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(4.dp))
        Text("$days", style = MaterialTheme.typography.titleMedium, color = brand.streak)
    }
}

/** Pulsing dot + "Ao vivo". */
@Composable
fun LiveBadge(modifier: Modifier = Modifier) {
    val brand = RodadaTheme.colors
    val reduce = LocalReduceMotion.current
    val pulse = if (reduce) 1f else rememberInfiniteTransition(label = "live").animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse",
    ).value
    Row(
        modifier
            .background(brand.live.copy(alpha = 0.12f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(8.dp).alpha(pulse).background(brand.live, CircleShape))
        Text("Ao vivo", style = MaterialTheme.typography.labelMedium, color = brand.live)
    }
}

/** Sets the status bar icon color while this screen is shown. */
@Composable
fun LightStatusBarIcons(light: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(light) {
        val window = (view.context as Activity).window
        val controller = WindowCompat.getInsetsController(window, view)
        val previous = controller.isAppearanceLightStatusBars
        controller.isAppearanceLightStatusBars = !light
        onDispose { controller.isAppearanceLightStatusBars = previous }
    }
}
