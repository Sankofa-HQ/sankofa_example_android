package dev.sankofa.example

import android.app.Application

class ExampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Hand off all SDK bootstrapping to `SankofaConnection`. If the
        // user has previously connected (SharedPreferences carries an
        // api_key + endpoint) the helper auto-initialises Sankofa,
        // SankofaSwitch, SankofaRemoteConfig, and SankofaPulse on the
        // app process so feature surfaces work out of `MainActivity`
        // straight away. If no creds are saved the helper does
        // nothing — `ConnectActivity` is the LAUNCHER and will collect
        // them on first run.
        SankofaConnection.bootstrap(this)
    }
}
