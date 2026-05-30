package dev.sankofa.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * First-run "connect" screen — mirror of the iOS `ConnectView` and the
 * web example's `ApiKeyGate`. Collects an API key + optional endpoint,
 * persists them via `SankofaConnection`, then routes to `MainActivity`.
 *
 * Marked as the LAUNCHER activity (see `AndroidManifest.xml`); if
 * credentials are already saved we bounce straight to `MainActivity`
 * inside `onCreate` so the user only sees this screen once per
 * project — exactly like the web example.
 */
class ConnectActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (SankofaConnection.isConnected()) {
            launchMain()
            return
        }

        setContentView(R.layout.activity_connect)

        val edtApiKey = findViewById<EditText>(R.id.edtApiKey)
        val edtEndpoint = findViewById<EditText>(R.id.edtEndpoint)
        val lblInferredEnv = findViewById<TextView>(R.id.lblInferredEnv)
        val lblError = findViewById<TextView>(R.id.lblError)
        val btnConnect = findViewById<Button>(R.id.btnConnect)
        val lnkGetKey = findViewById<TextView>(R.id.lnkGetKey)

        // Pre-fill endpoint with the previously-used value (or the
        // default). Lets the developer flip back to ConnectActivity
        // via "Disconnect" without re-typing the URL every time.
        edtEndpoint.setText(SankofaConnection.endpoint())

        // Pre-fill the API key too — saved value if present, otherwise the
        // built-in default so the example connects to the live server with a
        // single tap. afterTextChanged fires automatically and renders the
        // detected-environment pill.
        edtApiKey.setText(SankofaConnection.apiKey().ifEmpty { SankofaConnection.defaultApiKey() })

        // Live env detection — same `sk_test_…` / `sk_live_…` rule
        // used across the SDK family. Just decorative; the server is
        // the source of truth for which environment the key maps to.
        edtApiKey.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                val key = s?.toString().orEmpty()
                when {
                    key.startsWith("sk_test_") -> {
                        lblInferredEnv.text = "Detected TEST environment"
                        lblInferredEnv.setTextColor(0xFFEAB308.toInt())
                        lblInferredEnv.visibility = TextView.VISIBLE
                    }
                    key.startsWith("sk_live_") -> {
                        lblInferredEnv.text = "Detected LIVE environment"
                        lblInferredEnv.setTextColor(0xFF22C55E.toInt())
                        lblInferredEnv.visibility = TextView.VISIBLE
                    }
                    else -> lblInferredEnv.visibility = TextView.GONE
                }
                if (lblError.visibility == TextView.VISIBLE) lblError.visibility = TextView.GONE
            }
        })

        btnConnect.setOnClickListener {
            val key = edtApiKey.text.toString().trim()
            val endpoint = edtEndpoint.text.toString().trim()
            if (key.length < 8) {
                lblError.text = "That key looks too short — paste the full token."
                lblError.visibility = TextView.VISIBLE
                return@setOnClickListener
            }
            SankofaConnection.connect(applicationContext, key, endpoint)
            launchMain()
        }

        lnkGetKey.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://sankofa.dev")))
        }
    }

    private fun launchMain() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
