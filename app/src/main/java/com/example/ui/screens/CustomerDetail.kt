package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Customer
import com.example.data.model.Transaction
import com.example.ui.theme.*
import com.example.ui.viewmodel.CustomerViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetail(
    viewModel: CustomerViewModel,
    customerId: Long,
    isDark: Boolean,
    onBack: () -> Unit,
    onAddTransactionClick: (Long, String) -> Unit // Navigates to addition form with type
) {
    val context = LocalContext.current
    
    val customersList by viewModel.customersWithBalances.collectAsState()
    val debtor = remember(customersList, customerId) {
        customersList.find { it.customer.id == customerId }
    }

    val transactionsFlow = remember(customerId) { viewModel.getTransactionsForCustomer(customerId) }
    val transactions by transactionsFlow.collectAsState(initial = emptyList())

    fun formatCustomCurrency(amount: Double): String {
        val formatter = java.text.NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }
        return "${formatter.format(amount)} ر.ي"
    }

    fun formatArabicDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy/MM/dd • hh:mm a", Locale.ENGLISH)
        return sdf.format(Date(timestamp))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(debtor?.customer?.name ?: "تفاصيل العميل", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    if (debtor != null) {
                        IconButton(
                            onClick = {
                                viewModel.deleteCustomer(debtor.customer)
                                Toast.makeText(context, "تم حذف العميل بنجاح", Toast.LENGTH_SHORT).show()
                                onBack()
                            },
                            modifier = Modifier.testTag("delete_customer_icon")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف العميل", tint = NegativeRed)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDark) DarkBg else LightBg
                )
            )
        },
        containerColor = if (isDark) DarkBg else LightBg
    ) { paddingValues ->
        if (debtor == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("المستخدم غير موجود.")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // 1. Balance Header Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF1E1D2F) else Color.White
                    ),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF29283F) else Color(0xFFECECFA))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (debtor.netBalance > 0) "الرصيد المتبقي عليه للتحصيل" else "رصيد دفعات وتأمينات متبقي له",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = formatCustomCurrency(debtor.netBalance),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = if (debtor.netBalance > 0) NegativeRed else if (debtor.netBalance < 0) PositiveGreen else Color.Gray,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Interaction row (Call and Share Link)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    if (debtor.customer.phone.isNotBlank()) {
                                        try {
                                            val number = debtor.customer.phone
                                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "لم نتمكن من الاتصال", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "رقم الهاتف غير مسجل", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple.copy(alpha = 0.15f), contentColor = PrimaryPurple),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("اتصال واطلب الحساب", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    try {
                                        val mLink = "السلام عليكم ورحمة الله.. تذكير ببيان الحساب المالي المتبقي عليكم لدينا بمبلغ: ${formatCustomCurrency(debtor.netBalance)}."
                                        val formatted = debtor.customer.phone.replace("+", "").replace(" ", "")
                                        val waIntent = if (debtor.customer.phone.isNotBlank()) {
                                            Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$formatted&text=${Uri.encode(mLink)}"))
                                        } else {
                                            Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, mLink)
                                            }
                                        }
                                        context.startActivity(waIntent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "لا تتوفر وسيلة لمشاركة البيانات", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PositiveGreen.copy(alpha = 0.15f), contentColor = PositiveGreen),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("تذكير واتساب", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                try {
                                    val txsJson = transactions.joinToString(separator = ",") { 
                                        """{"amount":${it.amount},"type":"${it.type}","timestamp":${it.timestamp},"note":"${it.note.replace("\"", "\\\"")}"}""" 
                                    }
                                    val jsonString = """{"name":"${debtor.customer.name.replace("\"", "\\\"")}","phone":"${debtor.customer.phone}","txs":[$txsJson]}"""
                                    val base64Enc = android.util.Base64.encodeToString(jsonString.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP or android.util.Base64.URL_SAFE)
                                    val syncUrl = "https://hesabathabayeb.web.app/sync?data=$base64Enc"
                                    
                                    val syncMessage = "مرحباً ${debtor.customer.name}،\nإليك رابط المزامنة المباشر لحساباتك معنا في تطبيق حسابات حبايب. يمكنك فتح الرابط لاستيراد جميع معاملاتك ديناً وسداداً بشكل فوري وآمن:\n\n$syncUrl"
                                    
                                    val formatted = debtor.customer.phone.replace("+", "").replace(" ", "")
                                    val waIntent = if (debtor.customer.phone.isNotBlank()) {
                                        Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$formatted&text=${Uri.encode(syncMessage)}"))
                                    } else {
                                        Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, syncMessage)
                                        }
                                    }
                                    context.startActivity(waIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "لم نتمكن من توليد رابط المزامنة", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple, contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("مشاركة رابط المزامنة الذكي (WhatsApp)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 2. Transact buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Record debt (DEBT)
                    Button(
                        onClick = { onAddTransactionClick(customerId, "DEBT") },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("loan_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = NegativeRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("➖", fontSize = 14.sp, color = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("سجل عليه ديناً", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    // Record repayment (PAYMENT)
                    Button(
                        onClick = { onAddTransactionClick(customerId, "PAYMENT") },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("repay_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = PositiveGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("سجل سداداً ودفعة", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                // 3. Transactions Log Section
                Text(
                    text = "سجل وبيان المعاملات المتبادلة (${transactions.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFFC5C5D8) else Color(0xFF555268),
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp)
                )

                if (transactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد معاملات مسجلة لهذا العميل حتى الآن.",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(transactions) { tx ->
                            TransactionItem(
                                tx = tx,
                                isDark = isDark,
                                onDelete = { viewModel.deleteTransaction(tx) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionItem(
    tx: Transaction,
    isDark: Boolean,
    onDelete: () -> Unit
) {
    val formatter = java.text.NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
    }
    val amountText = "${formatter.format(tx.amount)} ر.ي"
    val dateText = SimpleDateFormat("yyyy/MM/dd • hh:mm a", Locale.ENGLISH).format(Date(tx.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1B1A2F) else Color.White
        ),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF29283F) else Color(0xFFECECFA))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (tx.type == "DEBT") NegativeRed.copy(alpha = 0.15f) else PositiveGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (tx.type == "DEBT") Icons.Default.ArrowForward else Icons.Default.ArrowBack,
                        contentDescription = null,
                        tint = if (tx.type == "DEBT") NegativeRed else PositiveGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = if (tx.type == "DEBT") "دين مسجل عليه" else "مبلغ مستلم منه",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF2D3436)
                    )
                    if (tx.note.isNotBlank()) {
                        Text(
                            text = tx.note,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    Text(
                        text = dateText,
                        fontSize = 10.sp,
                        color = Color.LightGray
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = amountText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = if (tx.type == "DEBT") NegativeRed else PositiveGreen
                )
                Spacer(modifier = Modifier.width(10.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete transaction", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
