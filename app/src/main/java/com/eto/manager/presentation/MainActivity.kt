package com.eto.manager.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eto.manager.presentation.admin.AdminView
import com.eto.manager.presentation.components.LivePhoneAlert
import com.eto.manager.presentation.components.SpotlightCard
import com.eto.manager.presentation.components.bounceClick
import com.eto.manager.presentation.components.glassmorphicCard
import com.eto.manager.presentation.components.magnetEffect
import com.eto.manager.presentation.doctor.DoctorView
import com.eto.manager.presentation.patient.PatientView
import com.eto.manager.presentation.receptionist.ReceptionistView
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.filled.Add
import com.eto.manager.presentation.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isDarkTheme by remember { mutableStateOf(false) }
            EtoTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: EtoViewModel = viewModel()
                    EtoAppShell(
                        viewModel = viewModel,
                        isDarkTheme = isDarkTheme,
                        onThemeToggle = { isDarkTheme = !isDarkTheme }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EtoAppShell(
    viewModel: EtoViewModel,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit
) {
    val currentRole by viewModel.currentRole.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    
    var showPhoneSimulator by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(0) } // 0: Home, 1: Queue, 2: History, 3: Profile
    var showProfileModal by remember { mutableStateOf(false) }
    var showWalkInDialog by remember { mutableStateOf(false) }

    val isDark = MaterialTheme.colorScheme.background == DarkBgStart

    // Linear background gradient
    val backgroundBrush = Brush.verticalGradient(
        colors = if (isDark) {
            listOf(DarkBgStart, DarkBgEnd)
        } else {
            listOf(LightBgStart, LightBgEnd)
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                val patientName by viewModel.patientName.collectAsState()
                val patientPhone by viewModel.patientPhone.collectAsState()
                
                val doctors by viewModel.doctors.collectAsState()
                val selectedDoctorId by viewModel.selectedDoctorId.collectAsState()
                val currentDoctor = doctors.find { it.id == selectedDoctorId }
                val doctorName = currentDoctor?.name ?: "Dr. Rahul Verma"

                CommonTopHeader(
                    currentRole = currentRole,
                    patientName = patientName,
                    doctorName = doctorName,
                    onNotificationClick = { showPhoneSimulator = !showPhoneSimulator },
                    onProfileClick = { showProfileModal = true },
                    onRoleChange = { role ->
                        viewModel.setRole(role)
                        activeTab = 0 // Reset tab when role changes
                    }
                )
            },
            bottomBar = {
                BottomFloatingNavBar(
                    currentRole = currentRole,
                    activeTab = activeTab,
                    onTabSelected = { activeTab = it },
                    onFabClick = { showWalkInDialog = true }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Content Switcher dependent on active role AND active bottom tab
                when (currentRole) {
                    UserRole.PATIENT -> PatientView(
                        viewModel = viewModel,
                        activeTab = activeTab,
                        onTabSelected = { activeTab = it },
                        onNotificationClick = { showPhoneSimulator = !showPhoneSimulator }
                    )
                    UserRole.RECEPTIONIST -> ReceptionistView(
                        viewModel = viewModel,
                        activeTab = activeTab,
                        showWalkInDialog = showWalkInDialog,
                        onDismissWalkIn = { showWalkInDialog = false }
                    )
                    UserRole.DOCTOR -> DoctorView(
                        viewModel = viewModel,
                        activeTab = activeTab,
                        onNotificationClick = { showPhoneSimulator = !showPhoneSimulator },
                        onProfileClick = { showProfileModal = true }
                    )
                    UserRole.ADMIN -> AdminView(viewModel, activeTab)
                }

                // Profile Modal Dialog
                if (showProfileModal) {
                    val patientName by viewModel.patientName.collectAsState()
                    val patientPhone by viewModel.patientPhone.collectAsState()

                    ProfileModalDialog(
                        currentRole = currentRole,
                        patientName = patientName,
                        patientPhone = patientPhone,
                        onPatientInfoSave = { name, phone ->
                            viewModel.patientName.value = name
                            viewModel.patientPhone.value = phone
                        },
                        onDismiss = { showProfileModal = false }
                    )
                }

                // Phone SMS alerts panel drawer
                AnimatedVisibility(
                    visible = showPhoneSimulator,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    VirtualPhoneDrawer(
                        notifications = notifications,
                        onClose = { showPhoneSimulator = false }
                    )
                }
            }
        }
    }
}

@Composable
fun CommonTopHeader(
    currentRole: UserRole,
    patientName: String,
    doctorName: String,
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit,
    onRoleChange: (UserRole) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background == DarkBgStart
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Column: Avatar & User info
        Row(verticalAlignment = Alignment.CenterVertically) {
            // ETO blue badge logo as avatar (matching screenshot)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2563EB)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocalHospital,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "Good Morning",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = when (currentRole) {
                        UserRole.PATIENT -> if (patientName.isBlank()) "John Doe" else patientName
                        UserRole.RECEPTIONIST -> "Receptionist"
                        UserRole.DOCTOR -> if (doctorName.isBlank()) "Dr. Sarah Jenkins" else doctorName
                        UserRole.ADMIN -> "Administrator"
                    },
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Right Row: Role Selector, Notification bell, Profile button
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Role select badge
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .clickable { menuExpanded = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val roleIcon = when (currentRole) {
                        UserRole.PATIENT -> Icons.Outlined.Person
                        UserRole.RECEPTIONIST -> Icons.Outlined.SupportAgent
                        UserRole.DOCTOR -> Icons.Outlined.MedicalServices
                        UserRole.ADMIN -> Icons.Outlined.AdminPanelSettings
                    }
                    Icon(
                        imageVector = roleIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = currentRole.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    UserRole.values().forEach { role ->
                        DropdownMenuItem(
                            text = { Text(role.name, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            onClick = {
                                onRoleChange(role)
                                menuExpanded = false
                            }
                        )
                    }
                }
            }

            // Notification Bell
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .clickable { onNotificationClick() }
                    .bounceClick(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = "Notifications",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Profile Button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .clickable { onProfileClick() }
                    .bounceClick(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = "Profile",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ProfileModalDialog(
    currentRole: UserRole,
    patientName: String,
    patientPhone: String,
    onPatientInfoSave: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var tempName by remember { mutableStateOf(patientName) }
    var tempPhone by remember { mutableStateOf(patientPhone) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "User Profile",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large Avatar Icon
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEFF6FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(36.dp)
                    )
                }

                when (currentRole) {
                    UserRole.PATIENT -> {
                        Text(
                            text = "Patient Profile Details",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedTextField(
                            value = tempName,
                            onValueChange = { tempName = it },
                            label = { Text("Name") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = tempPhone,
                            onValueChange = { tempPhone = it },
                            label = { Text("Phone Number") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                onPatientInfoSave(tempName, tempPhone)
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save Info", fontWeight = FontWeight.Bold)
                        }
                    }
                    UserRole.RECEPTIONIST -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.Start,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Desk Assignment: Reception Desk #1", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Shift Assignment: Morning (08:00 AM - 04:00 PM)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Console Access: Patient Check-In & Invoicing", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    UserRole.DOCTOR -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.Start,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Role: Consulting Doctor", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Clinic: ETO General Clinic Center", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Access Privileges: Consultations, Prescriptions & Diagnostics", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    UserRole.ADMIN -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.Start,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Role: Hospital Administrator", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Privileges: Full Database Reset & Simulation Controls", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun BottomFloatingNavBar(
    currentRole: UserRole,
    activeTab: Int,
    onTabSelected: (Int) -> Unit,
    onFabClick: () -> Unit = {}
) {
    val isDark = MaterialTheme.colorScheme.background == DarkBgStart
    val items = when (currentRole) {
        UserRole.PATIENT -> listOf(
            Triple("Home", Icons.Outlined.Home, 0),
            Triple("My Appointments", Icons.Outlined.CalendarToday, 1)
        )
        UserRole.DOCTOR -> listOf(
            Triple("Patients", Icons.Outlined.People, 0),
            Triple("Patient Review", Icons.Outlined.Assignment, 1),
            Triple("Profile", Icons.Outlined.Person, 2)
        )
        UserRole.RECEPTIONIST -> listOf(
            Triple("Queue", Icons.Outlined.People, 0),
            Triple("Requests", Icons.Outlined.Assignment, 1),
            Triple("Bills", Icons.Outlined.ReceiptLong, 2)
        )
        UserRole.ADMIN -> listOf(
            Triple("Home", Icons.Outlined.Home, 0),
            Triple("Queue", Icons.Outlined.HourglassEmpty, 1),
            Triple("History", Icons.Outlined.ReceiptLong, 2),
            Triple("Profile", Icons.Outlined.Person, 3)
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .glassmorphicCard(isDark, cornerRadius = 32.dp)
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { idx, (label, icon, index) ->
                    val isActive = activeTab == index
                    
                    if (idx > 0) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(20.dp)
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            )
                            .clickable { onTabSelected(index) }
                            .padding(horizontal = if (isActive) 16.dp else 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                            if (isActive) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
        
        if (currentRole == UserRole.RECEPTIONIST) {
            Spacer(modifier = Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEFF6FF))
                    .border(1.dp, Color(0xFFDBEAFE), CircleShape)
                    .clickable { onFabClick() }
                    .bounceClick(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Register Walk-in",
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun VirtualPhoneDrawer(
    notifications: List<NotificationItem>,
    onClose: () -> Unit
) {
    SpotlightCard(
        modifier = Modifier
            .width(320.dp)
            .fillMaxHeight(0.65f)
            .padding(16.dp)
            .border(
                width = 4.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                shape = RoundedCornerShape(32.dp)
            ),
        cornerRadius = 32.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // notch simulator
            Box(
                modifier = Modifier
                    .width(90.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (MaterialTheme.colorScheme.background == DarkBgStart) Color.Black else Color(0x33000000))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Phone Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Phone Notification Drawer",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onClose, modifier = Modifier.bounceClick()) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Simulator Drawer",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Alert List inside Simulator
            if (notifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No SMS alerts received yet.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(notifications) { item ->
                        LivePhoneAlert(
                            title = item.title,
                            message = item.message,
                            type = item.type
                        )
                    }
                }
            }
        }
    }
}
