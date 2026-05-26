package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.ui.viewmodel.CustomerViewModel
import com.example.ui.viewmodel.CustomerWithBalance
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

enum class ReportsSort {
    NAME, HIGHEST_DEBT, DATE
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReportsScreen(
    viewModel: CustomerViewModel,
    isDark: Boolean
) {
    val context = LocalContext.current
    val customersList by viewModel.customersWithBalances.collectAsState()
    val summary by viewModel.financialSummary.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()

    // 1. Navigation Tab states: 
    // Tab 0 = لوحة التحليلات والتحصيل (Dashboard & Analytics), Tab 1 = قوائم العملاء والمتابعة (Schedules & Actions)
    var selectedMainTab by remember { mutableStateOf(0) }

    // Multi-select and select mode states (For Lists tab)
    var selectedCustomerIds by remember { mutableStateOf(setOf<Int>()) }
    var isSelectMode by remember { mutableStateOf(false) }

    // Turn off selection mode if selections become empty
    LaunchedEffect(selectedCustomerIds) {
        if (selectedCustomerIds.isEmpty() && isSelectMode) {
            isSelectMode = false
        }
    }

    // Load and Save Local Goal Target to SharedPreferences
    val prefs = remember(context) { context.getSharedPreferences("hesabat_habayeb_prefs", Context.MODE_PRIVATE) }
    var collectionGoalTarget by remember { 
        mutableStateOf(prefs.getFloat("collection_goal", 500000f).toDouble())
    }
    LaunchedEffect(collectionGoalTarget) {
        prefs.edit().putFloat("collection_goal", collectionGoalTarget.toFloat()).apply()
    }

    // Business calculations
    val totalTheyOweMe = summary.first
    val totalIMeOwe = summary.second
    val totalPayments = transactions.filter { it.type == "PAYMENT" }.sumOf { it.amount }
    val totalDebts = transactions.filter { it.type == "DEBT" }.sumOf { it.amount }
    val debtorsCount = customersList.count { it.netBalance > 0 }

    // Time constant
    val currentTime = System.currentTimeMillis()

    // I. DEBT AGING CALCULATIONS (0-15, 16-30, 31-60, 61+ days)
    val agingData = remember(customersList, transactions) {
        var a0_15 = 0.0
        var a16_30 = 0.0
        var a31_60 = 0.0
        var a61_plus = 0.0

        customersList.filter { it.netBalance > 0 }.forEach { item ->
            val lastTime = item.lastTransactionTime ?: item.customer.createdAt
            val diffMs = currentTime - lastTime
            val days = (diffMs / (24 * 60 * 60 * 1000L)).coerceAtLeast(0)
            when {
                days <= 15 -> a0_15 += item.netBalance
                days in 16..30 -> a16_30 += item.netBalance
                days in 31..60 -> a31_60 += item.netBalance
                else -> a61_plus += item.netBalance
            }
        }
        listOf(a0_15, a16_30, a31_60, a61_plus)
    }
    val totalAgingSum = remember(agingData) { agingData.sum() }

    // II. SMART ACTIONS RECOMMENDATIONS (using requested score logic)
    // Priority = (Debt_Amount * 0.6) + (Days_Since_Last_Payment * 0.4)
    val smartRecommendations = remember(customersList, transactions) {
        customersList.filter { it.netBalance > 0 }.map { item ->
            // Find payments related to this customer
            val customerPayments = transactions.filter { it.customerId == item.customer.id && it.type == "PAYMENT" }
            val lastPaymentTime = customerPayments.maxOfOrNull { it.timestamp }
            
            val daysSinceLastPayment = if (lastPaymentTime != null) {
                (currentTime - lastPaymentTime) / (24.0 * 60.0 * 60.0 * 1000.0)
            } else {
                val daysSinceReg = (currentTime - item.customer.createdAt) / (24.0 * 60.0 * 60.0 * 1000.0)
                daysSinceReg.coerceAtLeast(30.0) // default 30 days penalty for no payments
            }

            // Normalizing values to prevent debt amount or days from dominating completely
            // score = Net_Balance * 0.6 + Days_Since_Last_Payment * 0.4
            val score = (item.netBalance * 0.6) + (daysSinceLastPayment * 0.4)
            Triple(item, daysSinceLastPayment.toInt(), score)
        }.sortedByDescending { it.third }.take(3)
    }

    // Dormant Debtors List (>30 days since last activity/payment)
    val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
    val dormantDebtors = remember(customersList) {
        customersList.filter { 
            val lastTime = it.lastTransactionTime
            it.netBalance > 0 && (lastTime == null || (currentTime - lastTime) > thirtyDaysMs)
        }
    }

    // All active debtors
    val topDebtors = remember(customersList) {
        customersList.filter { it.netBalance > 0 }
    }

    // Reports Sorting chips state
    var reportsSortBy by remember { mutableStateOf(ReportsSort.HIGHEST_DEBT) }
    var activeTabReports by remember { mutableStateOf(0) } // 0 = الأعلى مديونية, 1 = الخاملون (>30 يوم)

    // Share global report text format
    val textReport = remember(summary, topDebtors) {
        val formatter = java.text.NumberFormat.getNumberInstance(Locale.US)
        val dText = formatter.format(totalTheyOweMe)
        val pText = formatter.format(totalIMeOwe)
        val nText = formatter.format(summary.third)
        
        val debtorsJoined = topDebtors.sortedByDescending { it.netBalance }.take(10).mapIndexed { index, item ->
            "  ${index + 1}. ${item.customer.name}: ${formatter.format(item.netBalance)} ريال"
        }.joinToString("\n")

        "📊 *التقرير المالي - حسابات حبايب*\n" +
        "تاريخ التقرير: ${SimpleDateFormat("yyyy/MM/dd", Locale("ar")).format(Date())}\n\n" +
        "💵 *الملخص المالي العام:* \n" +
        "• ديون زبائن متبقية لي (المطالبات): $dText ريال\n" +
        "• تسديدات ودفعات مستلمة: ${formatter.format(totalPayments)} ريال\n" +
        "• عدد المدينين النشطين: $debtorsCount زبائن\n\n" +
        "⚠️ *أعلى زبائن عليهم متأخرات ديون:*\n" +
        (if (debtorsJoined.isNotBlank()) debtorsJoined else "  لا يوجد ديون مسجلة حالياً") +
        "\n\n_تم التصدير تلقائياً من تطبيق حسابات حبايب - محلي وآمن 100%_"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) DarkBg else LightBg)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Dynamic Header that morphs during Bulk Select is active
        if (isSelectMode) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF2D161A) else Color(0xFFFFECEF)
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, NegativeRed.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            selectedCustomerIds = emptySet()
                            isSelectMode = false
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Cancel", tint = NegativeRed)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تم تحديد ${selectedCustomerIds.size} من الزبائن",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF2C3E50)
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.deleteMultipleCustomers(
                                customersList.filter { selectedCustomerIds.contains(it.customer.id) }.map { it.customer }
                            ) {}
                            selectedCustomerIds = emptySet()
                            isSelectMode = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NegativeRed),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف الكل", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حذف جماعي", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            ScreenHeader(
                title = "التحليلات المالية والتحصيل",
                subtitle = "لوحات إحصائية تفصيلية وجدول التحصيل على جهازك بدون انترنت",
                isDark = isDark
            )
        }

        // Sub Navigation Tabs for Dashboard vs Receivables list
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .background(
                    if (isDark) Color(0xFF191829) else Color(0xFFEAEAFF),
                    RoundedCornerShape(14.dp)
                )
                .padding(4.dp)
        ) {
            TabButton(
                title = "لوحة التحليلات الذكية",
                subtitle = "أعمار الديون والأهداف",
                isSelected = selectedMainTab == 0,
                isDark = isDark,
                modifier = Modifier.weight(1f),
                onClick = { selectedMainTab = 0 }
            )
            TabButton(
                title = "جداول المتابعة والتحصيل",
                subtitle = "قائمة الزبائن النشطين",
                isSelected = selectedMainTab == 1,
                isDark = isDark,
                modifier = Modifier.weight(1f),
                onClick = { selectedMainTab = 1 }
            )
        }

        // TAB Content Switcher
        if (selectedMainTab == 0) {
            // ==================== TAB 0: ANALYTICS DASHBOARD ====================
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Section 1: Financial Cards (Receivables, Paid, Active Debtors)
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1. Receivables (الديون المستحقة) - Full Width main card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) Color(0xFF2D1E25) else Color(0xFFFFECEF)
                            ),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, if (isDark) Color(0x3DFF7675) else Color(0xFFF1D8D9))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "الديون المستحقة لي (المطالبات المالية)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isDark) Color(0xFFA09EB5) else Color(0xFF535260)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = viewModel.formatCurrency(totalTheyOweMe),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        color = NegativeRed
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .background(NegativeRed.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingDown,
                                        contentDescription = null,
                                        tint = NegativeRed,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        // Bottom Row: Paid Card & Active Debtors Card
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // 2. Paid Card (الدفعات المستردة)
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) Color(0xFF1E2F28) else Color(0xFFE8F8F0)
                                ),
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, if (isDark) Color(0x3D00B894) else Color(0xFFD0EFE0))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Payments,
                                        contentDescription = null,
                                        tint = PositiveGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "الدفعات المستردة",
                                        fontSize = 11.sp,
                                        color = if (isDark) Color(0xFFA09EB5) else Color(0xFF505C55)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = viewModel.formatCurrency(totalPayments),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PositiveGreen
                                    )
                                }
                            }

                            // 3. Active Debtors (العملاء النشطين)
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) Color(0xFF1E1D3F) else Color(0xFFEEEEFF)
                                ),
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, if (isDark) Color(0x3D6C5CE7) else Color(0xFFDCD6FD))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Groups,
                                        contentDescription = null,
                                        tint = PrimaryPurple,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "الزبائن المدينين",
                                        fontSize = 11.sp,
                                        color = if (isDark) Color(0xFFA09EB5) else Color(0xFF555268)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$debtorsCount زبائن",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryPurple
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 2: PDF Document Export & Native offline Messaging Actions
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                generatePdfReport(context, totalTheyOweMe, totalPayments, summary.third, topDebtors.sortedByDescending { it.netBalance }.take(5))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("export_pdf_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = "PDF", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تقرير PDF شامل للمراجعة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                try {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, textReport)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "مشاركة التقرير المالي:"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "لم نتمكن من إتمام المشاركة", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("share_whatsapp_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = PositiveGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("مشاركة الأرقام سريعاً", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Section 3: Goal Tracking Panel (Flexible Offline Collections Target Goal)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF1B1A2F) else Color.White
                        ),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF29283F) else Color(0xFFECECFA))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Flag, contentDescription = null, tint = SecondaryTurquoise, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "مؤشر هدف التحصيل الشهري",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color.White else Color(0xFF2D3436)
                                    )
                                }
                                
                                // Incremental Goal Adjustments (Offline and Local state)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (collectionGoalTarget > 50000) {
                                                collectionGoalTarget -= 50000
                                                viewModel.triggerVibration()
                                            }
                                        },
                                        modifier = Modifier.size(28.dp).background(if (isDark) Color(0xFF252342) else Color(0xFFF1F1FB), CircleShape)
                                    ) {
                                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease Goal", tint = PrimaryPurple, modifier = Modifier.size(14.dp))
                                    }
                                    
                                    IconButton(
                                        onClick = {
                                            collectionGoalTarget += 50000
                                            viewModel.triggerVibration()
                                        },
                                        modifier = Modifier.size(28.dp).background(if (isDark) Color(0xFF252342) else Color(0xFFF1F1FB), CircleShape)
                                    ) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = "Increase Goal", tint = PrimaryPurple, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            val progressFraction = if (collectionGoalTarget > 0) {
                                (totalPayments / collectionGoalTarget).coerceIn(0.0, 1.0)
                            } else 0.0

                            val progressPct = (progressFraction * 100).toInt()

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "المسترد الفعلي: ${viewModel.formatCurrency(totalPayments)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PositiveGreen
                                )
                                Text(
                                    text = "الهدف: ${viewModel.formatCurrency(collectionGoalTarget)} ($progressPct%)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryPurple
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Custom Visual progress track
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isDark) Color(0xFF25243C) else Color(0xFFEAEAFF))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(progressFraction.toFloat())
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(PositiveGreen, SecondaryTurquoise)
                                            )
                                        )
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            val remaining = (collectionGoalTarget - totalPayments).coerceAtLeast(0.0)
                            Text(
                                text = if (remaining > 0) "متبقى لتحقيق خطة الاسترداد المالي لهذا الشهر: ${viewModel.formatCurrency(remaining)}" else "تهانينا! لقد تجاوزت هدف التحصيل والاسترداد بنجاح! 🎉",
                                fontSize = 11.sp,
                                color = if (remaining > 0) Color.Gray else PositiveGreen,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Section 4: Local Client-Side Canvas Charting of Debt Aging
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1B1A2F) else Color.White),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF29283F) else Color(0xFFECECFA))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.TrendingDown, contentDescription = null, tint = NegativeRed, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تحليل أعمار الديون المعلقة (Aged Receivables)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color(0xFF2D3436)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "تقسيم مستحقاتك المالية حسب زمن التأخير من تاريخ آخر عملية لكل عميل",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            // Canvas multi-segmented graph
                            val colors = listOf(
                                Color(0xFF2ECC71), // 0-15
                                Color(0xFFF1C40F), // 16-30
                                Color(0xFFE67E22), // 31-60
                                Color(0xFFE74C3C)  // 61+
                            )

                            ComposeCanvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(18.dp)
                                    .clip(RoundedCornerShape(9.dp))
                            ) {
                                if (totalAgingSum <= 0.0) {
                                    drawRect(color = Color(0xFFE2E3ED))
                                } else {
                                    var currentX = 0f
                                    val parentWidth = size.width
                                    for (i in agingData.indices) {
                                        val amount = agingData[i]
                                        if (amount > 0) {
                                            val fraction = (amount / totalAgingSum).toFloat()
                                            val rectWidth = fraction * parentWidth
                                            drawRect(
                                                color = colors[i],
                                                topLeft = Offset(currentX, 0f),
                                                size = androidx.compose.ui.geometry.Size(rectWidth, size.height)
                                            )
                                            currentX += rectWidth
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Grid Legend indicators of the values
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val labels = listOf("جديدة (0-15 يوم)", "متوسطة (16-30 يوم)", "متأخرة (31-60 يوم)", "حرجة (أكثر من 60 يوم)")
                                
                                labels.forEachIndexed { i, labelText ->
                                    val valAmount = agingData[i]
                                    val pct = if (totalAgingSum > 0.0) ((valAmount / totalAgingSum) * 100).toInt() else 0
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(10.dp).background(colors[i], CircleShape))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(labelText, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = if (isDark) Color.White else Color.Black)
                                        }
                                        Text(
                                            text = "${viewModel.formatCurrency(valAmount)} ($pct%)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors[i]
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 5: Smart Offline Collection Recommendations (Priority sorting)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1B1A2F) else Color.White),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF29283F) else Color(0xFFECECFA))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Lightbulb, contentDescription = null, tint = SecondaryTurquoise, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "توصيات التحصيل الذكية لسرعة الاسترداد",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color(0xFF2D3436)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "تفضيل العملاء بموجب خوارزمية ذكية تحسب قيمة ديونهم مع أيام التأخير في السداد",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            if (smartRecommendations.isEmpty()) {
                                Text(
                                    text = "لا توجد توصيات متاحة، ليس لديك زبائن مدينين في الوقت الحالي.",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    smartRecommendations.forEachIndexed { idx, (item, daysSince, score) ->
                                        val urgencyColor = when(idx) {
                                            0 -> NegativeRed
                                            1 -> Color(0xFFE67E22)
                                            else -> Color(0xFFF1C40F)
                                        }
                                        val urgencyText = when(idx) {
                                            0 -> "أولوية قصوى واهتزاز عالي"
                                            1 -> "أولوية مرتفعة للمتابعة"
                                            else -> "متابعة دورية"
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(if (isDark) Color(0xFF24233D) else Color(0xFFF7F8FC), RoundedCornerShape(12.dp))
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = item.customer.name,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isDark) Color.White else Color(0xFF2D3436)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = urgencyText,
                                                        fontSize = 9.sp,
                                                        color = urgencyColor,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier
                                                            .background(urgencyColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "المبلغ المطلوب: ${viewModel.formatCurrency(item.netBalance)} • آخر سداد منذ: $daysSince يوم",
                                                    fontSize = 10.sp,
                                                    color = if (isDark) Color(0xFFA09EB5) else Color(0xFF5A527A)
                                                )
                                            }

                                            // WhatsApp Action reminder
                                            IconButton(
                                                onClick = {
                                                    try {
                                                        val formatBal = viewModel.formatCurrency(item.netBalance)
                                                        val waMsg = "السلام عليكم ورحمة الله.. تذكير لطيف ببيان الحساب الحالي لدينا، حيث يبلغ الرصيد المستحق كالتالي: $formatBal. نسعد ونتشرف بخدمتكم دائماً."
                                                        val formattedPhone = item.customer.phone.replace("+", "").replace(" ", "")
                                                        val intent = if (item.customer.phone.isNotBlank()) {
                                                            Intent(Intent.ACTION_VIEW).apply {
                                                                data = Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(waMsg)}")
                                                            }
                                                        } else {
                                                            Intent(Intent.ACTION_SEND).apply {
                                                                type = "text/plain"
                                                                putExtra(Intent.EXTRA_TEXT, waMsg)
                                                            }
                                                        }
                                                        context.startActivity(intent)
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "لم نتمكن من فتح تطبيق المشاركة", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(PositiveGreen.copy(alpha = 0.12f), CircleShape)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Share, 
                                                    contentDescription = "Share reminding text", 
                                                    tint = PositiveGreen, 
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 6: Local-Only and Privacy Notice Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF2C161D) else Color(0xFFFEF6F0)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF4C2A34) else Color(0xFFF3DFD5))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Security Status Offline",
                                tint = AccentPink,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "حكاية الخصوصية والأمان المحلي 100% 🔒",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color(0xFF8B3A2C)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "تتم جميع العمليات والمعالجات الحسابية لبيانات ديونك وإحصائياتها بالكامل وبشكل فوري محلياً داخل جهازك دون إرسالها لأي خوادم سحابية. حتى عند تفعيل ميزة المزامنة لجوجل درايف الاحتياطية سحابياً، تظل نسخة هاتفك الحالي ومستندها هي 'مصدر الحقيقة المالي المطلق والكامل' منعاً لأي تعارض.",
                                    fontSize = 10.sp,
                                    color = if (isDark) Color(0xFFA09EB5) else Color(0xFF5D544F),
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // ==================== TAB 1: RECEIVABLES LIST & TABLES ====================
            val originalReportList = if (activeTabReports == 0) topDebtors else dormantDebtors
            
            val sortedReportList = remember(originalReportList, reportsSortBy) {
                when (reportsSortBy) {
                    ReportsSort.NAME -> originalReportList.sortedBy { it.customer.name }
                    ReportsSort.HIGHEST_DEBT -> originalReportList.sortedByDescending { it.netBalance }
                    ReportsSort.DATE -> originalReportList.sortedByDescending { it.lastTransactionTime ?: 0L }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Section 1: Debtors list select mode tabs
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .background(
                                if (isDark) Color(0xFF1E1D2F) else Color(0x1F6C5CE7),
                                RoundedCornerShape(14.dp)
                            )
                            .padding(4.dp)
                    ) {
                        // Tab 0: Top Debtors
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (activeTabReports == 0) PrimaryPurple else Color.Transparent)
                                .clickable { activeTabReports = 0 }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "الأعلى مديونية (${topDebtors.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeTabReports == 0) Color.White else (if (isDark) Color.White else Color.Black)
                            )
                        }

                        // Tab 1: Dormant Debtors
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (activeTabReports == 1) PrimaryPurple else Color.Transparent)
                                .clickable { activeTabReports = 1 }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "الخاملون >30 يوم (${dormantDebtors.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeTabReports == 1) Color.White else (if (isDark) Color.White else Color.Black)
                            )
                        }
                    }
                }

                // Section 2: Firing option chips
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "ترتيب القائمة المالي حسب والفرز:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFA09EB5) else Color(0xFF747D8C),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val activeChipBg = PrimaryPurple
                            val activeChipText = Color.White
                            val inactiveChipBg = if (isDark) Color(0xFF1E1D2F) else Color(0xFFF1F2F6)
                            val inactiveChipText = if (isDark) Color(0xFFA09EB5) else Color(0xFF5A527A)

                            // Chip 1: الاسم
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (reportsSortBy == ReportsSort.NAME) activeChipBg else inactiveChipBg)
                                    .clickable { reportsSortBy = ReportsSort.NAME }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("الاسم", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (reportsSortBy == ReportsSort.NAME) activeChipText else inactiveChipText)
                            }

                            // Chip 2: الأعلى ديناً
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (reportsSortBy == ReportsSort.HIGHEST_DEBT) activeChipBg else inactiveChipBg)
                                    .clickable { reportsSortBy = ReportsSort.HIGHEST_DEBT }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("الأعلى ديناً", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (reportsSortBy == ReportsSort.HIGHEST_DEBT) activeChipText else inactiveChipText)
                            }

                            // Chip 3: التاريخ
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (reportsSortBy == ReportsSort.DATE) activeChipBg else inactiveChipBg)
                                    .clickable { reportsSortBy = ReportsSort.DATE }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("التاريخ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (reportsSortBy == ReportsSort.DATE) activeChipText else inactiveChipText)
                            }
                        }
                    }
                }

                // Section 3: List content
                if (sortedReportList.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E1D2F) else Color.White),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (activeTabReports == 0) "لا يوجد زبائن لديهم ديون متبقية حالياً." else "رائع! لا يوجد عملاء تراكمت ديونهم دون سداد مؤخراً.",
                                    fontSize = 13.sp,
                                    color = if (isDark) Color(0xFFA09EB5) else Color(0xFF747D8C),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(sortedReportList, key = { it.customer.id }) { debtor ->
                        val isSelected = selectedCustomerIds.contains(debtor.customer.id)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Visual check indicator when in select mode
                            if (isSelectMode) {
                                IconButton(onClick = {
                                    selectedCustomerIds = if (isSelected) {
                                        selectedCustomerIds - debtor.customer.id
                                    } else {
                                        selectedCustomerIds + debtor.customer.id
                                    }
                                }) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                                        contentDescription = "Select",
                                        tint = if (isSelected) NegativeRed else Color.Gray,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                TopDebtorRow(
                                    item = debtor,
                                    viewModel = viewModel,
                                    isDark = isDark,
                                    isSelected = isSelected,
                                    isSelectMode = isSelectMode,
                                    onSelectToggle = {
                                        if (!isSelectMode) {
                                            isSelectMode = true
                                        }
                                        selectedCustomerIds = if (isSelected) {
                                            selectedCustomerIds - debtor.customer.id
                                        } else {
                                            selectedCustomerIds + debtor.customer.id
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TabButton(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val containerBg = if (isSelected) {
        PrimaryPurple
    } else {
        Color.Transparent
    }

    val titleColor = if (isSelected) {
        Color.White
    } else {
        if (isDark) Color.White else Color.Black
    }

    val subtitleColor = if (isSelected) {
        Color.White.copy(alpha = 0.7f)
    } else {
        Color.Gray
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(containerBg)
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = titleColor, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, fontSize = 9.sp, color = subtitleColor, textAlign = TextAlign.Center)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TopDebtorRow(
    item: CustomerWithBalance,
    viewModel: CustomerViewModel,
    isDark: Boolean,
    isSelected: Boolean,
    isSelectMode: Boolean,
    onSelectToggle: () -> Unit
) {
    val context = LocalContext.current
    val balanceText = viewModel.formatCurrency(item.netBalance)
    val charLogo = item.customer.name.trim().take(1).uppercase()

    val waMessage = "السلام عليكم ورحمة الله.. تحية طيبة، نود تذكيركم ببيان الحساب الحالي لدينا، حيث يبلغ الرصيد المتبقي $balanceText. نقدّر تعاونكم الدائم ونسعد بخدمتكم."

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onLongClick = {
                    onSelectToggle()
                },
                onClick = {
                    if (isSelectMode) {
                        onSelectToggle()
                    }
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                if (isDark) Color(0xFF3A1F24) else Color(0xFFFFECEF)
            } else {
                if (isDark) Color(0xFF1E1D2F) else Color.White
            }
        ),
        border = if (isSelected) BorderStroke(1.dp, NegativeRed) else null,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (isSelected) NegativeRed.copy(alpha = 0.3f) else NegativeRed.copy(alpha = 0.15f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = charLogo,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = NegativeRed
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.customer.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF2D3436),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.customer.phone.isNotBlank()) {
                    Text(
                        text = item.customer.phone,
                        fontSize = 11.sp,
                        color = if (isDark) Color(0xFFA09EB5) else Color(0xFF5A527A)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = balanceText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = NegativeRed
                )

                // Beautiful green direct social sharing action to remind debtors
                if (!isSelectMode) {
                    IconButton(
                        onClick = {
                            try {
                                val textMessage = waMessage
                                val formattedPhone = item.customer.phone.replace("+", "").replace(" ", "")
                                val intent = if (item.customer.phone.isNotBlank()) {
                                    Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(textMessage)}")
                                    }
                                } else {
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, textMessage)
                                    }
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "لم نتمكن من فتح تطبيق المشاركة", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (isDark) Color(0xFF1E2F28) else Color(0xFFE8F8F0)
                        ),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = PositiveGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// Native PDF Generator: creates report.pdf on layout and opens system share intent!
private fun generatePdfReport(
    context: Context,
    totalDebt: Double,
    totalPayment: Double,
    net: Double,
    top5: List<CustomerWithBalance>
) {
    try {
        val document = PdfDocument()
        
        // Page specification: Standard A4 width 595 x height 842 pixels
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        // Paint components
        val titlePaint = Paint().apply {
            textSize = 24f
            color = 0xFF6C5CE7.toInt()
            isFakeBoldText = true
            textAlign = Paint.Align.RIGHT
        }

        val textPaint = Paint().apply {
            textSize = 14f
            color = 0xFF2D3436.toInt()
            textAlign = Paint.Align.RIGHT
        }

        val greenPaint = Paint().apply {
            textSize = 15f
            color = 0xFF00B894.toInt()
            isFakeBoldText = true
            textAlign = Paint.Align.RIGHT
        }

        val redPaint = Paint().apply {
            textSize = 15f
            color = 0xFFFF7675.toInt()
            isFakeBoldText = true
            textAlign = Paint.Align.RIGHT
        }

        val linePaint = Paint().apply {
            strokeWidth = 1f
            color = 0xFFDCDDE1.toInt()
        }

        var yPos = 60f

        // 1. Draw Title
        canvas.drawText("تقرير حسابات حبايب المالي", 540f, yPos, titlePaint)
        yPos += 30f
        canvas.drawLine(50f, yPos, 545f, yPos, linePaint)
        yPos += 40f

        // 2. Draw Metadata
        val format = SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale("ar"))
        canvas.drawText("التاريخ: ${format.format(Date())}", 540f, yPos, textPaint)
        yPos += 40f

        // 3. Draw Totals Card
        canvas.drawText("الملخص المالي العام:", 540f, yPos, Paint(titlePaint).apply { textSize = 16f })
        yPos += 30f

        canvas.drawText("إجمالي المبالغ المستحقة لي: ${totalDebt.toInt()} ريال", 540f, yPos, redPaint)
        yPos += 24f
        canvas.drawText("إجمالي المبالغ المسددة: ${totalPayment.toInt()} ريال", 540f, yPos, greenPaint)
        yPos += 24f
        canvas.drawText("الرصيد الكلي الصافي: ${net.toInt()} ريال", 540f, yPos, if (net >= 0) greenPaint else redPaint)
        yPos += 35f

        canvas.drawLine(50f, yPos, 545f, yPos, linePaint)
        yPos += 40f

        // 4. Draw Top 5 Debtors
        canvas.drawText("أعلى زبائن مدينين ومتبقي عليهم مبالغ:", 540f, yPos, Paint(titlePaint).apply { textSize = 16f })
        yPos += 30f

        if (top5.isEmpty()) {
            canvas.drawText("لا يوجد زبائن ذوي متبقي عليهم ديون في الوقت الحالي.", 540f, yPos, textPaint)
        } else {
            top5.forEachIndexed { index, person ->
                canvas.drawText(
                    "${index + 1}. ${person.customer.name} - متبقي عليه: ${person.netBalance.toInt()} ريال",
                    540f,
                    yPos,
                    textPaint
                )
                yPos += 28f
            }
        }

        // Draw Footer line
        yPos = 780f
        canvas.drawLine(50f, yPos, 545f, yPos, linePaint)
        yPos += 20f
        canvas.drawText("تم توليده بواسطة تطبيق حسابات حبايب للهواتف - محلي وآمن ١٠٠٪", 540f, yPos, Paint(textPaint).apply { textSize = 10f })

        document.finishPage(page)

        // Save PDF to cache and raise sharesheet share option
        val cacheDir = context.cacheDir
        val pdfFile = File(cacheDir, "hesabat_habayeb_report.pdf")
        val stream = FileOutputStream(pdfFile)
        document.writeTo(stream)
        document.close()
        stream.close()

        // Distribute PDF file via FileProvider
        val pdfUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "تصدير ومشاركة التقرير عبر:"))

    } catch (e: Exception) {
        Toast.makeText(context, "حدث خطأ أثناء تصدير ملف الـ PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}
