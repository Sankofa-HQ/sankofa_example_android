package dev.sankofa.example

import dev.sankofa.sdk.remoteconfig.ConfigType
import dev.sankofa.sdk.remoteconfig.ItemDecision
import dev.sankofa.sdk.remoteconfig.ItemReason
import dev.sankofa.sdk.switchmod.FlagDecision
import dev.sankofa.sdk.switchmod.FlagReason

// Canonical demo keys — identical to every other Sankofa example
// (web, react-native, html, ios, android, flutter). One dashboard
// config row drives every client, so keep these strings stable.

object DemoFlag {
    const val NEW_HOME_LAYOUT         = "new_home_layout"
    const val CHECKOUT_CTA_VARIANT    = "checkout_cta_variant"
    const val ONBOARDING_V2_ROLLOUT   = "onboarding_v2_rollout"
    const val AI_SUMMARY_KILL_SWITCH  = "ai_summary_kill_switch"
    const val AB_PRICING_PAGE         = "ab_pricing_page"
    const val PREMIUM_BADGE_VISIBLE   = "premium_badge_visible"

    val all = listOf(
        NEW_HOME_LAYOUT,
        CHECKOUT_CTA_VARIANT,
        ONBOARDING_V2_ROLLOUT,
        AI_SUMMARY_KILL_SWITCH,
        AB_PRICING_PAGE,
        PREMIUM_BADGE_VISIBLE,
    )

    val descriptions = mapOf(
        NEW_HOME_LAYOUT        to "Swap hero between classic and v2.",
        CHECKOUT_CTA_VARIANT   to "A/B/C variant — CTA copy + colour.",
        ONBOARDING_V2_ROLLOUT  to "Progressive rollout gate.",
        AI_SUMMARY_KILL_SWITCH to "Halt webhook pauses AI summary.",
        AB_PRICING_PAGE        to "Variant A/B on pricing copy.",
        PREMIUM_BADGE_VISIBLE  to "Show/hide the premium badge.",
    )

    val defaults: Map<String, FlagDecision> = mapOf(
        NEW_HOME_LAYOUT        to FlagDecision(value = false, reason = FlagReason.UNKNOWN, version = 0),
        CHECKOUT_CTA_VARIANT   to FlagDecision(value = true,  variant = "control", reason = FlagReason.UNKNOWN, version = 0),
        ONBOARDING_V2_ROLLOUT  to FlagDecision(value = false, reason = FlagReason.UNKNOWN, version = 0),
        AI_SUMMARY_KILL_SWITCH to FlagDecision(value = false, reason = FlagReason.UNKNOWN, version = 0),
        AB_PRICING_PAGE        to FlagDecision(value = true,  variant = "A", reason = FlagReason.UNKNOWN, version = 0),
        PREMIUM_BADGE_VISIBLE  to FlagDecision(value = true,  reason = FlagReason.UNKNOWN, version = 0),
    )
}

object DemoConfig {
    const val SUPPORT_URL                = "support_url"
    const val MAX_UPLOADS_PER_DAY        = "max_uploads_per_day"
    const val TRIAL_DISCOUNT_PCT         = "trial_discount_pct"
    const val MAINTENANCE_BANNER_ENABLED = "maintenance_banner_enabled"
    const val PRICING_TABLE              = "pricing_table"
    const val THEME_COLORS               = "theme_colors"

    val all = listOf(
        SUPPORT_URL,
        MAX_UPLOADS_PER_DAY,
        TRIAL_DISCOUNT_PCT,
        MAINTENANCE_BANNER_ENABLED,
        PRICING_TABLE,
        THEME_COLORS,
    )

    val descriptions = mapOf(
        SUPPORT_URL                to "String — support link target.",
        MAX_UPLOADS_PER_DAY        to "Int — daily upload ceiling.",
        TRIAL_DISCOUNT_PCT         to "Float 0–1 — trial discount.",
        MAINTENANCE_BANNER_ENABLED to "Bool — amber maintenance banner.",
        PRICING_TABLE              to "JSON — array of pricing tiers.",
        THEME_COLORS               to "JSON {primary, accent} — theme tokens.",
    )

    val defaults: Map<String, ItemDecision> = mapOf(
        SUPPORT_URL to ItemDecision(
            value = "https://support.sankofa.dev",
            type = ConfigType.STRING, reason = ItemReason.UNKNOWN, version = 0,
        ),
        MAX_UPLOADS_PER_DAY to ItemDecision(
            value = 25,
            type = ConfigType.INT, reason = ItemReason.UNKNOWN, version = 0,
        ),
        TRIAL_DISCOUNT_PCT to ItemDecision(
            value = 0.2,
            type = ConfigType.FLOAT, reason = ItemReason.UNKNOWN, version = 0,
        ),
        MAINTENANCE_BANNER_ENABLED to ItemDecision(
            value = false,
            type = ConfigType.BOOL, reason = ItemReason.UNKNOWN, version = 0,
        ),
        PRICING_TABLE to ItemDecision(
            value = listOf(
                mapOf("name" to "Starter",    "price" to 0,   "features" to listOf("1 project",          "1k events/mo")),
                mapOf("name" to "Pro",        "price" to 49,  "features" to listOf("Unlimited projects", "1M events/mo", "Replay")),
                mapOf("name" to "Enterprise", "price" to 199, "features" to listOf("SSO",                "Priority support", "Audit log")),
            ),
            type = ConfigType.JSON, reason = ItemReason.UNKNOWN, version = 0,
        ),
        THEME_COLORS to ItemDecision(
            value = mapOf("primary" to "#F5A623", "accent" to "#6366f1"),
            type = ConfigType.JSON, reason = ItemReason.UNKNOWN, version = 0,
        ),
    )
}

// Minimal typed wrapper mirroring the tiers shape all the other
// example projects parse out of `pricing_table`.
data class DemoPricingTier(
    val name: String,
    val price: Double,
    val features: List<String>,
) {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun parse(raw: Any?): List<DemoPricingTier> {
            if (raw !is List<*>) return emptyList()
            return raw.mapNotNull { row ->
                val m = row as? Map<String, Any?> ?: return@mapNotNull null
                val name = m["name"] as? String ?: return@mapNotNull null
                val price = (m["price"] as? Number)?.toDouble() ?: 0.0
                val features = (m["features"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                DemoPricingTier(name = name, price = price, features = features)
            }
        }
    }
}
