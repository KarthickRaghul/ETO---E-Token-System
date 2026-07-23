package com.eto.manager.presentation.doctor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eto.manager.domain.model.Doctor
import com.eto.manager.domain.model.Token
import com.eto.manager.domain.model.TokenStatus
import com.eto.manager.presentation.EtoViewModel
import com.eto.manager.presentation.components.EmptyState
import com.eto.manager.presentation.components.SectionHeader
import com.eto.manager.presentation.components.SpotlightCard
import com.eto.manager.presentation.components.bounceClick
import com.eto.manager.presentation.components.magnetEffect
import com.eto.manager.presentation.theme.SuccessGreen
import com.eto.manager.presentation.theme.WarningOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorView(viewModel: EtoViewModel, modifier: Modifier = Modifier) {
    val doctors by viewModel.doctors.collectAsState()
    val tokens by viewModel.tokens.collectAsState()
    val activeDocId by viewModel.selectedDoctorForView.collectAsState()

    val currentDoctor = doctors.find { it.id == activeDocId }
    val doctorQueue = tokens.filter { it.doctorId == activeDocId }
    val servingToken = doctorQueue.find { it.status == TokenStatus.SERVING }
    val pendingTokens = doctorQueue.filter { it.status == TokenStatus.PENDING }
    val completedTokens = doctorQueue.filter { it.status == TokenStatus.COMPLETED }

    var expandedMenu by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        // Doctor Selector dropdown
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Doctor Profile:", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            ExposedDropdownMenuBox(
                expanded = expandedMenu,
                onExpandedChange = { expandedMenu = !expandedMenu }
            ) {
                OutlinedTextField(
                    readOnly = true,
                    value = currentDoctor?.name ?: "Select Doctor",
                    onValueChange = {},
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMenu) },
                    modifier = Modifier.menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = expandedMenu,
                    onDismissRequest = { expandedMenu = false }
                ) {
                    doctors.forEach { doc ->
                        DropdownMenuItem(
                            text = { Text(doc.name) },
                            onClick = {
                                viewModel.selectDoctorForView(doc.id)
                                expandedMenu = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1.0f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Consultation workspace for serving patient
            if (servingToken != null) {
                item {
                    SectionHeader("Active Consultation Workspace")
                    ConsultationWorkspace(
                        token = servingToken,
                        onComplete = { diagnosis, prescription, fee ->
                            viewModel.completeConsultation(servingToken, diagnosis, prescription, fee)
                        }
                    )
                }
            } else {
                item {
                    SpotlightCard(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("No patient is currently in the consulting room.", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // Waiting Patients List
            item {
                SectionHeader("Waiting Queue (${pendingTokens.size} patients)")
            }
            if (pendingTokens.isEmpty()) {
                item {
                    EmptyState(message = "No patients waiting in queue.")
                }
            } else {
                items(pendingTokens) { tk ->
                    SpotlightCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(tk.patientName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Token: ${tk.tokenNumber} • Symptoms: ${tk.symptoms}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(WarningOrange.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Waiting", fontSize = 10.sp, color = WarningOrange, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Completed Patients List
            if (completedTokens.isNotEmpty()) {
                item {
                    SectionHeader("Completed Consultations Today")
                }
                items(completedTokens) { tk ->
                    SpotlightCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(tk.patientName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Diagnosis: ${tk.diagnosis ?: "N/A"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConsultationWorkspace(token: Token, onComplete: (String, String, Double) -> Unit) {
    var diagnosis by remember { mutableStateOf("") }
    var prescription by remember { mutableStateOf("") }
    var billFee by remember { mutableStateOf("500") } // default fee

    SpotlightCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(token.patientName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Token: ${token.tokenNumber} • Chief Complaint", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("ACTIVE CONSULT", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "\"${token.symptoms}\"",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = diagnosis,
            onValueChange = { diagnosis = it },
            label = { Text("Diagnosis Notes") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = prescription,
            onValueChange = { prescription = it },
            label = { Text("Prescribed Medication / Treatment") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = billFee,
            onValueChange = { billFee = it },
            label = { Text("Consultation Fee (INR)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val feeValue = billFee.toDoubleOrNull() ?: 500.0
                if (diagnosis.isNotEmpty() && prescription.isNotEmpty()) {
                    onComplete(diagnosis, prescription, feeValue)
                    diagnosis = ""
                    prescription = ""
                    billFee = "500"
                }
            },
            modifier = Modifier.fillMaxWidth().bounceClick().magnetEffect(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Finalize & Complete Consultation")
        }
    }
}
