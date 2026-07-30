package com.eto.manager.presentation.doctor

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.eto.manager.presentation.components.StatusBadge
import com.eto.manager.presentation.components.bounceClick
import com.eto.manager.presentation.components.glassmorphicCard
import com.eto.manager.presentation.components.magnetEffect
import com.eto.manager.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorView(
    viewModel: EtoViewModel, 
    activeTab: Int, 
    modifier: Modifier = Modifier
) {
    val doctors by viewModel.doctors.collectAsState()
    val tokens by viewModel.tokens.collectAsState()
    val activeDocId by viewModel.selectedDoctorForView.collectAsState()

    val currentDoctor = doctors.find { it.id == activeDocId }
    val doctorQueue = tokens.filter { it.doctorId == activeDocId }
    val servingToken = doctorQueue.find { it.status == TokenStatus.SERVING }
    val pendingTokens = doctorQueue.filter { it.status == TokenStatus.APPROVED }
    val completedTokens = doctorQueue.filter { it.status == TokenStatus.COMPLETED }

    var expandedMenu by remember { mutableStateOf(false) }
    val isDark = MaterialTheme.colorScheme.background == DarkBgStart

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(8.dp))
            // Doctor selector dropdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Doctor Account:", 
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
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
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expandedMenu,
                        onDismissRequest = { expandedMenu = false }
                    ) {
                        doctors.forEach { doc ->
                            DropdownMenuItem(
                                text = { Text(doc.name, fontSize = 13.sp) },
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
                when (activeTab) {
                    0 -> { // HOME TAB: Consultation Workspace
                        if (servingToken != null) {
                            item {
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
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp), 
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "No patient is currently in your consulting room.", 
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    1 -> { // QUEUE TAB: Waiting list
                        if (pendingTokens.isEmpty()) {
                            item {
                                EmptyState(message = "No patients currently waiting in your queue.")
                            }
                        } else {
                            items(pendingTokens) { tk ->
                                SpotlightCard(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column {
                                            Text(tk.patientName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text("Token: ${tk.tokenNumber} • Symptoms: ${tk.symptoms}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isDark) DarkWarningBg else LightWarningBg
                                                )
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                "Waiting", 
                                                fontSize = 10.sp, 
                                                color = if (isDark) DarkWarningText else LightWarningText, 
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> { // HISTORY TAB: Completed Consultations today
                        if (completedTokens.isEmpty()) {
                            item {
                                EmptyState(message = "No patients consulted today yet.")
                            }
                        } else {
                            items(completedTokens) { tk ->
                                SpotlightCard(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column {
                                            Text(tk.patientName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text("Diagnosis: ${tk.diagnosis ?: "N/A"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("Prescription: ${tk.prescription ?: "N/A"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                                        }
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle, 
                                            contentDescription = null, 
                                            tint = SuccessGreen,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    3 -> { // PROFILE TAB: Professional Details
                        if (currentDoctor != null) {
                            item {
                                SpotlightCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    cornerRadius = 28.dp
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocalHospital,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(
                                                currentDoctor.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                "${currentDoctor.specialty} • ${currentDoctor.departmentName}",
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Avg Service", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text("${currentDoctor.averageServiceTimeMinutes} mins", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        }

                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Rating", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("${currentDoctor.rating}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Status", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = if (currentDoctor.isAvailable) "Available Today" else "Off Duty",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (currentDoctor.isAvailable) SuccessGreen else ErrorRed
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
fun ConsultationWorkspace(token: Token, onComplete: (String, String, Double) -> Unit) {
    var diagnosis by remember { mutableStateOf("") }
    var prescription by remember { mutableStateOf("") }
    var billFee by remember { mutableStateOf("500") }

    SpotlightCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(token.patientName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("Token: ${token.tokenNumber} • Chief Complaint", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("ACTIVE CONSULT", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
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
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )
        )
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = prescription,
            onValueChange = { prescription = it },
            label = { Text("Prescribed Medication / Treatment") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )
        )
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = billFee,
            onValueChange = { billFee = it },
            label = { Text("Consultation Fee (INR)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )
        )
        Spacer(modifier = Modifier.height(20.dp))

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
            enabled = diagnosis.isNotBlank() && prescription.isNotBlank() && billFee.toDoubleOrNull() != null && (billFee.toDoubleOrNull() ?: 0.0) >= 0.0,
            modifier = Modifier
                .fillMaxWidth()
                .bounceClick()
                .magnetEffect(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Finalize & Complete Consultation", fontWeight = FontWeight.Bold)
        }
    }
}
