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
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.MedicalServices
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
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.LocalHospital,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "ETO Online",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            )
                        }
                    },
                    actions = {
                        // Dynamic light/dark theme switcher
                        IconButton(
                            onClick = onThemeToggle,
                            modifier = Modifier.bounceClick()
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                                contentDescription = "Toggle Theme",
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Role selection dropdown badge
                        Box(modifier = Modifier.padding(end = 12.dp)) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .bounceClick()
                                    .clickable { menuExpanded = true }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                val icon = when (currentRole) {
                                    UserRole.PATIENT -> Icons.Outlined.Person
                                    UserRole.RECEPTIONIST -> Icons.Outlined.SupportAgent
                                    UserRole.DOCTOR -> Icons.Outlined.MedicalServices
                                    UserRole.ADMIN -> Icons.Outlined.AdminPanelSettings
                                }
                                Icon(
                                    imageVector = icon, 
                                    contentDescription = null, 
                                    tint = MaterialTheme.colorScheme.primary, 
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = currentRole.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ExpandMore, 
                                    contentDescription = null, 
                                    tint = MaterialTheme.colorScheme.primary, 
                                    modifier = Modifier.size(16.dp)
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
                                            viewModel.setRole(role)
                                            menuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            bottomBar = {
                BottomFloatingNavBar(
                    activeTab = activeTab,
                    onTabSelected = { activeTab = it }
                )
            },
            floatingActionButton = {
                // SMS Virtual Alerts drawer trigger floating badge
                Box(
                    modifier = Modifier.padding(bottom = 80.dp) // Float above navigation bar
                ) {
                    FloatingActionButton(
                        onClick = { showPhoneSimulator = !showPhoneSimulator },
                        modifier = Modifier
                            .bounceClick()
                            .magnetEffect(),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "SMS Alerts Manager"
                            )
                            if (notifications.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 2.dp, end = 2.dp)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color.Red)
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Content Switcher dependent on active role AND active bottom tab
                when (currentRole) {
                    UserRole.PATIENT -> PatientView(viewModel, activeTab)
                    UserRole.RECEPTIONIST -> ReceptionistView(viewModel, activeTab)
                    UserRole.DOCTOR -> DoctorView(viewModel, activeTab)
                    UserRole.ADMIN -> AdminView(viewModel, activeTab)
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
fun BottomFloatingNavBar(
    activeTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background == DarkBgStart
    val items = listOf(
        Triple("Home", Icons.Outlined.Home, 0),
        Triple("Queue", Icons.Outlined.HourglassEmpty, 1),
        Triple("History", Icons.Outlined.ReceiptLong, 2),
        Triple("Profile", Icons.Outlined.Person, 3)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .glassmorphicCard(isDark, cornerRadius = 32.dp)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { (label, icon, index) ->
                val isActive = activeTab == index
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
