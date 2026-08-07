package com.eto.manager.presentation.receptionist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.eto.manager.presentation.components.SpotlightCard
import com.eto.manager.presentation.components.bounceClick
import com.eto.manager.presentation.components.magnetEffect
import com.eto.manager.presentation.components.glassmorphicCard
import com.eto.manager.presentation.theme.*

@Composable
fun ReceptionistView(
    viewModel: EtoViewModel,
    activeTab: Int,
    showWalkInDialog: Boolean,
    onDismissWalkIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val doctors by viewModel.doctors.collectAsState()
    val tokens by viewModel.tokens.collectAsState()

    val isDark = MaterialTheme.colorScheme.background == DarkBgStart

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        when (activeTab) {
            0 -> { // QUEUE TAB: Flat list of all active queue patients matching screenshot
                val activeQueue = tokens.filter { it.status == TokenStatus.APPROVED || it.status == TokenStatus.SERVING }
                    .sortedBy { it.id }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Current Queue",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                    }

                    if (activeQueue.isEmpty()) {
                        item {
                            EmptyState(message = "No patients in queue currently.")
                        }
                    } else {
                        items(activeQueue) { token ->
                            var showActionsDialog by remember { mutableStateOf(false) }

                            ReceptionistQueuePatientCard(
                                token = token,
                                isDark = isDark,
                                onClick = { showActionsDialog = true }
                            )

                            if (showActionsDialog) {
                                QueueActionDialog(
                                    token = token,
                                    onDismiss = { showActionsDialog = false },
                                    onCallNext = {
                                        viewModel.callNextPatient(token.doctorId)
                                        showActionsDialog = false
                                    },
                                    onSkip = {
                                        viewModel.skipPatient(token)
                                        showActionsDialog = false
                                    }
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(90.dp))
                    }
                }
            }

            1 -> { // REQUESTS TAB: Online Requests
                val requests = tokens.filter { it.status == TokenStatus.PENDING }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Online Token Requests",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                    }

                    if (requests.isEmpty()) {
                        item {
                            EmptyState(message = "No pending online requests.")
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

                    item {
                        Spacer(modifier = Modifier.height(90.dp))
                    }
                }
            }

            2 -> { // BILLS TAB: Pending consultation invoices
                val pendingBills = tokens.filter { it.status == TokenStatus.COMPLETED && it.paymentStatus == PaymentStatus.PENDING }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Pending Invoices",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                    }

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

                    item {
                        Spacer(modifier = Modifier.height(90.dp))
                    }
                }
            }
        }

        // Walk-in Registration form Dialog triggered by FAB
        if (showWalkInDialog) {
            AlertDialog(
                onDismissRequest = onDismissWalkIn,
                title = null,
                text = {
                    WalkInForm(
                        doctors = doctors,
                        onRegister = { name, phone, docId, symptoms ->
                            viewModel.registerWalkIn(name, phone, docId, symptoms)
                            onDismissWalkIn()
                        }
                    )
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = onDismissWalkIn) {
                        Text("Cancel", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

@Composable
fun ReceptionistQueuePatientCard(
    token: Token,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val colorIndex = remember(token.id) { token.id.toInt().coerceAtLeast(0) }
    val colorPairs = listOf(
        Pair(Color(0xFFEFF6FF), Color(0xFF2563EB)), // Blue
        Pair(Color(0xFFECFDF5), Color(0xFF059669)), // Green
        Pair(Color(0xFFF5F3FF), Color(0xFF7C3AED)), // Purple
        Pair(Color(0xFFFEF3C7), Color(0xFFD97706)), // Orange
        Pair(Color(0xFFFDF2F8), Color(0xFFDB2777)), // Pink
        Pair(Color(0xFFF0FDFA), Color(0xFF0D9488))  // Teal
    )
    val colorPair = colorPairs[colorIndex % colorPairs.size]
    val avatarBg = colorPair.first
    val avatarText = colorPair.second

    val initials = remember(token.patientName) {
        val parts = token.patientName.trim().split(" ")
        if (parts.size >= 2) {
            "${parts[0].firstOrNull() ?: 'P'}${parts[1].firstOrNull() ?: 'T'}"
        } else {
            token.patientName.take(2).uppercase()
        }
    }

    val simulatedAge = remember(token.id) { (token.id % 40 + 20).toInt() }
    val simulatedGender = remember(token.id) { if (token.id % 2L == 0L) "Male" else "Female" }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (isDark) Color(0xFF1E293B).copy(alpha = 0.6f) else Color.White)
            .border(
                1.dp,
                if (isDark) Color(0xFF334155).copy(alpha = 0.4f) else Color(0xFFEFF6FF),
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(avatarBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = avatarText
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Patient Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = token.patientName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$simulatedAge • $simulatedGender",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = token.departmentName,
                    fontSize = 12.sp,
                    color = avatarText,
                    fontWeight = FontWeight.Medium
                )
            }

            // Right side: Token Number
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = token.tokenNumber,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2563EB)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (token.status == TokenStatus.SERVING) "Serving" else "In Queue",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }

        // Top right status indicator dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(avatarText)
                .align(Alignment.TopEnd)
        )
    }
}

@Composable
fun QueueActionDialog(
    token: Token,
    onDismiss: () -> Unit,
    onCallNext: () -> Unit,
    onSkip: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Manage ${token.patientName}",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Text(
                text = "Perform operations for token ${token.tokenNumber} assigned to ${token.doctorName}.",
                fontSize = 14.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onCallNext,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Call Next", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onSkip) {
                Text("Skip Patient", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun RequestCard(token: Token, onApprove: () -> Unit, onReject: () -> Unit) {
    val isDark = MaterialTheme.colorScheme.background == DarkBgStart

    val warningBg = if (isDark) DarkWarningBg else LightWarningBg
    val warningText = if (isDark) DarkWarningText else LightWarningText

    SpotlightCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(token.patientName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("Phone: ${token.patientPhone}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(warningBg)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("Online Request", fontSize = 10.sp, color = warningText, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Assign Doctor: ${token.doctorName} (${token.departmentName})",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Symptoms: ${token.symptoms}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onReject,
                modifier = Modifier.bounceClick(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) DarkErrorBg else LightErrorBg,
                    contentColor = if (isDark) DarkErrorText else LightErrorText
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reject", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onApprove,
                modifier = Modifier
                    .bounceClick()
                    .magnetEffect(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) DarkSuccessBg else LightSuccessBg,
                    contentColor = if (isDark) DarkSuccessText else LightSuccessText
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Approve", fontWeight = FontWeight.Bold)
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
        Text("Create Walk-in Token", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Patient Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )
        )
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Patient Phone") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )
        )
        Spacer(modifier = Modifier.height(10.dp))

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
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                )
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                doctors.forEach { doc ->
                    DropdownMenuItem(
                        text = { Text("${doc.name} (${doc.specialty})", fontSize = 13.sp) },
                        onClick = {
                            selectedDoctor = doc
                            expanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = symptoms,
            onValueChange = { symptoms = it },
            label = { Text("Symptoms / Chief Complaint") },
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
                val docId = selectedDoctor?.id
                if (name.isNotEmpty() && phone.isNotEmpty() && docId != null) {
                    onRegister(name, phone, docId, symptoms)
                    name = ""
                    phone = ""
                    symptoms = ""
                    selectedDoctor = null
                }
            },
            enabled = name.isNotBlank() && phone.isNotBlank() && phone.all { it.isDigit() } && selectedDoctor != null && symptoms.isNotBlank(),
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
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Generate Walk-in Token", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun BillInvoiceCard(token: Token, onCollectPayment: () -> Unit) {
    val isDark = MaterialTheme.colorScheme.background == DarkBgStart

    SpotlightCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(token.patientName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("Token: ${token.tokenNumber} • Doctor: ${token.doctorName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDark) DarkErrorBg else LightErrorBg)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("UNPAID", fontSize = 10.sp, color = if (isDark) DarkErrorText else LightErrorText, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Diagnosis: ${token.diagnosis}",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Prescription: ${token.prescription}",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Total Fee: ₹${token.billAmount}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Button(
                onClick = onCollectPayment,
                modifier = Modifier
                    .bounceClick()
                    .magnetEffect(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) DarkSuccessBg else LightSuccessBg,
                    contentColor = if (isDark) DarkSuccessText else LightSuccessText
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Paid Cash", fontWeight = FontWeight.Bold)
            }
        }
    }
}
