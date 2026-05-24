package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.CustomerViewModel
import com.example.ui.viewmodel.CustomerWithBalance
import com.example.ui.viewmodel.SortOption
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: CustomerViewModel,
    isDark: Boolean,
    onAddCustomerClick: () -> Unit,
    onCustomerClick: (Int) -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    var isSearching by remember { mutableStateOf(false) }
    val customersList by viewModel.customersWithBalances.collectAsState()
    val summary by viewModel.financialSummary.collectAsState()

    val totalTheyOweMe = summary.first
    val totalIMeOwe = summary.second
    val totalNet = summary.third

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) DarkBg else LightBg)
    ) {
        // 1. Sleek Gradient Header Section (PrimaryPurple to AccentPurple Gradient)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(PrimaryPurple, Color(0xFF8C7EF2))
                    )
                )
                .padding(top = 28.dp, bottom = 36.dp, start = 20.dp, end = 20.dp)
        ) {
            // Header Top Row: Logo & App title + Subtitle with Toggled Search Block
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSearching) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchQuery.value = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .testTag("search_input"),
                        placeholder = { Text("ابحث عن اسم زبون...", color = Color.White.copy(alpha = 0.6f)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "ابحث", tint = Color.White) },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.White)
                                    }
                                }
                                IconButton(onClick = {
                                    isSearching = false
                                    viewModel.searchQuery.value = ""
                                }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                                }
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.2f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.15f),
                            disabledContainerColor = Color.White.copy(alpha = 0.15f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = Color.White
                        ),
                        singleLine = true
                    )
                } else {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = "Wallet Logo",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "حسابات حبايب",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "إدارة ديونك بكل سهولة",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    IconButton(
                        onClick = { isSearching = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Balance Card inside Header with sleek background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.18f))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "إجمالي الرصيد الصافي",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.75f),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = viewModel.formatCurrency(totalNet),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "ريال يمني",
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.60f),
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // 2. Overlapping Quick Stats Chips (-16.dp offset vertical overlap)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-16).dp)
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Stats Card 1: لي عند الناس (Debts due to me)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF1E1D2F) else Color.White
                ),
                border = BorderStroke(1.dp, if (isDark) Color(0x1F6C5CE7) else Color(0xFFF1F2F6))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(PositiveGreen.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "ما لي",
                            tint = PositiveGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "لي عند الناس",
                            fontSize = 10.sp,
                            color = if (isDark) Color(0xFFA09EB5) else Color(0xFF8C90A6),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = viewModel.formatCurrency(totalTheyOweMe),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PositiveGreen,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Stats Card 2: علي للناس (Debts I owe to others)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF1E1D2F) else Color.White
                ),
                border = BorderStroke(1.dp, if (isDark) Color(0x1F6C5CE7) else Color(0xFFF1F2F6))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(NegativeRed.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingDown,
                            contentDescription = "عليّ",
                            tint = NegativeRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "علي للناس",
                            fontSize = 10.sp,
                            color = if (isDark) Color(0xFFA09EB5) else Color(0xFF8C90A6),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = viewModel.formatCurrency(totalIMeOwe),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = NegativeRed,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Shift content upward
        Spacer(modifier = Modifier.height(2.dp))

        // Section header details (قائمة الزبائن + العدد)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "قائمة الزبائن",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color(0xFF2D3436)
            )
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(PrimaryPurple.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${customersList.size} " + if (customersList.size >= 11) "زبوناً" else "زبائن",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryPurple
                )
            }
        }

        // Customers List
        if (customersList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Empty",
                        tint = if (isDark) Color(0xFF323048) else Color(0xFFE2E2FF),
                        modifier = Modifier.size(90.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "لم نعثر على أي زبون بهذا الاسم" else "لا يوجد زبائن حالياً",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFA09EB5) else Color(0xFF5A527A)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "تأكد من كتابة الاسم بطريقة صحيحة" else "اضغط على زر (+) في الأسفل لإضافة زبونك الأول",
                        fontSize = 12.sp,
                        color = if (isDark) Color(0xFF747D8C) else Color(0xFF747D8C),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 80.dp)
            ) {
                items(customersList, key = { it.customer.id }) { item ->
                    CustomerRow(
                        item = item,
                        viewModel = viewModel,
                        isDark = isDark,
                        onClick = { onCustomerClick(item.customer.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun CustomerRow(
    item: CustomerWithBalance,
    viewModel: CustomerViewModel,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val charLogo = item.customer.name.trim().take(1).uppercase()
    val balanceText = viewModel.formatCurrency(kotlin.math.abs(item.netBalance))

    // Determine colors based on balance state
    // "أخضر إذا له عندي (balance < 0)، أحمر إذا عليه دين (balance > 0)، رمادي إذا تساوى"
    val indicatorColor = when {
        item.netBalance < 0 -> PositiveGreen
        item.netBalance > 0 -> NegativeRed
        else -> Color.Gray
    }

    val stateText = when {
        item.netBalance < 0 -> "له عندي (دائن)"
        item.netBalance > 0 -> "عليه دين (مدين)"
        else -> "متساوي"
    }

    val bgCardColor = if (isDark) Color(0xFF1E1D2F) else Color.White
    val borderColor = if (isDark) Color(0x1F6C5CE7) else Color(0x0F6C5CE7)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("customer_item_${item.customer.id}"),
        colors = CardDefaults.cardColors(containerColor = bgCardColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circle Logo with Initials
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        color = indicatorColor.copy(alpha = 0.15f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = charLogo,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = indicatorColor
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Text info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.customer.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF2D3436),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Last transaction date/notes
                val lastActivityStr = if (item.lastTransactionTime != null) {
                    val sdf = SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale("ar"))
                    val dateFormatted = sdf.format(Date(item.lastTransactionTime))
                    "آخر تعديل: $dateFormatted"
                } else {
                    "لا توجد عمليات حالياً"
                }
                
                Text(
                    text = lastActivityStr,
                    fontSize = 11.sp,
                    color = if (isDark) Color(0xFFA09EB5) else Color(0xFF747D8C),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Net balance side
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = balanceText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = indicatorColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stateText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = indicatorColor.copy(alpha = 0.85f)
                )
            }
        }
    }
}
