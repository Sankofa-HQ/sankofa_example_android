package dev.sankofa.example

import android.app.Application
import dev.sankofa.sdk.Sankofa
import dev.sankofa.sdk.SankofaConfig

class ExampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Sankofa SDK with local server defaults
        Sankofa.init(
            context = this,
            apiKey = "sk_test_b25f965d194d55bd071fb23921401e7c",
            config = SankofaConfig(
                endpoint = "http://192.168.1.81:8080", // "http://10.0.2.2:8080",
                recordSessions = true,
                maskAllInputs = true,
                debug = true,
                flushIntervalSeconds = 10,
                batchSize = 5
            )
        )
    }
}
