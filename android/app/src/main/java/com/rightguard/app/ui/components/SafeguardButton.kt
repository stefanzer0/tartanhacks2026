package com.rightguard.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rightguard.app.ui.theme.RightGuardTheme
import com.rightguard.app.ui.theme.SafeguardGreen
import com.rightguard.app.ui.theme.SafeguardGreenDark
import com.rightguard.app.ui.theme.SafeguardGreenLight

/**
 * A large, prominent circular button for activating safeguard mode.
 *
 * Visual design:
 * - Circular shape (200 dp)
 * - Deep green (#2E7D32) background
 * - White shield icon + "ACTIVATE SAFEGUARD" label
 * - Press animation (slight scale-down) for tactile feedback
 * - Elevated shadow for visual prominence
 */
@Composable
fun SafeguardButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "safeguardPressScale",
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .size(200.dp)
            .scale(scale)
            .shadow(
                elevation = if (enabled) 12.dp else 4.dp,
                shape = CircleShape,
                ambientColor = SafeguardGreen,
                spotColor = SafeguardGreen,
            ),
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = SafeguardGreen,
            contentColor = Color.White,
            disabledContainerColor = SafeguardGreenLight.copy(alpha = 0.4f),
            disabledContentColor = Color.White.copy(alpha = 0.6f),
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 8.dp,
            pressedElevation = 2.dp,
            disabledElevation = 2.dp,
        ),
        contentPadding = PaddingValues(16.dp),
        interactionSource = interactionSource,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "ACTIVATE",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
            )
            Text(
                text = "SAFEGUARD",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
            )
        }
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun SafeguardButtonPreview() {
    RightGuardTheme {
        SafeguardButton(onClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun SafeguardButtonDisabledPreview() {
    RightGuardTheme {
        SafeguardButton(onClick = {}, enabled = false)
    }
}
