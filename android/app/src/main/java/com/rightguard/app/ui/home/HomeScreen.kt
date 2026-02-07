package com.rightguard.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rightguard.app.domain.model.AppMode
import com.rightguard.app.ui.components.PanicButton
import com.rightguard.app.ui.components.SafeguardButton
import com.rightguard.app.ui.components.StatusBanner
import com.rightguard.app.ui.theme.PrimaryBlue
import com.rightguard.app.ui.theme.RightGuardTheme

/**
 * Home screen -- the primary dashboard of the RightGuard app.
 *
 * Layout (top to bottom):
 * 1. Top app bar with title + settings icon
 * 2. Greeting text with user name
 * 3. Status banner showing current mode
 * 4. Large circular "ACTIVATE SAFEGUARD" button
 * 5. Red hold-to-activate "PANIC" button
 * 6. Bottom row with "Incidents" history shortcut
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onActivateSafeguard: () -> Unit,
    onActivatePanic: () -> Unit,
    onOpenIncidents: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp),
                        )
                        Text(
                            text = "RightGuard",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings",
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Greeting
            AnimatedVisibility(
                visible = state.userName.isNotBlank(),
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Text(
                    text = "Hello, ${state.userName}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status banner
            StatusBanner(mode = state.currentMode)

            Spacer(modifier = Modifier.height(40.dp))

            // Central safeguard button
            SafeguardButton(
                onClick = {
                    viewModel.setMode(AppMode.SAFEGUARD)
                    onActivateSafeguard()
                },
                enabled = state.currentMode == AppMode.IDLE,
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Panic button
            PanicButton(
                onActivate = {
                    viewModel.setMode(AppMode.PANIC)
                    onActivatePanic()
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Hold for 3 seconds to activate",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Incidents shortcut
            FilledTonalButton(
                onClick = onOpenIncidents,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Filled.History,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(text = buildString {
                    append("Incidents")
                    if (state.incidentCount > 0) {
                        append(" (${state.incidentCount})")
                    }
                })
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    RightGuardTheme {
        // Preview without Hilt — supply UI directly
        HomeScreenContent(
            state = HomeUiState(
                currentMode = AppMode.IDLE,
                userName = "Alex",
                incidentCount = 3,
            ),
            onActivateSafeguard = {},
            onActivatePanic = {},
            onOpenIncidents = {},
            onOpenSettings = {},
            onSetMode = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomeScreenSafeguardPreview() {
    RightGuardTheme {
        HomeScreenContent(
            state = HomeUiState(
                currentMode = AppMode.SAFEGUARD,
                userName = "Alex",
                incidentCount = 3,
            ),
            onActivateSafeguard = {},
            onActivatePanic = {},
            onOpenIncidents = {},
            onOpenSettings = {},
            onSetMode = {},
        )
    }
}

/**
 * Stateless variant of the home screen used for Compose Previews (no Hilt
 * ViewModel required).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenContent(
    state: HomeUiState,
    onActivateSafeguard: () -> Unit,
    onActivatePanic: () -> Unit,
    onOpenIncidents: () -> Unit,
    onOpenSettings: () -> Unit,
    onSetMode: (AppMode) -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(28.dp),
                        )
                        Text(
                            text = "RightGuard",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings",
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            if (state.userName.isNotBlank()) {
                Text(
                    text = "Hello, ${state.userName}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            StatusBanner(mode = state.currentMode)

            Spacer(modifier = Modifier.height(40.dp))

            SafeguardButton(
                onClick = {
                    onSetMode(AppMode.SAFEGUARD)
                    onActivateSafeguard()
                },
                enabled = state.currentMode == AppMode.IDLE,
            )

            Spacer(modifier = Modifier.height(28.dp))

            PanicButton(
                onActivate = {
                    onSetMode(AppMode.PANIC)
                    onActivatePanic()
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Hold for 3 seconds to activate",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(40.dp))

            FilledTonalButton(
                onClick = onOpenIncidents,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Filled.History,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(text = buildString {
                    append("Incidents")
                    if (state.incidentCount > 0) {
                        append(" (${state.incidentCount})")
                    }
                })
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
