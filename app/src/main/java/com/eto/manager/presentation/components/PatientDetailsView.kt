package com.eto.manager.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.eto.manager.domain.model.Token
import com.eto.manager.domain.model.TokenStatus
import com.eto.manager.domain.model.getDisplayQueueNumber
import com.eto.manager.presentation.EtoViewModel
import com.eto.manager.presentation.UserRole
import com.eto.manager.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailsView(
    token: Token,
    role: UserRole,
    viewModel: EtoViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background == DarkBgStart
    val allTokens by viewModel.tokens.collectAsState()

    // Tab state
    val tabs = listOf("Overview", "History", "Reports", "Prescriptions", "Notes")
    var selectedTab by remember { mutableStateOf(0) }

    // Dialog state for Consultation details (Doctor view)
    var showConsultDialog by remember { mutableStateOf(false) }

    // Demographics simulation helper
    val simulatedAge = remember(token.id) { (token.id % 40 + 20).toInt() }
    val simulatedGender = remember(token.id) { if (token.id % 2L == 0L) "Male" else "Female" }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .etoBackground(isDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 1. Top bar matching third image
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
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
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                Text(
                    text = "Patient Details",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = { /* More Options */ },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0x33FFFFFF) else Color.White)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More Options",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Scrollable Content
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 2. Profile Header Card
                item {
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
                            .glassmorphicCard(isDark, cornerRadius = 24.dp)
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar Box
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEFF6FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initials,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color(0xFF2563EB)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = token.patientName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$simulatedAge Years • $simulatedGender",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "ID: PT${String.format("%04d", token.id % 1000)} • Token #${token.getDisplayQueueNumber(allTokens)}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            StatusBadge(token.status)
                        }
                    }
                }

                // 3. Sub-info row (4 small cards)
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        InfoMiniCard("Blood Group", "B+", modifier = Modifier.weight(1f))
                        InfoMiniCard("Allergies", "Penicillin", modifier = Modifier.weight(1f))
                        InfoMiniCard("Phone", "+91 ${token.patientPhone}", modifier = Modifier.weight(1.3f))
                        InfoMiniCard("Last Visit", "12 May 2024", modifier = Modifier.weight(1.2f))
                    }
                }

                // 4. Reason for Visit Card
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassmorphicCard(isDark, cornerRadius = 20.dp)
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Reason for Visit",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = token.symptoms,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 5. Tab Navigation Row
                item {
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        edgePadding = 0.dp,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        divider = {},
                        indicator = { tabPositions ->
                            // Custom invisible indicator or thin line
                        }
                    ) {
                        tabs.forEachIndexed { index, title ->
                            val isSelected = selectedTab == index
                            Tab(
                                selected = isSelected,
                                onClick = { selectedTab = index },
                                text = {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                                else Color.Transparent
                                            )
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = title,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 13.sp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                // 6. Tab Content Switch
                when (selectedTab) {
                    0 -> { // Overview content
                        item {
                            DetailsSectionCard(
                                title = "Symptoms",
                                subtitle = "Since 2 days",
                                bulletPoints = listOf("Chest pain (central)", "Shortness of breath", "Mild fatigue")
                            )
                        }
                        item {
                            DetailsSectionCard(
                                title = "Medical History",
                                bulletPoints = listOf("Hypertension (Diagnosed 2021)", "No history of diabetes", "No previous surgeries")
                            )
                        }
                        item {
                            DetailsSectionCard(
                                title = "Current Medications",
                                itemsWithChevrons = listOf("Amlodipine 5mg - Once daily")
                            )
                        }
                        item {
                            // Vitals Card
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .glassmorphicCard(isDark, cornerRadius = 24.dp)
                                    .padding(16.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "Vitals (Today)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        VitalStatItem("BP", "128/82", "mmHg", modifier = Modifier.weight(1f))
                                        VitalStatItem("Pulse", "88", "bpm", modifier = Modifier.weight(1f))
                                        VitalStatItem("SPO2", "98", "%", modifier = Modifier.weight(1f))
                                        VitalStatItem("Temp", "98.4", "°F", modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                        item {
                            // Documents Card
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .glassmorphicCard(isDark, cornerRadius = 24.dp)
                                    .padding(16.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "Documents & Reports",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        DocumentRow("Blood Report", "12 May 2024", "PDF • 1.2 MB")
                                        DocumentRow("X-Ray Chest", "12 May 2024", "JPG • 1.5 MB")
                                        DocumentRow("ECG Report", "12 May 2024", "PDF • 1.1 MB")
                                        
                                        // dashed border button mock
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(44.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                                                .border(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .clickable { /* Upload */ },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CloudUpload,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Upload Document",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            // Consultation Review
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .glassmorphicCard(isDark, cornerRadius = 24.dp)
                                    .padding(16.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "Doctor's Review",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    if (token.status == TokenStatus.COMPLETED && token.diagnosis != null) {
                                        Text(
                                            text = "Diagnosis: ${token.diagnosis}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Prescription: ${token.prescription}",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Text(
                                            text = "No review added yet. Add diagnosis, notes and plan for this patient.",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (role == UserRole.DOCTOR) {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            GlassButton(
                                                onClick = { showConsultDialog = true },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Add Review & Diagnosis", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .glassmorphicCard(isDark, cornerRadius = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No additional data available.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(100.dp)) }
            }

            // 7. Contextual Bottom Bar Actions based on role
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                when (role) {
                    UserRole.DOCTOR -> {
                        // bottom doctor quick actions row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            GlassButton(
                                onClick = { showConsultDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Add Review", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            GlassButton(
                                onClick = { showConsultDialog = true },
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Text("Add Rx", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            GlassButton(
                                onClick = { /* Order Tests */ },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Order Tests", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            GlassButton(
                                onClick = { /* Follow-up */ },
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Text("Schedule", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    UserRole.RECEPTIONIST -> {
                        if (token.status == TokenStatus.PENDING) {
                            // Approve / Reject buttons for Online requests
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                GlassButton(
                                    onClick = {
                                        viewModel.rejectToken(token)
                                        onBack()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Reject Request", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                }
                                GlassButton(
                                    onClick = {
                                        viewModel.approveToken(token)
                                        onBack()
                                    },
                                    modifier = Modifier.weight(1.5f)
                                ) {
                                    Text("Approve Request", color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                }
                            }
                        } else if (token.status == TokenStatus.APPROVED || token.status == TokenStatus.SERVING) {
                            // Queue actions for receptionist
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                GlassButton(
                                    onClick = {
                                        viewModel.skipPatient(token)
                                        onBack()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Skip Patient", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                }
                                GlassButton(
                                    onClick = {
                                        viewModel.callNextPatient(token.doctorId)
                                        onBack()
                                    },
                                    modifier = Modifier.weight(1.5f)
                                ) {
                                    Text("Call Next Patient", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    else -> {}
                }
            }
        }

        // Consult Dialog overlay for doctor
        if (showConsultDialog) {
            var diagnosis by remember { mutableStateOf("") }
            var prescription by remember { mutableStateOf("") }
            var billFee by remember { mutableStateOf("500") }

            AlertDialog(
                onDismissRequest = { showConsultDialog = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface),
                content = {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Text(
                                "Add Consultation Review",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
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
                                label = { Text("Prescribed Medication") },
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
                                label = { Text("Consultation Fee (₹)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                )
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showConsultDialog = false },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text("Cancel")
                                }

                                Button(
                                    onClick = {
                                        val feeVal = billFee.toDoubleOrNull() ?: 500.0
                                        viewModel.completeConsultation(token, diagnosis, prescription, feeVal)
                                        showConsultDialog = false
                                    },
                                    enabled = diagnosis.isNotBlank() && prescription.isNotBlank() && billFee.toDoubleOrNull() != null,
                                    modifier = Modifier.weight(2f),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text("Finalize", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun InfoMiniCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background == DarkBgStart
    Box(
        modifier = modifier
            .glassmorphicCard(isDark, cornerRadius = 14.dp)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun DetailsSectionCard(
    title: String,
    subtitle: String? = null,
    bulletPoints: List<String> = emptyList(),
    itemsWithChevrons: List<String> = emptyList()
) {
    val isDark = MaterialTheme.colorScheme.background == DarkBgStart
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphicCard(isDark, cornerRadius = 24.dp)
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            
            if (bulletPoints.isNotEmpty()) {
                bulletPoints.forEach { point ->
                    Row(
                        modifier = Modifier.padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = point,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (itemsWithChevrons.isNotEmpty()) {
                itemsWithChevrons.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VitalStatItem(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background == DarkBgStart
    Box(
        modifier = modifier
            .glassmorphicCard(isDark, cornerRadius = 14.dp)
            .padding(10.dp)
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                text = label,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = unit,
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun DocumentRow(
    name: String,
    date: String,
    size: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFEFF6FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = null,
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$date • $size",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
        
        IconButton(onClick = { /* Download */ }) {
            Icon(
                imageVector = Icons.Default.FileDownload,
                contentDescription = "Download",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
