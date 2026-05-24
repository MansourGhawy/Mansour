package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    isDark: Boolean = false,
    onTimeout: () -> Unit
) {
    // Elegant pulsing animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Wait for 2 seconds then invoke callback
    LaunchedEffect(key1 = true) {
        delay(2000)
        onTimeout()
    }

    val bgGradient = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(DarkBg, Color(0xFF1B1A2F))
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(LightBg, Color(0xFFE2E2FF))
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // App heart/wallet logo in modern glassmorphism bubble
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .scale(scale)
                    .background(
                        color = if (isDark) Color(0x336C5CE7) else Color(0x1F6C5CE7),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Wallet,
                    contentDescription = "Wallet Logo",
                    tint = PrimaryPurple,
                    modifier = Modifier.size(70.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "حسابات حبايب",
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                color = if (isDark) Color.White else PrimaryPurple
            )
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Text(
                text = "الدفتر الذكي والآمن لإدارة ديون وحسابات الزبائن",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDark) Color(0xFFA09EB5) else Color(0xFF747D8C)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
