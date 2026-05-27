package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.example.ui.theme.*
import com.example.ui.viewmodel.CustomerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionForm(
    viewModel: CustomerViewModel,
    customerId: Long,
    type: String, // "DEBT" or "PAYMENT"
    isDark: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val customersList by viewModel.customersWithBalances.collectAsState()
    val debtor = remember(customersList, customerId) { customersList.find { it.customer.id == customerId } }

    var amountStr by remember { mutableStateOf("0") }
    var note by remember { mutableStateOf("") }
    var calcExpression by remember { mutableStateOf("") }

    // Evaluates a simple expression string (containing +, -, *, /) safely
    fun evaluateSimpleMath(expr: String): Double {
        return try {
            val sanitized = expr.replace("×", "*").replace("÷", "/")
            if (sanitized.isBlank()) return 0.0
            
            // Basic sequential calculator evaluation
            val tokens = ArrayList<String>()
            var currentNum = StringBuilder()
            
            for (char in sanitized) {
                if (char in "+-*/") {
                    if (currentNum.isNotEmpty()) {
                        tokens.add(currentNum.toString())
                        currentNum = StringBuilder()
                    }
                    tokens.add(char.toString())
                } else {
                    currentNum.append(char)
                }
            }
            if (currentNum.isNotEmpty()) {
                tokens.add(currentNum.toString())
            }

            if (tokens.isEmpty()) return 0.0
            
            // Simple sequential operation left-to-right
            var result = tokens[0].toDoubleOrNull() ?: 0.0
            var i = 1
            while (i < tokens.size) {
                val op = tokens[i]
                val nextVal = tokens.getOrNull(i + 1)?.toDoubleOrNull() ?: 0.0
                when (op) {
                    "+" -> result += nextVal
                    "-" -> result -= nextVal
                    "*" -> result *= nextVal
                    "/" -> if (nextVal != 0.0) result /= nextVal
                }
                i += 2
            }
            result
        } catch (e: Exception) {
            0.0
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (type == "DEBT") "تسجيل رصيد متبقي عليه (دين)" else "تسجيل عملية سداد منه (دفعة)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
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
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("حدث خطأ: العميل غير متوفر")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    // Title section
                    Text(
                        text = "العميل: ${debtor.customer.name}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF2D3436)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Huge Amount Display
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF1E1D2F) else Color.White
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (calcExpression.isNotEmpty()) calcExpression else amountStr,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (type == "DEBT") NegativeRed else PositiveGreen,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "ريال يمني",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Text Input for Dynamic Note
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        placeholder = { Text("أدخل بياناً اختيارياً (مثال: حق القاطرة، مواد بناء)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("note_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = if (isDark) Color(0xFF2E2C4D) else Color(0xFFDCDDE1)
                        )
                    )
                }

                // Custom Numeric Calculator Grid
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val operatorBg = PrimaryPurple.copy(alpha = 0.15f)
                    
                    // Row 1: 7, 8, 9, ÷
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("7", "8", "9").forEach { num ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1.5f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isDark) Color(0xFF1E1D2F) else Color.White)
                                    .clickable {
                                        if (calcExpression.isNotEmpty()) calcExpression += num
                                        else {
                                            if (amountStr == "0") amountStr = num else amountStr += num
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(num, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.5f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(operatorBg)
                                .clickable {
                                    if (calcExpression.isEmpty()) calcExpression = amountStr + "÷"
                                    else calcExpression += "÷"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("÷", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PrimaryPurple)
                        }
                    }

                    // Row 2: 4, 5, 6, ×
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("4", "5", "6").forEach { num ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1.5f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isDark) Color(0xFF1E1D2F) else Color.White)
                                    .clickable {
                                        if (calcExpression.isNotEmpty()) calcExpression += num
                                        else {
                                            if (amountStr == "0") amountStr = num else amountStr += num
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(num, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Box(
                            modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1.5f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(operatorBg)
                                        .clickable {
                                            if (calcExpression.isEmpty()) calcExpression = amountStr + "×"
                                            else calcExpression += "×"
                                        },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("×", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PrimaryPurple)
                        }
                    }

                    // Row 3: 1, 2, 3, -
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("1", "2", "3").forEach { num ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1.5f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isDark) Color(0xFF1E1D2F) else Color.White)
                                    .clickable {
                                        if (calcExpression.isNotEmpty()) calcExpression += num
                                        else {
                                            if (amountStr == "0") amountStr = num else amountStr += num
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(num, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Box(
                            modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1.5f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(operatorBg)
                                        .clickable {
                                            if (calcExpression.isEmpty()) calcExpression = amountStr + "-"
                                            else calcExpression += "-"
                                        },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("-", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PrimaryPurple)
                        }
                    }

                    // Row 4: C, 0, =, +
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Clear All keys
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.5f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(NegativeRed.copy(alpha = 0.15f))
                                .clickable {
                                    amountStr = "0"
                                    calcExpression = ""
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("C", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NegativeRed)
                        }

                        // 0 Key
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.5f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0xFF1E1D2F) else Color.White)
                                .clickable {
                                    if (calcExpression.isNotEmpty()) calcExpression += "0"
                                    else {
                                        if (amountStr != "0") amountStr += "0"
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("0", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }

                        // Equals Key
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.5f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(PositiveGreen.copy(alpha = 0.15f))
                                .clickable {
                                    if (calcExpression.isNotEmpty()) {
                                        val calculated = evaluateSimpleMath(calcExpression)
                                        amountStr = calculated.toInt().toString()
                                        calcExpression = ""
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("=", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PositiveGreen)
                        }

                        // Plus Key
                        Box(
                            modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1.5f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(operatorBg)
                                        .clickable {
                                            if (calcExpression.isEmpty()) calcExpression = amountStr + "+"
                                            else calcExpression += "+"
                                        },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PrimaryPurple)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Final Submit Button
                    Button(
                        onClick = {
                            val finalAmt = if (calcExpression.isNotEmpty()) evaluateSimpleMath(calcExpression) else amountStr.toDoubleOrNull() ?: 0.0
                            if (finalAmt <= 0) {
                                Toast.makeText(context, "الرجاء إدخال مبلغ صحيح لتسجيل المعاملة", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.addTransaction(customerId, finalAmt, type, note)
                                Toast.makeText(context, "تم تسجيل المعاملة المالية بنجاح", Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_transaction_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        Text("تسجيل وحفظ المعاملة", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
