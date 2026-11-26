package br.edu.utfpr.rodada.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.edu.utfpr.rodada.data.SignInProvider
import br.edu.utfpr.rodada.ui.components.LightStatusBarIcons
import br.edu.utfpr.rodada.ui.components.RodadaMark
import br.edu.utfpr.rodada.ui.components.RodadaWordmark

@Composable
fun LoginScreen(
    providers: List<SignInProvider>,
    signingIn: SignInProvider?,
    error: String?,
    isDemo: Boolean,
    onSignIn: (SignInProvider) -> Unit,
) {
    LightStatusBarIcons(light = true)
    val violet = Color(0xFF5B3DF5)
    Box(Modifier.fillMaxSize().background(violet)) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 24.dp),
        ) {
            Spacer(Modifier.weight(1f))
            // The arrow doubles as the progress indicator while a sign-in flow is open.
            RodadaMark(Modifier.size(176.dp), spinning = signingIn != null, handColor = violet)
            Spacer(Modifier.height(28.dp))
            RodadaWordmark(handColor = violet)
            Spacer(Modifier.height(8.dp))
            Text(
                "Cada dia ativo vale um ponto. Mexa-se, conquiste o dia e suba no ranking do seu grupo.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.82f),
                modifier = Modifier.widthIn(max = 420.dp),
            )
            Spacer(Modifier.height(32.dp))
            providers.forEachIndexed { i, provider ->
                if (i > 0) Spacer(Modifier.height(12.dp))
                ProviderButton(
                    provider = provider,
                    primary = i == 0,
                    busy = signingIn == provider,
                    enabled = signingIn == null,
                    onClick = { onSignIn(provider) },
                )
            }
            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFFFD6DB),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                if (isDemo) "Modo demonstração: sem google-services.json qualquer botão entra com dados fictícios."
                else "Acesso só com conta ${providers.joinToString(", ", transform = SignInProvider::label).replace(Regex(", (?=[^,]+$)"), " ou ")}. Seu nome aparece para quem está nos seus grupos.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
            )
        }
    }
}

/** Google is the filled pill; the other providers are outlined so the screen keeps one main action. */
@Composable
private fun ProviderButton(provider: SignInProvider, primary: Boolean, busy: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val violet = Color(0xFF5B3DF5)
    val modifier = Modifier.fillMaxWidth().height(if (primary) 56.dp else 52.dp)
    val content: @Composable () -> Unit = {
        if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp, color = if (primary) violet else Color.White)
        else Text("Continuar com ${provider.label}", style = MaterialTheme.typography.labelLarge)
    }
    if (primary) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = violet,
                disabledContainerColor = Color.White.copy(alpha = 0.7f),
                disabledContentColor = violet,
            ),
        ) { content() }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier,
            shape = CircleShape,
            border = BorderStroke(1.5.dp, Color.White.copy(alpha = if (enabled) 0.6f else 0.3f)),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.White,
                disabledContentColor = Color.White.copy(alpha = 0.6f),
            ),
        ) { content() }
    }
}
