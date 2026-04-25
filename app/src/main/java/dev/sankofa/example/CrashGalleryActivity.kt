package dev.sankofa.example

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import dev.sankofa.sdk.SankofaScreen
import dev.sankofa.sdk.catchmod.CatchBreadcrumb
import dev.sankofa.sdk.catchmod.CatchCaptureOptions
import dev.sankofa.sdk.catchmod.CatchLevel
import dev.sankofa.sdk.catchmod.CatchUserContext
import dev.sankofa.sdk.catchmod.SankofaCatch

/**
 * CrashGalleryActivity — a deliberately misbehaving screen that
 * exercises every code path in Sankofa Catch. Mirrors the Node/Web
 * gallery so QA, dashboard demos, and symbolicator validation can
 * reproduce the full matrix of Android error shapes from one place.
 *
 * SDK contract used here (see `SankofaCatch.kt`):
 *   - `setUser` / `setTags` / `setExtra` establish ambient context
 *   - `addBreadcrumb(CatchBreadcrumb)` feeds the ring buffer
 *   - `captureException(t, CatchCaptureOptions(...))` handled path
 *   - `captureMessage(text, CatchCaptureOptions(level=WARNING))`
 *   - unhandled throws fall through to the chained
 *     `Thread.setDefaultUncaughtExceptionHandler` installed in
 *     `SankofaCatch.init(...)` from ExampleApplication.
 */
@SankofaScreen("Crash Gallery")
class CrashGalleryActivity : AppCompatActivity() {

    /**
     * Realistic business-level failure. Subclassed so the symbolicator
     * + dashboard group PaymentDeclined incidents distinctly rather
     * than lumping them under generic `Exception`.
     */
    class PaymentDeclinedException(
        val orderId: String,
        val gatewayCode: String,
        message: String = "payment gateway declined order $orderId ($gatewayCode)",
    ) : Exception(message)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crash_gallery)

        // Ambient context — every event captured on this screen will
        // inherit these. Matches the Node / Web galleries.
        SankofaCatch.setUser(
            CatchUserContext(
                id = "user_demo_gallery_42",
                email = "gallery-demo@sankofa.dev",
                username = "gallery-demo",
                segment = "qa",
            )
        )
        SankofaCatch.setTags(
            mapOf(
                "screen" to "crash_gallery",
                "platform" to "android",
                "demo" to "true",
            )
        )
        SankofaCatch.setExtra("gallery_version", "android-1.0")
        SankofaCatch.addBreadcrumb(
            CatchBreadcrumb(
                type = "navigation",
                category = "ui.lifecycle",
                message = "entered CrashGalleryActivity",
                level = CatchLevel.INFO,
            )
        )

        val status = findViewById<TextView>(R.id.crashStatus)

        // Every button wires through this helper so each press:
        //   1. pushes a rich breadcrumb
        //   2. updates the on-screen status line
        //   3. runs the scenario
        fun wire(id: Int, scenario: String, run: () -> Unit) {
            findViewById<Button>(id).setOnClickListener {
                SankofaCatch.addBreadcrumb(
                    CatchBreadcrumb(
                        type = "user",
                        category = "ui.click",
                        message = "tap:$scenario",
                        level = CatchLevel.INFO,
                        data = mapOf(
                            "scenario" to scenario,
                            "button_id" to resources.getResourceEntryName(id),
                        ),
                    )
                )
                status.text = "fired → $scenario"
                run()
            }
        }

        // 1. NullPointerException — unhandled. Kotlin !! on a null Any?
        wire(R.id.btnCrashNpe, "null_pointer") {
            val s: String? = null
            // Length on null receiver → NPE, which bubbles up to the
            // chained uncaught-exception handler.
            @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
            val n = s!!.length
            status.text = "unreachable ($n)"
        }

        // 2. ClassCastException — bad downcast. Unhandled.
        wire(R.id.btnCrashCce, "class_cast") {
            val any: Any = 42
            val s = any as String // throws CCE
            status.text = "unreachable ($s)"
        }

        // 3. IndexOutOfBoundsException — unhandled.
        wire(R.id.btnCrashIoobe, "index_out_of_bounds") {
            val list = listOf("a", "b", "c")
            val v = list[99]
            status.text = "unreachable ($v)"
        }

        // 4. ArithmeticException — integer divide by zero. Unhandled.
        wire(R.id.btnCrashDivZero, "divide_by_zero") {
            val a = 10
            val b = 0
            val c = a / b
            status.text = "unreachable ($c)"
        }

        // 5. IllegalStateException — realistic "used before init" shape.
        wire(R.id.btnCrashIse, "illegal_state") {
            val session: String? = null
            checkNotNull(session) { "checkout session not initialised" }
            status.text = "unreachable"
        }

        // 6. IllegalArgumentException — argument validation.
        wire(R.id.btnCrashIae, "illegal_argument") {
            val userAge = -5
            require(userAge >= 0) { "age must be non-negative, got $userAge" }
            status.text = "unreachable"
        }

        // 7. NumberFormatException — parse garbage.
        wire(R.id.btnCrashNfe, "number_format") {
            val raw = "banana"
            val n = raw.toInt()
            status.text = "unreachable ($n)"
        }

        // 8. StackOverflowError — unbounded recursion. Unhandled.
        wire(R.id.btnCrashSoe, "stack_overflow") {
            infiniteRecursion(0)
            status.text = "unreachable"
        }

        // 9. OutOfMemoryError — huge allocation. Unhandled.
        wire(R.id.btnCrashOom, "out_of_memory") {
            // 2 GiB byte array — blows past any reasonable heap.
            val hog = ByteArray(Int.MAX_VALUE / 1)
            status.text = "unreachable (${hog.size})"
        }

        // 10. Background-thread uncaught — exercises the chained
        //     Thread.setDefaultUncaughtExceptionHandler on a non-main
        //     thread.
        wire(R.id.btnCrashThread, "background_thread_throw") {
            Thread({
                throw RuntimeException("intentional background-thread failure")
            }, "crash-gallery-worker").start()
            status.text = "dispatched background-thread throw"
        }

        // 11. Custom business error — captured manually, with a
        //     fingerprint so the dashboard groups it by gatewayCode
        //     rather than by stack trace. Does not crash.
        wire(R.id.btnCrashPayment, "payment_declined_manual") {
            try {
                throw PaymentDeclinedException(
                    orderId = "ord_5f8c2b",
                    gatewayCode = "insufficient_funds",
                )
            } catch (e: PaymentDeclinedException) {
                val eventId = SankofaCatch.captureException(
                    e,
                    CatchCaptureOptions(
                        level = CatchLevel.ERROR,
                        tags = mapOf(
                            "gateway_code" to e.gatewayCode,
                            "order_id" to e.orderId,
                            "billing_flow" to "checkout_v2",
                        ),
                        extra = mapOf(
                            "cart_total_usd" to 129.00,
                            "retry_count" to 2,
                            "customer_tier" to "gold",
                        ),
                        // Group by gateway code, not stack — every
                        // "insufficient_funds" decline joins one
                        // incident in Catch.
                        fingerprint = listOf("payment_declined", e.gatewayCode),
                    ),
                )
                status.text = "captured payment decline → $eventId"
            }
        }

        // 12. captureMessage warning — non-error signal path.
        wire(R.id.btnCrashMessage, "capture_message_warning") {
            val eventId = SankofaCatch.captureMessage(
                "feature_flag_fallback: checkout_cta_variant missing on edge cache",
                CatchCaptureOptions(
                    level = CatchLevel.WARNING,
                    tags = mapOf("subsystem" to "flags"),
                    extra = mapOf(
                        "flag_key" to "checkout_cta_variant",
                        "fell_back_to" to "control",
                    ),
                ),
            )
            status.text = "captured warning → $eventId"
        }

        // 13. ANR — freeze the main thread for 6s. Android pops the
        //     "Application Not Responding" dialog after 5s; the
        //     watcher in SankofaCatch also reports an `anr` event.
        wire(R.id.btnCrashAnr, "anr_main_thread_freeze") {
            status.text = "freezing main thread 6s — expect ANR dialog"
            // Sleep on the UI thread — the whole point. Don't move
            // this to a background thread.
            Thread.sleep(6_500)
            status.text = "main thread resumed"
        }
    }

    private fun infiniteRecursion(depth: Int): Int {
        // Heap-allocated extra frame argument stops the JIT from
        // optimising this into a tail call on some runtimes.
        return infiniteRecursion(depth + 1) + 1
    }
}
