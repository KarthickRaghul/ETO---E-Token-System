package com.eto.manager.presentation.receptionist

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SupportAgent
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
import com.eto.manager.domain.model.PaymentStatus
import com.eto.manager.domain.model.Token
import com.eto.manager.domain.model.TokenStatus
import com.eto.manager.presentation.EtoViewModel
import com.eto.manager.presentation.components.EmptyState
import com.eto.manager.presentation.components.SectionHeader
import com.eto.manager.presentation.components.SpotlightCard
import com.eto.manager.presentation.components.bounceClick
import com.eto.manager.presentation.components.magnetEffect
import com.eto.manager.presentation.components.glassmorphicCard
import com.eto.manager.presentation.theme.*

@Composable
fun ReceptionistView(
    viewModel: EtoViewModel, 
    activeTab: Int, 
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
            0 -> { // HOME TAB: Online Requests & Walk-in Form
                var showWalkInForm by remember { mutableStateOf(false) }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (showWalkInForm) "Register Walk-in" else "Online Token Requests",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            )

                            // Quick Toggle Switch
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .clickable { showWalkInForm = !showWalkInForm }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (showWalkInForm) "Show Requests" else "Register Patient",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    if (showWalkInForm) {
                        item {
                            WalkInForm(doctors = doctors, onRegister = { name, phone, docId, symptoms ->
                                viewModel.registerWalkIn(name, phone, docId, symptoms)
                                showWalkInForm = false
                            })
                        }
                    } else {
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

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }

            1 -> { // QUEUE TAB: Active Doctor Queue progressions
                val activeQueues = doctors.filter { doc ->
                    tokens.any { it.doctorId == doc.id && (it.status == TokenStatus.APPROVED || it.status == TokenStatus.SERVING) }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Clinic Queue Manager",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                    }

                    if (activeQueues.isEmpty()) {
                        item {
                            EmptyState(message = "All doctor queues are currently empty.")
                        }
                    } else {
                        items(activeQueues) { doc ->
                            val docQueue = tokens.filter { it.doctorId == doc.id && (it.status == TokenStatus.APPROVED || it.status == TokenStatus.SERVING) }
                            QueueControlCard(
                                doctor = doc,
                                queue = docQueue,
                                onCallNext = { viewModel.callNextPatient(doc.id) },
                                onSkip = { viewModel.skipPatient(it) }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }

            2 -> { // HISTORY TAB: Billing & Invoices
                val pendingBills = tokens.filter { it.status == TokenStatus.COMPLETED && it.paymentStatus == PaymentStatus.PENDING }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Pending Invoices",
                            style = MaterialTheme.typography.headlineMedium.copy(
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
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }

            3 -> { // PROFILE TAB: Shift Stats
                val completedToday = tokens.filter { it.status == TokenStatus.COMPLETED }
                val totalPayments = completedToday.filter { it.paymentStatus == PaymentStatus.PAID }.sumOf { it.billAmount }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Front Desk Shift Stats",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                    }

                    item {
                        SpotlightCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 28.dp
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SupportAgent,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        "Reception Desk #1",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "Shift: Morning (08:00 AM - 04:00 PM)",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .glassmorphicCard(isDark, cornerRadius = 16.dp)
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Text("Registered Today", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("${tokens.size} Patients", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .glassmorphicCard(isDark, cornerRadius = 16.dp)
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Text("Collected Today", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("₹$totalPayments", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
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
fun QueueControlCard(
    doctor: Doctor,
    queue: List<Token>,
    onCallNext: () -> Unit,
    onSkip: (Token) -> Unit
) {
    val serving = queue.find { it.status == TokenStatus.SERVING }
    val pending = queue.filter { it.status == TokenStatus.APPROVED }

    val isDark = MaterialTheme.colorScheme.background == DarkBgStart

    SpotlightCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(doctor.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("${doctor.specialty} • Active Queue", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Button(
                onClick = onCallNext,
                enabled = pending.isNotEmpty() || serving != null,
                modifier = Modifier
                    .bounceClick()
                    .magnetEffect(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Call Next", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Now Serving", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = serving?.let { "${it.patientName} (${it.tokenNumber})" } ?: "Idle (waiting)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (serving != null) {
                            if (isDark) DarkSuccessText else LightSuccessText
                        } else MaterialTheme.colorScheme.onSurface
                    )
                }
                if (serving != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDark) DarkSuccessBg else LightSuccessBg)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Active", fontSize = 10.sp, color = if (isDark) DarkSuccessText else LightSuccessText, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (pending.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Waiting List (${pending.size} patients)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(6.dp))
            pending.forEach { tk ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("${tk.patientName} (${tk.tokenNumber})", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("Symptoms: ${tk.symptoms}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(
                        onClick = { onSkip(tk) },
                        modifier = Modifier.bounceClick()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close, 
                            contentDescription = "Skip", 
                            tint = if (isDark) DarkErrorText else LightErrorText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
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
