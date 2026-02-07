package com.rightguard.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rightguard.app.ui.theme.PanicRed
import com.rightguard.app.ui.theme.PanicRedDark
import com.rightguard.app.ui.theme.PanicRedLight
import com.rightguard.app.ui.theme.RightGuardTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val HOLD_DURATION_MS = 3000

/**
 * A red "PANIC" button that requires a long-press (hold for 3 seconds) to
 * activate. This prevents accidental triggers while remaining accessible
 * under stress.
 *
 * Visual feedback:
 * - A red progress bar fills left-to-right during the hold
 * - Label changes to "HOLD..." while pressing
 * - Haptic feedback on activation
 * - Warning icon for clear visual affordance
 *
 * @param onActivate Called once the user holds the button for the full
 *                   [HOLD_DURATION_MS] milliseconds.
 * @param modifier   Optional modifier.
 */
@Composable
fun PanicButton(
    onActivate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // 0f = not pressed, 1f = hold complete
    val holdProgress = remember { Animatable(0f) }
    var isHolding by remember { mutableStateOf(false) }
    var holdJob by remember { mutableStateOf<Job?>(null) }

    val progressFraction = holdProgress.value

    val fillColor = PanicRedLight.copy(alpha = 0.35f)
    val cornerRadius = 28.dp

    Surface(
        modifier = modifier
            .width(220.dp)
            .height(56.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isHolding = true
                        holdJob = scope.launch {
                            holdProgress.snapTo(0f)
                            holdProgress.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(
                                    durationMillis = HOLD_DURATION_MS,
                                    easing = LinearEasing,
                                ),
                            )
                            // Reached 1f => trigger
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onActivate()
                        }

                        // Wait for the press to end (finger lift or cancellation)
                        val released = tryAwaitRelease()
                        isHolding = false
                        if (holdProgress.value < 1f) {
                            // Cancelled before completion — reset
                            holdJob?.cancel()
                            holdJob = null
                            scope.launch { holdProgress.snapTo(0f) }
                        }
                    },
                )
            },
        shape = RoundedCornerShape(cornerRadius),
        color = PanicRed,
        shadowElevation = 6.dp,
    ) {
        Box(
            modifier = Modifier
                .drawBehind {
                    // Draw a progress fill overlay
                    if (progressFraction > 0f) {
                        drawRoundRect(
                            color = fillColor,
                            topLeft = Offset.Zero,
                            size = Size(
                                width = size.width * progressFraction,
                                height = size.height,
                            ),
                            cornerRadius = CornerRadius(
                                cornerRadius.toPx(),
                                cornerRadius.toPx(),
                            ),
                        )
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 24.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = if (isHolding) "HOLD..." else "PANIC",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                )
            }
        }
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun PanicButtonPreview() {
    RightGuardTheme {
        Box(modifier = Modifier.padding(24.dp)) {
            PanicButton(onActivate = {})
        }
    }
}
