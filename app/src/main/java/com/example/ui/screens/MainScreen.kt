package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.CustomerViewModel
import com.example.ui.viewmodel.CustomerWithBalance
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    viewModel: CustomerViewModel,
    isDark: Boolean,
    onCustomerClick: (Long) -> Unit,
    onAddCustomerClick: () -> Unit,
    onExit: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    val customersList by viewModel.customersWithBalances.collectAsState()
    val summary by viewModel.financialSummary.collectAsState()

    // Selection State
    var selectedCustomers by remember { mutableStateOf(setOf<Long>()) }
    var showExitDialog by remember { mutableStateOf(false) }

    val filteredCustomers = remember(customersList, searchQuery) {
        if (searchQuery.isBlank()) {
            customersList
        } else {
            customersList.filter { it.customer.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    fun formatCustomCurrency(amount: Double): String {
        val formatter = java.text.NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }
        return "${formatter.format(amount)} ر.ي"
    }

    // Intercept back clicks sequentially
    BackHandler(enabled = true) {
        if (selectedCustomers.isNotEmpty()) {
            selectedCustomers = emptySet()
        } else if (searchQuery.isNotEmpty()) {
            searchQuery = ""
        } else {
            showExitDialog = true
        }
    }

    // Elegant Exit Dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(PrimaryPurple.copy(alpha = 0.1f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "خروج من التطبيق",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    color = if (isDark) Color.White else Color(0xFF13121F)
                )
            },
            text = {
                Text(
                    text = "هل تريد البقاء في تطبيق حسابات حبايب ومتابعة ديونك؟",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = if (isDark) Color.LightGray else Color.DarkGray,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = { showExitDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("البقاء في التطبيق", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showExitDialog = false
                        onExit()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("خروج وإغلاق", color = Color.Gray, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddCustomerClick,
                containerColor = PrimaryPurple,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("add_customer_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة عميل")
            }
        },
        containerColor = if (isDark) DarkBg else LightBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // A. Search Bar Header
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("ابحث عن عميل بالاسم...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .testTag("search_field"),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryPurple,
                    unfocusedBorderColor = if (isDark) Color(0xFF2E2C4D) else Color(0xFFDCDDE1)
                )
            )

            // B. Side-by-Side Summary Cards (Habayeb Colors & Up/Down Arrows)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 'لي عند الناس' Card (Receivables)
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF2B191E) else Color(0xFFFFF1F1)
                    ),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF4C272E) else Color(0xFFFFD5D5))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "لي عند الناس",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFFFFB4B4) else Color(0xFFC53030)
                            )
                            Text("⬈", fontSize = 14.sp, color = NegativeRed, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        AutoSizeText(
                            text = formatCustomCurrency(summary.first),
                            baseFontSize = 18f,
                            color = NegativeRed
                        )
                    }
                }

                // 'علي للناس' Card (Payables)
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF132D24) else Color(0xFFF0FDF4)
                    ),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF1C4738) else Color(0xFFDCFCE7))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "علي للناس",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFFB4F4D5) else Color(0xFF15803D)
                            )
                            Text("⬊", fontSize = 14.sp, color = PositiveGreen, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        AutoSizeText(
                            text = formatCustomCurrency(summary.second),
                            baseFontSize = 18f,
                            color = PositiveGreen
                        )
                    }
                }
            }

            // Central Net Balance row widget so it still displays beautifully
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF1B1A2F) else Color.White
                ),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF29283F) else Color(0xFFECECFA))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الصافي الكلي للحسابات",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.LightGray else Color(0xFF2D3436)
                    )
                    val netColor = if (summary.third >= 0) PositiveGreen else NegativeRed
                    AutoSizeText(
                        text = formatCustomCurrency(summary.third),
                        baseFontSize = 16f,
                        color = netColor
                    )
                }
            }

            // C. Customers List Section
            val listCountText = if (selectedCustomers.isNotEmpty()) {
                "تم تحديد (${selectedCustomers.size})"
            } else {
                "العملاء والشخصيات (${filteredCustomers.size})"
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = listCountText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFFC5C5D8) else Color(0xFF555268)
                )
                if (selectedCustomers.isNotEmpty()) {
                    TextButton(onClick = { selectedCustomers = emptySet() }) {
                        Text("إلغاء التحديد", color = PrimaryPurple, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

            if (filteredCustomers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا يوجد عملاء حالياً.\nاضغط على + لإضافة عميل جديد.",
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
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
                    items(filteredCustomers) { item ->
                        val isSelected = selectedCustomers.contains(item.customer.id)
                        CustomerCard(
                            debtor = item,
                            isDark = isDark,
                            isSelected = isSelected,
                            onClick = {
                                if (selectedCustomers.isNotEmpty()) {
                                    selectedCustomers = if (isSelected) {
                                        selectedCustomers - item.customer.id
                                    } else {
                                        selectedCustomers + item.customer.id
                                    }
                                } else {
                                    onCustomerClick(item.customer.id)
                                }
                            },
                            onLongClick = {
                                selectedCustomers = selectedCustomers + item.customer.id
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CustomerCard(
    debtor: CustomerWithBalance,
    isDark: Boolean,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val formatter = java.text.NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
    }
    val balanceText = "${formatter.format(debtor.netBalance)} ر.ي"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                if (isDark) Color(0xFF232147) else Color(0xFFF0EDFF)
            } else {
                if (isDark) Color(0xFF1B1A2F) else Color.White
            }
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) PrimaryPurple else (if (isDark) Color(0xFF29283F) else Color(0xFFECECFA))
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = PrimaryPurple,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = debtor.customer.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF2D3436)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                if (debtor.customer.phone.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = debtor.customer.phone,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End, modifier = Modifier.widthIn(max = 130.dp)) {
                AutoSizeText(
                    text = balanceText,
                    baseFontSize = 15f,
                    color = if (debtor.netBalance > 0) NegativeRed else if (debtor.netBalance < 0) PositiveGreen else Color.Gray
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (debtor.netBalance > 0) "مطلوب منه" else if (debtor.netBalance < 0) "مسدد أكثر" else "متزن",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }
    }
}
