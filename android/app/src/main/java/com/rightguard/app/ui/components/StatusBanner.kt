package com.rightguard.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rightguard.app.domain.model.AppMode
import com.rightguard.app.ui.theme.PanicRed
import com.rightguard.app.ui.theme.PanicRedLight
import com.rightguard.app.ui.theme.RightGuardTheme
import com.rightguard.app.ui.theme.SafeguardGreen
import com.rightguard.app.ui.theme.SafeguardGreenLight

/**
 * A banner that displays the current app mode with colour-coded feedback.
 *
 * - [AppMode.IDLE]: neutral grey banner
 * - [AppMode.SAFEGUARD]: green banner with shield icon
 * - [AppMode.PANIC]: red pulsing banner with warning icon
 */
@Composable
fun StatusBanner(
    mode: AppMode,
    modifier: Modifier = Modifier,
) {
    val backgroundColor by animateColorAsState(
        targetValue = when (mode) {
            AppMode.IDLE -> MaterialTheme.colorScheme.surfaceVariant
            AppMode.SAFEGUARD -> SafeguardGreen
            AppMode.PANIC -> PanicRed
        },
        animationSpec = tween(durationMillis = 400),
        label = "bannerBgColor",
    )

    val contentColor = when (mode) {
        AppMode.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
        AppMode.SAFEGUARD -> Color.White
        AppMode.PANIC -> Color.White
    }

    // Pulsing indicator dot for active modes
    val indicatorColor: Color = if (mode == AppMode.PANIC) {
        val infiniteTransition = rememberInfiniteTransition(label = "panicPulse")
        val pulse by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween<Float>(durationMillis = 600),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "panicPulseAlpha",
        )
        PanicRedLight.copy(alpha = 0.5f + pulse * 0.5f)
    } else if (mode == AppMode.SAFEGUARD) {
        SafeguardGreenLight
    } else {
        Color.Transparent
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        shadowElevation = if (mode != AppMode.IDLE) 4.dp else 0.dp,
        tonalElevation = if (mode == AppMode.IDLE) 2.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Pulsing indicator dot
            if (mode != AppMode.IDLE) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(indicatorColor),
                )
            }

            // Mode icon
            when (mode) {
                AppMode.SAFEGUARD -> Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = "Safeguard active",
                    tint = contentColor,
                    modifier = Modifier.size(28.dp),
                )
                AppMode.PANIC -> Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = "Panic mode active",
                    tint = contentColor,
                    modifier = Modifier.size(28.dp),
                )
                AppMode.IDLE -> { /* no icon */ }
            }

            // Status text
            Text(
                text = when (mode) {
                    AppMode.IDLE -> "Ready"
                    AppMode.SAFEGUARD -> "SAFEGUARD ACTIVE"
                    AppMode.PANIC -> "PANIC MODE"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor,
            )
        }
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun StatusBannerIdlePreview() {
    RightGuardTheme {
        StatusBanner(mode = AppMode.IDLE, modifier = Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun StatusBannerSafeguardPreview() {
    RightGuardTheme {
        StatusBanner(mode = AppMode.SAFEGUARD, modifier = Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun StatusBannerPanicPreview() {
    RightGuardTheme {
        StatusBanner(mode = AppMode.PANIC, modifier = Modifier.padding(16.dp))
    }
}
