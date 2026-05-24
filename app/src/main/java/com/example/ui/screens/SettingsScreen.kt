package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

@OptIn(ExperimentalMaterial3Api::class)
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
        val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
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
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Group 1: UI Theme Settings
            Text(
                text = "الواجهة واللمسات الفنية",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFA09EB5) else Color(0xFF747D8C)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF1E1D2F) else Color.White
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(PrimaryPurple.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = PrimaryPurple,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "المظهر الداكن",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isDark) Color.White else Color(0xFF2D3436)
                            )
                            Text(
                                "تغيير سمات التطبيق بين الوضعين الليلى والنهاري",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFFA09EB5) else Color(0xFF747D8C)
                            )
                        }
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

            // Group 2: Backup and restore options
            Text(
                text = "النسخ الاحتياطي وحماية البيانات",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFA09EB5) else Color(0xFF747D8C)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF1E1D2F) else Color.White
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column {
                    // Export block
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                backupString = viewModel.exportBackup()
                                showBackupDialog = true
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(SecondaryTurquoise.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
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
                                "نسخة احتياطية للبيانات",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isDark) Color.White else Color(0xFF2D3436)
                            )
                            Text(
                                "تصدير كافة الزبائن والعمليات كرموز مشفرة لمشاركتها وحفظها",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFFA09EB5) else Color(0xFF747D8C)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    }

                    Divider(
                        color = if (isDark) Color(0x1BFFFFFF) else Color(0x0D000000),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    // Import block
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                restoreInputString = ""
                                showRestoreDialog = true
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(PrimaryPurple.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
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
                                "استيراد نسخة احتياطية",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isDark) Color.White else Color(0xFF2D3436)
                            )
                            Text(
                                "استيراد ملف أو ترميز نسخة احتياطية سابقة تم نسخها",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFFA09EB5) else Color(0xFF747D8C)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    }
                }
            }

            // Group 3: Cloud Google Drive Backup options
            Text(
                text = "النسخ الاحتياطي السحابي (Google Drive)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFA09EB5) else Color(0xFF747D8C)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF1E1D2F) else Color.White
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (!isGoogleSignedIn) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(PrimaryPurple.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cloud,
                                    contentDescription = null,
                                    tint = PrimaryPurple,
                                    modifier = Modifier.size(24.dp)
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
                                        .background(SecondaryTurquoise.copy(alpha = 0.15f), CircleShape),
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
                                        "النسخ الاحتياطي التلقائي اليومي",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isDark) Color.White else Color(0xFF2D3436)
                                    )
                                    Text(
                                        "مزامنة إلكترونية سحابية تلقائياً كل 24 ساعة بمجرد فتح التطبيق",
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

            // Group 4: Device Security Lock simulation
            Text(
                text = "الأمان وقفل الحساب",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFA09EB5) else Color(0xFF747D8C)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF1E1D2F) else Color.White
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
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
                                    .background(AccentPink.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = AccentPink,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "قفل بصمة الإصبع والرمز الشخصي",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isDark) Color.White else Color(0xFF2D3436)
                                )
                                Text(
                                    "قفل ومنع المتطفلين من تصفح سجلات الديون والزبائن",
                                    fontSize = 11.sp,
                                    color = if (isDark) Color(0xFFA09EB5) else Color(0xFF747D8C)
                                )
                            }
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
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = if (isDark) Color(0xFF2D2C45) else Color(0xFFF1F2F6)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                tempPinInput = ""
                                tempPinError = false
                                showPinDialog = true
                            }
                            .padding(16.dp),
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
                                    .background(PrimaryPurple.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = PrimaryPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "رمز الأمان الشخصي (PIN)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isDark) Color.White else Color(0xFF2D3436)
                                )
                                Text(
                                    "تعديل الرمز السري الحالي لفتح التطبيق: $securityPinCode",
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
                }
            }


            // Group 4: Reset data section
            Text(
                text = "الخطر والمحو الذاتي",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = NegativeRed
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0x3DFF7675) else Color(0x1FFF7675)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showClearConfirmDialog = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(NegativeRed.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint = NegativeRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "حذف جميع البيانات بالكامل",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = NegativeRed
                        )
                        Text(
                            "احذر! سيتم مسح كافة الزبائن وسجل العمليات تماماً للبدء من جديد",
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFFD3A4AC) else Color(0xFF8A5D64)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Version metadata info card
            Text(
                text = "عن تطبيق حسابات حبايب",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFA09EB5) else Color(0xFF747D8C)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF161524) else Color(0xFFEEF0FA)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "حسابات حبايب وشركاؤه",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = PrimaryPurple,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "برنامج محلي بالكامل لحفظ السجلات والديون، من تصميم وتطوير فريق قطينه للبرمجيات",
                        fontSize = 11.sp,
                        color = if (isDark) Color(0xFFA09EB5) else Color(0xFF5A527A),
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "الإصدار: v1.0.0 (بدون إنترنت آمن)\nأندرويد متوافق مع نظام كيتكات ومستويات API 24+",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }

    // Confirmation Popup Dialog for wipe
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = {
                Text("تأكيد مسح كافة البيانات وجدول العمليات", color = NegativeRed, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "تحذير صارم: هذا الإجراء سيمحو كافة ديون الزبائن والتسجيلات وجميع البيانات من على ذاكرة الهاتف بشكل نهائي.\nهل تود الاستمرار حقاً؟"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NegativeRed)
                ) {
                    Text("نعم، امسح كل شيء")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
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
                        Toast.makeText(context, "تم نسخ شفرة البيانات للحافظة بنجاح", Toast.LENGTH_SHORT).show()
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
                            val success = viewModel.importBackup(restoreInputString)
                            if (success) {
                                showRestoreDialog = false
                            }
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
