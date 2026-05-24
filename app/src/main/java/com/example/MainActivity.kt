package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
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
    object AddCustomer : AppScreen()
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
    val screenHistory = remember { mutableStateListOf<AppScreen>(AppScreen.Splash) }
    val currentScreen = screenHistory.lastOrNull() ?: AppScreen.Dashboard

    // Active bottom navigation tab inside Dashboard screen
    var activeTab by remember { mutableStateOf(HomeTab.DASHBOARD) }

    // Intercept hardware Android OS back button press
    BackHandler(enabled = screenHistory.size > 1) {
        if (screenHistory.size > 1) {
            screenHistory.removeLast()
        }
    }

    // Helper navigators
    fun navigateTo(screen: AppScreen) {
        screenHistory.add(screen)
    }

    fun navigateBack() {
        if (screenHistory.size > 1) {
            screenHistory.removeLast()
        }
    }

    val isFingerprintEnabled by viewModel.isFingerprintEnabled.collectAsState()
    val securityPinCode by viewModel.securityPin.collectAsState()
    var isAppUnlocked by remember { mutableStateOf(false) }

    LaunchedEffect(isFingerprintEnabled) {
        if (!isFingerprintEnabled) {
            isAppUnlocked = true
        }
    }

    if (currentScreen != AppScreen.Splash && isFingerprintEnabled && !isAppUnlocked) {
        LockScreen(
            correctPin = securityPinCode,
            isDark = isDark,
            onUnlockSuccess = { isAppUnlocked = true }
        )
    } else {
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
                            navigateTo(AppScreen.AddCustomer)
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
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        fadeIn(animationSpec = spring()) togetherWith fadeOut(animationSpec = spring())
                    },
                    label = "ScreenTransitions"
                ) { targetScreen ->
                    when (targetScreen) {
                        is AppScreen.Splash -> {
                            SplashScreen(isDark = isDark) {
                                isAppUnlocked = false // force lock screen evaluation on startup
                                screenHistory.clear()
                                screenHistory.add(AppScreen.Dashboard)
                            }
                        }

                        is AppScreen.Dashboard -> {
                            when (activeTab) {
                                HomeTab.DASHBOARD -> {
                                    MainScreen(
                                        viewModel = viewModel,
                                        isDark = isDark,
                                        onAddCustomerClick = { navigateTo(AppScreen.AddCustomer) },
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
    }
}
