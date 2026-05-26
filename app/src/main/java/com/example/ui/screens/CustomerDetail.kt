package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Customer
import com.example.data.model.Transaction
import com.example.ui.viewmodel.CustomerViewModel
import com.example.ui.viewmodel.CustomerWithBalance
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetail(
    viewModel: CustomerViewModel,
    isDark: Boolean,
    onBackClick: () -> Unit,
    onAddTransactionClick: (type: String) -> Unit,
    onEditTransactionClick: (transactionId: Int) -> Unit
) {
    val context = LocalContext.current
    val customerDetail by viewModel.selectedCustomerDetail.collectAsState()
    val transactions by viewModel.selectedCustomerTransactions.collectAsState()

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }
    var customerToDelete by remember { mutableStateOf<Customer?>(null) }

    if (customerDetail == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDark) DarkBg else LightBg),
            contentAlignment = Alignment.Center
        ) {
            Text("تحميل تفاصيل الزبون...", color = if (isDark) Color.White else PrimaryPurple)
        }
        return
    }

    val customerInfo = customerDetail!!.customer
    val balance = customerDetail!!.netBalance
    val balanceAbsText = viewModel.formatCurrency(kotlin.math.abs(balance))

    val themeColor = when {
        balance > 0 -> PositiveGreen
        balance < 0 -> NegativeRed
        else -> Color.Gray
    }

    val themeLabel = when {
        balance > 0 -> "متبقي عليه دين"
        balance < 0 -> "له رصيد عندي"
        else -> "الحساب متساوي"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تفاصيل الحساب", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                actions = {
                    // Delete Entire Customer option
                    IconButton(onClick = {
                        customerToDelete = customerInfo
                        showDeleteConfirmDialog = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = "حذف الزبون",
                            tint = NegativeRed
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDark) DarkBg else LightBg,
                    titleContentColor = if (isDark) Color.White else PrimaryPurple,
                    navigationIconContentColor = if (isDark) Color.White else PrimaryPurple
                )
            )
        },
        containerColor = if (isDark) DarkBg else LightBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Top
        ) {

            // Header Section: Avatar, Name, Phone
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large initial logo
                val initialChar = customerInfo.name.trim().take(1).uppercase()
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .background(
                            color = if (isDark) Color(0xFF25233D) else Color(0x1F6C5CE7),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initialChar,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = PrimaryPurple
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = customerInfo.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF2D3436)
                    )
                    
                    if (customerInfo.phone.isNotBlank()) {
                        Text(
                            text = customerInfo.phone,
                            fontSize = 14.sp,
                            color = if (isDark) Color(0xFFA09EB5) else Color(0xFF747D8C)
                        )
                    } else {
                        Text(
                            text = "لا يوجد رقم مسجل",
                            fontSize = 12.sp,
                            color = if (isDark) Color(0xFF5E5C70) else Color(0xFF747D8C)
                        )
                    }
                }

                // Call and WhatsApp icons
                if (customerInfo.phone.isNotBlank()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Call button
                        IconButton(
                            onClick = {
                                try {
                                    val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:${customerInfo.phone}")
                                    }
                                    context.startActivity(dialIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "لم نتمكن من تشغيل الاتصال", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .background(PrimaryPurple.copy(alpha = 0.15f), CircleShape)
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "اتصال",
                                tint = PrimaryPurple,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // WhatsApp button
                        IconButton(
                            onClick = {
                                try {
                                    val textMessage = "السلام عليكم، أخي ${customerInfo.name}، بلغ حسابكم في السجل مبلغ وقدره $balanceAbsText ($themeLabel) نرجوا سرعة السداد، لدوام خدمتكم☺️."
                                    val formattedPhone = customerInfo.phone.replace("+", "").replace(" ", "")
                                    val waIntent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(textMessage)}")
                                    }
                                    context.startActivity(waIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "تأكد من تثبيت واتساب على جوالك", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .background(PositiveGreen.copy(alpha = 0.15f), CircleShape)
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Message,
                                contentDescription = "واتساب",
                                tint = PositiveGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Current Balance Glass card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .clip(RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF1E1D2F) else Color(0x3D6C5CE7)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "المبلغ المتبقي الحالي",
                        fontSize = 13.sp,
                        color = if (isDark) Color(0xFFA09EB5) else Color(0xFF5A527A)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = balanceAbsText,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = themeColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = themeLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColor.copy(alpha = 0.9f)
                    )
                }
            }

            // Two main transaction buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Button 1: Add Debt (دين عليه) - Red Button
                Button(
                    onClick = { onAddTransactionClick("DEBT") },
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("add_debt_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = NegativeRed),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "دين")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إضافة دين عليه", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                // Button 2: Pay/Receive Payment (تسديد دفعة منه) - Green Button
                Button(
                    onClick = { onAddTransactionClick("PAYMENT") },
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("add_payment_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = PositiveGreen),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "تسديد")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تسديد دفعة منه", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Transaction History Section Title & Statement Exporters (Print, WhatsApp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "سجل المعاملات السابقة",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else PrimaryPurple
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // PRINT STATEMENT BUTTON
                    IconButton(
                        onClick = {
                            try {
                                printCustomerStatement(context, customerInfo, transactions)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "فشل بدء الطباعة", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .background(PrimaryPurple.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "طباعة كشف الحساب",
                            tint = PrimaryPurple,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // WHATSAPP STATEMENT BUTTON
                    IconButton(
                        onClick = {
                            try {
                                val fullTextText = formatWhatsappStatement(customerInfo.name, transactions, balance, viewModel)
                                val formattedPhone = customerInfo.phone.replace("+", "").replace(" ", "")
                                val shareIntent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(fullTextText)}")
                                }
                                context.startActivity(shareIntent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "يرجى تثبيت تطبيق واتساب", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .background(PositiveGreen.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "إرسال كشف واتساب",
                            tint = PositiveGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Scrollable Ledger List
            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = "No receipts",
                            tint = if (isDark) Color(0xFF323048) else Color(0xFFE2E2FF),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "لا توجد معاملات مسجلة",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFA09EB5) else Color(0xFF747D8C)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "اضغط على الأزرار بالأسفل لتسجيل أول حركة مالية للزبون",
                            fontSize = 12.sp,
                            color = if (isDark) Color(0xFF747D8C) else Color(0xFF8C90A6),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(transactions, key = { it.id }) { transaction ->
                        TransactionRow(
                            transaction = transaction,
                            viewModel = viewModel,
                            isDark = isDark,
                            onEdit = { onEditTransactionClick(transaction.id) },
                            onDelete = { transactionToDelete = transaction }
                        )
                    }
                }
            }
        }
    }

    // Confirmation Popup Dialog for Deleting Customer or Transaction
    val currentCustToDelete = customerToDelete
    if (showDeleteConfirmDialog && currentCustToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmDialog = false
                customerToDelete = null
            },
            title = {
                Text("تأكيد حذف الزبون", color = NegativeRed, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "هل أنت متأكد تماماً من حذف الزبون '${currentCustToDelete.name}' ومسح سجله المالي بالكامل؟\nهذا الإجراء لا يمكن التراجع عنه مطلقاً."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomer(currentCustToDelete) {
                            onBackClick()
                        }
                        showDeleteConfirmDialog = false
                        customerToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NegativeRed)
                ) {
                    Text("نعم، احذف الزبون")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteConfirmDialog = false
                    customerToDelete = null
                }) {
                    Text("إلغاء")
                }
            }
        )
    }

    val currentTransToDelete = transactionToDelete
    if (currentTransToDelete != null) {
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = {
                Text("تأكيد حذف المعاملة", color = NegativeRed, fontWeight = FontWeight.Bold)
            },
            text = {
                val actionType = if (currentTransToDelete.type == "DEBT") "دين عليه" else "تسديد منه"
                Text(
                    "هل أنت متأكد من حذف معاملة الـ ($actionType) بمبلغ: ${viewModel.formatCurrency(currentTransToDelete.amount)}؟"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransaction(currentTransToDelete)
                        transactionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NegativeRed)
                ) {
                    Text("احذف المعاملة")
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun TransactionRow(
    transaction: Transaction,
    viewModel: CustomerViewModel,
    isDark: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val sdf = SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale("ar"))
    val fDate = sdf.format(Date(transaction.timestamp))

    val isDebt = transaction.type == "DEBT"
    val cardColor = if (isDebt) {
        if (isDark) PositiveGreen.copy(alpha=0.06f) else PositiveGreen.copy(alpha=0.04f)
    } else {
        if (isDark) NegativeRed.copy(alpha=0.06f) else NegativeRed.copy(alpha=0.04f)
    }
    val mainColor = if (isDebt) PositiveGreen else NegativeRed
    val amountText = (if (isDebt) "+" else "-") + viewModel.formatCurrency(transaction.amount)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Transaction icon inside small box
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(mainColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isDebt) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                    contentDescription = null,
                    tint = mainColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info (Type, Notes, Time)
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isDebt) "دين جديد" else "دفعة مسددة",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF2D3436)
                    )
                    Text(
                        text = amountText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = mainColor
                    )
                }

                if (transaction.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = transaction.notes,
                        fontSize = 12.sp,
                        color = if (isDark) Color(0xFFDCDDE1) else Color(0xFF474B4F),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = fDate,
                    fontSize = 10.sp,
                    color = if (isDark) Color(0xFFA09EB5) else Color(0xFF747D8C)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action modifier buttons
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = if (isDark) Color(0xFFA09EB5) else PrimaryPurple,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = NegativeRed.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// Printer generation helper
fun printCustomerStatement(context: Context, customer: Customer, transactions: List<Transaction>) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
    val jobName = "Hesabat_Habayeb_${customer.name}"
    
    val prefs = context.getSharedPreferences("hesabat_habayeb_prefs", Context.MODE_PRIVATE)
    val bName = prefs.getString("business_name", "حسابات حبايب") ?: "حسابات حبايب"
    val bPhone = prefs.getString("business_phone", "777777777") ?: "777777777"
    val bAddress = prefs.getString("business_address", "اليمن") ?: "اليمن"
    val bNotes = prefs.getString("business_notes", "نسعد لخدمتكم دائماً") ?: "نسعد لخدمتكم دائماً"
    
    val htmlContent = buildString {
        append("<!DOCTYPE html><html><head><meta charset='UTF-8'><style>")
        append("body { font-family: sans-serif; direction: rtl; padding: 20px; color: #2D3436; }")
        append(".header { text-align: center; border-bottom: 2px solid #6C5CE7; padding-bottom: 15px; margin-bottom: 20px; }")
        append(".header h1 { color: #6C5CE7; margin: 0; font-size: 24px; }")
        append(".header p { margin: 5px 0 0 0; color: #747D8C; font-size: 14px; }")
        append(".info-table { width: 100%; margin-bottom: 25px; border-collapse: collapse; }")
        append(".info-table td { padding: 8px; font-size: 14px; }")
        append(".info-table td.label { font-weight: bold; color: #5A527A; width: 120px; }")
        append(".tran-table { width: 100%; border-collapse: collapse; margin-top: 15px; }")
        append(".tran-table th { background-color: #6C5CE7; color: white; padding: 10px; text-align: right; font-size: 14px; }")
        append(".tran-table td { padding: 10px; border-bottom: 1px solid #DFE4EA; font-size: 13px; }")
        append(".status-badge-debt { color: #FF7675; font-weight: bold; }")
        append(".status-badge-payment { color: #00B894; font-weight: bold; }")
        append(".summary-box { background-color: #F1F3FF; border: 1px solid #DFE4EA; border-radius: 8px; padding: 15px; margin-top: 25px; text-align: left; }")
        append(".summary-box p { margin: 5px 0; font-size: 15px; }")
        append(".footer { margin-top: 40px; text-align: center; font-size: 11px; color: #A09EB5; border-top: 1px dashed #DFE4EA; padding-top: 10px; }")
        append("</style></head><body>")
        
        append("<div class='header'>")
        append("<h1>$bName</h1>")
        append("<p>كشف حساب زبون تفصيلي وموثوق | هاتف: $bPhone</p>")
        append("<p>العنوان: $bAddress</p>")
        append("</div>")
        
        append("<table class='info-table'>")
        append("<tr><td class='label'>اسم الزبون:</td><td>${customer.name}</td>")
        append("<td class='label'>تاريخ الكشف:</td><td>${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}</td></tr>")
        append("<tr><td class='label'>رقم الجوال:</td><td>${customer.phone}</td>")
        append("<td class='label'>ملاحظات الزبون:</td><td>${customer.notes}</td></tr>")
        append("</table>")
        
        append("<table class='tran-table'>")
        append("<tr><th>التاريخ</th><th>النوع</th><th>المبلغ</th><th>البيان والتفاصيل</th></tr>")
        
        var totalDebts = 0.0
        var totalPayments = 0.0
        
        for (item in transactions) {
            val amountStr = String.format("%.2f", item.amount)
            val isDebt = item.type == "DEBT"
            if (isDebt) totalDebts += item.amount else totalPayments += item.amount
            
            val typeStr = if (isDebt) "دين (عليه)" else "سداد (له)"
            val typeClass = if (isDebt) "status-badge-debt" else "status-badge-payment"
            val dateStr = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(item.timestamp))
            
            append("<tr>")
            append("<td>$dateStr</td>")
            append("<td class='$typeClass'>$typeStr</td>")
            append("<td>$amountStr ريال</td>")
            append("<td>${item.notes}</td>")
            append("</tr>")
        }
        
        append("</table>")
        
        val balance = totalDebts - totalPayments
        val balanceText = if (balance > 0) "دين عليه" else if (balance < 0) "له سداد" else "خالص"
        val balanceAbs = Math.abs(balance)
        
        append("<div class='summary-box'>")
        append("<p style='text-align: right;'><strong>إجمالي الديون:</strong> ${String.format("%.2f", totalDebts)} ريال</p>")
        append("<p style='text-align: right;'><strong>إجمالي السدادات:</strong> ${String.format("%.2f", totalPayments)} ريال</p>")
        append("<p style='text-align: right; font-size:17px; color:#6C5CE7;'><strong>صافي الرصيد الحالي:</strong> ${String.format("%.2f", balanceAbs)} ريال (${balanceText})</p>")
        append("</div>")
        
        append("<div class='footer'>")
        append("<p>$bNotes</p>")
        append("<p>تم توليد هذا الكشف عبر تطبيق $bName - نظام إدارة المعاملات المالية والديون</p>")
        append("</div>")
        
        append("</body></html>")
    }
    
    val webView = android.webkit.WebView(context)
    webView.webViewClient = object : android.webkit.WebViewClient() {
        override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
            val printAdapter = webView.createPrintDocumentAdapter(jobName)
            printManager.print(jobName, printAdapter, android.print.PrintAttributes.Builder().build())
        }
    }
    webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
}

// WhatsApp detailed text formatter
fun formatWhatsappStatement(customerName: String, transactions: List<Transaction>, netBalance: Double, viewModel: CustomerViewModel): String {
    return buildString {
        append("📄 *كشف حساب تفصيلي من تطبيق حسابات حبايب* 📄\n")
        append("------------------------------------------\n")
        append("👤 *الزبون:* $customerName\n")
        append("📅 *تاريخ الكشف:* ${SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date())}\n")
        append("------------------------------------------\n")
        
        if (transactions.isEmpty()) {
            append("لا توجد معاملات مسجلة حالياً.\n")
        } else {
            append("📋 *العمليات الأخيرة:*\n")
            transactions.take(15).forEach { item ->
                val typeStr = if (item.type == "DEBT") "🔴 دين عليه:" else "🟢 سداد منه:"
                val dateStr = SimpleDateFormat("MM/dd", Locale.getDefault()).format(Date(item.timestamp))
                append("• [$dateStr] $typeStr ${item.amount} ريال\n")
                if (item.notes.isNotBlank()) {
                    append("  _البيان: ${item.notes}_\n")
                }
            }
            if (transactions.size > 15) {
                append("... وعمليات أخرى سابقة\n")
            }
        }
        
        append("------------------------------------------\n")
        val balAbs = Math.abs(netBalance)
        val balLabel = when {
            netBalance < 0 -> "له رصيد عندي"
            netBalance > 0 -> "متبقي عليه دين"
            else -> "الحساب متساوي"
        }
        append("💰 *صافي الرصيد الحالي:* ${viewModel.formatCurrency(balAbs)} ($balLabel)\n")
        append("------------------------------------------\n")
    }
}
