package com.example.DhoonHub.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember // Import remember

@Composable
fun EqualizerAnimation(
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition()
    val animationValues = (1..4).map { index -> // Use index for unique random values
        val duration = remember(index) { (400..600).random() } // Remember duration per bar
        infiniteTransition.animateFloat(
            initialValue = 0.1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = duration, // Use remembered duration
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    Canvas(modifier = modifier.size(24.dp)) {
        val barWidth = size.width / 5
        val barSpacing = barWidth / 2

        animationValues.forEachIndexed { index, animValue ->
            val barHeight = size.height * animValue.value
            drawRoundRect(
                color = barColor,
                topLeft = Offset(
                    x = (barWidth + barSpacing) * index,
                    y = size.height - barHeight
                ),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }
    }
}