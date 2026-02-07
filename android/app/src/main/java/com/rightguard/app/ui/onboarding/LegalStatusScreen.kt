package com.rightguard.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rightguard.app.domain.model.CitizenshipStatus
import com.rightguard.app.domain.model.ImmigrationStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalStatusScreen(
    onNext: () -> Unit,
    onBack: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.state.collectAsState()
    var immigrationDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Legal Status") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Citizenship Status",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "This helps us provide rights guidance specific to your legal status. You may skip this.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(modifier = Modifier.selectableGroup()) {
                CitizenshipStatus.entries.forEach { status ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = uiState.citizenshipStatus == status,
                                onClick = { viewModel.updateCitizenshipStatus(status) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.citizenshipStatus == status,
                            onClick = null
                        )
                        Text(
                            text = formatCitizenshipStatus(status),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }

            if (uiState.citizenshipStatus != CitizenshipStatus.US_CITIZEN) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Immigration Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                ExposedDropdownMenuBox(
                    expanded = immigrationDropdownExpanded,
                    onExpandedChange = { immigrationDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = formatImmigrationStatus(uiState.immigrationStatus),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Immigration Status") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = immigrationDropdownExpanded)
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )

                    ExposedDropdownMenu(
                        expanded = immigrationDropdownExpanded,
                        onDismissRequest = { immigrationDropdownExpanded = false }
                    ) {
                        ImmigrationStatus.entries.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(formatImmigrationStatus(status)) },
                                onClick = {
                                    viewModel.updateImmigrationStatus(status)
                                    immigrationDropdownExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Privacy",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column {
                        Text(
                            text = "Your privacy is protected",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "All data stays on your device and is encrypted. " +
                                    "Nothing is sent to any server. You can change or " +
                                    "delete this information at any time in Settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f).height(52.dp)
                ) {
                    Text("Back")
                }

                Button(
                    onClick = onNext,
                    modifier = Modifier.weight(1f).height(52.dp)
                ) {
                    Text("Next")
                }
            }
        }
    }
}

private fun formatCitizenshipStatus(status: CitizenshipStatus): String {
    return when (status) {
        CitizenshipStatus.US_CITIZEN -> "U.S. Citizen"
        CitizenshipStatus.PERMANENT_RESIDENT -> "Permanent Resident"
        CitizenshipStatus.VISA_HOLDER -> "Visa Holder"
        CitizenshipStatus.UNDOCUMENTED -> "Undocumented"
        CitizenshipStatus.PREFER_NOT_TO_SAY -> "Prefer Not to Say"
    }
}

private fun formatImmigrationStatus(status: ImmigrationStatus): String {
    return when (status) {
        ImmigrationStatus.NOT_APPLICABLE -> "Not Applicable"
        ImmigrationStatus.VALID_VISA -> "Valid Visa"
        ImmigrationStatus.EXPIRED_VISA -> "Expired Visa"
        ImmigrationStatus.ASYLUM_SEEKER -> "Asylum Seeker"
        ImmigrationStatus.DACA -> "DACA"
        ImmigrationStatus.TPS -> "TPS"
        ImmigrationStatus.PREFER_NOT_TO_SAY -> "Prefer Not to Say"
    }
}
