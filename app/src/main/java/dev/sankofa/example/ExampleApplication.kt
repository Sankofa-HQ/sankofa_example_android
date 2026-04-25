package dev.sankofa.example

import android.app.Application
import dev.sankofa.sdk.Sankofa
import dev.sankofa.sdk.SankofaConfig
import dev.sankofa.sdk.catchmod.SankofaCatch
import dev.sankofa.sdk.remoteconfig.SankofaRemoteConfig
import dev.sankofa.sdk.switchmod.SankofaSwitch

class ExampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Sankofa SDK with local server defaults
        Sankofa.init(
            context = this,
            apiKey = "sk_test_b25f965d194d55bd071fb23921401e7c",
            config = SankofaConfig(
                endpoint = "http://192.168.1.241:8080", // "http://10.0.2.2:8080",
                recordSessions = true,
                maskAllInputs = true,
                debug = true,
                flushIntervalSeconds = 10,
                batchSize = 5
            )
        )

        // Switch + Config — init seeds bundled defaults and registers
        // with the Traffic Cop so the first handshake routes flag /
        // config payloads straight into these singletons. Calls before
        // the handshake lands return the bundled defaults below, which
        // keeps the Lab screen + MainActivity banner rendering on
        // first launch.
        SankofaSwitch.init(this, DemoFlag.defaults)
        SankofaRemoteConfig.init(this, DemoConfig.defaults)

        // Catch — installs the chained uncaught-exception handler and
        // the ANR watcher. Without this the Crash Gallery activity's
        // scenarios would still crash the process but wouldn't report.
        SankofaCatch.init(
            context = applicationContext,
            environment = "live",
            release = "sankofa-example-android@1.0",
            appVersion = "1.0",
        )
    }
}
