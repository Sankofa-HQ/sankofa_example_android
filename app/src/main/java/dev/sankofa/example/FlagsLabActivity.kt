package dev.sankofa.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sankofa.sdk.Sankofa
import dev.sankofa.sdk.SankofaScreen
import dev.sankofa.sdk.remoteconfig.ConfigType
import dev.sankofa.sdk.remoteconfig.ItemDecision
import dev.sankofa.sdk.remoteconfig.SankofaRemoteConfig
import dev.sankofa.sdk.switchmod.Cancellation
import dev.sankofa.sdk.switchmod.FlagDecision
import dev.sankofa.sdk.switchmod.SankofaSwitch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Flags + Config Lab — single-scroll view that:
 *   - Applies every canonical demo flag/config into a live preview
 *   - Tables every decision with its key, value, reason, version
 *
 * `onChange` subscribers bump a `rev` state so the view re-renders as
 * soon as a handshake refresh lands dashboard edits.
 */
@SankofaScreen("Flags & Config Lab")
class FlagsLabActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Sankofa.screen("Flags & Config Lab")
        setContent {
            MaterialTheme {
                Surface(color = Color(0xFF0F0F14), modifier = Modifier.fillMaxSize()) {
                    LabScreen()
                }
            }
        }
    }
}

@Composable
private fun LabScreen() {
    // rev is the re-render trigger. Every onChange subscription bumps
    // it, causing the derived snapshot reads below to recompute.
    var rev by remember { mutableStateOf(0) }

    DisposableEffect(Unit) {
        val handles = mutableListOf<Cancellation>()
        for (key in DemoFlag.all) {
            handles += SankofaSwitch.onChange(key) { rev++ }
        }
        for (key in DemoConfig.all) {
            handles += SankofaRemoteConfig.onChange(key) { rev++ }
        }
        onDispose {
            handles.forEach { it.cancel() }
        }
    }

    // Snapshot reads. Using `rev` here ties them to the subscription.
    @Suppress("UNUSED_EXPRESSION") rev
    val flags = DemoFlag.all.associateWith { (SankofaSwitch.getDecision(it) ?: DemoFlag.defaults[it])!! }
    val config = DemoConfig.all.associateWith { (SankofaRemoteConfig.getDecision(it) ?: DemoConfig.defaults[it])!! }

    val themePrimary = hexColor(stringMapValue(config[DemoConfig.THEME_COLORS]?.value, "primary")) ?: Color(0xFFF5A623)
    val themeAccent = hexColor(stringMapValue(config[DemoConfig.THEME_COLORS]?.value, "accent")) ?: Color(0xFF6366F1)

    val supportUrl = config[DemoConfig.SUPPORT_URL]?.value as? String ?: "https://support.sankofa.dev"
    val maxUploads = (config[DemoConfig.MAX_UPLOADS_PER_DAY]?.value as? Number)?.toInt() ?: 25
    val discount = (config[DemoConfig.TRIAL_DISCOUNT_PCT]?.value as? Number)?.toDouble() ?: 0.0
    val maintenance = config[DemoConfig.MAINTENANCE_BANNER_ENABLED]?.value as? Boolean ?: false
    val tiers = run {
        val parsed = DemoPricingTier.parse(config[DemoConfig.PRICING_TABLE]?.value)
        val arm = flags[DemoFlag.AB_PRICING_PAGE]?.variant ?: "A"
        if (arm == "B") parsed.reversed() else parsed
    }

    val newHome = flags[DemoFlag.NEW_HOME_LAYOUT]?.value ?: false
    val ctaVariant = flags[DemoFlag.CHECKOUT_CTA_VARIANT]?.variant ?: "control"
    val onboardingV2 = flags[DemoFlag.ONBOARDING_V2_ROLLOUT]?.value ?: false
    val aiHalted = flags[DemoFlag.AI_SUMMARY_KILL_SWITCH]?.value ?: false
    val pricingArm = flags[DemoFlag.AB_PRICING_PAGE]?.variant ?: "A"
    val premiumBadge = flags[DemoFlag.PREMIUM_BADGE_VISIBLE]?.value ?: true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (maintenance) {
            MaintenanceBanner()
        }

        HeroCard(newHome = newHome, themePrimary = themePrimary, themeAccent = themeAccent, ctaVariant = ctaVariant)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            MiniCard(modifier = Modifier.weight(1f), eyebrow = "AI SUMMARY") {
                if (aiHalted) {
                    Text("🛑 Paused", color = Color(0xFFFCA5A5), fontWeight = FontWeight.Bold)
                    Text(
                        "ai_summary_kill_switch halted. Halt webhooks flip this live.",
                        color = Color(0xFF94A3B8), fontSize = 11.sp,
                    )
                } else {
                    Text("Ready for queries", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Kill switch clear.", color = Color(0xFF94A3B8), fontSize = 11.sp)
                }
            }
            MiniCard(modifier = Modifier.weight(1f), eyebrow = "UPLOADS") {
                Text("$maxUploads / day", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = {
                        // Read the flag on press — records an exposure row.
                        SankofaSwitch.getFlag(DemoFlag.ONBOARDING_V2_ROLLOUT, false)
                    },
                    enabled = onboardingV2,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (onboardingV2) themeAccent else Color.DarkGray,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (onboardingV2) "Open uploader (v2)" else "Coming soon",
                        fontSize = 12.sp,
                    )
                }
            }
        }

        PricingCard(
            tiers = tiers,
            discount = discount,
            themePrimary = themePrimary,
            pricingArm = pricingArm,
            premiumBadge = premiumBadge,
        )

        MiniCard(eyebrow = "SUPPORT") {
            Text(
                supportUrl,
                color = themeAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            )
            Text("From support_url.", color = Color(0xFF94A3B8), fontSize = 11.sp)
        }

        SectionLabel("SANKOFA SWITCH — LIVE DECISIONS")
        for (key in DemoFlag.all) {
            FlagRow(key = key, decision = flags[key]!!)
        }

        SectionLabel("SANKOFA CONFIG — TYPED REMOTE VALUES")
        for (key in DemoConfig.all) {
            ConfigRow(key = key, decision = config[key]!!)
        }
    }
}

@Composable
private fun MaintenanceBanner() {
    Surface(
        color = Color(0xFFF59E0B).copy(alpha = 0.15f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(10.dp)),
    ) {
        Text(
            "⚠️  Maintenance window — some features may be slow.",
            color = Color(0xFFFBBF24),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Composable
private fun HeroCard(
    newHome: Boolean,
    themePrimary: Color,
    themeAccent: Color,
    ctaVariant: String,
) {
    val ctaLabel = when (ctaVariant) {
        "blue" -> "Try it free"
        "red" -> "Upgrade now"
        else -> "Get started"
    }
    val ctaBg = when (ctaVariant) {
        "blue" -> Color(0xFF2563EB)
        "red" -> Color(0xFFDC2626)
        else -> themePrimary
    }

    Surface(
        color = if (newHome) themePrimary.copy(alpha = 0.12f) else Color(0xFF1A1A2E),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, themeAccent.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Eyebrow(if (newHome) "HERO LAYOUT: V2" else "HERO LAYOUT: CLASSIC")
            Text(
                if (newHome) "Analytics for modern teams" else "Ship analytics in minutes",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
            )
            Text(
                "Driven by new_home_layout and theme_colors.",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    // Reading the variant records an exposure row.
                    SankofaSwitch.getVariant(DemoFlag.CHECKOUT_CTA_VARIANT, "control")
                },
                colors = ButtonDefaults.buttonColors(containerColor = ctaBg),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(ctaLabel, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            Text("CTA variant: $ctaVariant", color = Color(0xFF64748B), fontSize = 11.sp)
        }
    }
}

@Composable
private fun PricingCard(
    tiers: List<DemoPricingTier>,
    discount: Double,
    themePrimary: Color,
    pricingArm: String,
    premiumBadge: Boolean,
) {
    Surface(
        color = Color(0xFF1A1A2E),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, themePrimary.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Eyebrow("PRICING — ARM $pricingArm")
                    Text(
                        if (pricingArm == "B") "Enterprise-first pricing" else "Simple pricing, scales with you",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (premiumBadge) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(themePrimary.copy(alpha = 0.2f))
                            .border(1.dp, themePrimary.copy(alpha = 0.5f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            "✨ Premium",
                            color = themePrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            for (tier in tiers) {
                PricingTileRow(tier = tier, discount = discount, themePrimary = themePrimary)
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun PricingTileRow(tier: DemoPricingTier, discount: Double, themePrimary: Color) {
    val discounted = (tier.price * (1 - discount)).coerceAtLeast(0.0)
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp)),
    ) {
        Row(modifier = Modifier.padding(10.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(tier.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    "\$${discounted.toInt()}/mo",
                    color = themePrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                if (discount > 0 && tier.price > 0) {
                    Text(
                        "${(discount * 100).toInt()}% off trial",
                        color = Color(0xFFFBBF24),
                        fontSize = 10.sp,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                for (f in tier.features) {
                    Text("• $f", color = Color(0xFF94A3B8), fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun MiniCard(
    modifier: Modifier = Modifier,
    eyebrow: String,
    content: @Composable () -> Unit,
) {
    Surface(
        color = Color(0xFF1A1A2E),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp)),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Eyebrow(eyebrow)
            Spacer(Modifier.height(6.dp))
            content()
        }
    }
}

@Composable
private fun Eyebrow(text: String) {
    Text(
        text,
        color = Color(0xFF94A3B8),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.4.sp,
    )
}

@Composable
private fun SectionLabel(text: String) {
    Spacer(Modifier.height(6.dp))
    Eyebrow(text)
}

@Composable
private fun FlagRow(key: String, decision: FlagDecision) {
    val value = if (decision.variant.isNotEmpty()) decision.variant else decision.value.toString()
    Surface(
        color = Color(0xFF1A1A2E),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    key,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
                Text(
                    DemoFlag.descriptions[key] ?: "",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    value,
                    color = Color(0xFFFDA4AF),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
                Text(
                    "${decision.reason.wireName} · v${decision.version}",
                    color = Color(0xFF64748B),
                    fontSize = 10.sp,
                )
            }
        }
    }
}

@Composable
private fun ConfigRow(key: String, decision: ItemDecision) {
    val rendered = when {
        decision.type == ConfigType.JSON -> renderJson(decision.value)
        decision.value is String -> "\"${decision.value}\""
        else -> decision.value?.toString() ?: "null"
    }
    Surface(
        color = Color(0xFF1A1A2E),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    key,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
                Text(
                    DemoConfig.descriptions[key] ?: "",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                Text(
                    rendered,
                    color = Color(0xFFFDA4AF),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    maxLines = 3,
                )
                Text(
                    "${decision.type.wireName} · ${decision.reason.wireName} · v${decision.version}",
                    color = Color(0xFF64748B),
                    fontSize = 10.sp,
                )
            }
        }
    }
}

// ─── Helpers ────────────────────────────────────────────────────────

private fun hexColor(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    val raw = hex.trimStart('#')
    val value = runCatching { raw.toLong(16) }.getOrNull() ?: return null
    return when (raw.length) {
        6 -> Color((0xFF000000L or value).toULong().toLong())
        8 -> Color(value.toULong().toLong())
        else -> null
    }
}

@Suppress("UNCHECKED_CAST")
private fun stringMapValue(raw: Any?, key: String): String? {
    val map = raw as? Map<String, Any?> ?: return null
    return map[key] as? String
}

private fun renderJson(value: Any?): String = when (value) {
    null -> "null"
    is Map<*, *> -> JSONObject(value.mapKeys { it.key.toString() }).toString()
    is List<*> -> JSONArray(value).toString()
    else -> value.toString()
}
