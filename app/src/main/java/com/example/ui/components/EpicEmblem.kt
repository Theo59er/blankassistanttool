package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGold
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple

@Composable
fun EpicEmblem(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    isActive: Boolean = true,
    isDreaming: Boolean = false,
    iconColor: Color = NeonCyan
) {
    val infiniteTransition = rememberInfiniteTransition(label = "emblem_anim")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isDreaming) 8000 else 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val counterRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isDreaming) 10000 else 5500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "counter_rotation"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isDreaming) 2400 else 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val ringColor1 = if (isDreaming) NeonPurple else NeonCyan
    val ringColor2 = if (isDreaming) NeonPink else NeonPurple
    val ringColor3 = if (isDreaming) NeonGold else NeonPink

    Box(
        modifier = modifier
            .size(size)
            .scale(if (isActive) pulseScale else 1f),
        contentAlignment = Alignment.Center
    ) {
        // Futuristic Rotating Cyber Rings
        Canvas(modifier = Modifier.fillMaxSize().rotate(rotation)) {
            val strokeWidth = size.toPx() * 0.05f
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(ringColor1, ringColor2, Color.Transparent, ringColor1)
                ),
                startAngle = 0f,
                sweepAngle = 240f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Canvas(
            modifier = Modifier
                .size(size * 0.78f)
                .rotate(counterRotation)
        ) {
            val strokeWidth = size.toPx() * 0.04f
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(ringColor3, ringColor2, Color.Transparent, ringColor3)
                ),
                startAngle = 90f,
                sweepAngle = 180f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Inner glowing core
        Box(
            modifier = Modifier
                .size(size * 0.58f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            (if (isDreaming) NeonPurple else NeonCyan).copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isDreaming) Icons.Default.Bedtime else Icons.Default.Psychology,
                contentDescription = "KI Emblem",
                tint = iconColor,
                modifier = Modifier.size(size * 0.38f)
            )
        }
    }
}
