package br.edu.utfpr.rodada

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.edu.utfpr.rodada.ui.HomeViewModel
import br.edu.utfpr.rodada.ui.RodadaApp
import br.edu.utfpr.rodada.ui.SessionViewModel
import br.edu.utfpr.rodada.ui.screens.LoginScreen
import br.edu.utfpr.rodada.ui.theme.RodadaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val c = container
        setContent {
            RodadaTheme {
                val session: SessionViewModel = viewModel(factory = viewModelFactory { initializer { SessionViewModel(c) } })
                val s by session.ui.collectAsStateWithLifecycle()
                val user = s.user
                if (user == null) {
                    LoginScreen(
                        providers = s.providers,
                        signingIn = s.signingIn,
                        error = s.error,
                        isDemo = s.isDemo,
                        onSignIn = { session.signIn(it, this@MainActivity) },
                    )
                } else {
                    // One ViewModel per user: switching accounts drops the previous state.
                    key(user.uid) {
                        val vm: HomeViewModel = viewModel(
                            key = "home-${user.uid}",
                            factory = viewModelFactory { initializer { HomeViewModel(c, user) } },
                        )
                        RodadaApp(vm, onSignOut = session::signOut)
                    }
                }
            }
        }
    }
}
