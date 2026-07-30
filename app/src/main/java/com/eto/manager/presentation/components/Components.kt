package com.eto.manager.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eto.manager.domain.model.TokenStatus
import com.eto.manager.presentation.theme.*
import kotlinx.coroutines.launch

// --- Glassmorphic Modifiers ---

/**
 * Glassmorphic Card Background & Border & Shadow
 */
fun Modifier.glassmorphicCard(isDark: Boolean, cornerRadius: Dp = 28.dp): Modifier = composed {
    val cardBg = if (isDark) DarkCardBg else LightCardBg
    val borderColor = if (isDark) DarkCardBorder else LightCardBorder
    val shadowColor = if (isDark) Color(0x33000000) else Color(0x0F0F172A)
    
    this
        .shadow(
            elevation = 10.dp,
            shape = RoundedCornerShape(cornerRadius),
            clip = false,
            ambientColor = shadowColor,
            spotColor = shadowColor
        )
        .background(
            color = cardBg,
            shape = RoundedCornerShape(cornerRadius)
        )
        .border(
            width = 1.dp,
            color = borderColor,
            shape = RoundedCornerShape(cornerRadius)
        )
}

// --- ReactBits Ports: Modifiers ---

/**
 * Magnet Effect: Item moves towards drag gesture and springs back on release.
 */
fun Modifier.magnetEffect(): Modifier = this

/**
 * Bounce Click: Scales down slightly on press and rebounds on release.
 */
fun Modifier.bounceClick(): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "scale"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    tryAwaitRelease()
                    isPressed = false
                }
            )
        }
}

/**
 * Shimmer Modifier: Adds animated loading skeleton waves.
 */
fun Modifier.shimmer(): Modifier = composed {
    val isDark = MaterialTheme.colorScheme.background == DarkBgStart
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = -300f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    
    val baseColor = if (isDark) Color(0x331E293B) else Color(0x33E2E8F0)
    val shimmerColors = listOf(
        baseColor.copy(alpha = 0.8f),
        baseColor.copy(alpha = 0.3f),
        baseColor.copy(alpha = 0.8f),
    )
    
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim, translateAnim),
        end = Offset(translateAnim + 250f, translateAnim + 250f)
    )
    
    this.background(brush = brush)
}

// --- ReactBits Ports: Components ---

/**
 * ShinyText: Text displaying a progressive shimmer glow.
 */
@Composable
fun ShinyText(
    text: String,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "shiny")
    val translateAnim by transition.animateFloat(
        initialValue = -500f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shiny"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val softColor = if (MaterialTheme.colorScheme.background == DarkBgStart) DarkTextPrimary else LightTextSecondary
    
    val brush = Brush.linearGradient(
        colors = listOf(
            primaryColor,
            softColor,
            primaryColor
        ),
        start = Offset(translateAnim, 0f),
        end = Offset(translateAnim + 250f, 0f)
    )

    Text(
        text = text,
        style = style.copy(brush = brush),
        modifier = modifier
    )
}

/**
 * SpotlightCard: Card rendering a radial cursor/touch gradient glow.
 */
@Composable
fun SpotlightCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background == DarkBgStart
    var touchPosition by remember { mutableStateOf(Offset.Unspecified) }
    val spotlightRadius = 160.dp
    val spotlightColor = if (isDark) {
        DarkPrimaryBlue.copy(alpha = 0.12f)
    } else {
        LightPrimaryBlue.copy(alpha = 0.08f)
    }

    Box(
        modifier = modifier
            .glassmorphicCard(isDark, cornerRadius)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull()
                        if (change != null) {
                            touchPosition = if (change.pressed) {
                                change.position
                            } else {
                                Offset.Unspecified
                            }
                        }
                    }
                }
            }
            .drawWithContent {
                drawContent()
                if (touchPosition != Offset.Unspecified) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(spotlightColor, Color.Transparent),
                            center = touchPosition,
                            radius = spotlightRadius.toPx()
                        ),
                        radius = spotlightRadius.toPx(),
                        center = touchPosition
                    )
                }
            }
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            content()
        }
    }
}

/**
 * EmptyState: Premium empty screen indicator.
 */
@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background == DarkBgStart
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .graphicsLayer(scaleX = scale, scaleY = scale)
                .clip(CircleShape)
                .background(if (isDark) DarkSoftBlue else LightSoftBlue),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
    }
}

// --- Standard Shared Widgets ---

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
        ),
        modifier = modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun StatusBadge(status: TokenStatus) {
    val isDark = MaterialTheme.colorScheme.background == DarkBgStart
    val (text, bgColor, textColor) = when (status) {
        TokenStatus.PENDING -> {
            if (isDark) Triple("Pending", DarkWarningBg, DarkWarningText)
            else Triple("Pending", LightWarningBg, LightWarningText)
        }
        TokenStatus.APPROVED -> {
            if (isDark) Triple("Approved", Color(0x26818CF8), Color(0xFF818CF8))
            else Triple("Approved", Color(0xFFEEF2FF), Color(0xFF4F46E5))
        }
        TokenStatus.SERVING -> {
            if (isDark) Triple("Serving", DarkSuccessBg, DarkSuccessText)
            else Triple("Serving", LightSuccessBg, LightSuccessText)
        }
        TokenStatus.COMPLETED -> {
            if (isDark) Triple("Completed", DarkSoftBlue, DarkPrimaryBlue)
            else Triple("Completed", LightSoftBlue, LightPrimaryBlue)
        }
        TokenStatus.SKIPPED -> {
            if (isDark) Triple("Skipped", DarkErrorBg, DarkErrorText)
            else Triple("Skipped", LightErrorBg, LightErrorText)
        }
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun MetricCard(
    label: String,
    value: String,
    subValue: String = "",
    modifier: Modifier = Modifier
) {
    SpotlightCard(modifier = modifier, cornerRadius = 24.dp) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (subValue.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subValue,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun LivePhoneAlert(
    title: String,
    message: String,
    type: String,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background == DarkBgStart
    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassmorphicCard(isDark, cornerRadius = 16.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = type,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = message,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
