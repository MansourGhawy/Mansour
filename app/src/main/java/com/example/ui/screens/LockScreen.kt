package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkBg
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.NegativeRed

@Composable
fun LockScreen(
    onUnlockDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sharedPref = remember { context.getSharedPreferences("hesabat_prefs", Context.MODE_PRIVATE) }
    val savedPasscode = remember { sharedPref.getString("passcode", "") ?: "" }

    var enteredDigits by remember { mutableStateOf("") }

    LaunchedEffect(enteredDigits) {
        if (enteredDigits.length == 4) {
            if (enteredDigits == savedPasscode) {
                onUnlockDismiss()
            } else {
                Toast.makeText(context, "رمز المرور خاطئ! الرجاء إدخال الرمز الصحيح", Toast.LENGTH_SHORT).show()
                enteredDigits = ""
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 48.dp, horizontal = 32.dp)
        ) {
            // Header block
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "حسابات حبايب آمنة",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "أدخل رمز الدخول (البين) لفتح الخزانة والمتابعة",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Passcode entered state indicator dots
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    listOf(1, 2, 3, 4).forEach { index ->
                        val filled = enteredDigits.length >= index
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(if (filled) PrimaryPurple else Color(0xFF2E2C4D))
                        )
                    }
                }
            }

            // Simple custom numeric keypad
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Keypad rows
                listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9")
                ).forEach { gridRow ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        gridRow.forEach { char ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1.5f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF1E1D2F))
                                    .clickable {
                                        if (enteredDigits.length < 4) enteredDigits += char
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(char, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                        }
                    }
                }

                // Call row clear/0/back
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Reset inputs
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.5f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(NegativeRed.copy(alpha = 0.15f))
                            .clickable { enteredDigits = "" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("C", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NegativeRed)
                    }

                    // 0 key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.5f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1E1D2F))
                            .clickable {
                                if (enteredDigits.length < 4) enteredDigits += "0"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("0", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }

                    // Backspace code
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.5f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1E1D2F))
                            .clickable {
                                if (enteredDigits.isNotEmpty()) {
                                    enteredDigits = enteredDigits.dropLast(1)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⌫", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
