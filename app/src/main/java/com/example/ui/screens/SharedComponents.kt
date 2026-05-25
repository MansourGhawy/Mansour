package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Color constants matching user request
val PrimaryPurple = Color(0xFF6C5CE7)
val SecondaryTurquoise = Color(0xFF00CEC9)
val AccentPink = Color(0xFFFD79A8)
val LightBg = Color(0xFFF8F9FE)
val DarkBg = Color(0xFF141221)

val PositiveGreen = Color(0xFF2ECC71)
val NegativeRed = Color(0xFFE74C3C)

@Composable
fun GlassyCard(
    modifier: Modifier = Modifier,
    isDark: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val bgBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF25233D), Color(0xFF1B1A2F))
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFF1F3FF))
        )
    }
    
    val borderColor = if (isDark) Color(0x22FFFFFF) else Color(0x1F6C5CE7)

    Card(
        modifier = modifier
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .drawBehind {
                    drawRect(brush = bgBrush)
                }
                .padding(16.dp)
        ) {
            content()
        }
    }
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(PrimaryPurple, SecondaryTurquoise),
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(18.dp)),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(),
        enabled = enabled
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(
                        brush = Brush.horizontalGradient(colors)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    isDark: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = if (isDark) Color.White else PrimaryPurple
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDark) Color(0xFFA09EB5) else Color(0xFF747D8C)
            )
        }
    }
}
