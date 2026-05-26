package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.example.data.model.Transaction
import com.example.ui.viewmodel.CustomerViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

// Simple safe math expression parser for the calculator
private fun evaluateExpression(expr: String): Double? {
    if (expr.isBlank()) return null
    return try {
        val tokens = mutableListOf<String>()
        var currentNum = ""
        for (char in expr) {
            if (char.isDigit() || char == '.') {
                currentNum += char
            } else if (char in "+-*/") {
                if (currentNum.isNotEmpty()) {
                    tokens.add(currentNum)
                    currentNum = ""
                }
                tokens.add(char.toString())
            }
        }
        if (currentNum.isNotEmpty()) {
            tokens.add(currentNum)
        }
        
        if (tokens.isEmpty()) return null
        
        // Resolve * and /
        val tempTokens = mutableListOf<String>()
        var idx = 0
        while (idx < tokens.size) {
            val token = tokens[idx]
            if (token == "*" || token == "/") {
                if (tempTokens.isEmpty() || idx + 1 >= tokens.size) return null
                val prevVal = tempTokens.removeAt(tempTokens.size - 1).toDoubleOrNull() ?: return null
                val nextVal = tokens[idx + 1].toDoubleOrNull() ?: return null
                val res = if (token == "*") prevVal * nextVal else {
                    if (nextVal == 0.0) return null
                    prevVal / nextVal
                }
                tempTokens.add(res.toString())
                idx += 2
            } else {
                tempTokens.add(token)
                idx++
            }
        }
        
        if (tempTokens.isEmpty()) return null
        
        // Resolve + and -
        var total = tempTokens[0].toDoubleOrNull() ?: return null
        var p = 1
        while (p < tempTokens.size) {
            val op = tempTokens[p]
            if (p + 1 >= tempTokens.size) return null
            val nextVal = tempTokens[p + 1].toDoubleOrNull() ?: return null
            total = if (op == "+") total + nextVal else if (op == "-") total - nextVal else return null
            p += 2
        }
        total
    } catch (e: Exception) {
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionForm(
    viewModel: CustomerViewModel,
    isDark: Boolean,
    defaultType: String, // "DEBT" or "PAYMENT"
    transactionIdToEdit: Int? = null,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val customerDetail by viewModel.selectedCustomerDetail.collectAsState()

    val debtRed = androidx.compose.ui.res.colorResource(id = com.example.R.color.debt_red)
    val paymentGreen = androidx.compose.ui.res.colorResource(id = com.example.R.color.payment_green)
    
    // Form fields
    var txType by remember { mutableStateOf(defaultType) }
    var amountStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var calTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }

    var isEditMode by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }

    // Calculator states
    var showCalculator by remember { mutableStateOf(false) }
    var calcExpression by remember { mutableStateOf("") }

    // Coroutine and animation states
    val scope = rememberCoroutineScope()
    val notesFocusRequester = remember { FocusRequester() }
    var isHighlighted by remember { mutableStateOf(false) }

    val highlightBgColor by animateColorAsState(
        targetValue = if (isHighlighted) {
            (if (txType == "DEBT") debtRed else paymentGreen).copy(alpha = 0.15f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(durationMillis = 600)
    )

    // Preload if editing
    LaunchedEffect(transactionIdToEdit) {
        if (transactionIdToEdit != null) {
            isEditMode = true
            val trans = viewModel.allTransactions.value.firstOrNull { it.id == transactionIdToEdit }
            if (trans != null) {
                txType = trans.type
                amountStr = if (trans.amount % 1.0 == 0.0) trans.amount.toLong().toString() else trans.amount.toString()
                notes = trans.notes
                calTimestamp = trans.timestamp
            }
        }
    }

    if (customerDetail == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDark) DarkBg else LightBg),
            contentAlignment = Alignment.Center
        ) {
            Text("تحميل...", color = if (isDark) Color.White else PrimaryPurple)
        }
        return
    }

    val customer = customerDetail!!.customer

    // Live display of formatted price
    val amountDouble = amountStr.toDoubleOrNull() ?: 0.0
    val dynamicFormattedPrice = if (amountDouble > 0) {
        viewModel.formatCurrency(amountDouble)
    } else {
        "٠ ريال"
    }

    // Prepare Date and Time string
    val sdf = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale("ar"))
    val fTimeStr = sdf.format(Date(calTimestamp))

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditMode) "تعديل المعاملة" else "معاملة جديدة",
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else PrimaryPurple
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = if (isDark) Color.White else PrimaryPurple
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDark) DarkBg else LightBg
                )
            )
        },
        containerColor = if (isDark) DarkBg else LightBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Scrollable Content Region
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp) // Ultra condensed
            ) {
                // Recipient info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF1E1D2F) else Color(0x1F6C5CE7)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(PrimaryPurple, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = customer.name.trim().take(1).uppercase(),
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "الحساب المستهدف: ${customer.name}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color.White else PrimaryPurple
                        )
                    }
                }

                // Transaction Type tab selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) Color(0xFF1E1D2F) else Color(0xFFE2E2FF))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // DEBT selector (دين عليه)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (txType == "DEBT") debtRed else Color.Transparent)
                            .clickable { txType = "DEBT" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "دين جديد (عليه)",
                            fontWeight = FontWeight.Bold,
                            color = if (txType == "DEBT") Color.White else (if (isDark) Color(0xFFA09EB5) else Color(0xFF5A527A)),
                            fontSize = 13.sp
                        )
                    }

                    // PAYMENT selector (تسديد منه)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (txType == "PAYMENT") paymentGreen else Color.Transparent)
                            .clickable { txType = "PAYMENT" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "تسديد دفعة (منه)",
                            fontWeight = FontWeight.Bold,
                            color = if (txType == "PAYMENT") Color.White else (if (isDark) Color(0xFFA09EB5) else Color(0xFF5A527A)),
                            fontSize = 13.sp
                        )
                    }
                }

                // 1. Amount input with inline Calculator trailingIcon and Focus navigation and highlight feedback
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { input ->
                        // Permit digits and at most one decimal point for high-precision decimal operations
                        if (input.all { it.isDigit() || it == '.' } && input.count { it == '.' } <= 1) {
                            amountStr = input
                            if (input.isNotBlank()) amountError = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(highlightBgColor, RoundedCornerShape(18.dp))
                        .testTag("amount_input"),
                    label = { Text("المبلغ بالريال اليمني *") },
                    placeholder = { Text("مثال: 10000.50") },
                    leadingIcon = {
                        Text(
                            "YR",
                            fontWeight = FontWeight.Black,
                            color = if (txType == "DEBT") debtRed else paymentGreen,
                            modifier = Modifier.padding(start = 12.dp, end = 8.dp)
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            calcExpression = amountStr
                            showCalculator = true
                        }) {
                            Icon(
                                 imageVector = Icons.Default.Calculate,
                                 contentDescription = "آلة حاسبة",
                                 tint = if (isDark) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.6f)
                            )
                        }
                    },
                    textStyle = TextStyle(
                        textAlign = TextAlign.End,
                        textDirection = TextDirection.Rtl,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (isDark) Color.White else Color.Black
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }
                    ),
                    isError = amountError,
                    supportingText = {
                        if (amountError) {
                            Text("الرجاء إدخال مبلغ صحيح أكبر من صفر", color = debtRed)
                        }
                    },
                    shape = RoundedCornerShape(18.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (txType == "DEBT") debtRed else paymentGreen,
                        unfocusedBorderColor = if (isDark) Color(0xFF323048) else Color(0xFFDCDDE1),
                        errorBorderColor = debtRed
                    )
                )

                // Dynamic Grand Formatted Preview bubble (قراءة حية للمبلغ لتأكيد القيمة)
                AnimatedVisibility(visible = amountStr.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (txType == "DEBT") debtRed.copy(alpha = 0.11f) else paymentGreen.copy(alpha = 0.11f))
                            .padding(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = if (txType == "DEBT") debtRed else paymentGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "المبلغ المكتوب: $dynamicFormattedPrice",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (txType == "DEBT") debtRed else paymentGreen
                            )
                        }
                    }
                }

                // 2. Custom date-picker and time-picker toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) Color(0xFF1E1D2F) else Color(0xFFF0F1FA))
                        .clickable {
                            // Open Date Picker followed by Time Picker
                            val calendar = Calendar.getInstance().apply { timeInMillis = calTimestamp }
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    calendar.set(Calendar.YEAR, y)
                                    calendar.set(Calendar.MONTH, m)
                                    calendar.set(Calendar.DAY_OF_MONTH, d)

                                    TimePickerDialog(
                                        context,
                                        { _, hr, min ->
                                            calendar.set(Calendar.HOUR_OF_DAY, hr)
                                            calendar.set(Calendar.MINUTE, min)
                                            calTimestamp = calendar.timeInMillis
                                        },
                                        calendar.get(Calendar.HOUR_OF_DAY),
                                        calendar.get(Calendar.MINUTE),
                                        false
                                    ).show()
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "تاريخ",
                            tint = PrimaryPurple,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = fTimeStr,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color.White else Color(0xFF2D3436)
                        )
                    }
                    Text(
                        text = "تعديل التاريخ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryPurple
                    )
                }

                // 3. Notes/Memo input with Done action to hide keyboard and clear focus
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .focusRequester(notesFocusRequester)
                        .testTag("memo_input"),
                    label = { Text("تفاصيل المعاملة / البيان (اختياري)") },
                    placeholder = { Text("مثال: ثمن كرتون عصير وقهوة وسكر...") },
                    leadingIcon = { Icon(Icons.Default.Notes, contentDescription = "تفاصيل") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    ),
                    shape = RoundedCornerShape(16.dp),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (txType == "DEBT") debtRed else paymentGreen,
                        unfocusedBorderColor = if (isDark) Color(0xFF323048) else Color(0xFFDCDDE1)
                    )
                )
            }

            // High-efficiency visual progress line that updates instantly
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(if (txType == "DEBT") debtRed else paymentGreen)
            )

            // Sticky Bottom Save Bar (Always outside scroll view)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDark) DarkBg else LightBg)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                val isAmountValid = (amountStr.toDoubleOrNull() ?: 0.0) > 0.0
                val finalSaveBtnColors = if (txType == "DEBT") {
                    listOf(debtRed, debtRed.copy(alpha = 0.85f))
                } else {
                    listOf(paymentGreen, paymentGreen.copy(alpha = 0.85f))
                }

                GradientButton(
                    text = if (isEditMode) "تعديل المعاملة وحفظها" else "حفظ المعاملة كأصل مالي",
                    onClick = {
                        val amountVal = amountStr.toDoubleOrNull() ?: 0.0
                        if (amountVal <= 0) {
                            amountError = true
                            viewModel.triggerVibration()
                        } else {
                            if (isEditMode && transactionIdToEdit != null) {
                                val updatedTrans = Transaction(
                                    id = transactionIdToEdit,
                                    customerId = customer.id,
                                    amount = amountVal,
                                    type = txType,
                                    notes = notes,
                                    timestamp = calTimestamp
                                )
                                viewModel.updateTransaction(updatedTrans)
                                onBackClick()
                            } else {
                                viewModel.addTransaction(customer.id, amountVal, txType, notes, calTimestamp) {
                                    onBackClick()
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_transaction_button"),
                    colors = finalSaveBtnColors,
                    enabled = isAmountValid // Zero-value protection! Disable if amount <= 0
                )
            }
        }
    }

    // Modern Floating Modal Bottom Sheet Calculator (Glassmorphic look)
    if (showCalculator) {
        ModalBottomSheet(
            onDismissRequest = { showCalculator = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = if (isDark) Color(0xEE1A1926) else Color(0xEEFFFFFF), // Glassmorphism semi-transparent bg
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
            dragHandle = { BottomSheetDefaults.DragHandle() },
            tonalElevation = 8.dp
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "الآلة الحاسبة السريعة",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else PrimaryPurple
                        )
                        Text(
                            text = if (txType == "DEBT") "دين عليه" else "تسديد منه",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (txType == "DEBT") debtRed else paymentGreen,
                            modifier = Modifier
                                .background(
                                    (if (txType == "DEBT") debtRed else paymentGreen).copy(alpha = 0.12f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Calculator Display
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isDark) Color(0xFF161524) else Color.White)
                            .border(
                                BorderStroke(1.dp, if (isDark) Color(0xFF2D2C45) else Color(0xFFE2E2FF)),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(16.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = if (calcExpression.isEmpty()) "٠" else calcExpression,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isDark) Color.White else Color(0xFF2D3436)
                            )
                            val previewVal = evaluateExpression(calcExpression)
                            if (previewVal != null) {
                                val formattedPreview = if (previewVal % 1.0 == 0.0) {
                                    previewVal.toLong().toString()
                                } else {
                                    "%.2f".format(Locale.ENGLISH, previewVal)
                                }
                                Text(
                                    text = "= $formattedPreview",
                                    fontSize = 15.sp,
                                    color = if (txType == "DEBT") debtRed else paymentGreen,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            } else {
                                Spacer(modifier = Modifier.height(20.dp))
                            }
                        }
                    }

                    // Keys layout
                    val btnBg = if (isDark) Color(0xFF2D2C45) else Color(0xFFE2E3ED)
                    val operatorBg = PrimaryPurple.copy(alpha = 0.15f)
                    val textCol = if (isDark) Color.White else Color(0xFF2D3436)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1.5f).clip(RoundedCornerShape(12.dp)).background(operatorBg).clickable { calcExpression += "/" }, contentAlignment = Alignment.Center) { Text("/", fontWeight = FontWeight.Bold, color = PrimaryPurple, fontSize = 18.sp) }
                        Box(modifier = Modifier.weight(1f).aspectRatio(1.5f).clip(RoundedCornerShape(12.dp)).background(btnBg).clickable { calcExpression += "9" }, contentAlignment = Alignment.Center) { Text("9", fontWeight = FontWeight.Bold, color = textCol) }
                        Box(modifier = Modifier.weight(1f).aspectRatio(1.5f).clip(RoundedCornerShape(12.dp)).background(btnBg).clickable { calcExpression += "8" }, contentAlignment = Alignment.Center) { Text("8", fontWeight = FontWeight.Bold, color = textCol) }
                        Box(modifier = Modifier.weight(1f).aspectRatio(1.5f).clip(RoundedCornerShape(12.dp)).background(btnBg).clickable { calcExpression += "7" }, contentAlignment = Alignment.Center) { Text("7", fontWeight = FontWeight.Bold, color = textCol) }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1.5f).clip(RoundedCornerShape(12.dp)).background(operatorBg).clickable { calcExpression += "*" }, contentAlignment = Alignment.Center) { Text("×", fontWeight = FontWeight.Bold, color = PrimaryPurple, fontSize = 18.sp) }
                        Box(modifier = Modifier.weight(1f).aspectRatio(1.5f).clip(RoundedCornerShape(12.dp)).background(btnBg).clickable { calcExpression += "6" }, contentAlignment = Alignment.Center) { Text("6", fontWeight = FontWeight.Bold, color = textCol) }
                        Box(modifier = Modifier.weight(1f).aspectRatio(1.5f).clip(RoundedCornerShape(12.dp)).background(btnBg).clickable { calcExpression += "5" }, contentAlignment = Alignment.Center) { Text("5", fontWeight = FontWeight.Bold, color = textCol) }
                        Box(modifier = Modifier.weight(1f).aspectRatio(1.5f).clip(RoundedCornerShape(12.dp)).background(btnBg).clickable { calcExpression += "4" }, contentAlignment = Alignment.Center) { Text("4", fontWeight = FontWeight.Bold, color = textCol) }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1.5f).clip(RoundedCornerShape(12.dp)).background(operatorBg).clickable { calcExpression += "-" }, contentAlignment = Alignment.Center) { Text("-", fontWeight = FontWeight.Bold, color = PrimaryPurple, fontSize = 18.sp) }
                        Box(modifier = Modifier.weight(1f).aspectRatio(1.5f).clip(RoundedCornerShape(12.dp)).background(btnBg).clickable { calcExpression += "3" }, contentAlignment = Alignment.Center) { Text("3", fontWeight = FontWeight.Bold, color = textCol) }
                        Box(modifier = Modifier.weight(1f).aspectRatio(1.5f).clip(RoundedCornerShape(12.dp)).background(btnBg).clickable { calcExpression += "2" }, contentAlignment = Alignment.Center) { Text("2", fontWeight = FontWeight.Bold, color = textCol) }
                        Box(modifier = Modifier.weight(1f).aspectRatio(1.5f).clip(RoundedCornerShape(12.dp)).background(btnBg).clickable { calcExpression += "1" }, contentAlignment = Alignment.Center) { Text("1", fontWeight = FontWeight.Bold, color = textCol) }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1.5f).clip(RoundedCornerShape(12.dp)).background(operatorBg).clickable { calcExpression += "+" }, contentAlignment = Alignment.Center) { Text("+", fontWeight = FontWeight.Bold, color = PrimaryPurple, fontSize = 18.sp) }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.5f)
                                .clip(RoundedCornerShape(12.dp))
                                .background((if (txType == "DEBT") debtRed else paymentGreen).copy(alpha = 0.15f))
                                .clickable {
                                    val eval = evaluateExpression(calcExpression)
                                    if (eval != null) {
                                        calcExpression = if (eval % 1.0 == 0.0) {
                                            eval.toLong().toString()
                                        } else {
                                            "%.2f".format(Locale.ENGLISH, eval)
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("=", fontWeight = FontWeight.Bold, color = if (txType == "DEBT") debtRed else paymentGreen, fontSize = 18.sp)
                        }
                        Box(modifier = Modifier.weight(1f).aspectRatio(1.5f).clip(RoundedCornerShape(12.dp)).background(btnBg).clickable { calcExpression += "0" }, contentAlignment = Alignment.Center) { Text("0", fontWeight = FontWeight.Bold, color = textCol) }
                        Box(modifier = Modifier.weight(1f).aspectRatio(1.5f).clip(RoundedCornerShape(12.dp)).background(btnBg).clickable { if (!calcExpression.endsWith(".")) calcExpression += "." }, contentAlignment = Alignment.Center) { Text(".", fontWeight = FontWeight.Bold, color = textCol, fontSize = 18.sp) }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val evalAmount = evaluateExpression(calcExpression) ?: calcExpression.toDoubleOrNull()
                        val isCalcValValid = evalAmount != null && evalAmount > 0.0

                        Button(
                            onClick = {
                                val eval = evaluateExpression(calcExpression) ?: calcExpression.toDoubleOrNull()
                                if (eval != null && eval > 0) {
                                    amountStr = if (eval % 1.0 == 0.0) {
                                        eval.toLong().toString()
                                    } else {
                                        "%.2f".format(Locale.ENGLISH, eval)
                                    }
                                    amountError = false

                                    // Slide Down / Confirm Animation Visual Feedback Highlighting
                                    scope.launch {
                                        isHighlighted = true
                                        delay(1200)
                                        isHighlighted = false
                                    }
                                }
                                showCalculator = false

                                // Auto-focus memo field after closing bottom sheet
                                scope.launch {
                                    delay(350) // Wait for bottom sheet slide down
                                    notesFocusRequester.requestFocus()
                                }
                            },
                            enabled = isCalcValValid, // Zero-Value protection on apply button
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (txType == "DEBT") debtRed else paymentGreen,
                                disabledContainerColor = (if (txType == "DEBT") debtRed else paymentGreen).copy(alpha = 0.35f)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(0.7f)
                                .height(54.dp)
                        ) {
                            Text("تأكيد الحساب", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        }

                        OutlinedButton(
                            onClick = { calcExpression = "" },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(0.3f)
                                .height(54.dp),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF323048) else Color(0xFFDCDDE1)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isDark) Color.White else Color.Black)
                        ) {
                            Text("مسح", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}
