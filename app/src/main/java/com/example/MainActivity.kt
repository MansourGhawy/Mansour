package com.example

import android.os.Bundle
import android.content.Context
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.shadow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.CustomerViewModel
import kotlinx.coroutines.flow.collectLatest

sealed class AppScreen {
    object Splash : AppScreen()
    object Dashboard : AppScreen()
    data class AddCustomer(val initialName: String = "") : AppScreen()
    object CustomerDetail : AppScreen()
    data class AddTransaction(val defaultType: String) : AppScreen()
    data class EditTransaction(val transactionId: Int) : AppScreen()
}

enum class HomeTab {
    DASHBOARD,
    REPORTS,
    SETTINGS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: CustomerViewModel = viewModel()
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()

            // Root Application Theme wrapper
            MyApplicationTheme(darkTheme = isDarkTheme) {
                // Ensure layout direction is strictly Right to Left (RTL) for perfect Arabic UI
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    AppNavigationContainer(viewModel = viewModel, isDark = isDarkTheme)
                }
            }
        }
    }
}

@Composable
fun AppNavigationContainer(
    viewModel: CustomerViewModel,
    isDark: Boolean
) {
    val context = LocalContext.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    // Observe shared status message toasts from viewModel
    LaunchedEffect(key1 = true) {
        viewModel.statusMessage.collectLatest { msg ->
            if (msg.isNotBlank()) {
                try {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        Toast.makeText(context.applicationContext, msg, Toast.LENGTH_SHORT).show()
                    }
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
            }
        }
    }

    // Custom Backstack State Machine
    var screenHistory by remember { mutableStateOf<List<AppScreen>>(listOf(AppScreen.Splash)) }
    val currentScreen = screenHistory.lastOrNull() ?: AppScreen.Dashboard

    val isFingerprintEnabled by viewModel.isFingerprintEnabled.collectAsState()
    val securityPinCode by viewModel.securityPin.collectAsState()
    var isAppUnlocked by remember { mutableStateOf(false) }

    LaunchedEffect(isFingerprintEnabled) {
        if (!isFingerprintEnabled) {
            isAppUnlocked = true
        }
    }

    // Active bottom navigation tab inside Dashboard screen
    var activeTab by remember { mutableStateOf(HomeTab.DASHBOARD) }

    val activity = (LocalContext.current as? androidx.activity.ComponentActivity)
    val prefs = remember { context.getSharedPreferences("hesabat_habayeb_prefs", Context.MODE_PRIVATE) }
    var showExitDialog by remember { mutableStateOf(false) }

    // Intercept hardware Android OS back button press
    val isLocked = currentScreen != AppScreen.Splash && isFingerprintEnabled && !isAppUnlocked
    BackHandler(enabled = !isLocked) {
        if (screenHistory.size > 1) {
            screenHistory = screenHistory.dropLast(1)
        } else {
            // Check skip_exit_dialog setting
            val skipExitDialog = prefs.getBoolean("skip_exit_dialog", false)
            if (skipExitDialog) {
                activity?.finish()
            } else {
                showExitDialog = true
            }
        }
    }

    // Helper navigators
    fun navigateTo(screen: AppScreen) {
        screenHistory = screenHistory + screen
    }

    fun navigateBack() {
        if (screenHistory.size > 1) {
            screenHistory = screenHistory.dropLast(1)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (showExitDialog) {
            var donotShowAgain by remember { mutableStateOf(false) }
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showExitDialog = false },
                properties = androidx.compose.ui.window.DialogProperties(
                    usePlatformDefaultWidth = false
                )
            ) {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl
                ) {
                    androidx.compose.material3.Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .shadow(
                                elevation = 12.dp,
                                shape = RoundedCornerShape(24.dp)
                            ),
                        shape = RoundedCornerShape(24.dp),
                        color = if (isDark) Color(0xFF1E1D2F) else Color.White
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Header Icon Container
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(
                                        color = if (isDark) Color(0xFF2E2A5D) else Color(0xFFF0EDFF),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                    contentDescription = "تسجيل الخروج",
                                    tint = PrimaryPurple,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Title TextView
                            Text(
                                text = "تأكيد الخروج",
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFC3BCF8) else Color(0xFF3A2C85),
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Description TextView
                            Text(
                                text = "هل أنت متأكد من رغبتك في الخروج من تطبيق حسابات حبايب؟",
                                color = if (isDark) Color(0xFFA09EB5) else Color(0xFF4B5563),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 22.sp,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Checkbox Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                        indication = null,
                                        onClick = { donotShowAgain = !donotShowAgain }
                                    )
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(
                                            color = if (donotShowAgain) PrimaryPurple else Color.Transparent,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .border(
                                            width = 1.5.dp,
                                            color = if (donotShowAgain) PrimaryPurple else (if (isDark) Color(0xFF42405F) else Color(0xFFD1D5DB)),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .testTag("skip_exit_checkbox"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (donotShowAgain) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Text(
                                    text = "عدم الإظهار مجدداً",
                                    fontSize = 13.sp,
                                    color = if (isDark) Color(0xFFA09EB5) else Color(0xFF4B5563),
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Action Buttons Row (RTL Compliant - Horizontal)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Button 1: Cancel/Stay ("متابعة العمل" - prominent brand gradient)
                                Button(
                                    onClick = { showExitDialog = false },
                                    shape = RoundedCornerShape(24.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent
                                    ),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(44.dp)
                                        .testTag("exit_dismiss_button")
                                        .shadow(elevation = 2.dp, shape = RoundedCornerShape(24.dp))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                                    colors = listOf(
                                                        Color(0xFF6C5CE7),
                                                        Color(0xFF5A49D8)
                                                    )
                                                ),
                                                shape = RoundedCornerShape(24.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "متابعة العمل",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }

                                // Button 2: Exit ("خروج" - soft coral red tint)
                                Button(
                                    onClick = {
                                        if (donotShowAgain) {
                                            prefs.edit().putBoolean("skip_exit_dialog", true).apply()
                                        }
                                        showExitDialog = false
                                        activity?.finish()
                                    },
                                    shape = RoundedCornerShape(24.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isDark) Color(0xFF3F2327) else Color(0xFFFFF1F0)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = 1.dp,
                                        color = if (isDark) Color(0xFFEF4444).copy(alpha = 0.5f) else Color(0xFFFCA5A5)
                                    ),
                                    modifier = Modifier
                                        .weight(0.8f)
                                        .height(44.dp)
                                        .testTag("exit_confirm_button")
                                ) {
                                    Text(
                                        text = "خروج",
                                        color = Color(0xFFEF4444),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = if (isDark) DarkBg else LightBg,
            bottomBar = {
                // Display Bottom bar only on primary Dashboard/Reports/Settings screen
                if (currentScreen == AppScreen.Dashboard) {
                    NavigationBar(
                        containerColor = if (isDark) Color(0xFF1E1D2F) else Color.White,
                        tonalElevation = 8.dp,
                        modifier = Modifier.navigationBarsPadding()
                    ) {
                        // TAB 1: الرئيسية
                        NavigationBarItem(
                            selected = activeTab == HomeTab.DASHBOARD,
                            onClick = {
                                viewModel.searchQuery.value = ""
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                activeTab = HomeTab.DASHBOARD
                                viewModel.triggerVibration()
                            },
                            icon = { Icon(Icons.Default.Home, contentDescription = "الرئيسية") },
                            label = { Text("الرئيسية") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = PrimaryPurple,
                                unselectedTextColor = Color.Gray,
                                indicatorColor = PrimaryPurple
                            )
                        )

                        // TAB 2: التقارير
                        NavigationBarItem(
                            selected = activeTab == HomeTab.REPORTS,
                            onClick = {
                                viewModel.searchQuery.value = ""
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                activeTab = HomeTab.REPORTS
                                viewModel.triggerVibration()
                            },
                            icon = { Icon(Icons.Default.Assessment, contentDescription = "التقارير") },
                            label = { Text("التقارير") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = PrimaryPurple,
                                unselectedTextColor = Color.Gray,
                                indicatorColor = PrimaryPurple
                            )
                        )

                        // TAB 3: الإعدادات
                        NavigationBarItem(
                            selected = activeTab == HomeTab.SETTINGS,
                            onClick = {
                                viewModel.searchQuery.value = ""
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                activeTab = HomeTab.SETTINGS
                                viewModel.triggerVibration()
                            },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "الإعدادات") },
                            label = { Text("الإعدادات") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = PrimaryPurple,
                                unselectedTextColor = Color.Gray,
                                indicatorColor = PrimaryPurple
                            )
                        )
                    }
                }
            },
            floatingActionButton = {
                // Show FAB inside Dashboard Tab only
                if (currentScreen == AppScreen.Dashboard && activeTab == HomeTab.DASHBOARD) {
                    FloatingActionButton(
                        onClick = {
                            viewModel.triggerVibration()
                            navigateTo(AppScreen.AddCustomer())
                        },
                        containerColor = PrimaryPurple,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "اضف زبون")
                    }
                }
            }
        ) { innerPadding ->
            // Animated transition layout between screen pages
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Crossfade(
                    targetState = currentScreen,
                    label = "ScreenTransitions"
                ) { targetScreen ->
                    when (targetScreen) {
                        is AppScreen.Splash -> {
                            SplashScreen(isDark = isDark) {
                                isAppUnlocked = false // force lock screen evaluation on startup
                                screenHistory = listOf(AppScreen.Dashboard)
                            }
                        }

                        is AppScreen.Dashboard -> {
                            when (activeTab) {
                                HomeTab.DASHBOARD -> {
                                    MainScreen(
                                        viewModel = viewModel,
                                        isDark = isDark,
                                        onAddCustomerClick = { name -> navigateTo(AppScreen.AddCustomer(name)) },
                                        onCustomerClick = { customerId ->
                                            viewModel.selectCustomer(customerId)
                                            navigateTo(AppScreen.CustomerDetail)
                                        }
                                    )
                                }
                                HomeTab.REPORTS -> {
                                    ReportsScreen(viewModel = viewModel, isDark = isDark)
                                }
                                HomeTab.SETTINGS -> {
                                    SettingsScreen(viewModel = viewModel, isDark = isDark)
                                }
                            }
                        }

                        is AppScreen.AddCustomer -> {
                            CustomerForm(
                                initialName = targetScreen.initialName,
                                viewModel = viewModel,
                                isDark = isDark,
                                onBackClick = { navigateBack() }
                            )
                        }

                        is AppScreen.CustomerDetail -> {
                            CustomerDetail(
                                viewModel = viewModel,
                                isDark = isDark,
                                onBackClick = { navigateBack() },
                                onAddTransactionClick = { type ->
                                    navigateTo(AppScreen.AddTransaction(type))
                                },
                                onEditTransactionClick = { transactionId ->
                                    navigateTo(AppScreen.EditTransaction(transactionId))
                                }
                            )
                        }

                        is AppScreen.AddTransaction -> {
                            TransactionForm(
                                viewModel = viewModel,
                                isDark = isDark,
                                defaultType = targetScreen.defaultType,
                                onBackClick = { navigateBack() }
                            )
                        }

                        is AppScreen.EditTransaction -> {
                            TransactionForm(
                                viewModel = viewModel,
                                isDark = isDark,
                                defaultType = "DEBT",
                                transactionIdToEdit = targetScreen.transactionId,
                                onBackClick = { navigateBack() }
                            )
                        }
                    }
                }
            }
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = isLocked,
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.fadeOut()
        ) {
            LockScreen(
                correctPin = securityPinCode,
                isDark = isDark,
                onUnlockSuccess = { isAppUnlocked = true }
            )
        }
    }
}
