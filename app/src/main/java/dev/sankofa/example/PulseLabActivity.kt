package dev.sankofa.example

import android.app.Activity
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sankofa.sdk.SankofaScreen
import dev.sankofa.sdk.pulse.PulseEvent
import dev.sankofa.sdk.pulse.PulseEventPayload
import dev.sankofa.sdk.pulse.PulseSubscription
import dev.sankofa.sdk.pulse.SankofaPulse
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pulse Lab — exercises every public surface of [SankofaPulse]:
 *   • Shows registration status
 *   • Lets the user toggle a "Pro user" flag that's forwarded as
 *     `userProperties.plan` so the gated `psv_demo_product_research`
 *     survey only matches when toggled on.
 *   • One card per canonical demo survey (NPS, CSAT, Pro-only research)
 *     with [Show] + [Check eligibility] actions.
 *   • Live event log — every lifecycle event from `SankofaPulse.on()`
 *     lands in a scrolling list so you can see the full survey flow
 *     (shown → partial_saved → completed | dismissed) at a glance.
 */
@SankofaScreen("Pulse Lab")
class PulseLabActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(color = Color(0xFF0F0F14), modifier = Modifier.fillMaxSize()) {
                    PulseLabScreen(activity = this)
                }
            }
        }
    }
}

@Composable
private fun PulseLabScreen(activity: Activity) {
    var proUser by remember { mutableStateOf(false) }
    val events = remember { mutableStateListOf<PulseLogEntry>() }
    val coroutineScope = rememberCoroutineScope()
    var registered by remember { mutableStateOf(SankofaPulse.isRegistered()) }
    val eligibility = remember { androidx.compose.runtime.mutableStateMapOf<String, String>() }

    DisposableEffect(Unit) {
        val handles = mutableListOf<PulseSubscription>()
        for (event in PulseEvent.values()) {
            handles += SankofaPulse.on(event) { payload ->
                events.add(0, PulseLogEntry.from(payload))
                if (events.size > 50) events.removeAt(events.size - 1)
            }
        }
        onDispose { handles.forEach { it.cancel() } }
    }

    LaunchedEffect(Unit) {
        // Cheap re-check in case the activity loaded before
        // ExampleApplication's register() resolved.
        registered = SankofaPulse.isRegistered()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Pulse Lab",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            "Drive every Sankofa Pulse surface from one place. Surveys are seeded by the `seed_pulse` Go command.",
            color = Color(0xFF94A3B8),
            fontSize = 12.sp,
        )

        RegistrationCard(registered = registered)

        ProUserToggleCard(
            checked = proUser,
            onCheckedChange = { proUser = it },
        )

        SectionLabel("DEMO SURVEYS")

        for (id in DemoSurvey.all) {
            SurveyCard(
                surveyId = id,
                eligibility = eligibility[id],
                onShow = {
                    SankofaPulse.show(
                        surveyId = id,
                        activity = activity,
                        properties = mapOf("plan" to if (proUser) "pro" else "free"),
                    )
                },
                onCheck = {
                    coroutineScope.launch {
                        val decision = SankofaPulse.isEligible(
                            surveyId = id,
                            properties = mapOf("plan" to if (proUser) "pro" else "free"),
                        )
                        eligibility[id] = if (decision.eligible) {
                            "✅ eligible"
                        } else {
                            "🚫 ${decision.reason ?: "ineligible"}"
                        }
                    }
                },
            )
        }

        SectionLabel("EVENT LOG")

        if (events.isEmpty()) {
            Text(
                "No events yet — try Show on a survey above.",
                color = Color(0xFF64748B),
                fontSize = 12.sp,
            )
        } else {
            for (entry in events) {
                EventRow(entry)
            }
        }
    }
}

@Composable
private fun RegistrationCard(registered: Boolean) {
    Surface(
        color = Color(0xFF1A1A2E),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (registered) Color(0xFF22C55E).copy(alpha = 0.4f)
                else Color(0xFFEF4444).copy(alpha = 0.4f),
                RoundedCornerShape(12.dp),
            ),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Eyebrow(if (registered) "REGISTERED" else "NOT REGISTERED")
            Spacer(Modifier.height(6.dp))
            Text(
                if (registered) {
                    "SankofaPulse.register() succeeded — handshakes flow."
                } else {
                    "Pulse hasn't registered. Confirm Sankofa.init ran before SankofaPulse.register()."
                },
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ProUserToggleCard(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Surface(
        color = Color(0xFF1A1A2E),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp)),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Eyebrow("HOST CONTEXT")
                Spacer(Modifier.height(4.dp))
                Text("Pro user", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    "Forwarded as userProperties.plan = \"${if (checked) "pro" else "free"}\". Gates psv_demo_product_research.",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFFF5A623),
                    checkedTrackColor = Color(0xFFF5A623).copy(alpha = 0.3f),
                ),
            )
        }
    }
}

@Composable
private fun SurveyCard(
    surveyId: String,
    eligibility: String?,
    onShow: () -> Unit,
    onCheck: () -> Unit,
) {
    Surface(
        color = Color(0xFF1A1A2E),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp)),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                DemoSurvey.titles[surveyId] ?: surveyId,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            )
            Text(
                surveyId,
                color = Color(0xFF94A3B8),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                DemoSurvey.descriptions[surveyId] ?: "",
                color = Color(0xFFCBD5E1),
                fontSize = 12.sp,
            )
            if (!eligibility.isNullOrEmpty()) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        eligibility,
                        color = Color(0xFFFDA4AF),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onShow,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF5A623)),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Show", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onCheck,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Check eligibility", color = Color(0xFFF5A623), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun EventRow(entry: PulseLogEntry) {
    Surface(
        color = Color(0xFF111118),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(8.dp)),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row {
                Text(
                    entry.event,
                    color = Color(0xFFF5A623),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    entry.timestamp,
                    color = Color(0xFF64748B),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                )
            }
            Text(
                entry.surveyId,
                color = Color(0xFFCBD5E1),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
            )
            entry.responseId?.let {
                Text("response_id: $it", color = Color(0xFF94A3B8), fontSize = 10.sp)
            }
            entry.reason?.let {
                Text("reason: $it", color = Color(0xFF94A3B8), fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Spacer(Modifier.height(6.dp))
    Eyebrow(text)
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

private data class PulseLogEntry(
    val event: String,
    val surveyId: String,
    val responseId: String?,
    val reason: String?,
    val timestamp: String,
) {
    companion object {
        private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
        fun from(payload: PulseEventPayload): PulseLogEntry = PulseLogEntry(
            event = payload.event.wireName,
            surveyId = payload.surveyId,
            responseId = payload.responseId,
            reason = payload.reason,
            timestamp = timeFormat.format(Date()),
        )
    }
}

