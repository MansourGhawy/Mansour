package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Transaction
import com.example.ui.viewmodel.CustomerViewModel
import java.text.SimpleDateFormat
import java.util.*

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

    // Preload if editing
    LaunchedEffect(transactionIdToEdit) {
        if (transactionIdToEdit != null) {
            isEditMode = true
            val trans = viewModel.allTransactions.value.firstOrNull { it.id == transactionIdToEdit }
            if (trans != null) {
                txType = trans.type
                amountStr = trans.amount.toInt().toString()
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
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                    modifier = Modifier.padding(14.dp),
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
                        .background(if (txType == "DEBT") PositiveGreen else Color.Transparent)
                        .clickable { txType = "DEBT" }
                        .padding(vertical = 12.dp),
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
                        .background(if (txType == "PAYMENT") NegativeRed else Color.Transparent)
                        .clickable { txType = "PAYMENT" }
                        .padding(vertical = 12.dp),
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

            // 1. Amount input with inline Calculator trailingIcon and Focus navigation
            OutlinedTextField(
                value = amountStr,
                onValueChange = { input ->
                    if (input.all { it.isDigit() }) {
                        amountStr = input
                        if (input.isNotBlank()) amountError = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("amount_input"),
                label = { Text("المبلغ بالريال اليمني *") },
                placeholder = { Text("مثال: 1000") },
                leadingIcon = {
                    Text(
                        "YR",
                        fontWeight = FontWeight.Black,
                        color = if (txType == "DEBT") PositiveGreen else NegativeRed,
                        modifier = Modifier.padding(start = 12.dp, end = 8.dp)
                    )
                },
                trailingIcon = {
                    IconButton(onClick = {
                        showCalculator = !showCalculator
                        if (showCalculator && calcExpression.isEmpty()) {
                            calcExpression = amountStr
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = "آلة حاسبة",
                            tint = if (showCalculator) PrimaryPurple else (if (isDark) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.6f))
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }
                ),
                isError = amountError,
                supportingText = {
                    if (amountError) {
                        Text("الرجاء إدخال مبلغ صحيح أكبر من صفر", color = NegativeRed)
                    }
                },
                shape = RoundedCornerShape(18.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (txType == "DEBT") PositiveGreen else NegativeRed,
                    unfocusedBorderColor = if (isDark) Color(0xFF323048) else Color(0xFFDCDDE1),
                    errorBorderColor = NegativeRed
                )
            )

            // Beautiful slide-down expandable custom calculator widget
            AnimatedVisibility(visible = showCalculator) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF1E1D2F) else Color(0xFFF0F1FA)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF2D2C45) else Color(0xFFE2E2FF))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Calculator Display
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isDark) Color(0xFF161524) else Color.White)
                                .padding(12.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = if (calcExpression.isEmpty()) "٠" else calcExpression,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color(0xFF2D3436)
                                )
                                val previewVal = evaluateExpression(calcExpression)
                                if (previewVal != null) {
                                    Text(
                                        text = "= ${previewVal.toLong()}",
                                        fontSize = 14.sp,
                                        color = PositiveGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Calculator Keys Layout Grid (4 rows)
                        val btnBg = if (isDark) Color(0xFF2D2C45) else Color(0xFFE2E3ED)
                        val operatorBg = PrimaryPurple.copy(alpha = 0.15f)
                        val textCol = if (isDark) Color.White else Color(0xFF2D3436)
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.weight(1f).aspectRatio(1.5f).clip(RoundedCornerShape(12.dp)).background(btnBg).clickable { calcExpression += "7" }, contentAlignment = Alignment.Center) { Text("7", fontWeight = FontWeight.Bold, color = textCol) }
                            Box(modifier = Modifier.weight(1f).aspectRatio(1.5f).clip(RoundedCornerShape(12.dp)).background(btnBg).clickable { calcExpression += "8" }, contentAlignment = Alignment.Center) { Text("8", fontWeight = FontWeight.Bold, color = textCol) }
                            Box(modifier = Modifier.weight(1f).aspectRatio(1.5f).clip(RoundedCornerShape(12.dp)).background(btnBg).clickable { calcExpression += "9" }, contentAlignment = Alignment.Center) { Text("9", fontWeight = FontWeight.Bold, color = textCol) }
                            Box(modifier = Modifier.weight(1f).aspectRatio(1.5f).clip(RoundedCornerShape(12.dp)).background(operatorBg).clickable { calcExpression += "/" }, contentAlignment = Alignment.Center) { Text("/", fontWeight = FontWeight.Bold, color = PrimaryPurple, fontSize = 18.sp) }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.weight(1f).aspectRatio(1.5f).clip(RoundedCornerShape(12.dp)).background(btnBg).clickable { calcExpression += "4" }, contentAlignment = Alignment.Center) { Text("4", fontWeight = FontWeight.Bold, color = textCol) }
                            Box(modifier = Modifier.weight(1f).aspectRatio(1.5f).clip(RoundedCornerShape(12.dp)).background(btnBg).clickable { calcExpression += "5" }, contentAlignment = Alignment.Center) { Text("5", fontWeight = FontWeight.Bold, color = textCol) }
                            Box(modifier = Modifier.weight(1f).aspectRatio(1.5f).clip(RoundedCornerShape(12.dp)).background(btnBg).clickable { calcExpression += "6" }, contentAlignment = Alignment.Center) { Text("6", fontWeight = FontWeight.Bold, color = textCol) }
                            Box(modifier = Modifier.weight(1f).aspectRatio(1.5f).clip(RoundedCornerShape(12.dp)).background(operatorBg).clickable { calcExpression += "*" }, contentAlignment = Alignment.Center) { Text("×", fontWeight = FontWeight.Bold, color = PrimaryPurple, fontSize = 18.sp) }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.weight(1f).aspectRatio(1.5f).clip(RoundedCornerShape(12.dp)).background(btnBg).clickable { calcExpression += "1" }, contentAlignment = Alignment.Center) { Text("1", fontWeight = FontWeight.Bold, color = textCol) }
                            Box(modifier = Modifier.weight(1f).aspectRatio(1.5f).clip(RoundedCornerShape(12.dp)).background(btnBg).clickable { calcExpression += "2" }, contentAlignment = Alignment.Center) { Text("2", fontWeight = FontWeight.Bold, color = textCol) }
                            Box(modifier = Modifier.weight(1f).aspectRatio(1.5f).clip(RoundedCornerShape(12.dp)).background(btnBg).clickable { calcExpression += "3" }, contentAlignment = Alignment.Center) { Text("3", fontWeight = FontWeight.Bold, color = textCol) }
                            Box(modifier = Modifier.weight(1f).aspectRatio(1.5f).clip(RoundedCornerShape(12.dp)).background(operatorBg).clickable { calcExpression += "-" }, contentAlignment = Alignment.Center) { Text("-", fontWeight = FontWeight.Bold, color = PrimaryPurple, fontSize = 18.sp) }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.weight(1f).aspectRatio(1.5f).clip(RoundedCornerShape(12.dp)).background(PositiveGreen.copy(alpha = 0.15f)).clickable { calcExpression = "" }, contentAlignment = Alignment.Center) { Text("مسح", fontWeight = FontWeight.Bold, color = PositiveGreen, fontSize = 12.sp) }
                            Box(modifier = Modifier.weight(1f).aspectRatio(1.5f).clip(RoundedCornerShape(12.dp)).background(btnBg).clickable { calcExpression += "0" }, contentAlignment = Alignment.Center) { Text("0", fontWeight = FontWeight.Bold, color = textCol) }
                            Box(modifier = Modifier.weight(1f).aspectRatio(1.5f).clip(RoundedCornerShape(12.dp)).background(NegativeRed.copy(alpha = 0.15f)).clickable { 
                                val eval = evaluateExpression(calcExpression)
                                if (eval != null) {
                                    calcExpression = eval.toLong().toString()
                                }
                            }, contentAlignment = Alignment.Center) { Text("=", fontWeight = FontWeight.Bold, color = PositiveGreen, fontSize = 18.sp) }
                            Box(modifier = Modifier.weight(1f).aspectRatio(1.5f).clip(RoundedCornerShape(12.dp)).background(operatorBg).clickable { calcExpression += "+" }, contentAlignment = Alignment.Center) { Text("+", fontWeight = FontWeight.Bold, color = PrimaryPurple, fontSize = 18.sp) }
                        }

                        // Apply Button
                        Button(
                            onClick = {
                                val eval = evaluateExpression(calcExpression) ?: calcExpression.toDoubleOrNull()
                                if (eval != null && eval > 0) {
                                    amountStr = eval.toLong().toString()
                                    amountError = false
                                } else if (calcExpression.all { it.isDigit() } && calcExpression.isNotBlank()) {
                                    amountStr = calcExpression
                                    amountError = false
                                }
                                showCalculator = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("تطبيق المبلغ المتوقع لحساب السجل", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Dynamic Grand Formatted Preview bubble (قراءة حية للمبلغ لتأكيد القيمة)
            AnimatedVisibility(visible = amountStr.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (txType == "DEBT") PositiveGreen.copy(alpha = 0.11f) else NegativeRed.copy(alpha = 0.11f))
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = if (txType == "DEBT") PositiveGreen else NegativeRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "المبلغ المكتوب: $dynamicFormattedPrice",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (txType == "DEBT") PositiveGreen else NegativeRed
                        )
                    }
                }
            }

            // 2. Custom date-picker and time-picker toggle
            Text(
                text = "التاريخ والوقت:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFA09EB5) else Color(0xFF747D8C)
            )

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
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "تاريخ",
                        tint = PrimaryPurple,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = fTimeStr,
                        fontSize = 14.sp,
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
                    focusedBorderColor = if (txType == "DEBT") PositiveGreen else NegativeRed,
                    unfocusedBorderColor = if (isDark) Color(0xFF323048) else Color(0xFFDCDDE1)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Save transaction button
            val finalSaveBtnColors = if (txType == "DEBT") {
                listOf(PositiveGreen, SecondaryTurquoise)
            } else {
                listOf(NegativeRed, AccentPink)
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
                colors = finalSaveBtnColors
            )
        }
    }
}
