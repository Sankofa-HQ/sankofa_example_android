package dev.sankofa.example

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import dev.sankofa.sdk.Sankofa
import dev.sankofa.sdk.SankofaScreen
import dev.sankofa.sdk.remoteconfig.SankofaRemoteConfig
import dev.sankofa.sdk.switchmod.SankofaSwitch

@SankofaScreen("Home Screen XML")
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Hook up the continuous bounds testing animation
        val pulsarView = findViewById<View>(R.id.animatedPulsarView)
        ObjectAnimator.ofFloat(pulsarView, "rotation", 0f, 360f).apply {
            duration = 2000
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }

        // Maintenance banner — driven by `maintenance_banner_enabled`
        // remote config. Read on resume so a handshake refresh in the
        // background picks up a flip on the next foreground.
        val maintenanceView = findViewById<TextView>(R.id.maintenanceBanner)
        maintenanceView.visibility =
            if (SankofaRemoteConfig.get(DemoConfig.MAINTENANCE_BANNER_ENABLED, false)) View.VISIBLE
            else View.GONE

        // Flag-driven CTA — label + colour flip with the
        // `checkout_cta_variant` A/B/C flag. Reading the variant on
        // press records an exposure row that feeds experiment math.
        val ctaButton = findViewById<Button>(R.id.btnFlagCta)
        val ctaVariant = SankofaSwitch.getVariant(DemoFlag.CHECKOUT_CTA_VARIANT, "control")
        ctaButton.text = when (ctaVariant) {
            "blue" -> "Try it free"
            "red"  -> "Upgrade now"
            else   -> "Fire showcase event"
        }
        ctaButton.setBackgroundColor(when (ctaVariant) {
            "blue" -> Color.parseColor("#2563EB")
            "red"  -> Color.parseColor("#DC2626")
            else   -> Color.parseColor("#8B5CF6")
        })
        ctaButton.setOnClickListener {
            // Variant read records the exposure server-side.
            SankofaSwitch.getVariant(DemoFlag.CHECKOUT_CTA_VARIANT, "control")
            Sankofa.track("cta_showcase_pressed", mapOf("variant" to ctaVariant))
            Toast.makeText(this, "Fired with variant=$ctaVariant", Toast.LENGTH_SHORT).show()
        }

        // Launch Flags Lab
        findViewById<Button>(R.id.btnOpenFlagsLab).setOnClickListener {
            startActivity(Intent(this, FlagsLabActivity::class.java))
        }

        // Navigation to Compose test
        findViewById<Button>(R.id.btnNavCompose).setOnClickListener {
            startActivity(Intent(this, ComposeStressActivity::class.java))
        }

        // Launch Crash Gallery
        findViewById<Button>(R.id.btnOpenCrashGallery).setOnClickListener {
            startActivity(Intent(this, CrashGalleryActivity::class.java))
        }

        findViewById<Button>(R.id.btnTrackPurchase).setOnClickListener {
            Sankofa.track(
                "purchase_completed",
                mapOf(
                    "item" to "Premium Plan",
                    "price" to 99.99,
                    "currency" to "USD"
                )
            )
            Toast.makeText(this, "Tracked 'purchase_completed'", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnIdentifyUser).setOnClickListener {
            Sankofa.identify("user_prod_778899")
            Toast.makeText(this, "Identified user_prod_778899", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnSetPerson).setOnClickListener {
            Sankofa.setPerson(
                name = "Samuel Asare",
                email = "samuel@sankofa.dev"
            )
            Toast.makeText(this, "Person profile updated", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnReset).setOnClickListener {
            Sankofa.reset()
            Toast.makeText(this, "Identity reset (Anonymous)", Toast.LENGTH_SHORT).show()
        }
    }
}
