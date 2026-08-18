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

    // Dynamic Database profiles
    val patientProfile by viewModel.patientProfile.collectAsState()
    val doctorProfile by viewModel.doctorProfile.collectAsState()
    val receptionistProfile by viewModel.receptionistProfile.collectAsState()
    val isProfileLoading by viewModel.isProfileLoading.collectAsState()

    // Patient editing states
    var isEditingPatient by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(patientName) }
    var editPhone by remember { mutableStateOf(patientPhone) }
    
    var editEmail by remember { mutableStateOf(patientProfile?.email ?: "") }
    var editDob by remember { mutableStateOf(patientProfile?.date_of_birth ?: "") }
    var editGender by remember { mutableStateOf(patientProfile?.gender ?: "") }
    var editBloodGroup by remember { mutableStateOf(patientProfile?.blood_group ?: "") }
    var editAllergies by remember { mutableStateOf(patientProfile?.allergies ?: "") }
    var editConditions by remember { mutableStateOf(patientProfile?.conditions ?: "") }
    var editMedications by remember { mutableStateOf(patientProfile?.current_medications ?: "") }
    var editAddress by remember { mutableStateOf(patientProfile?.address ?: "") }
    var editEmergencyName by remember { mutableStateOf(patientProfile?.emergency_contact_name ?: "") }
    var editEmergencyPhone by remember { mutableStateOf(patientProfile?.emergency_contact_phone ?: "") }

    LaunchedEffect(patientName, patientPhone) {
        editName = patientName
        editPhone = patientPhone
    }

    LaunchedEffect(patientProfile) {
        patientProfile?.let {
            editEmail = it.email ?: ""
            editDob = it.date_of_birth ?: ""
            editGender = it.gender ?: ""
            editBloodGroup = it.blood_group ?: ""
            editAllergies = it.allergies ?: ""
            editConditions = it.conditions ?: ""
            editMedications = it.current_medications ?: ""
            editAddress = it.address ?: ""
            editEmergencyName = it.emergency_contact_name ?: ""
            editEmergencyPhone = it.emergency_contact_phone ?: ""
        }
    }

    LaunchedEffect(currentRole) {
        viewModel.fetchUserProfile(currentRole)
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
                        onClick = { viewModel.logout() },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Log Out",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (isProfileLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Spacer(modifier = Modifier.height(2.dp))
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
                                    UserRole.PATIENT -> {
                                        val pName = patientProfile?.let { "${it.first_name} ${it.last_name}" } ?: patientName
                                        if (pName.isNotEmpty()) pName.first().toString() else "P"
                                    }
                                    UserRole.DOCTOR -> {
                                        val dName = doctorProfile?.name ?: currentDoctor?.name ?: "Dr. Rahul Verma"
                                        dName.split(" ").lastOrNull()?.first()?.toString() ?: "D"
                                    }
                                    UserRole.RECEPTIONIST -> {
                                        val rName = receptionistProfile?.let { "${it.first_name} ${it.last_name}" } ?: "Neha Sharma"
                                        if (rName.isNotEmpty()) rName.first().toString() else "R"
                                    }
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
                                        UserRole.PATIENT -> patientProfile?.let { "${it.first_name} ${it.last_name}" } ?: patientName
                                        UserRole.DOCTOR -> doctorProfile?.name ?: currentDoctor?.name ?: "Dr. Rahul Verma"
                                        UserRole.RECEPTIONIST -> receptionistProfile?.let { "${it.first_name} ${it.last_name}" } ?: "Neha Sharma"
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
                                            UserRole.PATIENT -> "Patient ID: ${patientProfile?.id?.take(8)?.uppercase() ?: "PT0001"}"
                                            UserRole.DOCTOR -> "Doctor ID: ${doctorProfile?.id?.take(8)?.uppercase() ?: "DR0001"}"
                                            UserRole.RECEPTIONIST -> "Employee ID: ${receptionistProfile?.employee_number ?: "RC0001"}"
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
                                                editEmail = patientProfile?.email ?: ""
                                                editDob = patientProfile?.date_of_birth ?: ""
                                                editGender = patientProfile?.gender ?: ""
                                                editBloodGroup = patientProfile?.blood_group ?: ""
                                                editAllergies = patientProfile?.allergies ?: ""
                                                editConditions = patientProfile?.conditions ?: ""
                                                editMedications = patientProfile?.current_medications ?: ""
                                                editAddress = patientProfile?.address ?: ""
                                                editEmergencyName = patientProfile?.emergency_contact_name ?: ""
                                                editEmergencyPhone = patientProfile?.emergency_contact_phone ?: ""
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
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = editEmail,
                                        onValueChange = { editEmail = it },
                                        label = { Text("Email Address") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = editDob,
                                        onValueChange = { editDob = it },
                                        label = { Text("Date of Birth (YYYY-MM-DD)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = editGender,
                                        onValueChange = { editGender = it },
                                        label = { Text("Gender") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = editBloodGroup,
                                        onValueChange = { editBloodGroup = it },
                                        label = { Text("Blood Group") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = editAllergies,
                                        onValueChange = { editAllergies = it },
                                        label = { Text("Allergies") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = editConditions,
                                        onValueChange = { editConditions = it },
                                        label = { Text("Chronic Conditions") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = editMedications,
                                        onValueChange = { editMedications = it },
                                        label = { Text("Current Medications") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = editAddress,
                                        onValueChange = { editAddress = it },
                                        label = { Text("Address") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = editEmergencyName,
                                        onValueChange = { editEmergencyName = it },
                                        label = { Text("Emergency Contact Name") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = editEmergencyPhone,
                                        onValueChange = { editEmergencyPhone = it },
                                        label = { Text("Emergency Contact Phone") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = {
                                            val nameParts = editName.trim().split(" ")
                                            val first = nameParts.getOrNull(0) ?: ""
                                            val last = nameParts.drop(1).joinToString(" ")
                                            viewModel.updatePatientProfile(
                                                firstName = first,
                                                lastName = last,
                                                email = editEmail,
                                                newPhone = editPhone,
                                                dateOfBirth = editDob,
                                                gender = editGender,
                                                bloodGroup = editBloodGroup,
                                                allergies = editAllergies,
                                                conditions = editConditions,
                                                currentMedications = editMedications,
                                                address = editAddress,
                                                emergencyContactName = editEmergencyName,
                                                emergencyContactPhone = editEmergencyPhone
                                            )
                                            isEditingPatient = false
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Save Changes", fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        ProfileDetailRow(Icons.Outlined.CalendarToday, "Date of Birth", if (patientProfile?.date_of_birth.isNullOrBlank()) "Not set" else patientProfile?.date_of_birth!!)
                                        ProfileDetailRow(Icons.Outlined.Person, "Gender", if (patientProfile?.gender.isNullOrBlank()) "Not set" else patientProfile?.gender!!)
                                        ProfileDetailRow(Icons.Outlined.Phone, "Phone Number", patientProfile?.phone ?: patientPhone)
                                        ProfileDetailRow(Icons.Outlined.Email, "Email", if (patientProfile?.email.isNullOrBlank()) "Not set" else patientProfile?.email!!)
                                        ProfileDetailRow(Icons.Outlined.Home, "Address", if (patientProfile?.address.isNullOrBlank()) "Not set" else patientProfile?.address!!)
                                        ProfileDetailRow(Icons.Outlined.Phone, "Emergency Contact", 
                                            if (patientProfile?.emergency_contact_name != null) 
                                                "${patientProfile?.emergency_contact_name} (${patientProfile?.emergency_contact_phone ?: ""})" 
                                            else "Not set"
                                        )
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
                                    InfoMiniCard("Blood Group", if (patientProfile?.blood_group.isNullOrBlank()) "Not set" else patientProfile?.blood_group!!, modifier = Modifier.weight(1f))
                                    InfoMiniCard("Allergies", if (patientProfile?.allergies.isNullOrBlank()) "Not set" else patientProfile?.allergies!!, modifier = Modifier.weight(1f))
                                    InfoMiniCard("Conditions", if (patientProfile?.conditions.isNullOrBlank()) "Not set" else patientProfile?.conditions!!, modifier = Modifier.weight(1f))
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
                                    ProfileDetailRow(Icons.Outlined.CalendarToday, "Member Since", patientProfile?.created_at ?: "12 Jan 2024")
                                    ProfileDetailRow(Icons.Outlined.Home, "Saved Hospitals", "${patientProfile?.savedHospitalsCount ?: 5} Hospitals")
                                    ProfileDetailRow(Icons.Outlined.Assignment, "Appointment History", "${patientProfile?.appointmentCount ?: 18} Appointments")
                                }
                            }
                        }
                    }

                    UserRole.DOCTOR -> {
                        // Availability Toggle & Schedule
                        item {
                            val isDocAvailable = doctorProfile?.isAvailable ?: (currentDoctor?.isAvailable == true)
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
                                                    .background(if (isDocAvailable) SuccessGreen else ErrorRed)
                                            )
                                            Text(
                                                text = if (isDocAvailable) "Available" else "Off Duty",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDocAvailable) SuccessGreen else ErrorRed
                                            )
                                        }
                                    }

                                    Switch(
                                        checked = isDocAvailable,
                                        onCheckedChange = {
                                            val docId = doctorProfile?.id ?: currentDoctor?.id
                                            if (docId != null) {
                                                viewModel.toggleDoctorAvailability(docId, it)
                                            }
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    ProfileDetailRow(Icons.Outlined.CalendarToday, "Working Days", doctorProfile?.working_days ?: "Mon - Sat")
                                    ProfileDetailRow(Icons.Outlined.Schedule, "Consultation Hours", doctorProfile?.consultation_hours ?: "09:00 AM - 05:00 PM")
                                    ProfileDetailRow(Icons.Outlined.Timer, "Appointment Duration", doctorProfile?.appointment_duration ?: "15 mins per patient")
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
                                    ProfileDetailRow(Icons.Outlined.Star, "Specialization", doctorProfile?.specialization ?: "Cardiologist")
                                    ProfileDetailRow(Icons.Outlined.Home, "Department", doctorProfile?.department_name ?: "Cardiology")
                                    ProfileDetailRow(Icons.Outlined.Assignment, "Qualification", doctorProfile?.qualification ?: "MBBS, MD (Cardiology)")
                                    ProfileDetailRow(Icons.Outlined.Timer, "Experience", doctorProfile?.experience ?: "10+ Years")
                                    ProfileDetailRow(Icons.Outlined.AttachMoney, "Consultation Fee", "₹${doctorProfile?.consultation_fee?.toInt() ?: 800}")
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
                                    ProfileDetailRow(Icons.Outlined.Home, "Hospital", doctorProfile?.hospital_name ?: "City Care Hospital")
                                    ProfileDetailRow(Icons.Outlined.Assignment, "Room / Cabin", doctorProfile?.room_cabin ?: "Cardiology OPD - 204")
                                    ProfileDetailRow(Icons.Outlined.Phone, "Phone Number", doctorProfile?.phone ?: "+91 98765 43210")
                                    ProfileDetailRow(Icons.Outlined.Email, "Email", doctorProfile?.email ?: "rahul.verma@eto.com")
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
                                    ProfileDetailRow(Icons.Outlined.Home, "Hospital", receptionistProfile?.hospital_name ?: "City Care Hospital")
                                    ProfileDetailRow(Icons.Outlined.Person, "Department", receptionistProfile?.department_name ?: "Front Desk")
                                    ProfileDetailRow(Icons.Outlined.Assignment, "Designation", receptionistProfile?.designation ?: "Senior Receptionist")
                                    ProfileDetailRow(Icons.Outlined.Schedule, "Shift", receptionistProfile?.shift ?: "Morning Shift")
                                    ProfileDetailRow(Icons.Outlined.CalendarToday, "Working Days", receptionistProfile?.working_days ?: "Mon - Sat")
                                    ProfileDetailRow(Icons.Outlined.Timer, "Working Hours", receptionistProfile?.working_hours ?: "08:00 AM - 04:00 PM")
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
