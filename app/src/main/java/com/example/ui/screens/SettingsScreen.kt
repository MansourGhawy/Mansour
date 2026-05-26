package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.BorderStroke
import com.example.data.repository.GoogleDriveBackupHelper
import com.google.android.gms.common.api.ApiException
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.CustomerViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    viewModel: CustomerViewModel,
    isDark: Boolean
) {
    val context = LocalContext.current
    val isFingerprintEnabled by viewModel.isFingerprintEnabled.collectAsState()
    val securityPinCode by viewModel.securityPin.collectAsState()

    var showPinDialog by remember { mutableStateOf(false) }
    var tempPinInput by remember { mutableStateOf("") }
    var tempPinError by remember { mutableStateOf(false) }

    // Google Drive flow variables
    val isGoogleSignedIn by viewModel.isGoogleSignedIn.collectAsState()
    val googleEmail by viewModel.googleEmail.collectAsState()
    val googleDisplayName by viewModel.googleDisplayName.collectAsState()
    val autoBackupEnabled by viewModel.autoBackupEnabled.collectAsState()
    val lastBackupStatus by viewModel.lastBackupStatus.collectAsState()
    val driveSyncing by viewModel.driveSyncing.collectAsState()

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            if (account != null) {
                viewModel.refreshGoogleDriveState()
                Toast.makeText(context, "تم ربط حساب جوجل بنجاح: ${account.email}", Toast.LENGTH_SHORT).show()
                viewModel.syncBackupToGoogleDrive()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "لم يتم ربط الحساب: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var backupString by remember { mutableStateOf("") }
    var restoreInputString by remember { mutableStateOf("") }

    var showDisableLockConfirmDialog by remember { mutableStateOf(false) }
    var disablePinConfirmInput by remember { mutableStateOf("") }
    var disablePinConfirmError by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) DarkBg else LightBg)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        ScreenHeader(
            title = "الإعدادات",
            subtitle = "أدوات التحكم بالنظام والنسخ الاحتياطي والأمان",
            isDark = isDark
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // CARD A: Visuals & Appearance (الواجهة واللمسات الفنية)
            Card(
                modifier = Modifier.fillMaxWidth().testTag("card_visuals_settings"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF1E1D2F) else Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = if (isDark) Color(0xFF2E2A5D) else Color(0xFFF0EDFF),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = PrimaryPurple,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "الواجهة واللمسات الفنية",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (isDark) Color.White else Color(0xFF2D3436)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Dark Mode Toggle Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "المظهر الداكن",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isDark) Color.White else Color(0xFF2D3436)
                            )
                            Text(
                                text = "تغيير سمات التطبيق بين الوضعين الليلى والنهاري",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFFA09EB5) else Color(0xFF747D8C)
                            )
                        }
                        Switch(
                            checked = isDark,
                            onCheckedChange = { viewModel.toggleDarkTheme() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PrimaryPurple,
                                checkedTrackColor = PrimaryPurple.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.testTag("dark_mode_switch")
                        )
                    }
                }
            }

            // CARD B: Business Profile (بيانات المتجر والنشاط التجاري)
            Card(
                modifier = Modifier.fillMaxWidth().testTag("card_business_settings"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF1E1D2F) else Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = if (isDark) Color(0xFF2E2A5D) else Color(0xFFE6F0FA),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Store,
                                contentDescription = null,
                                tint = PrimaryPurple,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "بيانات المتجر والنشاط التجاري",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (isDark) Color.White else Color(0xFF2D3436)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val prefs = remember { context.getSharedPreferences("hesabat_habayeb_prefs", Context.MODE_PRIVATE) }
                    var bName by remember { mutableStateOf(prefs.getString("business_name", "حسابات حبايب") ?: "حسابات حبايب") }
                    var bPhone by remember { mutableStateOf(prefs.getString("business_phone", "777777777") ?: "777777777") }
                    var bAddress by remember { mutableStateOf(prefs.getString("business_address", "اليمن") ?: "اليمن") }
                    var bNotes by remember { mutableStateOf(prefs.getString("business_notes", "نسعد لخدمتكم دائماً") ?: "نسعد لخدمتكم دائماً") }

                    val inputBg = if (isDark) Color(0xFF222135) else Color(0xFFF5F4FC)
                    val inputBorder = if (isDark) Color(0xFF323048) else Color(0xFFE5E7EB)

                    OutlinedTextField(
                        value = bName,
                        onValueChange = { 
                            bName = it
                            prefs.edit().putString("business_name", it).apply()
                        },
                        label = { Text("اسم المحل / النشاط التجاري") },
                        modifier = Modifier.fillMaxWidth().testTag("business_name_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = inputBg,
                            unfocusedContainerColor = inputBg,
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = inputBorder,
                            focusedLabelColor = PrimaryPurple,
                            focusedTextColor = if (isDark) Color.White else Color.Black,
                            unfocusedTextColor = if (isDark) Color.White else Color.Black
                        )
                    )

                    OutlinedTextField(
                        value = bPhone,
                        onValueChange = { 
                            bPhone = it
                            prefs.edit().putString("business_phone", it).apply()
                        },
                        label = { Text("رقم هاتف المحل") },
                        modifier = Modifier.fillMaxWidth().testTag("business_phone_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = inputBg,
                            unfocusedContainerColor = inputBg,
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = inputBorder,
                            focusedLabelColor = PrimaryPurple,
                            focusedTextColor = if (isDark) Color.White else Color.Black,
                            unfocusedTextColor = if (isDark) Color.White else Color.Black
                        )
                    )

                    OutlinedTextField(
                        value = bAddress,
                        onValueChange = { 
                            bAddress = it
                            prefs.edit().putString("business_address", it).apply()
                        },
                        label = { Text("عنوان النشاط") },
                        modifier = Modifier.fillMaxWidth().testTag("business_address_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = inputBg,
                            unfocusedContainerColor = inputBg,
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = inputBorder,
                            focusedLabelColor = PrimaryPurple,
                            focusedTextColor = if (isDark) Color.White else Color.Black,
                            unfocusedTextColor = if (isDark) Color.White else Color.Black
                        )
                    )

                    OutlinedTextField(
                        value = bNotes,
                        onValueChange = { 
                            bNotes = it
                            prefs.edit().putString("business_notes", it).apply()
                        },
                        label = { Text("ملاحظات تذييل الفاتورة والكشف") },
                        modifier = Modifier.fillMaxWidth().testTag("business_notes_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = inputBg,
                            unfocusedContainerColor = inputBg,
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = inputBorder,
                            focusedLabelColor = PrimaryPurple,
                            focusedTextColor = if (isDark) Color.White else Color.Black,
                            unfocusedTextColor = if (isDark) Color.White else Color.Black
                        )
                    )
                }
            }

            // CARD C: Data & Backups (النسخ الاحتياطي وحماية البيانات)
            Card(
                modifier = Modifier.fillMaxWidth().testTag("card_backup_settings"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF1E1D2F) else Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = if (isDark) Color(0xFF2E2A5D) else Color(0xFFE6FFFA),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cloud,
                                contentDescription = null,
                                tint = PrimaryPurple,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "النسخ الاحتياطي ومزامنة البيانات",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (isDark) Color.White else Color(0xFF2D3436)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Export Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch {
                                    backupString = viewModel.exportBackup()
                                    showBackupDialog = true
                                }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = SecondaryTurquoise.copy(alpha = 0.12f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Backup,
                                contentDescription = null,
                                tint = SecondaryTurquoise,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "تصدير نسخة احتياطية محلية",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isDark) Color.White else Color(0xFF2D3436)
                            )
                            Text(
                                text = "تصدير كافة الزبائن والعمليات كرموز مشفرة لمشاركتها وحفظها",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFFA09EB5) else Color(0xFF747D8C)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    HorizontalDivider(color = if (isDark) Color(0x1BFFFFFF) else Color(0x0D000000))

                    // Import Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                restoreInputString = ""
                                showRestoreDialog = true
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = PrimaryPurple.copy(alpha = 0.12f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restore,
                                contentDescription = null,
                                tint = PrimaryPurple,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "استيراد نسخة احتياطية محلية",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isDark) Color.White else Color(0xFF2D3436)
                            )
                            Text(
                                text = "استيراد ملف أو ترميز نسخة احتياطية سابقة تم نسخها",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFFA09EB5) else Color(0xFF747D8C)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    HorizontalDivider(color = if (isDark) Color(0x1BFFFFFF) else Color(0x0D000000))

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "النسخ الاحتياطي السحابي المستمر",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = PrimaryPurple,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    // Cloud flow integration
                    if (!isGoogleSignedIn) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        color = PrimaryPurple.copy(alpha = 0.12f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cloud,
                                    contentDescription = null,
                                    tint = PrimaryPurple,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Text(
                                text = "مُزامنة السجلات سحابياً",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isDark) Color.White else Color(0xFF2D3436)
                            )
                            Text(
                                text = "اربط حساب جوجل درايف الخاص بك لتفعيل ميزة النسخ السحابي والحفاظ على ديون الزبائن وسجل المعاملات آمنًا من الضياع في حال تبديل الهاتف.",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFFA09EB5) else Color(0xFF747D8C),
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            Button(
                                onClick = {
                                    try {
                                        val signInClient = GoogleDriveBackupHelper.getGoogleSignInClient(context)
                                        if (signInClient != null) {
                                            googleSignInLauncher.launch(signInClient.signInIntent)
                                        } else {
                                            Toast.makeText(context, "خدمات جوجل بلاي غير متوفرة حالياً", Toast.LENGTH_LONG).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "فشل بدء تسجيل الدخول: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("ربط حساب Google للاحتفاظ بالسجلات", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // User Info Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            color = SecondaryTurquoise.copy(alpha = 0.12f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDone,
                                        contentDescription = null,
                                        tint = SecondaryTurquoise,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = googleDisplayName ?: "مستخدم جوجل",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isDark) Color.White else Color(0xFF2D3436)
                                    )
                                    Text(
                                        text = googleEmail ?: "",
                                        fontSize = 11.sp,
                                        color = if (isDark) Color(0xFFA09EB5) else Color(0xFF747D8C)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SecondaryTurquoise.copy(alpha = 0.12f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "متصل بالدرايف",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SecondaryTurquoise
                                    )
                                }
                            }

                            HorizontalDivider(color = if (isDark) Color(0x1BFFFFFF) else Color(0x0D000000))

                            // Sync operations
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Upload Button
                                Button(
                                    onClick = { viewModel.syncBackupToGoogleDrive() },
                                    enabled = !driveSyncing,
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).height(42.dp)
                                ) {
                                    if (driveSyncing) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("نسخ سحابي الآن", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                // Download/Restore Button
                                Button(
                                    onClick = { viewModel.restoreBackupFromGoogleDrive() },
                                    enabled = !driveSyncing,
                                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryTurquoise),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).height(42.dp)
                                ) {
                                    if (driveSyncing) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("استعادة من درايف", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = if (isDark) Color(0x1BFFFFFF) else Color(0x0D000000))

                            // Switch for automated backup daily
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "النسخ الاحتياطي التلقائي اليومي",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isDark) Color.White else Color(0xFF2D3436)
                                    )
                                    Text(
                                        text = "مزامنة إلكترونية سحابية تلقائياً كل 24 ساعة بمجرد فتح التطبيق",
                                        fontSize = 11.sp,
                                        color = if (isDark) Color(0xFFA09EB5) else Color(0xFF747D8C)
                                    )
                                }
                                Switch(
                                    checked = autoBackupEnabled,
                                    onCheckedChange = { viewModel.setAutoBackupEnabled(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = PrimaryPurple,
                                        checkedTrackColor = PrimaryPurple.copy(alpha = 0.4f)
                                    )
                                )
                            }

                            // Footer backup status
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isDark) Color(0xFF161524) else Color(0xFFEEF0FA), RoundedCornerShape(10.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = lastBackupStatus,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFA09EB5) else Color(0xFF5A527A)
                                )
                                Text(
                                    text = "فصل الحساب",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NegativeRed,
                                    modifier = Modifier.clickable {
                                        try {
                                            val signInClient = GoogleDriveBackupHelper.getGoogleSignInClient(context)
                                            if (signInClient != null) {
                                                signInClient.signOut().addOnCompleteListener {
                                                    viewModel.refreshGoogleDriveState()
                                                }
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // CARD D: Security & Safe Danger Zone (الأمان والمنطقة الحساسة)
            Card(
                modifier = Modifier.fillMaxWidth().testTag("card_security_settings"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF1E1D2F) else Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = if (isDark) Color(0xFF2E2A5D) else Color(0xFFFFF0F6),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = PrimaryPurple,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "الحماية وإدارة الحساب",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (isDark) Color.White else Color(0xFF2D3436)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Bio switch row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "قفل بصمة الإصبع والرمز الشخصي",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isDark) Color.White else Color(0xFF2D3436)
                            )
                            Text(
                                text = "قفل ومنع المتطفلين من تصفح سجلات الديون والزبائن",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFFA09EB5) else Color(0xFF747D8C)
                            )
                        }
                        Switch(
                            checked = isFingerprintEnabled,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    if (securityPinCode.length < 4) {
                                        Toast.makeText(context, "الرجاء تعيين رمز PIN أولاً لضمان عدم إغلاق حسابك", Toast.LENGTH_LONG).show()
                                        tempPinInput = ""
                                        tempPinError = false
                                        showPinDialog = true
                                    } else {
                                        viewModel.toggleFingerprint()
                                        Toast.makeText(context, "تم تفعيل قفل بصمة الإصبع والرمز الشخصي بنجاح!", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    showDisableLockConfirmDialog = true
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentPink,
                                checkedTrackColor = AccentPink.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.testTag("fingerprint_switch")
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = if (isDark) Color(0xFF2D2C45) else Color(0xFFF1F2F6)
                    )

                    // PIN Modify Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                tempPinInput = ""
                                tempPinError = false
                                showPinDialog = true
                            }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = PrimaryPurple.copy(alpha = 0.12f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = PrimaryPurple,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "رمز الأمان الشخصي (PIN)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isDark) Color.White else Color(0xFF2D3436)
                                )
                                Text(
                                    text = "تعديل الرمز السري الحالي لفتح التطبيق: $securityPinCode",
                                    fontSize = 11.sp,
                                    color = if (isDark) Color(0xFFA09EB5) else Color(0xFF747D8C)
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFFA09EB5) else Color(0xFF747D8C)
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = if (isDark) Color(0xFF2D2C45) else Color(0xFFF1F2F6)
                    )

                    // Redesigned Delete Feature (حذف جميع البيانات بالكامل) with safety long press
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("unsafe_delete_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF2C191D) else Color(0xFFFFF1F0)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isDark) Color(0xFF5A252D) else Color(0xFFFCA5A5)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        Toast.makeText(context, "تنبيه: اضغط باستمرار (لمدة ثانيتين على الأقل) لبدء إجرءات مسح البيانات كخطوة أمان.", Toast.LENGTH_SHORT).show()
                                    },
                                    onLongClick = {
                                        showClearConfirmDialog = true
                                    }
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        color = Color(0xFFEF4444).copy(alpha = 0.15f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteForever,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "حذف جميع بيانات التطبيق والزبائن",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFFEF4444)
                                )
                                Text(
                                    text = "اضغط مطولاً هنا لتأكيد تصفير الحساب الآمن",
                                    fontSize = 11.sp,
                                    color = if (isDark) Color(0xFFE5B4B9) else Color(0xFF991B1B)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFEF4444).copy(alpha = 0.1f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "منطقة خطرة",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF4444)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // RE-DESIGNED "ABOUT APP" & SUPPORT WIDGET BOARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF222135) else Color(0xFFF8F7FF)
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "تطبيق حسابات حبايب",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = PrimaryPurple,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "تصميم وتطوير م/منصور قطينه للبرمجيات",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFC3BCF8) else Color(0xFF3A2C85),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // Harmonized WhatsApp Pill Button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .clickable {
                                try {
                                    val intent = android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse("https://wa.me/967774004399")
                                    )
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "فشل فتح تطبيق WhatsApp", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF6C5CE7),
                                        Color(0xFF5A49D8)
                                    )
                                )
                            )
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "WhatsApp Link",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "الدعم الفني المباشر",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "00967774004399",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.85f),
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "الإصدار: v1.0.0 (بدون إنترنت آمن)\nأندرويد متوافق مع نظام كيتكات ومستويات API 24+",
                        fontSize = 10.sp,
                        color = if (isDark) Color(0xFFA09EB5) else Color(0xFF7F8C8D),
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    // Confirmation Popup Dialog for wipe
    if (showClearConfirmDialog) {
        var inputPhrase by remember { mutableStateOf("") }
        var inputError by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { 
                showClearConfirmDialog = false 
                inputPhrase = ""
                inputError = false
            },
            title = {
                Text("تأكيد مسح كافة البيانات وجدول العمليات", color = NegativeRed, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "تحذير صارم: هذا الإجراء سيمحو كافة ديون الزبائن والتسجيلات وجميع البيانات من على ذاكرة الهاتف بشكل نهائي.\n\n" +
                        "كإجراء أمان إضافي، سأقوم بأخذ نسخة احتياطية محلية طارئة لحفظها كشبكة أمان بالخلفية قبل الحذف.\n\n" +
                        "لتأكيد الحذف النهائي، يرجى كتابة العبارة التالية بالضبط:\n" +
                        "أوافق على الحذف النهائي"
                    )
                    OutlinedTextField(
                        value = inputPhrase,
                        onValueChange = { 
                            inputPhrase = it
                            inputError = false
                        },
                        modifier = Modifier.fillMaxWidth().testTag("delete_confirmation_phrase_input"),
                        placeholder = { Text("أوافق على الحذف النهائي") },
                        isError = inputError,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NegativeRed,
                            unfocusedBorderColor = if (isDark) Color(0xFF323048) else Color(0xFFE5E5E5),
                            errorBorderColor = NegativeRed
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputPhrase.trim() == "أوافق على الحذف النهائي") {
                            viewModel.clearAllDataWithEmergencyBackup(
                                onSuccess = {
                                    showClearConfirmDialog = false
                                    inputPhrase = ""
                                },
                                onFailure = { errMsg ->
                                    Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show()
                                }
                            )
                        } else {
                            inputError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NegativeRed),
                    enabled = inputPhrase.trim() == "أوافق على الحذف النهائي"
                ) {
                    Text("نعم، امسح كل شيء")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showClearConfirmDialog = false 
                    inputPhrase = ""
                    inputError = false
                }) {
                    Text("رجوع")
                }
            }
        )
    }

    // Export Backup shareable text output dialog
    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = {
                Text("تصدير النسخة الاحتياطية", color = PrimaryPurple, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("قم بنسخ شفرة البيانات التالية وحفظها في بيئة آمنة تضمن لك حفظ السجلات:")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = backupString,
                        onValueChange = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        readOnly = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 10.sp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("hesabat_habayeb_backup", backupString)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Backup code copied to clipboard. Keep it safe!", Toast.LENGTH_SHORT).show()
                        showBackupDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Text("انسخ الرمز")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

    // Import Backup paste input dialog
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = {
                Text("استرجاع النسخة الاحتياطية", color = PrimaryPurple, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("قم بلصق شفرة الرمز الصادر من النسخة الاحتياطية وتأكيده لاسترجاع السجل بالكامل:")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = restoreInputString,
                        onValueChange = { restoreInputString = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        placeholder = { Text("الصق هنا رمز النسخة الاحتياطية...") },
                        textStyle = LocalTextStyle.current.copy(fontSize = 10.sp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (restoreInputString.isNotBlank()) {
                            viewModel.importBackup(restoreInputString)
                            showRestoreDialog = false
                        } else {
                            Toast.makeText(context, "الرمز فارغ، يرجى لصق شفرة صالحة", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Text("استرجع الآن")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Security PIN Modify Dialog
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = {
                Text("تعديل رمز الأمان (PIN)", color = PrimaryPurple, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("أدخل رمزاً سرياً جديداً مكوناً من 4 أرقام لحماية سجل حسابك:")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempPinInput,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() } && input.length <= 4) {
                                tempPinInput = input
                                tempPinError = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("pin_code_input"),
                        placeholder = { Text("مثال: 1234") },
                        isError = tempPinError,
                        supportingText = {
                            if (tempPinError) {
                                Text("يجب أن يتكون الرمز من 4 أرقام تماماً", color = NegativeRed)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempPinInput.length == 4) {
                            viewModel.updateSecurityPin(tempPinInput)
                            showPinDialog = false
                        } else {
                            tempPinError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Text("حفظ الرمز")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (showDisableLockConfirmDialog) {
        AlertDialog(
            onDismissRequest = { 
                showDisableLockConfirmDialog = false 
                disablePinConfirmInput = ""
                disablePinConfirmError = false
            },
            title = {
                Text("تأكيد إلغاء قفل الحساب", color = NegativeRed, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("لتأكيد تعطيل القفل الأمني، الرجاء إدخال رمز الأمان الحالي ($securityPinCode):")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = disablePinConfirmInput,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() } && input.length <= 4) {
                                disablePinConfirmInput = input
                                disablePinConfirmError = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("disable_pin_confirm_input"),
                        placeholder = { Text("رمز PIN الحالي") },
                        isError = disablePinConfirmError,
                        supportingText = {
                            if (disablePinConfirmError) {
                                Text("رمز PIN غير صحيح", color = NegativeRed)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (disablePinConfirmInput == securityPinCode) {
                            viewModel.toggleFingerprint()
                            showDisableLockConfirmDialog = false
                            disablePinConfirmInput = ""
                            Toast.makeText(context, "تم إلغاء قفل الحساب بنجاح", Toast.LENGTH_SHORT).show()
                        } else {
                            disablePinConfirmError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NegativeRed)
                ) {
                    Text("تعطيل القفل")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showDisableLockConfirmDialog = false 
                    disablePinConfirmInput = ""
                    disablePinConfirmError = false
                }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
