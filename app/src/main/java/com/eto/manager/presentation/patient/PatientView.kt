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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.eto.manager.presentation.components.EmptyState
import com.eto.manager.presentation.components.SectionHeader
import com.eto.manager.presentation.components.ShinyText
import com.eto.manager.presentation.components.SpotlightCard
import com.eto.manager.presentation.components.StatusBadge
import com.eto.manager.presentation.components.bounceClick
import com.eto.manager.presentation.components.magnetEffect
import com.eto.manager.presentation.components.shimmer
import com.eto.manager.presentation.theme.*

@Composable
fun PatientView(
    viewModel: EtoViewModel, 
    activeTab: Int, 
    modifier: Modifier = Modifier
) {
    val name by viewModel.patientName.collectAsState()
    val phone by viewModel.patientPhone.collectAsState()
    val doctors by viewModel.doctors.collectAsState()
    val departments by viewModel.departments.collectAsState()
    val tokens by viewModel.tokens.collectAsState()
    val selectedDocId by viewModel.selectedDoctorId.collectAsState()
    val symptoms by viewModel.symptomsInput.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedDeptId by remember { mutableStateOf<String?>(null) }

    // Filter tokens for this patient
    val patientTokens = tokens.filter { it.patientPhone == phone }

    val isDark = MaterialTheme.colorScheme.background == DarkBgStart

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        when (activeTab) {
            0 -> { // HOME TAB: Browse Departments, Search & Book Doctors
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        // Glassmorphic Search Bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search doctor specialty, name...", fontSize = 14.sp) },
                            leadingIcon = { 
                                Icon(
                                    imageVector = Icons.Default.Search, 
                                    contentDescription = null, 
                                    tint = MaterialTheme.colorScheme.primary
                                ) 
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        )
                    }

                    item {
                        SectionHeader("Select Department")
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // All category pill
                            item {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (selectedDeptId == null) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        )
                                        .clickable { selectedDeptId = null }
                                        .padding(horizontal = 18.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        "All",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (selectedDeptId == null) MaterialTheme.colorScheme.onPrimary 
                                                else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            items(departments) { dept ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (selectedDeptId == dept.id) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        )
                                        .clickable { selectedDeptId = dept.id }
                                        .padding(horizontal = 18.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        dept.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (selectedDeptId == dept.id) MaterialTheme.colorScheme.onPrimary 
                                                else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    item {
                        SectionHeader("Available Specialists")
                    }

                    // Filtering logic
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
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .shimmer()
                            )
                        }
                    } else if (filteredDoctors.isEmpty()) {
                        item {
                            EmptyState(message = "No specialists available matching constraints.")
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
                    
                    item {
                        Spacer(modifier = Modifier.height(80.dp)) // padding at the bottom for floating nav
                    }
                }
            }

            1 -> { // QUEUE TAB: Active Token Tracking
                val activeTokens = patientTokens.filter { it.status == TokenStatus.PENDING || it.status == TokenStatus.APPROVED || it.status == TokenStatus.SERVING }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "My Active Tokens",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                    }

                    if (activeTokens.isEmpty()) {
                        item {
                            EmptyState(message = "No active booking token found.\nSwitch to the Home tab to book an appointment.")
                        }
                    } else {
                        items(activeTokens) { activeToken ->
                            ActiveTokenCard(
                                activeToken = activeToken,
                                tokens = tokens,
                                isDark = isDark
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }

            2 -> { // HISTORY TAB: Consultations & Medical Prescription History
                val historyTokens = patientTokens.filter { it.status == TokenStatus.COMPLETED }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Prescription & History",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                    }

                    if (historyTokens.isEmpty()) {
                        item {
                            EmptyState(message = "No completed consultations found in your history log.")
                        }
                    } else {
                        items(historyTokens) { history ->
                            HistoryCard(token = history)
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }

            3 -> { // PROFILE TAB: Edit Patient Identity details
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Patient Identity Profile",
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
                            Text(
                                "Profile Details", 
                                style = MaterialTheme.typography.titleMedium, 
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = name,
                                onValueChange = { viewModel.patientName.value = it },
                                label = { Text("Full Name", fontSize = 13.sp) },
                                leadingIcon = { 
                                    Icon(
                                        imageVector = Icons.Default.Person, 
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    ) 
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = phone,
                                onValueChange = { viewModel.patientPhone.value = it },
                                label = { Text("Mobile Number", fontSize = 13.sp) },
                                leadingIcon = { 
                                    Icon(
                                        imageVector = Icons.Default.Phone, 
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    ) 
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
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
fun QueueDetailItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label, 
            fontSize = 11.sp, 
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value, 
            fontSize = 14.sp, 
            fontWeight = FontWeight.Bold, 
            color = MaterialTheme.colorScheme.onSurface
        )
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
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 28.dp
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
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = doctor.name, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${doctor.specialty} • ${doctor.departmentName}", 
                        fontSize = 12.sp, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star, 
                    contentDescription = null, 
                    tint = Color(0xFFFFB300), 
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${doctor.rating}", 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Avg consult: ${doctor.averageServiceTimeMinutes} mins", 
                fontSize = 12.sp, 
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                    label = { Text("Describe Symptoms / Special Requests", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onCancel,
                        modifier = Modifier.bounceClick(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent, 
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onSubmit,
                        enabled = symptoms.isNotBlank(),
                        modifier = Modifier
                            .bounceClick()
                            .magnetEffect(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Book Token", fontWeight = FontWeight.Bold)
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Select Doctor", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun HistoryCard(token: Token) {
    val isDark = MaterialTheme.colorScheme.background == DarkBgStart

    SpotlightCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = token.doctorName, 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = token.departmentName, 
                    fontSize = 12.sp, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isDark) DarkSuccessBg else LightSuccessBg
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    "Completed", 
                    fontSize = 10.sp, 
                    color = if (isDark) DarkSuccessText else LightSuccessText, 
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Symptoms: ${token.symptoms}", 
            fontSize = 12.sp, 
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        if (token.diagnosis != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Assignment,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
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
                }
            }
        }
        
        if (token.billAmount > 0) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Consultation Fee: ₹${token.billAmount}", 
                    fontSize = 12.sp, 
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (token.paymentStatus == com.eto.manager.domain.model.PaymentStatus.PAID) "Paid" else "Payment Pending",
                    fontSize = 12.sp,
                    color = if (token.paymentStatus == com.eto.manager.domain.model.PaymentStatus.PAID) SuccessGreen else Color(0xFFD97706),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ActiveTokenCard(
    activeToken: Token,
    tokens: List<Token>,
    isDark: Boolean
) {
    val doctorQueue = tokens.filter { it.doctorId == activeToken.doctorId && (it.status == TokenStatus.APPROVED || it.status == TokenStatus.SERVING) }
    val servingToken = doctorQueue.find { it.status == TokenStatus.SERVING }
    val pendingQueue = doctorQueue.filter { it.status == TokenStatus.APPROVED }.sortedBy { it.id }
    val positionIndex = pendingQueue.indexOfFirst { it.id == activeToken.id }
    val aheadCount = if (positionIndex >= 0) positionIndex + (if (servingToken != null) 1 else 0) else 0

    val transition = rememberInfiniteTransition(label = "halo")
    val haloScale1 by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "halo1"
    )
    val haloAlpha1 by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "halo1Alpha"
    )

    val haloScale2 by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, delayMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "halo2"
    )
    val haloAlpha2 by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, delayMillis = 1100, easing = FastOutSlowInEasing),
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

    val sweepColor1 = MaterialTheme.colorScheme.primary
    val sweepColor2 = MaterialTheme.colorScheme.secondary
    val sweepBrush = remember(sweepColor1, sweepColor2) {
        Brush.sweepGradient(
            colors = listOf(sweepColor1, sweepColor2, sweepColor1)
        )
    }

    SpotlightCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        cornerRadius = 32.dp
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    activeToken.doctorName, 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    activeToken.departmentName, 
                    style = MaterialTheme.typography.bodyMedium, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusBadge(activeToken.status)
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Circular tracker container
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(170.dp)
                .align(Alignment.CenterHorizontally)
        ) {
            // Fading Halo animations
            Canvas(modifier = Modifier.size(120.dp)) {
                drawCircle(
                    color = sweepColor2,
                    radius = size.width / 2 * haloScale1,
                    alpha = haloAlpha1
                )
                drawCircle(
                    color = sweepColor2,
                    radius = size.width / 2 * haloScale2,
                    alpha = haloAlpha2
                )
            }

            // Outer track & progress arc
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Track Background
                drawCircle(
                    color = sweepColor1.copy(alpha = 0.08f),
                    style = Stroke(width = 8.dp.toPx())
                )
                // Sweep Arc
                drawArc(
                    brush = sweepBrush,
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            
            // Inner capsule circle
            Box(
                modifier = Modifier
                    .size(122.dp)
                    .clip(CircleShape)
                    .background(
                        if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "ACTIVE TOKEN", 
                        fontSize = 9.sp, 
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    ShinyText(
                        text = activeToken.tokenNumber,
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            QueueDetailItem("Now Serving", servingToken?.tokenNumber ?: "None")
            QueueDetailItem("Ahead Of You", "$aheadCount Patients")
            QueueDetailItem("Est. Wait Time", "${aheadCount * 12} mins")
        }
    }
}
