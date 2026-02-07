package com.rightguard.app.ui.safeguard

import android.speech.tts.TextToSpeech
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rightguard.app.domain.model.EncounterType
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafeguardScreen(
    onEndEncounter: (Long) -> Unit,
    onPanic: () -> Unit,
    viewModel: SafeguardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Show errors via Snackbar
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    // Initialize TTS
    DisposableEffect(Unit) {
        val tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                viewModel.tts?.language = Locale.US
            }
        }
        viewModel.tts = tts
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    // Auto-activate on first composition
    LaunchedEffect(Unit) {
        if (!state.isActive) {
            viewModel.activate()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.FiberManualRecord,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            " SAFEGUARD ACTIVE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2E7D32),
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = onPanic) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = "Panic",
                            tint = Color.Red
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Encounter Type Selector
            if (!state.isActive) {
                EncounterTypeSelector(
                    selected = state.encounterType,
                    onSelect = { viewModel.setEncounterType(it) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Camera Preview Area (placeholder)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color.Black, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.FiberManualRecord,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(24.dp)
                    )
                    Text("Camera Recording", color = Color.White, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // AI Guidance Overlay
            AiGuidanceOverlay(state = state)

            Spacer(modifier = Modifier.weight(1f))

            // Voice Input Bar
            VoiceInputBar(
                transcription = state.currentTranscription,
                isListening = state.isListening,
                isLoadingAdvice = state.isLoadingAdvice,
                onSendText = { viewModel.queryAiAdvice(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // End Encounter Button
            Button(
                onClick = {
                    val id = viewModel.endEncounter()
                    onEndEncounter(id)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF424242)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Stop, contentDescription = null)
                Text("  END ENCOUNTER", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AiGuidanceOverlay(state: SafeguardUiState) {
    val scrollState = rememberScrollState()

    AnimatedVisibility(visible = state.aiAdvice != null || state.isLoadingAdvice) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A237E).copy(alpha = 0.9f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (state.isLoadingAdvice) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else {
                state.aiAdvice?.let { advice ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(scrollState)
                    ) {
                        // Your Rights
                        if (advice.rights.isNotEmpty()) {
                            Text(
                                "YOUR RIGHTS",
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            advice.rights.forEach { right ->
                                Text("  $right", color = Color.White, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Say This
                        Text(
                            "SAY THIS",
                            color = Color(0xFFFFB300),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            advice.advice,
                            color = Color.White,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // De-escalation
                        if (advice.deEscalationTips.isNotEmpty()) {
                            Text(
                                "DO NOT DO",
                                color = Color(0xFFEF5350),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            advice.deEscalationTips.forEach { tip ->
                                Text("  $tip", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceInputBar(
    transcription: String,
    isListening: Boolean,
    isLoadingAdvice: Boolean,
    onSendText: (String) -> Unit
) {
    var manualInput by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mic indicator
            Icon(
                if (isListening) Icons.Filled.Mic else Icons.Filled.MicOff,
                contentDescription = "Microphone",
                tint = if (isListening) Color(0xFF4CAF50) else Color.Gray,
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        if (isListening) Color(0xFF4CAF50).copy(alpha = 0.2f) else Color.Transparent,
                        CircleShape
                    )
                    .padding(4.dp)
            )

            // Transcription / Manual input
            OutlinedTextField(
                value = if (transcription.isNotBlank()) transcription else manualInput,
                onValueChange = { manualInput = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                placeholder = {
                    Text(
                        if (isListening) "Listening..." else "Describe your situation...",
                        fontSize = 13.sp
                    )
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall
            )

            // Send button
            Button(
                onClick = {
                    val text = if (transcription.isNotBlank()) transcription else manualInput
                    if (text.isNotBlank()) {
                        onSendText(text)
                        manualInput = ""
                    }
                },
                enabled = !isLoadingAdvice,
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                contentPadding = ButtonDefaults.ContentPadding
            ) {
                Text("GO", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncounterTypeSelector(
    selected: EncounterType,
    onSelect: (EncounterType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected.name.replace("_", " "),
            onValueChange = {},
            readOnly = true,
            label = { Text("Encounter Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            EncounterType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.name.replace("_", " ")) },
                    onClick = {
                        onSelect(type)
                        expanded = false
                    }
                )
            }
        }
    }
}
