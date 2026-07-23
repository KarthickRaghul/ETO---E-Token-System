package com.eto.manager.presentation.patient

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eto.manager.domain.model.Doctor
import com.eto.manager.domain.model.Token
import com.eto.manager.domain.model.TokenStatus
import com.eto.manager.presentation.EtoViewModel
import com.eto.manager.presentation.components.SectionHeader
import com.eto.manager.presentation.components.StatusBadge
import com.eto.manager.presentation.components.ShinyText
import com.eto.manager.presentation.components.SpotlightCard
import com.eto.manager.presentation.components.bounceClick
import com.eto.manager.presentation.components.magnetEffect
import com.eto.manager.presentation.components.EmptyState
import com.eto.manager.presentation.components.shimmer
import com.eto.manager.presentation.theme.ErrorRed
import com.eto.manager.presentation.theme.SteelBlueMedium
import com.eto.manager.presentation.theme.SuccessGreen
import com.eto.manager.presentation.theme.WarningOrange

@Composable
fun PatientView(viewModel: EtoViewModel, modifier: Modifier = Modifier) {
    val name by viewModel.patientName.collectAsState()
    val phone by viewModel.patientPhone.collectAsState()
    val doctors by viewModel.doctors.collectAsState()
    val departments by viewModel.departments.collectAsState()
    val tokens by viewModel.tokens.collectAsState()
    val selectedDocId by viewModel.selectedDoctorId.collectAsState()
    val symptoms by viewModel.symptomsInput.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedDeptId by remember { mutableStateOf<String?>(null) }

    // Patient tokens
    val patientTokens = tokens.filter { it.patientPhone == phone }
    val activeToken = patientTokens.find { it.status == TokenStatus.PENDING || it.status == TokenStatus.SERVING }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Patient Profile info
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Patient Identity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { viewModel.patientName.value = it },
                        label = { Text("Full Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { viewModel.patientPhone.value = it },
                        label = { Text("Phone Number") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors()
                    )
                }
            }
        }

        // Live Queue Tracker
        if (activeToken != null) {
            // Find active queue of the same doctor to compute position ahead
            val doctorQueue = tokens.filter { it.doctorId == activeToken.doctorId && (it.status == TokenStatus.PENDING || it.status == TokenStatus.SERVING) }
            val servingToken = doctorQueue.find { it.status == TokenStatus.SERVING }
            
            // Patient index in pending queue
            val pendingQueue = doctorQueue.filter { it.status == TokenStatus.PENDING }.sortedBy { it.id }
            val positionIndex = pendingQueue.indexOfFirst { it.id == activeToken.id }
            val aheadCount = if (positionIndex >= 0) positionIndex + (if (servingToken != null) 1 else 0) else 0

            item {
                val transition = rememberInfiniteTransition(label = "halo")
                val haloScale1 by transition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.4f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "halo1"
                )
                val haloAlpha1 by transition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "halo1Alpha"
                )

                val haloScale2 by transition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.4f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, delayMillis = 1000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "halo2"
                )
                val haloAlpha2 by transition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, delayMillis = 1000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "halo2Alpha"
                )

                val progressTarget = if (aheadCount >= 0) {
                    (1f / (aheadCount + 1).toFloat()).coerceIn(0.15f, 1.0f)
                } else 1.0f

                val animatedProgress by animateFloatAsState(
                    targetValue = progressTarget,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessVeryLow),
                    label = "progress"
                )

                val sweepColor1 = MaterialTheme.colorScheme.secondary
                val sweepColor2 = MaterialTheme.colorScheme.primary
                val sweepBrush = remember(sweepColor1, sweepColor2) {
                    Brush.sweepGradient(
                        colors = listOf(sweepColor1, sweepColor2, sweepColor1)
                    )
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Live Token Tracking", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(activeToken.departmentName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                            }
                            StatusBadge(activeToken.status)
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(160.dp)
                                .align(Alignment.CenterHorizontally)
                        ) {
                            // Pulse Halo Rings
                            Canvas(modifier = Modifier.size(110.dp)) {
                                drawCircle(
                                    color = SteelBlueMedium,
                                    radius = size.width / 2 * haloScale1,
                                    alpha = haloAlpha1
                                )
                                drawCircle(
                                    color = SteelBlueMedium,
                                    radius = size.width / 2 * haloScale2,
                                    alpha = haloAlpha2
                                )
                            }

                            // Progress ring background and sweep progress
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                // Draw Track
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.15f),
                                    style = Stroke(width = 8.dp.toPx())
                                )
                                // Draw Animated Progress Arc with Sweep Gradient
                                drawArc(
                                    brush = sweepBrush,
                                    startAngle = -90f,
                                    sweepAngle = animatedProgress * 360f,
                                    useCenter = false,
                                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                            
                            // Core Card overlay inside ring
                            Box(
                                modifier = Modifier
                                    .size(116.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("TOKEN", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    ShinyText(
                                        text = activeToken.tokenNumber,
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            QueueDetailItem("Dr. Name", activeToken.doctorName)
                            QueueDetailItem("Serving", servingToken?.tokenNumber ?: "None")
                            QueueDetailItem("Ahead", "$aheadCount patients")
                            QueueDetailItem("Est. Wait", "${aheadCount * 15} mins")
                        }
                    }
                }
            }
        }

        // Search & Book Appointment (only if no active token to avoid double bookings in demo)
        if (activeToken == null) {
            item {
                SectionHeader("Select Department")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // All Category
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selectedDeptId == null) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface
                            )
                            .clickable { selectedDeptId = null }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            "All",
                            color = if (selectedDeptId == null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    departments.forEach { dept ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selectedDeptId == dept.id) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surface
                                )
                                .clickable { selectedDeptId = dept.id }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                dept.name,
                                color = if (selectedDeptId == dept.id) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            item {
                SectionHeader("Available Doctors")
            }

            val filteredDoctors = doctors.filter { doc ->
                val matchesSearch = doc.name.contains(searchQuery, ignoreCase = true) || doc.specialty.contains(searchQuery, ignoreCase = true)
                val matchesDept = selectedDeptId == null || doc.departmentId == selectedDeptId
                matchesSearch && matchesDept
            }

            if (doctors.isEmpty()) {
                items(3) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .shimmer()
                    )
                }
            } else if (filteredDoctors.isEmpty()) {
                item {
                    EmptyState(message = "No doctors available matching the criteria.")
                }
            } else {
                items(filteredDoctors) { doc ->
                    DoctorCard(
                        doctor = doc,
                        isSelected = selectedDocId == doc.id,
                        symptoms = symptoms,
                        onSelect = { viewModel.selectDoctor(doc.id) },
                        onCancel = { viewModel.selectDoctor(null) },
                        onSymptomsChange = { viewModel.symptomsInput.value = it },
                        onSubmit = { viewModel.requestToken() }
                    )
                }
            }
        }

        // Consultations & Prescription History
        val historyTokens = patientTokens.filter { it.status == TokenStatus.COMPLETED }
        if (historyTokens.isNotEmpty()) {
            item {
                SectionHeader("Medical & Consultation History")
            }
            items(historyTokens) { history ->
                HistoryCard(token = history)
            }
        }
    }
}

@Composable
fun QueueDetailItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
fun DoctorCard(
    doctor: Doctor,
    isSelected: Boolean,
    symptoms: String,
    onSelect: () -> Unit,
    onCancel: () -> Unit,
    onSymptomsChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    SpotlightCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalHospital,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(doctor.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("${doctor.specialty} • ${doctor.departmentName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("${doctor.rating}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Avg Consult Time: ${doctor.averageServiceTimeMinutes} min", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = if (doctor.isAvailable) "Available Today" else "Unavailable",
                fontSize = 12.sp,
                color = if (doctor.isAvailable) SuccessGreen else ErrorRed,
                fontWeight = FontWeight.Bold
            )
        }

        AnimatedVisibility(visible = isSelected) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                OutlinedTextField(
                    value = symptoms,
                    onValueChange = onSymptomsChange,
                    label = { Text("Pre-consultation Symptoms / Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onCancel,
                        modifier = Modifier.bounceClick(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onSurface)
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onSubmit,
                        modifier = Modifier.bounceClick().magnetEffect(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Book Token")
                    }
                }
            }
        }

        if (!isSelected && doctor.isAvailable) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onSelect,
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick()
                    .magnetEffect(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Select Doctor")
            }
        }
    }
}

@Composable
fun HistoryCard(token: Token) {
    SpotlightCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(token.doctorName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(token.departmentName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(SuccessGreen.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("Completed", fontSize = 11.sp, color = SuccessGreen, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Symptoms: ${token.symptoms}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        if (token.diagnosis != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Row {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Assignment,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text("Diagnosis: ${token.diagnosis}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text("Prescription: ${token.prescription}", fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        if (token.billAmount > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Consultation Fee: ₹${token.billAmount}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = if (token.paymentStatus == com.eto.manager.domain.model.PaymentStatus.PAID) "Paid" else "Payment Pending",
                    fontSize = 12.sp,
                    color = if (token.paymentStatus == com.eto.manager.domain.model.PaymentStatus.PAID) SuccessGreen else WarningOrange,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
