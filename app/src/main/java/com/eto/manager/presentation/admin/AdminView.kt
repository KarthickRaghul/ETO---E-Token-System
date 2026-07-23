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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
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
import com.eto.manager.presentation.components.magnetEffect
import com.eto.manager.presentation.theme.ErrorRed
import com.eto.manager.presentation.theme.SuccessGreen

@Composable
fun AdminView(viewModel: EtoViewModel, modifier: Modifier = Modifier) {
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

    // Text Measurer for Canvas text drawings
    val textMeasurer = rememberTextMeasurer()

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

    val dataValues = listOf(12f, 38f, 27f, 19f, 6f, 32f, 21f)
    val maxVal = 45f
    val points = remember(canvasSize) {
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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stats Cards Grid (utilizing Refactored MetricCards that use SpotlightCard)
        item {
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

        // Custom Analytics Interactive Canvas Graph
        item {
            SpotlightCard(modifier = Modifier.fillMaxWidth()) {
                Text("Patient Flow Trend (Hourly)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Tap on points to view details", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))
                
                val chartColor = MaterialTheme.colorScheme.primary
                val accentColor = MaterialTheme.colorScheme.secondary

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
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

                        val labels = listOf("12 Patients", "38 Patients (Peak)", "27 Patients", "19 Patients", "6 Patients", "32 Patients", "21 Patients")
                        val times = listOf("9 AM", "10 AM", "11 AM", "12 PM", "1 PM", "2 PM", "3 PM")

                        // Draw Grid Horizontal Lines
                        val gridLineCount = 4
                        for (i in 0..gridLineCount) {
                            val y = size.height * i / gridLineCount
                            drawLine(
                                color = Color.Gray.copy(alpha = 0.15f),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1f
                            )
                        }

                        // Generate spline path based on animation progress
                        if (points.isNotEmpty()) {
                            val path = Path().apply {
                                moveTo(points[0].x, points[0].y)
                                for (i in 1 until points.size) {
                                    val current = points[i]
                                    val previous = points[i - 1]
                                    val controlX = (previous.x + current.x) / 2
                                    
                                    // Stagger drawing horizontally with chartEntranceProgress
                                    val endX = previous.x + (current.x - previous.x) * chartEntranceProgress
                                    val endY = previous.y + (current.y - previous.y) * chartEntranceProgress
                                    
                                    cubicTo(controlX, previous.y, controlX, endY, endX, endY)
                                }
                            }

                            // Draw Spline
                            drawPath(
                                path = path,
                                color = chartColor,
                                style = Stroke(width = 6f)
                            )
                        }

                        // Draw Markers
                        points.forEachIndexed { idx, point ->
                            // Only render nodes that the entry animation has reached
                            if (point.x <= size.width * chartEntranceProgress + 50f) {
                                val isSelected = selectedNodeIndex == idx
                                drawCircle(
                                    color = if (isSelected) accentColor else chartColor,
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

                        // Draw Active Tooltip Box & Guides
                        selectedNodeIndex?.let { idx ->
                            val node = points[idx]
                            
                            // Dotted vertical line guide
                            drawLine(
                                color = accentColor.copy(alpha = 0.5f),
                                start = Offset(node.x, 0f),
                                end = Offset(node.x, size.height),
                                strokeWidth = 2f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )

                            // Measure text
                            val tooltipText = "${times[idx]}: ${labels[idx]}"
                            val textLayoutResult = textMeasurer.measure(
                                text = tooltipText,
                                style = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            )

                            val paddingX = 16f
                            val paddingY = 8f
                            val tooltipW = textLayoutResult.size.width + paddingX * 2
                            val tooltipH = textLayoutResult.size.height + paddingY * 2
                            val tooltipX = (node.x - tooltipW / 2).coerceIn(10f, size.width - tooltipW - 10f)
                            val tooltipY = (node.y - tooltipH - 20f).coerceAtLeast(10f)

                            // Draw Tooltip container
                            drawRoundRect(
                                color = Color.Black.copy(alpha = 0.85f),
                                topLeft = Offset(tooltipX, tooltipY),
                                size = Size(tooltipW, tooltipH),
                                cornerRadius = CornerRadius(8f, 8f)
                            )

                            // Draw Tooltip text
                            drawText(
                                textLayoutResult = textLayoutResult,
                                topLeft = Offset(tooltipX + paddingX, tooltipY + paddingY)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
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

        // Settings / Config controls
        item {
            SectionHeader("Queue Settings")
            SpotlightCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Simulated Queue Ticker", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Simulate patients calling and queue movements automatically in the background.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = isSimulating,
                        onCheckedChange = { viewModel.toggleSimulation() },
                        colors = SwitchDefaults.colors(checkedThumbColor = SuccessGreen),
                        modifier = Modifier.bounceClick()
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Clear Session Database", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ErrorRed)
                        Text("Reset all active appointments, tokens, diagnoses, and notification histories.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = { viewModel.clearAllData() },
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                        modifier = Modifier.bounceClick().magnetEffect()
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset")
                    }
                }
            }
        }

        // Directory overview lists
        item {
            SectionHeader("Hospital Directory")
            SpotlightCard(modifier = Modifier.fillMaxWidth()) {
                Text("Active Departments (${departments.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                departments.forEach { dept ->
                    Text("• ${dept.name} - ${dept.description}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Active Doctors (${doctors.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                doctors.forEach { doc ->
                    Text("• ${doc.name} (${doc.specialty})", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
