package dev.sankofa.example

import android.content.Context
import android.content.SharedPreferences
import dev.sankofa.sdk.Sankofa
import dev.sankofa.sdk.SankofaConfig
import dev.sankofa.sdk.pulse.SankofaPulse
import dev.sankofa.sdk.remoteconfig.SankofaRemoteConfig
import dev.sankofa.sdk.switchmod.SankofaSwitch

/**
 * Holds the user-supplied endpoint + API key and persists them via
 * `SharedPreferences`. Mirrors the iOS / Flutter / web example pattern:
 *
 *   - First launch with no saved creds → host shows `ConnectActivity`.
 *   - User connects → creds saved + SDK initialised.
 *   - Subsequent launches auto-init from the saved creds and skip the
 *     connect screen.
 *   - Settings → "Disconnect" wipes the prefs and bounces back to
 *     `ConnectActivity`.
 *
 * The SDK is **only** initialised after a successful connect — we no
 * longer call `Sankofa.init` from `Application.onCreate` with a
 * hardcoded key.
 */
object SankofaConnection {

    private const val PREFS_NAME = "sankofa_example_connection"
    private const val KEY_API_KEY = "api_key"
    private const val KEY_ENDPOINT = "endpoint"
    private const val DEFAULT_ENDPOINT = "https://api.sankofa.dev"
    private const val DEFAULT_API_KEY = "sk_live_4f3679b3311d935d8bb58d6cbeab9a57"

    private var prefs: SharedPreferences? = null
    private var initialised: Boolean = false

    /** Called from `ExampleApplication.onCreate`. Auto-initialises the
     *  SDK if credentials are already persisted, so the developer gets
     *  the "skip connect on second launch" UX without per-activity
     *  bootstrap noise. */
    fun bootstrap(context: Context) {
        val app = context.applicationContext
        prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedKey = apiKey()
        val savedEndpoint = endpoint()
        if (!initialised && savedKey.isNotEmpty()) {
            initialiseSDK(app, savedKey, savedEndpoint)
        }
    }

    fun isConnected(): Boolean = apiKey().isNotEmpty()

    fun apiKey(): String = prefs?.getString(KEY_API_KEY, "")?.trim().orEmpty()

    /** The pre-filled key shown on the connect screen when none is saved. */
    fun defaultApiKey(): String = DEFAULT_API_KEY

    fun endpoint(): String {
        val stored = prefs?.getString(KEY_ENDPOINT, "")?.trim().orEmpty()
        return stored.ifEmpty { DEFAULT_ENDPOINT }
    }

    /** `sk_test_…` / `sk_live_…` → "test" / "live". Optional — only
     *  used to tag the Catch environment + render an env pill in the
     *  Settings sheet. */
    fun inferredEnvironment(): String? {
        val key = apiKey()
        return when {
            key.startsWith("sk_test_") -> "test"
            key.startsWith("sk_live_") -> "live"
            else -> null
        }
    }

    /** Persist creds and initialise the SDK. Safe to call multiple
     *  times — the SDK guards its own `isInitialized` flag and a
     *  re-init is a no-op. */
    fun connect(context: Context, apiKey: String, endpoint: String) {
        val app = context.applicationContext
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isEmpty()) return
        val trimmedEndpoint = endpoint.trim().ifEmpty { DEFAULT_ENDPOINT }

        val p = prefs ?: app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).also { prefs = it }
        p.edit()
            .putString(KEY_API_KEY, trimmedKey)
            .putString(KEY_ENDPOINT, trimmedEndpoint)
            .apply()

        initialiseSDK(app, trimmedKey, trimmedEndpoint)
    }

    /** Clear saved credentials so the next launch starts at the
     *  connect screen. We deliberately do NOT call any SDK shutdown
     *  here — `Sankofa.init` is one-shot and the in-process state
     *  survives until the user kills the app, which matches the iOS
     *  / web examples' "forget the key, restart the process" model. */
    fun disconnect() {
        prefs?.edit()
            ?.remove(KEY_API_KEY)
            ?.remove(KEY_ENDPOINT)
            ?.apply()
    }

    private fun initialiseSDK(appContext: Context, apiKey: String, endpoint: String) {
        if (initialised) return

        // Switch + Config — init seeds bundled defaults and registers
        // with the Traffic Cop. Run BEFORE Sankofa.init so the
        // auto-discovered flag/config snapshots Catch attaches to its
        // events on the very first crash already see something useful.
        SankofaSwitch.init(appContext, DemoFlag.defaults)
        SankofaRemoteConfig.init(appContext, DemoConfig.defaults)

        Sankofa.init(
            context = appContext,
            apiKey = apiKey,
            config = SankofaConfig(
                endpoint = endpoint,
                recordSessions = true,
                maskAllInputs = true,
                debug = true,
                flushIntervalSeconds = 10,
                batchSize = 5,
                release = "sankofa-example-android@1.0",
                appVersion = "1.0",
                catchEnvironment = inferredEnvironment() ?: "dev",
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

        SankofaPulse.register(appContext)
        initialised = true
    }
}
