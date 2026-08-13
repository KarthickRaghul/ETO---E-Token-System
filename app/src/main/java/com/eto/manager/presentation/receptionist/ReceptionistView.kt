package com.eto.manager.presentation.receptionist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.RowScope
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
import com.eto.manager.presentation.components.PatientDetailsView
import com.eto.manager.presentation.components.etoBackground
import com.eto.manager.presentation.components.GlassButton
import com.eto.manager.presentation.UserRole
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

    var selectedTokenForDetails by remember { mutableStateOf<Token?>(null) }
    var selectedBillForDetails by remember { mutableStateOf<Token?>(null) }
    var billFilter by remember { mutableStateOf("All") }

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        if (selectedTokenForDetails != null) {
            PatientDetailsView(
                token = selectedTokenForDetails!!,
                role = UserRole.RECEPTIONIST,
                viewModel = viewModel,
                onBack = { selectedTokenForDetails = null }
            )
        } else if (selectedBillForDetails != null) {
            BillDetailsOverlay(
                token = selectedBillForDetails!!,
                viewModel = viewModel,
                isDark = isDark,
                onBack = { selectedBillForDetails = null }
            )
        } else {
            Box(
                modifier = Modifier
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
                                    ReceptionistQueuePatientCard(
                                        token = token,
                                        isDark = isDark,
                                        onClick = { selectedTokenForDetails = token }
                                    )
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
                                    MinimalPatientCard(
                                        token = req,
                                        isDark = isDark,
                                        onClick = { selectedTokenForDetails = req }
                                    )
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(90.dp))
                            }
                        }
                    }

                    2 -> { // BILLS TAB: Refined statistics and filters
                        val completedTokens = tokens.filter { it.status == TokenStatus.COMPLETED }
                        val todayRevenue = completedTokens.sumOf { it.billAmount }
                        val pendingCount = completedTokens.count { it.paymentStatus == PaymentStatus.PENDING }

                        val filteredBills = when (billFilter) {
                            "Paid" -> completedTokens.filter { it.paymentStatus == PaymentStatus.PAID }
                            "Pending" -> completedTokens.filter { it.paymentStatus == PaymentStatus.PENDING }
                            "Cancelled" -> tokens.filter { it.status == TokenStatus.SKIPPED }
                            else -> completedTokens + tokens.filter { it.status == TokenStatus.SKIPPED }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Billing & Invoices",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                )
                            }

                            // 1. Stats Row
                            item {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .glassmorphicCard(isDark, cornerRadius = 20.dp)
                                            .padding(14.dp)
                                    ) {
                                        Column {
                                            Text(
                                                "Today's Revenue",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                fontWeight = FontWeight.Medium
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                "₹${todayRevenue.toInt()}",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF10B981)
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .glassmorphicCard(isDark, cornerRadius = 20.dp)
                                            .padding(14.dp)
                                    ) {
                                        Column {
                                            Text(
                                                "Pending Bills",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                fontWeight = FontWeight.Medium
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                "$pendingCount",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFF59E0B)
                                            )
                                        }
                                    }
                                }
                            }

                            // 2. Filter Chips
                            item {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    listOf("All", "Paid", "Pending", "Cancelled").forEach { filter ->
                                        val isSelected = billFilter == filter
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .clickable { billFilter = filter }
                                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = filter,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // 3. Bills List
                            if (filteredBills.isEmpty()) {
                                item {
                                    EmptyState(message = "No invoices found for this filter.")
                                }
                            } else {
                                items(filteredBills) { bill ->
                                    MinimalBillCard(
                                        token = bill,
                                        isDark = isDark,
                                        onClick = { selectedBillForDetails = bill }
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
                    text = "${token.departmentName} • ${token.doctorName}",
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
fun MinimalPatientCard(
    token: Token,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val avatarBg = Color(0xFFEFF6FF)
    val avatarText = Color(0xFF2563EB)

    val initials = remember(token.patientName) {
        val parts = token.patientName.trim().split(" ")
        if (parts.size >= 2) {
            "${parts[0].firstOrNull() ?: 'P'}${parts[1].firstOrNull() ?: 'T'}"
        } else {
            token.patientName.take(2).uppercase()
        }
    }

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

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = token.patientName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Assign Doctor: ${token.doctorName}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFEF3C7))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    "Request",
                    fontSize = 10.sp,
                    color = Color(0xFFD97706),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun MinimalBillCard(
    token: Token,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val isPaid = token.paymentStatus == PaymentStatus.PAID
    val isCancelled = token.status == TokenStatus.SKIPPED
    val initials = remember(token.patientName) {
        val parts = token.patientName.trim().split(" ")
        if (parts.size >= 2) {
            "${parts[0].firstOrNull() ?: 'P'}${parts[1].firstOrNull() ?: 'T'}"
        } else {
            token.patientName.take(2).uppercase()
        }
    }

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
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEFF6FF)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2563EB)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = token.patientName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "INV-ETO-${token.id} • ${token.doctorName}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${token.billAmount.toInt()}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isCancelled) Color(0xFFF3F4F6)
                            else if (isPaid) Color(0xFFEBFDF5)
                            else Color(0xFFFEF2F2)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isCancelled) "CANCELLED" else if (isPaid) "PAID" else "PENDING",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCancelled) Color(0xFF6B7280) else if (isPaid) Color(0xFF10B981) else Color(0xFFEF4444)
                    )
                }
            }
        }
    }
}

@Composable
fun BillDetailsOverlay(
    token: Token,
    viewModel: EtoViewModel,
    isDark: Boolean,
    onBack: () -> Unit
) {
    var selectedPaymentMethod by remember { mutableStateOf("Cash") }
    val baseAmount = token.billAmount
    val facilityCharge = 100.0
    val gst = (baseAmount + facilityCharge) * 0.18
    val totalAmount = baseAmount + facilityCharge + gst

    Box(
        modifier = Modifier
            .fillMaxSize()
            .etoBackground(isDark)
            .clickable(enabled = false) {}
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0x33FFFFFF) else Color.White)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                }
                Text("Invoice Details", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (token.status == TokenStatus.SKIPPED) Color(0xFFF3F4F6)
                            else if (token.paymentStatus == PaymentStatus.PAID) Color(0xFFEBFDF5)
                            else Color(0xFFFEF2F2)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (token.status == TokenStatus.SKIPPED) "CANCELLED" else if (token.paymentStatus == PaymentStatus.PAID) "PAID" else "PENDING",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (token.status == TokenStatus.SKIPPED) Color(0xFF6B7280) else if (token.paymentStatus == PaymentStatus.PAID) Color(0xFF10B981) else Color(0xFFEF4444)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Invoice Details Card
                item {
                    SpotlightCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
                        Column(modifier = Modifier.padding(4.dp)) {
                            Text("Invoice INV-ETO-${token.id}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Patient Name:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(token.patientName, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Patient Phone:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(token.patientPhone, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Doctor Consulted:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(token.doctorName, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Billing Items Card
                item {
                    SpotlightCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
                        Column(modifier = Modifier.padding(4.dp)) {
                            Text("Charges Breakdown", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Consultation Fee", fontSize = 13.sp)
                                Text("₹${baseAmount.toInt()}.00", fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Facility Charges", fontSize = 13.sp)
                                Text("₹${facilityCharge.toInt()}.00", fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("GST (18%)", fontSize = 13.sp)
                                Text("₹${gst.toInt()}.00", fontSize = 13.sp)
                            }
                            Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Total Amount", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text("₹${totalAmount.toInt()}.00", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                // Payment Method Selector
                if (token.paymentStatus == PaymentStatus.PENDING && token.status != TokenStatus.SKIPPED) {
                    item {
                        SpotlightCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
                            Column(modifier = Modifier.padding(4.dp)) {
                                Text("Select Payment Method", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    listOf("Cash", "UPI", "Card").forEach { method ->
                                        val isSelected = selectedPaymentMethod == method
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .clickable { selectedPaymentMethod = method }
                                                .padding(vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(method, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
            ) {
                if (token.paymentStatus == PaymentStatus.PENDING && token.status != TokenStatus.SKIPPED) {
                    com.eto.manager.presentation.components.GlassButton(
                        onClick = {
                            viewModel.recordPayment(token)
                            onBack()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Collect Payment ($selectedPaymentMethod)", fontWeight = FontWeight.Bold)
                    }
                } else {
                    com.eto.manager.presentation.components.GlassButton(
                        onClick = { /* Print/Download PDF */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download Invoice PDF", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
