package com.rightguard.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// ── Static (fallback) colour schemes ─────────────────────────────────────────

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryBlueLight,
    onPrimaryContainer = Color.White,

    secondary = SecondaryAmber,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryAmberLight,
    onSecondaryContainer = Color.Black,

    tertiary = SafeguardGreen,
    onTertiary = Color.White,
    tertiaryContainer = SafeguardGreenLight,
    onTertiaryContainer = Color.Black,

    error = PanicRed,
    onError = OnErrorLight,
    errorContainer = PanicRedLight,
    onErrorContainer = Color.Black,

    background = BackgroundLight,
    onBackground = OnBackgroundLight,

    surface = SurfaceLight,
    onSurface = OnSurfaceLight,

    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,

    outline = OutlineLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlueLight,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryBlueDark,
    onPrimaryContainer = Color.White,

    secondary = SecondaryAmberLight,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryAmberDark,
    onSecondaryContainer = Color.White,

    tertiary = SafeguardGreenLight,
    onTertiary = Color.Black,
    tertiaryContainer = SafeguardGreenDark,
    onTertiaryContainer = Color.White,

    error = PanicRedLight,
    onError = OnErrorDark,
    errorContainer = PanicRedDark,
    onErrorContainer = Color.White,

    background = BackgroundDark,
    onBackground = OnBackgroundDark,

    surface = SurfaceDark,
    onSurface = OnSurfaceDark,

    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,

    outline = OutlineDark,
)

// ── Public theme composable ──────────────────────────────────────────────────

/**
 * RightGuard application theme.
 *
 * On Android 12+ (API 31) the theme picks up the user's Material You dynamic
 * palette so the app feels native. On older devices it falls back to the
 * hand-picked safety-oriented colour scheme defined in [Color.kt].
 *
 * @param darkTheme  Whether to use the dark variant. Defaults to system setting.
 * @param dynamicColor  Whether to use Material You dynamic colours on supported
 *                      devices. Defaults to `true`.
 * @param content  The composable content wrapped by this theme.
 */
@Composable
fun RightGuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = RightGuardTypography,
        content = content,
    )
}
