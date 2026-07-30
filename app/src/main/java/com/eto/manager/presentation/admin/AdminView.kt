package com.eto.manager.presentation.admin

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eto.manager.domain.model.TokenStatus
import com.eto.manager.presentation.EtoViewModel
import com.eto.manager.presentation.components.MetricCard
import com.eto.manager.presentation.components.SectionHeader
import com.eto.manager.presentation.components.SpotlightCard
import com.eto.manager.presentation.components.bounceClick
import com.eto.manager.presentation.components.glassmorphicCard
import com.eto.manager.presentation.components.magnetEffect
import com.eto.manager.presentation.theme.*

@Composable
fun AdminView(
    viewModel: EtoViewModel, 
    activeTab: Int, 
    modifier: Modifier = Modifier
) {
    val doctors by viewModel.doctors.collectAsState()
    val departments by viewModel.departments.collectAsState()
    val tokens by viewModel.tokens.collectAsState()
    val isSimulating by viewModel.isSimulationActive.collectAsState()

    // Calculate core metrics
    val totalTokens = tokens.size
    val completedTokens = tokens.filter { it.status == TokenStatus.COMPLETED }
    val totalConsults = completedTokens.size
    val totalRevenue = completedTokens.sumOf { it.billAmount }
    val averageWait = if (completedTokens.isNotEmpty()) completedTokens.map { it.estimatedWaitMinutes }.average().toInt() else 0

    val textMeasurer = rememberTextMeasurer()
    val isDark = MaterialTheme.colorScheme.background == DarkBgStart

    // Entrance Animation trigger
    var startChartAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        startChartAnimation = true
    }
    val chartEntranceProgress by animateFloatAsState(
        targetValue = if (startChartAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1500),
        label = "chartEntrance"
    )

    // Touch coordinate tracking for Spline Chart Tooltips
    var selectedNodeIndex by remember { mutableStateOf<Int?>(null) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    val dataValues = remember(tokens) {
        val hourCounts = IntArray(7) { 0 }
        tokens.forEach { token ->
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = token.createdAt }
            val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
            val slot = when (hour) {
                9 -> 0
                10 -> 1
                11 -> 2
                12 -> 3
                13 -> 4
                14 -> 5
                15 -> 6
                else -> (token.id % 7).toInt() // distribute other data
            }
            hourCounts[slot]++
        }
        hourCounts.map { it.toFloat() + 5f }
    }
    val maxVal = remember(dataValues) {
        (dataValues.maxOrNull() ?: 10f).coerceAtLeast(10f)
    }
    val points = remember(canvasSize, dataValues, maxVal) {
        if (canvasSize.width == 0f || canvasSize.height == 0f) emptyList()
        else {
            val paddingX = 40f
            val paddingY = 40f
            val chartW = canvasSize.width - paddingX * 2
            val chartH = canvasSize.height - paddingY * 2
            dataValues.mapIndexed { index, value ->
                Offset(
                    x = paddingX + chartW * index / (dataValues.size - 1),
                    y = paddingY + chartH * (1f - value / maxVal)
                )
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        when (activeTab) {
            0 -> { // HOME TAB: Analytics Overview
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        SectionHeader("Hospital Queue Analytics")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MetricCard("Total Tokens", "$totalTokens", modifier = Modifier.weight(1f))
                            MetricCard("Completed", "$totalConsults", modifier = Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MetricCard("Total Revenue", "₹${totalRevenue.toInt()}", modifier = Modifier.weight(1f))
                            MetricCard("Avg Wait Time", "${averageWait}m", modifier = Modifier.weight(1f))
                        }
                    }

                    // Interactive Spline Chart Canvas
                    item {
                        SpotlightCard(modifier = Modifier.fillMaxWidth()) {
                            Text("Patient Flow Trend (Hourly)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("Tap on points to view details", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(16.dp))

                            val chartColor = MaterialTheme.colorScheme.primary
                            val onSurfaceColor = MaterialTheme.colorScheme.onSurface

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                            ) {
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .onSizeChanged { canvasSize = Size(it.width.toFloat(), it.height.toFloat()) }
                                        .pointerInput(points) {
                                            detectTapGestures { offset ->
                                                var closestIndex = -1
                                                var minDistance = Float.MAX_VALUE
                                                points.forEachIndexed { index, point ->
                                                    val dist = Math.abs(point.x - offset.x)
                                                    if (dist < minDistance && dist < 50f) {
                                                        minDistance = dist
                                                        closestIndex = index
                                                    }
                                                }
                                                selectedNodeIndex = if (closestIndex != -1) closestIndex else null
                                            }
                                        }
                                ) {
                                    val peakVal = dataValues.maxOrNull()
                                    val labels = dataValues.map { valF ->
                                        val count = (valF - 5f).toInt()
                                        if (valF == peakVal && count > 0) "$count Patients (Peak)" else "$count Patients"
                                    }
                                    val times = listOf("9 AM", "10 AM", "11 AM", "12 PM", "1 PM", "2 PM", "3 PM")

                                    // Draw Grid Lines
                                    val gridLineCount = 4
                                    for (i in 0..gridLineCount) {
                                        val y = size.height * i / gridLineCount
                                        drawLine(
                                            color = onSurfaceColor.copy(alpha = 0.08f),
                                            start = Offset(0f, y),
                                            end = Offset(size.width, y),
                                            strokeWidth = 1f
                                        )
                                    }

                                    // Draw Spline curve
                                    if (points.isNotEmpty()) {
                                        val path = Path().apply {
                                            moveTo(points[0].x, points[0].y)
                                            for (i in 1 until points.size) {
                                                val current = points[i]
                                                val previous = points[i - 1]
                                                val controlX = (previous.x + current.x) / 2
                                                val endX = previous.x + (current.x - previous.x) * chartEntranceProgress
                                                val endY = previous.y + (current.y - previous.y) * chartEntranceProgress
                                                cubicTo(controlX, previous.y, controlX, endY, endX, endY)
                                            }
                                        }

                                        drawPath(
                                            path = path,
                                            color = chartColor,
                                            style = Stroke(width = 6f)
                                        )
                                    }

                                    // Draw node markers
                                    points.forEachIndexed { idx, point ->
                                        if (point.x <= size.width * chartEntranceProgress + 50f) {
                                            val isSelected = selectedNodeIndex == idx
                                            drawCircle(
                                                color = if (isSelected) chartColor else chartColor.copy(alpha = 0.8f),
                                                radius = if (isSelected) 10f else 6f,
                                                center = point
                                            )
                                            drawCircle(
                                                color = Color.White,
                                                radius = if (isSelected) 5f else 3f,
                                                center = point
                                            )
                                        }
                                    }

                                    // Draw interactive tooltip popup
                                    selectedNodeIndex?.let { idx ->
                                        val node = points[idx]
                                        drawLine(
                                            color = chartColor.copy(alpha = 0.3f),
                                            start = Offset(node.x, 0f),
                                            end = Offset(node.x, size.height),
                                            strokeWidth = 2f,
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                        )

                                        val tooltipText = "${times[idx]}: ${labels[idx]}"
                                        val textLayoutResult = textMeasurer.measure(
                                            text = tooltipText,
                                            style = TextStyle(
                                                color = if (isDark) DarkTextPrimary else LightTextPrimary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )

                                        val padX = 16f
                                        val padY = 8f
                                        val tooltipW = textLayoutResult.size.width + padX * 2
                                        val tooltipH = textLayoutResult.size.height + padY * 2
                                        val tooltipX = (node.x - tooltipW / 2).coerceIn(10f, size.width - tooltipW - 10f)
                                        val tooltipY = (node.y - tooltipH - 20f).coerceAtLeast(10f)

                                        drawRoundRect(
                                            color = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                                            topLeft = Offset(tooltipX, tooltipY),
                                            size = Size(tooltipW, tooltipH),
                                            cornerRadius = CornerRadius(8f, 8f)
                                        )

                                        drawText(
                                            textLayoutResult = textLayoutResult,
                                            topLeft = Offset(tooltipX + padX, tooltipY + padY)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("9 AM", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("11 AM", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("1 PM", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("3 PM", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }

            1 -> { // QUEUE TAB: Queue settings / simulation
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Simulation Settings",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                    }

                    item {
                        SpotlightCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Simulated Queue Ticker", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Progress active queues automatically in background every 20 seconds.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = isSimulating,
                                    onCheckedChange = { viewModel.toggleSimulation() },
                                    colors = SwitchDefaults.colors(checkedThumbColor = SuccessGreen),
                                    modifier = Modifier.bounceClick()
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Clear Session Logs", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ErrorRed)
                                    Text("Flush all active appointments, tokens, invoice records, and phone alerts.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Button(
                                    onClick = { viewModel.clearAllData() },
                                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                                    modifier = Modifier
                                        .bounceClick()
                                        .magnetEffect(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reset Logs", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }

            2 -> { // HISTORY TAB: Hospital Directory
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Clinic Directory",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                    }

                    item {
                        SpotlightCard(modifier = Modifier.fillMaxWidth()) {
                            Text("Active Departments (${departments.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(6.dp))
                            departments.forEach { dept ->
                                Text("• ${dept.name} - ${dept.description}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Text("Registered Doctors (${doctors.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(6.dp))
                            doctors.forEach { doc ->
                                Text("• ${doc.name} (${doc.specialty})", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }

            3 -> { // PROFILE TAB: Admin info
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Admin Configuration",
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
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.AdminPanelSettings,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        "Hospital Administrator",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "ETO Manager Console v1.0.0",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .glassmorphicCard(isDark, cornerRadius = 16.dp)
                                    .padding(16.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Clinic ID: CLINIC-ETO-75892", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                                    Text("Connection Mode: Local SQLite Database", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Mock Simulator Status: Background Ticker Active", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
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
