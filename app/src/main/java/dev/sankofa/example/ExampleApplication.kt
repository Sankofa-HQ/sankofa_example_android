package dev.sankofa.example

import android.app.Application
import dev.sankofa.sdk.Sankofa
import dev.sankofa.sdk.SankofaConfig
import dev.sankofa.sdk.pulse.SankofaPulse
import dev.sankofa.sdk.remoteconfig.SankofaRemoteConfig
import dev.sankofa.sdk.switchmod.SankofaSwitch

class ExampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Switch + Config — init seeds bundled defaults and registers
        // with the Traffic Cop. Run BEFORE Sankofa.init so the
        // auto-discovered flag/config snapshots Catch attaches to its
        // events on the very first crash already see something useful.
        SankofaSwitch.init(this, DemoFlag.defaults)
        SankofaRemoteConfig.init(this, DemoConfig.defaults)

        // One-line init. enableCatch=true (default) auto-installs the
        // chained Thread.UncaughtExceptionHandler + ANR watcher and
        // wires Switch/Config snapshots onto every captured event —
        // no separate `SankofaCatch.init` call needed.
        Sankofa.init(
            context = this,
            apiKey = "sk_test_b25f965d194d55bd071fb23921401e7c",
            config = SankofaConfig(
                endpoint = "http://192.168.1.241:8080", // "http://10.0.2.2:8080",
                recordSessions = true,
                maskAllInputs = true,
                debug = true,
                flushIntervalSeconds = 10,
                batchSize = 5,
                release = "sankofa-example-android@1.0",
                appVersion = "1.0",
                // 🚀 Phase B — beforeSend hook. Runs AFTER an event is
                // composed but BEFORE it's enqueued. Return null to
                // drop entirely; return the event (possibly mutated)
                // to ship it. Throws swallowed.
                //   - Drop "[noise]" messages (framework warnings
                //     you can't fix).
                //   - Scrub `user_email` from extras so PII doesn't
                //     leak to the dashboard.
                beforeSend = { event ->
                    when {
                        event.message?.contains("[noise]") == true -> null
                        event.extra?.containsKey("user_email") == true -> event.copy(
                            extra = event.extra!!.toMutableMap()
                                .also { it["user_email"] = "[redacted]" }
                        )
                        else -> event
                    }
                },
            )
        )

        // Pulse — surveys (NPS, CSAT, custom). register() pulls the
        // apiKey + endpoint from Sankofa.init's cache, so it must
        // run AFTER Sankofa.init. The PulseLab activity below
        // surfaces a "not registered" message if this returns false.
        SankofaPulse.register(applicationContext)
    }
}
