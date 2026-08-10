package com.eto.manager.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eto.manager.presentation.EtoViewModel
import com.eto.manager.presentation.UserRole
import com.eto.manager.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileView(
    currentRole: UserRole,
    viewModel: EtoViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background == DarkBgStart

    // Collect states
    val patientName by viewModel.patientName.collectAsState()
    val patientPhone by viewModel.patientPhone.collectAsState()
    val doctors by viewModel.doctors.collectAsState()
    val selectedDoctorId by viewModel.selectedDoctorId.collectAsState()
    val currentDoctor = doctors.find { it.id == selectedDoctorId }

    // Patient editing states
    var isEditingPatient by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(patientName) }
    var editPhone by remember { mutableStateOf(patientPhone) }

    LaunchedEffect(patientName, patientPhone) {
        editName = patientName
        editPhone = patientPhone
    }

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
            // Transparent Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "Profile",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = {},
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = {},
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Profile Header Card
                item {
                    SpotlightCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 28.dp
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Avatar Box
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isDark) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                        else Color(0xFFEFF6FF)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                val initials = when (currentRole) {
                                    UserRole.PATIENT -> if (patientName.isNotEmpty()) patientName.first().toString() else "P"
                                    UserRole.DOCTOR -> currentDoctor?.name?.split(" ")?.lastOrNull()?.first()?.toString() ?: "D"
                                    UserRole.RECEPTIONIST -> "N"
                                    UserRole.ADMIN -> "A"
                                }
                                Text(
                                    text = initials,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp,
                                    color = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF2563EB)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = when (currentRole) {
                                        UserRole.PATIENT -> patientName
                                        UserRole.DOCTOR -> currentDoctor?.name ?: "Dr. Rahul Verma"
                                        UserRole.RECEPTIONIST -> "Neha Sharma"
                                        UserRole.ADMIN -> "Super Admin"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Role Badge
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                when (currentRole) {
                                                    UserRole.PATIENT -> Color(0xFFE0F2FE)
                                                    UserRole.DOCTOR -> Color(0xFFEFF6FF)
                                                    UserRole.RECEPTIONIST -> Color(0xFFF3E8FF)
                                                    UserRole.ADMIN -> Color(0xFFFEF3C7)
                                                }
                                            )
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = currentRole.name,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (currentRole) {
                                                UserRole.PATIENT -> Color(0xFF0369A1)
                                                UserRole.DOCTOR -> Color(0xFF2563EB)
                                                UserRole.RECEPTIONIST -> Color(0xFF7E22CE)
                                                UserRole.ADMIN -> Color(0xFFB45309)
                                            }
                                        )
                                    }

                                    Text(
                                        text = when (currentRole) {
                                            UserRole.PATIENT -> "Patient ID: PT${String.format("%04d", (patientPhone.hashCode().coerceAtLeast(0) % 1000))}"
                                            UserRole.DOCTOR -> "Doctor ID: DR0001"
                                            UserRole.RECEPTIONIST -> "Employee ID: RC0001"
                                            UserRole.ADMIN -> "Admin ID: AD0001"
                                        },
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                // 2. Role-Specific Profile Sections
                when (currentRole) {
                    UserRole.PATIENT -> {
                        // Patient Personal Info
                        item {
                            SpotlightCard(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 24.dp
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Personal Information", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text(
                                        text = if (isEditingPatient) "Cancel" else "Edit",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        modifier = Modifier.clickable {
                                            if (isEditingPatient) {
                                                editName = patientName
                                                editPhone = patientPhone
                                            }
                                            isEditingPatient = !isEditingPatient
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                if (isEditingPatient) {
                                    OutlinedTextField(
                                        value = editName,
                                        onValueChange = { editName = it },
                                        label = { Text("Name") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = editPhone,
                                        onValueChange = { editPhone = it },
                                        label = { Text("Phone Number") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = {
                                            viewModel.patientName.value = editName
                                            viewModel.patientPhone.value = editPhone
                                            isEditingPatient = false
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Save Changes", fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        ProfileDetailRow(Icons.Outlined.CalendarToday, "Date of Birth", "12 May 1996")
                                        ProfileDetailRow(Icons.Outlined.Person, "Gender", "Male")
                                        ProfileDetailRow(Icons.Outlined.Phone, "Phone Number", patientPhone)
                                        ProfileDetailRow(Icons.Outlined.Email, "Email", "aarav.sharma@email.com")
                                        ProfileDetailRow(Icons.Outlined.Home, "Address", "221B Baker Street, London, UK")
                                    }
                                }
                            }
                        }

                        // Medical Info Mini Cards
                        item {
                            Column {
                                Text("Medical Information", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 4.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    InfoMiniCard("Blood Group", "B+", modifier = Modifier.weight(1f))
                                    InfoMiniCard("Allergies", "Penicillin", modifier = Modifier.weight(1f))
                                    InfoMiniCard("Conditions", "None", modifier = Modifier.weight(1f))
                                }
                            }
                        }

                        // ETO Stats Info
                        item {
                            SpotlightCard(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 24.dp
                            ) {
                                Text("ETO Information", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.height(16.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    ProfileDetailRow(Icons.Outlined.CalendarToday, "Member Since", "12 Jan 2024")
                                    ProfileDetailRow(Icons.Outlined.Home, "Saved Hospitals", "5 Hospitals")
                                    ProfileDetailRow(Icons.Outlined.Assignment, "Appointment History", "18 Appointments")
                                }
                            }
                        }
                    }

                    UserRole.DOCTOR -> {
                        // Availability Toggle & Schedule
                        item {
                            SpotlightCard(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 24.dp
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Availability", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(if (currentDoctor?.isAvailable == true) SuccessGreen else ErrorRed)
                                            )
                                            Text(
                                                text = if (currentDoctor?.isAvailable == true) "Available" else "Off Duty",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (currentDoctor?.isAvailable == true) SuccessGreen else ErrorRed
                                            )
                                        }
                                    }

                                    Switch(
                                        checked = currentDoctor?.isAvailable == true,
                                        onCheckedChange = {
                                            if (currentDoctor != null) {
                                                viewModel.toggleDoctorAvailability(currentDoctor.id, it)
                                            }
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    ProfileDetailRow(Icons.Outlined.CalendarToday, "Working Days", "Mon - Sat")
                                    ProfileDetailRow(Icons.Outlined.Schedule, "Consultation Hours", "09:00 AM - 05:00 PM")
                                    ProfileDetailRow(Icons.Outlined.Timer, "Appointment Duration", "15 mins per patient")
                                }
                            }
                        }

                        // Professional Info
                        item {
                            SpotlightCard(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 24.dp
                            ) {
                                Text("Professional Information", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.height(16.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    ProfileDetailRow(Icons.Outlined.Star, "Specialization", "Cardiologist")
                                    ProfileDetailRow(Icons.Outlined.Home, "Department", "Cardiology")
                                    ProfileDetailRow(Icons.Outlined.Assignment, "Qualification", "MBBS, MD (Cardiology)")
                                    ProfileDetailRow(Icons.Outlined.Timer, "Experience", "10+ Years")
                                    ProfileDetailRow(Icons.Outlined.AttachMoney, "Consultation Fee", "₹800")
                                }
                            }
                        }

                        // Contact & Workplace
                        item {
                            SpotlightCard(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 24.dp
                            ) {
                                Text("Contact & Workplace", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.height(16.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    ProfileDetailRow(Icons.Outlined.Home, "Hospital", "City Care Hospital")
                                    ProfileDetailRow(Icons.Outlined.Assignment, "Room / Cabin", "Cardiology OPD - 204")
                                    ProfileDetailRow(Icons.Outlined.Phone, "Phone Number", "+91 98765 43210")
                                    ProfileDetailRow(Icons.Outlined.Email, "Email", "rahul.verma@eto.com")
                                }
                            }
                        }
                    }

                    UserRole.RECEPTIONIST -> {
                        // Work Information
                        item {
                            SpotlightCard(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 24.dp
                            ) {
                                Text("Work Information", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.height(16.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    ProfileDetailRow(Icons.Outlined.Home, "Hospital", "City Care Hospital")
                                    ProfileDetailRow(Icons.Outlined.Person, "Department", "Front Desk")
                                    ProfileDetailRow(Icons.Outlined.Assignment, "Designation", "Senior Receptionist")
                                    ProfileDetailRow(Icons.Outlined.Schedule, "Shift", "Morning Shift")
                                    ProfileDetailRow(Icons.Outlined.CalendarToday, "Working Days", "Mon - Sat")
                                    ProfileDetailRow(Icons.Outlined.Timer, "Working Hours", "08:00 AM - 04:00 PM")
                                }
                            }
                        }

                        // Permissions & Access list
                        item {
                            Column {
                                Text("Permissions / Access", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 4.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PermissionAccessCard("Patient Registration", true, modifier = Modifier.weight(1f))
                                    PermissionAccessCard("Token Mgmt", true, modifier = Modifier.weight(1f))
                                    PermissionAccessCard("Appt Requests", true, modifier = Modifier.weight(1f))
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PermissionAccessCard("Queue Mgmt", true, modifier = Modifier.weight(1f))
                                    PermissionAccessCard("Billing", true, modifier = Modifier.weight(1f))
                                    PermissionAccessCard("Payments", true, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    UserRole.ADMIN -> {
                        item {
                            SpotlightCard(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 24.dp
                            ) {
                                Text("System Privileges", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.height(16.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    ProfileDetailRow(Icons.Outlined.Settings, "Role", "Hospital Administrator")
                                    ProfileDetailRow(Icons.Outlined.Assignment, "Privileges", "Full Database Access, Simulator Controls")
                                }
                            }
                        }
                    }
                }

                // 3. Settings Items List
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Settings", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 4.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        SettingsNavigationItem("Notifications", Icons.Outlined.Notifications)
                        if (currentRole == UserRole.RECEPTIONIST) {
                            SettingsNavigationItem("Queue Preferences", Icons.Outlined.Assignment)
                        }
                        SettingsNavigationItem("Privacy & Security", Icons.Outlined.Lock)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
fun ProfileDetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun PermissionAccessCard(label: String, isGranted: Boolean, modifier: Modifier = Modifier) {
    val isDark = MaterialTheme.colorScheme.background == DarkBgStart
    Box(
        modifier = modifier
            .glassmorphicCard(isDark, cornerRadius = 16.dp)
            .padding(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0F2FE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFF0369A1),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun SettingsNavigationItem(label: String, icon: ImageVector) {
    val isDark = MaterialTheme.colorScheme.background == DarkBgStart
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphicCard(isDark, cornerRadius = 16.dp)
            .clickable {}
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
