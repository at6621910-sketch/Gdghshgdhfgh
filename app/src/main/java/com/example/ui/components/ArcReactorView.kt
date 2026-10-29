package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AssistantState
import com.example.ui.theme.JarvisAccentGreen
import com.example.ui.theme.JarvisAccentRed
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanDark
import com.example.ui.theme.JarvisCyanLight
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArcReactorView(
    state: AssistantState,
    audioLevel: Float,
    isSpeaking: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "reactor_anim")

    // Slow continuous rotation for outer ring
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_rot"
    )

    // Reverse rotation for inner accents
    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "inner_rot"
    )

    // Breathing pulse
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (state == AssistantState.LISTENING || isSpeaking) 700 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    val activeColor = when (state) {
        AssistantState.LISTENING -> JarvisAccentGreen
        AssistantState.THINKING -> JarvisCyanLight
        AssistantState.SPEAKING -> JarvisCyan
        AssistantState.STOPPED -> Color(0xFF6C87A3)
        AssistantState.ERROR -> JarvisAccentRed
        AssistantState.READY -> JarvisCyan
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(170.dp)
                .clip(CircleShape)
                .testTag("arc_reactor_core")
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = activeColor, bounded = false),
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(160.dp)) {
                val center = Offset(size.width / 2, size.height / 2)
                val baseRadius = (size.minDimension / 2) - 8.dp.toPx()
                val pulseRadius = baseRadius * breathScale * (1f + (audioLevel * 0.12f))

                // 1. Soft Ambient Radial Glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            activeColor.copy(alpha = 0.35f),
                            activeColor.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = pulseRadius * 1.25f
                    ),
                    radius = pulseRadius * 1.25f,
                    center = center
                )

                // 2. Outer segmented tech arcs
                val arcStroke = 2.5.dp.toPx()
                for (i in 0 until 4) {
                    val startAngle = outerRotation + (i * 90f) + 10f
                    drawArc(
                        color = activeColor.copy(alpha = 0.7f),
                        startAngle = startAngle,
                        sweepAngle = 70f,
                        useCenter = false,
                        topLeft = Offset(center.x - pulseRadius, center.y - pulseRadius),
                        size = Size(pulseRadius * 2, pulseRadius * 2),
                        style = Stroke(width = arcStroke, cap = StrokeCap.Round)
                    )
                }

                // 3. Middle circular boundary
                val midRadius = pulseRadius * 0.82f
                drawCircle(
                    color = activeColor,
                    radius = midRadius,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )

                // 4. Inner dashed spokes
                val innerRadius = midRadius * 0.74f
                val spokeCount = 12
                for (i in 0 until spokeCount) {
                    val angleRad = Math.toRadians((innerRotation + (i * (360f / spokeCount))).toDouble())
                    val x1 = center.x + (innerRadius * 0.72f * cos(angleRad)).toFloat()
                    val y1 = center.y + (innerRadius * 0.72f * sin(angleRad)).toFloat()
                    val x2 = center.x + (innerRadius * cos(angleRad)).toFloat()
                    val y2 = center.y + (innerRadius * sin(angleRad)).toFloat()
                    drawLine(
                        color = JarvisCyanLight.copy(alpha = 0.6f),
                        start = Offset(x1, y1),
                        end = Offset(x2, y2),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // 5. Innermost reactor core circle
                val coreRadius = innerRadius * 0.55f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White,
                            activeColor,
                            JarvisCyanDark
                        ),
                        center = center,
                        radius = coreRadius
                    ),
                    radius = coreRadius,
                    center = center
                )

                // 6. Center highlight ring
                drawCircle(
                    color = Color.White.copy(alpha = 0.85f),
                    radius = coreRadius * 0.4f,
                    center = center
                )
            }

            // Central sci-fi symbol "◉"
            Text(
                text = "◉",
                color = Color(0xFF07111F),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Audio Frequency Equalizer Bars (active when listening or speaking)
        if (state == AssistantState.LISTENING || isSpeaking) {
            AudioWaveEqualizer(color = activeColor, audioLevel = audioLevel)
        }
    }
}

@Composable
fun AudioWaveEqualizer(color: Color, audioLevel: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_anim")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_offset"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        val barCount = 9
        for (i in 0 until barCount) {
            val distFromCenter = kotlin.math.abs(i - (barCount / 2))
            val baseHeight = 24.dp - (distFromCenter * 3).dp
            val dynamicHeight = (baseHeight * (0.4f + (audioLevel * 0.6f) + (waveOffset * 0.3f))).coerceIn(4.dp, 28.dp)

            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .width(3.dp)
                    .height(dynamicHeight)
                    .clip(CircleShape)
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawRect(color = color)
                }
            }
        }
    }
}
