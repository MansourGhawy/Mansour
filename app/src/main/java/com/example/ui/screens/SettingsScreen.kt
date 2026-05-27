package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isDark: Boolean,
    onDarkThemeChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val sharedPref = remember { context.getSharedPreferences("hesabat_prefs", Context.MODE_PRIVATE) }
    
    var usePasscode by remember { mutableStateOf(sharedPref.getBoolean("use_passcode", false)) }
    var passcodeStr by remember { mutableStateOf(sharedPref.getString("passcode", "") ?: "") }
    var tempPasscode by remember { mutableStateOf(passcodeStr) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) DarkBg else LightBg)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "الإعدادات",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) Color.White else Color(0xFF1E1F30)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Card 1: Appearance Settings
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF1E1D2F) else Color.White
            ),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF29283F) else Color(0xFFECECFA))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("المظهر والواجهة", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryPurple)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = Color.Gray)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("الوضع الليلي (تفعيل)", fontSize = 14.sp)
                    }
                    Switch(
                        checked = isDark,
                        onCheckedChange = { onDarkThemeChange(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = PrimaryPurple)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card 2: Passcode App Lock settings
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF1E1D2F) else Color.White
            ),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF29283F) else Color(0xFFECECFA))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryPurple)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("قفل التطبيق وحماية البيانات", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryPurple)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("طلب رمز مرور (PIN) عند الفتح", fontSize = 14.sp)
                    Switch(
                        checked = usePasscode,
                        onCheckedChange = { isChecked ->
                            usePasscode = isChecked
                            sharedPref.edit().putBoolean("use_passcode", isChecked).apply()
                            if (!isChecked) {
                                passcodeStr = ""
                                tempPasscode = ""
                                sharedPref.edit().putString("passcode", "").apply()
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = PrimaryPurple)
                    )
                }

                if (usePasscode) {
                    Divider(color = if (isDark) Color(0xFF29283F) else Color(0xFFECECFA))

                    OutlinedTextField(
                        value = tempPasscode,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() } && input.length <= 4) {
                                tempPasscode = input
                            }
                        },
                        label = { Text("رمز مرور الحساب (4 أرقام)") },
                        placeholder = { Text("مثال: 1234") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pins_input_setting"),
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryPurple) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryPurple)
                    )

                    Button(
                        onClick = {
                            if (tempPasscode.length != 4) {
                                Toast.makeText(context, "الرجاء إدخال رمز مرور مكون من 4 أرقام", Toast.LENGTH_SHORT).show()
                            } else {
                                passcodeStr = tempPasscode
                                sharedPref.edit().putString("passcode", tempPasscode).apply()
                                Toast.makeText(context, "تم حفظ رمز المرور وتنشيط القفل بنجاح", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Text("حفظ رمز الدخول", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section 3: Safe Info Note
        Text(
            text = "تطبيق حسابات حبايب للهواتف - محلي وآمن ١٠٠٪\nجميع بياناتك مسجلة بالكامل محلياً وفي الخزنة الرقمية على هاتفك الشخصي فقط.",
            fontSize = 11.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )
    }
}
