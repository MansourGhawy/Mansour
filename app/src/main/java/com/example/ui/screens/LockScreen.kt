package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LockScreen(
    correctPin: String,
    isDark: Boolean,
    onUnlockSuccess: () -> Unit
) {
    val context = LocalContext.current

    // Intercept hardware OS back buttons specifically while lock screen is active
    BackHandler(enabled = true) {
        // Do nothing to prevent exiting, no toasts avoiding overload on InputDispatcher
    }

    var inputPin by remember { mutableStateOf("") }
    var isPinError by remember { mutableStateOf(false) }
    var isBiometricScanning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Shake animation state for incorrect PIN
    val offsetX = remember { Animatable(0f) }

    fun handleDigitPress(digit: String) {
        if (inputPin.length < 4) {
            isPinError = false
            inputPin += digit
            if (inputPin.length == 4) {
                if (inputPin == correctPin) {
                    onUnlockSuccess()
                } else {
                    // Start error shake
                    scope.launch {
                        isPinError = true
                        Toast.makeText(context, "رمز الأمان خاطئ، يرجى المحاولة مجدداً", Toast.LENGTH_SHORT).show()
                        inputPin = ""
                        // Shake routine
                        repeat(3) {
                            offsetX.animateTo(20f, spring(dampingRatio = Spring.DampingRatioHighBouncy))
                            offsetX.animateTo(-20f, spring(dampingRatio = Spring.DampingRatioHighBouncy))
                        }
                        offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioHighBouncy))
                    }
                }
            }
        }
    }

    fun handleDeletePress() {
        if (inputPin.isNotEmpty()) {
            inputPin = inputPin.dropLast(1)
            isPinError = false
        }
    }

    fun handleBiometricPress() {
        if (isBiometricScanning) return
        scope.launch {
            isBiometricScanning = true
            delay(1200) // Simulated scan progress animation
            isBiometricScanning = false
            Toast.makeText(context, "تم تأكيد الهوية بالبصمة بنجاح!", Toast.LENGTH_SHORT).show()
            onUnlockSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) DarkBg else LightBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Spacer for vertical balance
            Spacer(modifier = Modifier.height(30.dp))

            // Upper Panel: Lock icon & Titles
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.offset(x = offsetX.value.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            if (isPinError) NegativeRed.copy(alpha = 0.15f) else PrimaryPurple.copy(alpha = 0.15f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPinError) Icons.Default.Lock else Icons.Default.Lock,
                        contentDescription = "Lock Logo",
                        tint = if (isPinError) NegativeRed else PrimaryPurple,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = "رمز الأمان مطلوب",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF2D3436)
                )

                Text(
                    text = if (isPinError) "الرمز المدخل غير صحيح" else "الرجاء إدخال الرمز المكون من 4 أرقام لفتح التطبيق",
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    color = if (isPinError) NegativeRed else (if (isDark) Color(0xFFA09EB5) else Color(0xFF747D8C)),
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Dots representing passcode state
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 4) {
                        val isFilled = i < inputPin.length
                        val color = when {
                            isPinError -> NegativeRed
                            isFilled -> PrimaryPurple
                            else -> if (isDark) Color(0xFF2F2B4A) else Color(0xFFDCDDE1)
                        }
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }
            }

            // Fingerprint scanner visual when biometric scanning is active
            if (isBiometricScanning) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = SecondaryTurquoise,
                        modifier = Modifier.size(54.dp),
                        strokeWidth = 4.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "جاري التحقق من بصمة الإصبع...",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SecondaryTurquoise
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Lower Panel: 3x4 Numerical Grid keyboard
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Keypad Rows
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9")
                )

                for (row in keys) {
                    Row(
                        modifier = Modifier.fillMaxWidth(0.85f),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        for (key in row) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1.3f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isDark) Color(0xFF1E1D2F) else Color(0xFFEFEFFB))
                                    .clickable { handleDigitPress(key) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = key,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color(0xFF2D3436)
                                )
                            }
                        }
                    }
                }

                // Last Row: Biometric Shortcut | 0 | Backspace delete
                Row(
                    modifier = Modifier.fillMaxWidth(0.85f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Biometric/Fingerprint Trigger
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.3f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isDark) Color(0xFF251A32) else Color(0xFFFFEBEE))
                            .clickable { handleBiometricPress() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "بصمة الإصبع",
                            tint = AccentPink,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Key 0
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.3f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isDark) Color(0xFF1E1D2F) else Color(0xFFEFEFFB))
                            .clickable { handleDigitPress("0") },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "0",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF2D3436)
                        )
                    }

                    // Key Backspace
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.3f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isDark) Color(0xFF252136) else Color(0xFFF1F2F6))
                            .clickable { handleDeletePress() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Backspace,
                            contentDescription = "حذف ومسح",
                            tint = if (isDark) Color(0xFFA09EB5) else Color(0xFF5A527A),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
