package app.siphon

import android.app.Application
import app.siphon.di.AppContainer

/**
 * Application entry point. Siphon uses a hand-rolled composition root
 * ([AppContainer]) instead of a DI framework: the object graph is small,
 * construction is explicit and traceable, and builds stay free of annotation
 * processing. See docs/architecture.md ("Dependency injection").
 */
class SiphonApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
