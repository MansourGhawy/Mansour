package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.ui.text.withStyle
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    viewModel: CustomerViewModel,
    isDark: Boolean,
    onAddCustomerClick: (String) -> Unit,
    onCustomerClick: (Int) -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    var isSearching by remember { mutableStateOf(false) }
    var isSearchFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val recentSearches by viewModel.recentSearches.collectAsState()
    val customersList by viewModel.customersWithBalances.collectAsState()
    val summary by viewModel.financialSummary.collectAsState()
    val filterOption by viewModel.filterOption.collectAsState()

    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    val selectedCustomers = remember { mutableStateListOf<CustomerWithBalance>() }
    val isSelectionMode = selectedCustomers.isNotEmpty()
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("حذف الزبائن المحددين", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من حذف ${selectedCustomers.size} من الزبائن والسجل المالي الخاص بهم بالكامل؟ لا يمكن التراجع عن هذا الإجراء.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteMultipleCustomers(selectedCustomers.map { it.customer }) {
                        selectedCustomers.clear()
                        showDeleteDialog = false
                    }
                }) {
                    Text("حذف", color = NegativeRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

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
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(PrimaryPurple, Color(0xFF8C7EF2))
                    )
                )
                .statusBarsPadding()
                .padding(top = 6.dp, bottom = 14.dp, start = 20.dp, end = 20.dp)
        ) {
            // Header Top Row: Logo & App title + Subtitle with Toggled Search Block
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectionMode) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { selectedCustomers.clear() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close Selection", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${selectedCustomers.size} محدد",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                    }
                } else if (isSearching) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchQuery.value = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .testTag("search_input")
                            .focusRequester(focusRequester)
                            .onFocusChanged { isSearchFocused = it.isFocused },
                        placeholder = { Text("ابحث عن اسم زبون...", color = Color.White.copy(alpha = 0.6f)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "ابحث", tint = Color.White) },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { 
                                        viewModel.searchQuery.value = "" 
                                        focusRequester.requestFocus()
                                    }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.White)
                                    }
                                }
                                IconButton(onClick = {
                                    isSearching = false
                                    viewModel.searchQuery.value = ""
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
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

                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }
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

            AnimatedVisibility(
                visible = isSearching && searchQuery.isEmpty() && isSearchFocused && recentSearches.isNotEmpty() && !isSelectionMode,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "عمليات البحث الأخيرة",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.clearRecentSearches() }
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "مسح الكل",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        recentSearches.forEach { search ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.clickable {
                                    viewModel.searchQuery.value = search
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = search,
                                        color = Color.White,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Main Balance Card inside Header with sleek background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.18f))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "إجمالي الرصيد الصافي",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = viewModel.formatCurrency(totalNet),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ريال",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.70f),
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
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
            val isGreenActive = filterOption == com.example.ui.viewmodel.FilterOption.RECEIVABLES
            val greenScale by androidx.compose.animation.core.animateFloatAsState(targetValue = if (isGreenActive && !isSelectionMode) 1.05f else 1.0f)

            // Stats Card 1: لي عند الناس (Debts due to me)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .scale(greenScale)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(enabled = !isSelectionMode) {
                        keyboardController?.hide()
                        if (isGreenActive) {
                            viewModel.filterOption.value = com.example.ui.viewmodel.FilterOption.ALL
                        } else {
                            viewModel.filterOption.value = com.example.ui.viewmodel.FilterOption.RECEIVABLES
                        }
                    },
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF1E1D2F) else Color.White
                ),
                border = BorderStroke(if (isGreenActive && !isSelectionMode) 2.dp else 1.dp, if (isGreenActive && !isSelectionMode) PositiveGreen else (if (isDark) Color(0x1F6C5CE7) else Color(0xFFF1F2F6))),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isGreenActive && !isSelectionMode) 8.dp else 0.dp)
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

            val isRedActive = filterOption == com.example.ui.viewmodel.FilterOption.PAYABLES
            val redScale by androidx.compose.animation.core.animateFloatAsState(targetValue = if (isRedActive && !isSelectionMode) 1.05f else 1.0f)

            // Stats Card 2: علي للناس (Debts I owe to others)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .scale(redScale)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(enabled = !isSelectionMode) {
                        keyboardController?.hide()
                        if (isRedActive) {
                            viewModel.filterOption.value = com.example.ui.viewmodel.FilterOption.ALL
                        } else {
                            viewModel.filterOption.value = com.example.ui.viewmodel.FilterOption.PAYABLES
                        }
                    },
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF1E1D2F) else Color.White
                ),
                border = BorderStroke(if (isRedActive && !isSelectionMode) 2.dp else 1.dp, if (isRedActive && !isSelectionMode) NegativeRed else (if (isDark) Color(0x1F6C5CE7) else Color(0xFFF1F2F6))),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isRedActive && !isSelectionMode) 8.dp else 0.dp)
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

        // Vertical space optimization: reduce the spacer and use a zero-height offset compensator since the cards were offset by -16.dp.
        Spacer(modifier = Modifier.height(0.dp))

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp),
            thickness = 0.5.dp,
            color = if (isDark) Color(0xFF323048) else Color(0xFFE5E5E5)
        )

        // Section header details (قائمة الزبائن + العدد)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "قائمة الزبائن",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color(0xFF2D3436)
            )
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(PrimaryPurple.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${customersList.size} " + if (customersList.size >= 11) "زبوناً" else "زبائن",
                    fontSize = 10.sp,
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
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = "No Results",
                            tint = if (isDark) Color(0xFF323048) else Color(0xFFE2E2FF),
                            modifier = Modifier.size(90.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "لم نعثر على زبون بالاسم:",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFA09EB5) else Color(0xFF5A527A)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "'$searchQuery'",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = PrimaryPurple
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { 
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                onAddCustomerClick(searchQuery) 
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "إضافة كزبون جديد", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        val filter = filterOption
                        val extIcon = if (filter == com.example.ui.viewmodel.FilterOption.RECEIVABLES) {
                            Icons.Default.TrendingUp
                        } else if (filter == com.example.ui.viewmodel.FilterOption.PAYABLES) {
                            Icons.Default.TrendingDown
                        } else {
                            Icons.Default.FolderOpen
                        }
                        
                        Icon(
                            imageVector = extIcon,
                            contentDescription = "Empty",
                            tint = if (isDark) Color(0xFF323048) else Color(0xFFE2E2FF),
                            modifier = Modifier.size(90.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val titleText = when (filter) {
                            com.example.ui.viewmodel.FilterOption.RECEIVABLES -> "لا توجد ديون مستحقة لك"
                            com.example.ui.viewmodel.FilterOption.PAYABLES -> "لا توجد مبالغ مستحقة عليك"
                            else -> "لا يوجد زبائن حالياً"
                        }
                        
                        val subtitleText = when (filter) {
                            com.example.ui.viewmodel.FilterOption.RECEIVABLES -> "رائع! جميع زبائنك قاموا بتسديد ديونهم أو ليس عليهم التزامات حالياً."
                            com.example.ui.viewmodel.FilterOption.PAYABLES -> "ممتاز! ليس عليك أي ديون أو مستحقات مالية للآخرين حالياً."
                            else -> "اضغط على زر (+) في الأسفل لإضافة زبونك الأول"
                        }
                        
                        Text(
                            text = titleText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFA09EB5) else Color(0xFF5A527A)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = subtitleText,
                            fontSize = 12.sp,
                            color = if (isDark) Color(0xFF747D8C) else Color(0xFF747D8C),
                            textAlign = TextAlign.Center
                        )
                        
                        if (filter != com.example.ui.viewmodel.FilterOption.ALL) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { 
                                    viewModel.filterOption.value = com.example.ui.viewmodel.FilterOption.ALL
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(text = "عرض جميع الزبائن", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 80.dp)
            ) {
                items(customersList, key = { it.customer.id }) { item ->
                    CustomerRow(
                        modifier = Modifier.animateItemPlacement(),
                        item = item,
                        searchQuery = searchQuery,
                        viewModel = viewModel,
                        isDark = isDark,
                        isSelected = selectedCustomers.contains(item),
                        isSelectionMode = isSelectionMode,
                        onLongClick = {
                            if (!isSelectionMode) {
                                selectedCustomers.add(item)
                                isSearching = false
                                viewModel.searchQuery.value = ""
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            }
                        },
                        onClick = {
                            if (isSelectionMode) {
                                if (selectedCustomers.contains(item)) {
                                    selectedCustomers.remove(item)
                                } else {
                                    selectedCustomers.add(item)
                                }
                            } else {
                                if (searchQuery.isNotEmpty()) {
                                    viewModel.addRecentSearch(item.customer.name)
                                }
                                onCustomerClick(item.customer.id)
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun CustomerRow(
    item: CustomerWithBalance,
    modifier: Modifier = Modifier,
    searchQuery: String = "",
    viewModel: CustomerViewModel,
    isDark: Boolean,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onLongClick: () -> Unit = {},
    onClick: () -> Unit
) {
    val charLogo = item.customer.name.trim().take(1).uppercase()
    val balanceText = viewModel.formatCurrency(kotlin.math.abs(item.netBalance))

    // Determine colors based on balance state
    // "أخضر إذا عليه دين (balance > 0)، أحمر إذا له عندي (balance < 0)، رمادي إذا تساوى"
    val indicatorColor = when {
        item.netBalance > 0 -> PositiveGreen // Receivable (لي عند الناس)
        item.netBalance < 0 -> NegativeRed   // Payable (علي للناس)
        else -> Color.Gray
    }

    val stateText = when {
        item.netBalance > 0 -> "عليه دين"
        item.netBalance < 0 -> "له عندي"
        else -> "متساوي"
    }

    val bgCardColor = if (isSelected) PrimaryPurple.copy(alpha = 0.15f) else (if (isDark) Color(0xFF1E1D2F) else Color.White)
    val borderColor = if (isSelected) PrimaryPurple else (if (isDark) Color(0x1F6C5CE7) else Color(0x0F6C5CE7))

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongClick() },
                    onTap = { onClick() }
                )
            }
            .testTag("customer_item_${item.customer.id}"),
        colors = CardDefaults.cardColors(containerColor = bgCardColor),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
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
                var searchHighlighted = false
                if (searchQuery.isNotEmpty()) {
                    val normName = com.example.utils.normalizeArabic(item.customer.name)
                    val normQuery = com.example.utils.normalizeArabic(searchQuery)
                    val startIndex = normName.indexOf(normQuery)
                    if (startIndex >= 0 && startIndex + normQuery.length <= item.customer.name.length) {
                        var tempHighText: androidx.compose.ui.text.AnnotatedString? = null
                        try {
                            val endIndex = startIndex + normQuery.length
                            tempHighText = androidx.compose.ui.text.buildAnnotatedString {
                                append(item.customer.name.substring(0, startIndex))
                                withStyle(style = androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Black, background = PrimaryPurple.copy(alpha=0.3f))) {
                                    append(item.customer.name.substring(startIndex, endIndex))
                                }
                                append(item.customer.name.substring(endIndex))
                            }
                            searchHighlighted = true
                        } catch (e: Exception) {
                            searchHighlighted = false
                        }
                        if (tempHighText != null) {
                            Text(
                                text = tempHighText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) Color.White else Color(0xFF2D3436),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                
                if (!searchHighlighted) {
                    Text(
                        text = item.customer.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF2D3436),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
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
