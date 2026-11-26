package br.edu.utfpr.rodada

import android.app.Application
import android.content.Context
import br.edu.utfpr.rodada.data.AuthRepository
import br.edu.utfpr.rodada.data.ChallengeRepository
import br.edu.utfpr.rodada.data.DemoAuthRepository
import br.edu.utfpr.rodada.data.DemoChallengeRepository
import br.edu.utfpr.rodada.data.FirebaseAuthRepository
import br.edu.utfpr.rodada.data.FirestoreChallengeRepository
import br.edu.utfpr.rodada.data.PhotoStore
import br.edu.utfpr.rodada.data.Preferences
import com.google.firebase.FirebaseApp

/** App dependencies. Uses Firebase when google-services.json exists; otherwise demo mode. */
class AppContainer(context: Context) {
    private val firebaseReady = BuildConfig.HAS_FIREBASE && FirebaseApp.getApps(context).isNotEmpty()

    val auth: AuthRepository = if (firebaseReady) FirebaseAuthRepository(context) else DemoAuthRepository()
    val repository: ChallengeRepository = if (firebaseReady) FirestoreChallengeRepository() else DemoChallengeRepository()
    val preferences = Preferences(context)
    val photos = PhotoStore(context)
}

class RodadaApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

val Context.container: AppContainer get() = (applicationContext as RodadaApplication).container
