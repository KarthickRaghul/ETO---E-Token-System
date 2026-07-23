package com.eto.manager.presentation.receptionist

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.eto.manager.domain.model.PaymentStatus
import com.eto.manager.domain.model.Token
import com.eto.manager.domain.model.TokenStatus
import com.eto.manager.presentation.EtoViewModel
import com.eto.manager.presentation.components.EmptyState
import com.eto.manager.presentation.components.SectionHeader
import com.eto.manager.presentation.components.SpotlightCard
import com.eto.manager.presentation.components.bounceClick
import com.eto.manager.presentation.components.magnetEffect
import com.eto.manager.presentation.theme.ErrorRed
import com.eto.manager.presentation.theme.SuccessGreen
import com.eto.manager.presentation.theme.WarningOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceptionistView(viewModel: EtoViewModel, modifier: Modifier = Modifier) {
    val doctors by viewModel.doctors.collectAsState()
    val tokens by viewModel.tokens.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Requests, 1: Walk-In, 2: Active Queues, 3: Billing

    Column(modifier = modifier.fillMaxSize()) {
        // Tab switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val tabs = listOf("Requests", "Walk-In", "Queues", "Billing")
            tabs.forEachIndexed { index, label ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (activeTab == index) MaterialTheme.colorScheme.primaryContainer
                            else Color.Transparent
                        )
                        .bounceClick()
                        .clickable { activeTab = index }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (activeTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (activeTab) {
                0 -> { // Requests
                    val requests = tokens.filter { it.status == TokenStatus.PENDING }
                    if (requests.isEmpty()) {
                        item {
                            EmptyState(message = "No pending online token requests.")
                        }
                    } else {
                        items(requests) { req ->
                            RequestCard(
                                token = req,
                                onApprove = { viewModel.approveToken(req) },
                                onReject = { viewModel.rejectToken(req) }
                            )
                        }
                    }
                }
                1 -> { // Walk-In Registration
                    item {
                        WalkInForm(doctors = doctors, onRegister = { name, phone, docId, symptoms ->
                            viewModel.registerWalkIn(name, phone, docId, symptoms)
                        })
                    }
                }
                2 -> { // Active Queues
                    val activeQueues = doctors.filter { doc ->
                        tokens.any { it.doctorId == doc.id && (it.status == TokenStatus.PENDING || it.status == TokenStatus.SERVING) }
                    }
                    if (activeQueues.isEmpty()) {
                        item {
                            EmptyState(message = "All doctor queues are currently empty.")
                        }
                    } else {
                        activeQueues.forEach { doc ->
                            val docQueue = tokens.filter { it.doctorId == doc.id && (it.status == TokenStatus.PENDING || it.status == TokenStatus.SERVING) }
                            item {
                                QueueControlCard(
                                    doctor = doc,
                                    queue = docQueue,
                                    onCallNext = { viewModel.callNextPatient(doc.id) },
                                    onSkip = { viewModel.skipPatient(it) }
                                )
                            }
                        }
                    }
                }
                3 -> { // Billing & Invoice Management
                    val pendingBills = tokens.filter { it.status == TokenStatus.COMPLETED && it.paymentStatus == PaymentStatus.PENDING }
                    if (pendingBills.isEmpty()) {
                        item {
                            EmptyState(message = "No pending consultation invoices.")
                        }
                    } else {
                        items(pendingBills) { bill ->
                            BillInvoiceCard(
                                token = bill,
                                onCollectPayment = { viewModel.recordPayment(bill) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RequestCard(token: Token, onApprove: () -> Unit, onReject: () -> Unit) {
    SpotlightCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(token.patientName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Phone: ${token.patientPhone}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(WarningOrange.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("Online Request", fontSize = 11.sp, color = WarningOrange, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text("Doctor: ${token.doctorName} (${token.departmentName})", fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Text("Symptoms: ${token.symptoms}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onReject,
                modifier = Modifier.bounceClick(),
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
            ) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reject")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onApprove,
                modifier = Modifier.bounceClick().magnetEffect(),
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Approve")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalkInForm(doctors: List<Doctor>, onRegister: (String, String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var symptoms by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var selectedDoctor by remember { mutableStateOf<Doctor?>(null) }

    SpotlightCard(modifier = Modifier.fillMaxWidth()) {
        Text("Create Walk-in Token", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Patient Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Patient Phone") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                readOnly = true,
                value = selectedDoctor?.name ?: "Select Doctor",
                onValueChange = {},
                label = { Text("Assign Doctor") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                doctors.forEach { doc ->
                    DropdownMenuItem(
                        text = { Text("${doc.name} (${doc.specialty})") },
                        onClick = {
                            selectedDoctor = doc
                            expanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = symptoms,
            onValueChange = { symptoms = it },
            label = { Text("Symptoms / Chief Complaint") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val docId = selectedDoctor?.id
                if (name.isNotEmpty() && phone.isNotEmpty() && docId != null) {
                    onRegister(name, phone, docId, symptoms)
                    name = ""
                    phone = ""
                    symptoms = ""
                    selectedDoctor = null
                }
            },
            modifier = Modifier.fillMaxWidth().bounceClick().magnetEffect(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Generate Walk-in Token")
        }
    }
}

@Composable
fun QueueControlCard(
    doctor: Doctor,
    queue: List<Token>,
    onCallNext: () -> Unit,
    onSkip: (Token) -> Unit
) {
    val serving = queue.find { it.status == TokenStatus.SERVING }
    val pending = queue.filter { it.status == TokenStatus.PENDING }

    SpotlightCard(modifier = Modifier.fillMaxWidth()) {
        Text(doctor.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text("${doctor.specialty} • Active Queue", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Now Consulting", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    serving?.let { "${it.patientName} (${it.tokenNumber})" } ?: "None (idle)",
                    fontWeight = FontWeight.Bold,
                    color = if (serving != null) SuccessGreen else MaterialTheme.colorScheme.onSurface
                )
            }

            Button(
                onClick = onCallNext,
                enabled = pending.isNotEmpty() || serving != null,
                modifier = Modifier.bounceClick().magnetEffect(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Call Next")
            }
        }

        if (pending.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text("Waiting list (${pending.size} patients)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            pending.forEach { tk ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("${tk.patientName} (${tk.tokenNumber})", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("Symptoms: ${tk.symptoms}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(
                        onClick = { onSkip(tk) },
                        modifier = Modifier.bounceClick()
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Skip", tint = ErrorRed)
                    }
                }
            }
        }
    }
}

@Composable
fun BillInvoiceCard(token: Token, onCollectPayment: () -> Unit) {
    SpotlightCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(token.patientName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Token: ${token.tokenNumber} • Doctor: ${token.doctorName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(ErrorRed.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("UNPAID", fontSize = 11.sp, color = ErrorRed, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Diagnosis: ${token.diagnosis}", fontSize = 13.sp)
        Text("Prescription: ${token.prescription}", fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Total Fee: ₹${token.billAmount}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Button(
                onClick = onCollectPayment,
                modifier = Modifier.bounceClick().magnetEffect(),
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
            ) {
                Icon(Icons.Default.Payment, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Record Cash Paid")
            }
        }
    }
}
