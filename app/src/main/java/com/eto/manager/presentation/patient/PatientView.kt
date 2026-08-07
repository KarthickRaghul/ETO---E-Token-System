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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.PaddingValues
import com.eto.manager.presentation.UserRole
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import androidx.compose.foundation.border
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import com.eto.manager.R
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
    onTabSelected: (Int) -> Unit = {},
    onNotificationClick: () -> Unit = {},
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
    var isBookingFlowActive by remember { mutableStateOf(false) }

    var selectedHospital by remember { mutableStateOf<MockHospitalInfo?>(null) }
    var selectedDoctor by remember { mutableStateOf<Doctor?>(null) }
    var symptomsText by remember { mutableStateOf("") }

    val mockHospitalsList = remember {
        listOf(
            MockHospitalInfo(
                id = "h1",
                name = "City Care Hospital",
                distanceTime = "1.2 km • 18 min",
                rating = "4.6",
                latitude = 13.0827,
                longitude = 80.2707,
                specialties = listOf("Cardiology", "General Medicine"),
                address = "12, Poonamallee High Rd, Chennai",
                imageRes = R.drawable.hospital_thumbnail
            ),
            MockHospitalInfo(
                id = "h2",
                name = "Sunrise Clinic",
                distanceTime = "2.4 km • 24 min",
                rating = "4.4",
                latitude = 13.0850,
                longitude = 80.2800,
                specialties = listOf("Dermatology", "Pediatrics"),
                address = "45, Cathedral Rd, Chennai",
                imageRes = R.drawable.hospital_thumbnail
            ),
            MockHospitalInfo(
                id = "h3",
                name = "St. Jude Hospital",
                distanceTime = "3.5 km • 30 min",
                rating = "4.7",
                latitude = 13.0780,
                longitude = 80.2600,
                specialties = listOf("General Medicine", "Pediatrics"),
                address = "78, Anna Salai, Chennai",
                imageRes = R.drawable.hospital_thumbnail
            )
        )
    }

    val queryLower = searchQuery.trim().lowercase()
    val isDermatologyMatch = queryLower.contains("derm")
    val isCardiologyMatch = queryLower.contains("cardio") || queryLower.contains("heart")
    val isPediatricsMatch = queryLower.contains("pedi") || queryLower.contains("child") || queryLower.contains("kid")
    val isGeneralMatch = queryLower.contains("general") || queryLower.contains("physician") || queryLower.contains("medicine") || queryLower.contains("doctor")

    val filteredHospitals = remember(searchQuery, mockHospitalsList) {
        if (searchQuery.isBlank()) {
            mockHospitalsList
        } else {
            mockHospitalsList.filter { hospital ->
                hospital.name.contains(searchQuery, ignoreCase = true) ||
                hospital.specialties.any { specialty ->
                    val specLower = specialty.lowercase()
                    specLower.contains(queryLower) ||
                    (specLower == "dermatology" && isDermatologyMatch) ||
                    (specLower == "cardiology" && isCardiologyMatch) ||
                    (specLower == "pediatrics" && isPediatricsMatch) ||
                    (specLower == "general medicine" && isGeneralMatch)
                }
            }
        }
    }

    // Filter tokens for this patient
    val patientTokens = tokens.filter { it.patientPhone == phone }

    val isDark = MaterialTheme.colorScheme.background == DarkBgStart

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        when (activeTab) {
            0 -> { // HOME TAB
                if (isBookingFlowActive) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 16.dp)
                    ) {
                        // 1. Back button & Title row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable { 
                                        if (selectedDoctor != null) {
                                            selectedDoctor = null
                                        } else if (selectedHospital != null) {
                                            selectedHospital = null
                                        } else {
                                            isBookingFlowActive = false 
                                        }
                                    }
                                    .padding(end = 8.dp)
                                    .size(24.dp)
                            )
                            Text(
                                text = if (selectedDoctor != null) "Describe Symptoms"
                                       else if (selectedHospital != null) "Select Specialist"
                                       else "Select Hospital",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // 2. Search bar (only visible if hospital is not yet selected)
                        if (selectedHospital == null) {
                            // alternating placeholder text
                            val placeholders = listOf(
                                "Enter hospital name or specialisation...",
                                "Search: Dermatologist, Cardiologist, Pediatrician...",
                                "Search: City Care Hospital, Sunrise Clinic..."
                            )
                            var placeholderIndex by remember { mutableStateOf(0) }
                            LaunchedEffect(Unit) {
                                while(true) {
                                    kotlinx.coroutines.delay(3000)
                                    placeholderIndex = (placeholderIndex + 1) % placeholders.size
                                }
                            }
                            val currentPlaceholder = placeholders[placeholderIndex]

                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text(currentPlaceholder, fontSize = 14.sp) },
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
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // 3. Maximized map taking up a nice height
                        val context = LocalContext.current
                        LaunchedEffect(Unit) {
                            org.osmdroid.config.Configuration.getInstance().userAgentValue = context.packageName
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (selectedHospital != null) 160.dp else 260.dp) // adjust height dynamically
                                .clip(RoundedCornerShape(24.dp))
                                .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    MapView(ctx).apply {
                                        setMultiTouchControls(true)
                                        controller.setZoom(14.0)
                                        controller.setCenter(GeoPoint(13.0827, 80.2707))
                                    }
                                },
                                update = { mapView ->
                                    mapView.overlays.clear()
                                    filteredHospitals.forEach { hosp ->
                                        val marker = org.osmdroid.views.overlay.Marker(mapView).apply {
                                            position = GeoPoint(hosp.latitude, hosp.longitude)
                                            setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM)
                                            title = hosp.name
                                            subDescription = hosp.address
                                            setOnMarkerClickListener { m, mv ->
                                                selectedHospital = hosp
                                                m.showInfoWindow()
                                                true
                                            }
                                        }
                                        mapView.overlays.add(marker)
                                    }
                                    if (selectedHospital != null) {
                                        mapView.controller.animateTo(GeoPoint(selectedHospital!!.latitude, selectedHospital!!.longitude))
                                        mapView.controller.setZoom(16.0)
                                    } else {
                                        mapView.controller.setCenter(GeoPoint(13.0827, 80.2707))
                                        mapView.controller.setZoom(14.0)
                                    }
                                    mapView.invalidate()
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        // 4. Details list & select area
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (selectedHospital == null) {
                                // List hospitals
                                item {
                                    Text(
                                        text = "Matching Hospitals (${filteredHospitals.size})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (filteredHospitals.isEmpty()) {
                                    item {
                                        EmptyState(message = "No matching hospitals found.")
                                    }
                                } else {
                                    items(filteredHospitals) { hosp ->
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
                                                .clickable { selectedHospital = hosp }
                                                .padding(16.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(56.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
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
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = hosp.name,
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = hosp.distanceTime,
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "Specialties: ${hosp.specialties.joinToString(", ")}",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowRight,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                )
                                            }
                                        }
                                    }
                                }
                            } else if (selectedDoctor == null) {
                                // Show selected hospital card, then list of doctors
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                                            .padding(14.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column {
                                                Text(selectedHospital!!.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                                                Text(selectedHospital!!.address, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            TextButton(onClick = { selectedHospital = null }) {
                                                Text("Change", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                item {
                                    Text(
                                        text = "Available Specialists at ${selectedHospital!!.name}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                val hospitalDoctors = doctors.filter { doc ->
                                    selectedHospital!!.specialties.contains(doc.departmentName)
                                }

                                if (hospitalDoctors.isEmpty()) {
                                    item {
                                        EmptyState(message = "No specialists are currently active at this hospital.")
                                    }
                                } else {
                                    items(hospitalDoctors) { doc ->
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
                                                .clickable { selectedDoctor = doc }
                                                .padding(16.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFFEFF6FF)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Person,
                                                        contentDescription = null,
                                                        tint = Color(0xFF2563EB),
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(doc.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                                                    Text(doc.specialty, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "Avg Wait Time: ${doc.averageServiceTimeMinutes} mins",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowRight,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Booking form: Symptoms and click confirm booking
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                                            .padding(14.dp)
                                    ) {
                                        Column {
                                            Text("Hospital: ${selectedHospital!!.name}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text("Doctor: ${selectedDoctor!!.name} (${selectedDoctor!!.specialty})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            TextButton(
                                                onClick = { selectedDoctor = null },
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Text("Change Doctor", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }

                                item {
                                    Text(
                                        text = "Describe Symptoms / Health Issues",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    OutlinedTextField(
                                        value = symptomsText,
                                        onValueChange = { symptomsText = it },
                                        placeholder = { Text("e.g. Migraine headache, persistent dry cough, rash on hand...", fontSize = 13.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        minLines = 3,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                        )
                                    )
                                }

                                item {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            viewModel.selectDoctor(selectedDoctor!!.id)
                                            viewModel.symptomsInput.value = symptomsText
                                            viewModel.requestToken()
                                            
                                            // reset states
                                            symptomsText = ""
                                            selectedDoctor = null
                                            selectedHospital = null
                                            isBookingFlowActive = false
                                            onTabSelected(1)
                                        },
                                        enabled = symptomsText.isNotBlank(),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .bounceClick()
                                            .magnetEffect(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Text("Book Appointment Token", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }
                                }
                            }
                            
                            item {
                                Spacer(modifier = Modifier.height(100.dp))
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        item {
                            // Book an Appointment Card
                            SpotlightCard(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    selectedHospital = null
                                    selectedDoctor = null
                                    symptomsText = ""
                                    searchQuery = ""
                                    isBookingFlowActive = true
                                },
                                cornerRadius = 24.dp
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Color(0xFFEFF6FF)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.CalendarToday,
                                                contentDescription = null,
                                                tint = Color(0xFF2563EB),
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(
                                                text = "Book an Appointment",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Search by symptoms, specialisation or hospital near you",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF2563EB)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            // Nearby Hospitals Section
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Nearby Hospitals",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "View all",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF2563EB),
                                    modifier = Modifier.clickable {
                                        selectedHospital = null
                                        selectedDoctor = null
                                        symptomsText = ""
                                        searchQuery = ""
                                        isBookingFlowActive = true
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            // Map Card using OpenStreetMap
                            val context = LocalContext.current
                            LaunchedEffect(Unit) {
                                Configuration.getInstance().userAgentValue = context.packageName
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                            ) {
                                AndroidView(
                                    factory = { ctx ->
                                        MapView(ctx).apply {
                                            setMultiTouchControls(true)
                                            controller.setZoom(15.0)
                                            // Center on Chennai coordinates
                                            val mapCenter = GeoPoint(13.0827, 80.2707)
                                            controller.setCenter(mapCenter)

                                            // Add ETO Hospital Center marker
                                            val marker = Marker(this).apply {
                                                position = mapCenter
                                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                                title = "ETO Hospital Center"
                                            }
                                            overlays.add(marker)
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        item {
                            // Horizontal scrolling list of mock hospital cards
                            val mockHospitals = listOf(
                                MockHospital("City Care Hospital", "1.2 km • 18 min", "4.6", R.drawable.hospital_thumbnail),
                                MockHospital("Sunrise Clinic", "2.4 km • 24 min", "4.4", R.drawable.hospital_thumbnail),
                                MockHospital("St. Jude Hospital", "3.5 km • 30 min", "4.7", R.drawable.hospital_thumbnail)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(mockHospitals) { hospital ->
                                    NearbyHospitalCard(hospital)
                                }
                            }
                        }

                        // My Active Appointment
                        val activeToken = patientTokens.find { it.status == TokenStatus.PENDING || it.status == TokenStatus.APPROVED || it.status == TokenStatus.SERVING }
                        if (activeToken != null) {
                            item {
                                Text(
                                    text = "My Active Appointment",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                val doctorQueue = tokens.filter { it.doctorId == activeToken.doctorId && (it.status == TokenStatus.APPROVED || it.status == TokenStatus.SERVING) }
                                val servingToken = doctorQueue.find { it.status == TokenStatus.SERVING }
                                val pendingQueue = doctorQueue.filter { it.status == TokenStatus.APPROVED }.sortedBy { it.id }
                                val positionIndex = pendingQueue.indexOfFirst { it.id == activeToken.id }
                                val aheadCount = if (positionIndex >= 0) positionIndex + (if (servingToken != null) 1 else 0) else 0

                                val progressTarget = if (aheadCount >= 0) {
                                    (1f / (aheadCount + 1).toFloat()).coerceIn(0.15f, 1.0f)
                                } else 1.0f

                                val animatedProgress by animateFloatAsState(
                                    targetValue = progressTarget,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessVeryLow),
                                    label = "progress"
                                )

                                SpotlightCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    cornerRadius = 24.dp
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFEFF6FF)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.LocalHospital,
                                                    contentDescription = null,
                                                    tint = Color(0xFF2563EB),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = "City Care Hospital",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "${activeToken.departmentName} • ${activeToken.doctorName}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFEBFDF5))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "Confirmed",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF10B981)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        ActiveAppointmentStat("Token No.", activeToken.tokenNumber, isPrimary = true)
                                        ActiveAppointmentStat("Current Token", servingToken?.tokenNumber ?: "None")
                                        ActiveAppointmentStat("Patients Ahead", "$aheadCount")
                                        ActiveAppointmentStat("Est. Waiting", "${aheadCount * 12} min")
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(animatedProgress)
                                                .fillMaxHeight()
                                                .clip(CircleShape)
                                                .background(Color(0xFF3B82F6))
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = "You'll be notified when it's almost your turn",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Button(
                                        onClick = { onTabSelected(1) },
                                        modifier = Modifier.fillMaxWidth().height(44.dp).bounceClick(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFEFF6FF),
                                            contentColor = Color(0xFF2563EB)
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Track Queue", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowRight,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            // Quick Actions Section
                            Text(
                                text = "Quick Actions",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                QuickActionCard("My Health\nRecords", Icons.Outlined.MedicalServices, modifier = Modifier.weight(1f))
                                QuickActionCard("Prescriptions", Icons.Outlined.ReceiptLong, modifier = Modifier.weight(1f))
                                QuickActionCard("Lab Reports", Icons.Outlined.SupportAgent, modifier = Modifier.weight(1f))
                                QuickActionCard("Bills", Icons.Outlined.AdminPanelSettings, modifier = Modifier.weight(1f))
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }

            1 -> { // MY APPOINTMENTS TAB
                val activeTokens = patientTokens.filter { it.status == TokenStatus.PENDING || it.status == TokenStatus.APPROVED || it.status == TokenStatus.SERVING }
                val historyTokens = patientTokens.filter { it.status == TokenStatus.COMPLETED }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "My Appointments",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                    }

                    if (activeTokens.isNotEmpty()) {
                        item {
                            SectionHeader("Active Queues")
                        }
                        items(activeTokens) { activeToken ->
                            ActiveTokenCard(
                                activeToken = activeToken,
                                tokens = tokens,
                                isDark = isDark
                            )
                        }
                    }

                    item {
                        SectionHeader("Past Consultations")
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

@Composable
fun HospitalMapPin(text: String, modifier: Modifier = Modifier, isBlue: Boolean = false) {
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isBlue) Color(0xFF3B82F6) else Color.White)
            .border(1.dp, if (isBlue) Color.Transparent else Color(0xFFE2E8F0), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isBlue) Color.White else Color(0xFF2563EB)
        )
    }
}

data class MockHospitalInfo(
    val id: String,
    val name: String,
    val distanceTime: String,
    val rating: String,
    val latitude: Double,
    val longitude: Double,
    val specialties: List<String>,
    val address: String,
    val imageRes: Int
)

data class MockHospital(
    val name: String,
    val distanceTime: String,
    val rating: String,
    val imageRes: Int
)

@Composable
fun NearbyHospitalCard(hospital: MockHospital) {
    SpotlightCard(
        modifier = Modifier.width(220.dp),
        cornerRadius = 20.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.Image(
                painter = painterResource(id = hospital.imageRes),
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = hospital.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = hospital.distanceTime,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFDEF7EC))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Open",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF03543F)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = hospital.rating,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveAppointmentStat(label: String, value: String, isPrimary: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (isPrimary) Color(0xFF2563EB) else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun QuickActionCard(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    SpotlightCard(
        modifier = modifier.height(100.dp),
        cornerRadius = 20.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEFF6FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 13.sp
            )
        }
    }
}
