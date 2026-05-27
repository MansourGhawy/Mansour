package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.screens.*
import com.example.ui.theme.HesabatHabayebTheme
import com.example.ui.viewmodel.CustomerViewModel

sealed class Screen {
    object MainList : Screen()
    object CustomerForm : Screen()
    data class CustomerDetail(val id: Long) : Screen()
    data class TransactionForm(val id: Long, val type: String) : Screen()
}

class MainActivity : ComponentActivity() {

    private val viewModel: CustomerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val sharedPref = remember { getSharedPreferences("hesabat_prefs", Context.MODE_PRIVATE) }
            
            // Themes States
            var isDarkTheme by remember { 
                mutableStateOf(sharedPref.getBoolean("dark_theme", false)) 
            }

            // Splash, Security Lock, and Navigation states
            var showSplash by remember { mutableStateOf(true) }
            var isLocked by remember { 
                mutableStateOf(sharedPref.getBoolean("use_passcode", false) && sharedPref.getString("passcode", "")?.isNotEmpty() == true) 
            }

            // Stateful Backstack
            val backStack = remember { mutableStateListOf<Screen>(Screen.MainList) }
            val currentScreen = backStack.lastOrNull() ?: Screen.MainList

            // Bottom Navigation States (Holds when on MainList tab)
            var selectedTab by remember { mutableIntStateOf(0) }

            fun navigateTo(screen: Screen) {
                backStack.add(screen)
            }

            fun navigateBack() {
                if (backStack.size > 1) {
                    backStack.removeAt(backStack.lastIndex)
                }
            }

            HesabatHabayebTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when {
                        showSplash -> {
                            SplashScreen(
                                onSplashFinished = { showSplash = false }
                            )
                        }
                        isLocked -> {
                            LockScreen(
                                onUnlockDismiss = { isLocked = false }
                            )
                        }
                        else -> {
                            // Hardware Backpress sequential flow handler
                            BackHandler(enabled = backStack.size > 1) {
                                navigateBack()
                            }

                            Scaffold(
                                bottomBar = {
                                    if (currentScreen is Screen.MainList) {
                                        BottomNavigationBar(
                                            selectedTab = selectedTab,
                                            onTabSelected = { tab -> selectedTab = tab }
                                        )
                                    }
                                }
                            ) { paddingValues ->
                                Box(modifier = Modifier.padding(paddingValues)) {
                                    when (currentScreen) {
                                        is Screen.MainList -> {
                                            when (selectedTab) {
                                                0 -> MainScreen(
                                                    viewModel = viewModel,
                                                    isDark = isDarkTheme,
                                                    onCustomerClick = { customerId ->
                                                        navigateTo(Screen.CustomerDetail(customerId))
                                                    },
                                                    onAddCustomerClick = {
                                                        navigateTo(Screen.CustomerForm)
                                                    },
                                                    onExit = { finish() }
                                                )
                                                1 -> ReportsScreen(
                                                    viewModel = viewModel,
                                                    isDark = isDarkTheme
                                                )
                                                2 -> SettingsScreen(
                                                    isDark = isDarkTheme,
                                                    onDarkThemeChange = { nextThemeVal ->
                                                        isDarkTheme = nextThemeVal
                                                        sharedPref.edit().putBoolean("dark_theme", nextThemeVal).apply()
                                                    }
                                                )
                                            }
                                        }
                                        is Screen.CustomerForm -> {
                                            CustomerForm(
                                                viewModel = viewModel,
                                                isDark = isDarkTheme,
                                                onBack = { navigateBack() }
                                            )
                                        }
                                        is Screen.CustomerDetail -> {
                                            CustomerDetail(
                                                viewModel = viewModel,
                                                customerId = currentScreen.id,
                                                isDark = isDarkTheme,
                                                onBack = { navigateBack() },
                                                onAddTransactionClick = { uid, txType ->
                                                    navigateTo(Screen.TransactionForm(uid, txType))
                                                }
                                            )
                                        }
                                        is Screen.TransactionForm -> {
                                            TransactionForm(
                                                viewModel = viewModel,
                                                customerId = currentScreen.id,
                                                type = currentScreen.type,
                                                isDark = isDarkTheme,
                                                onBack = { navigateBack() }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar {
        val tabs = listOf(
            Triple(0, "الحسابات", Icons.Default.Person),
            Triple(1, "التقارير", Icons.Default.List),
            Triple(2, "الإعدادات", Icons.Default.Settings)
        )

        tabs.forEach { (index, title, icon) ->
            NavigationBarItem(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                icon = { Icon(icon, contentDescription = title) },
                label = { Text(title) }
            )
        }
    }
}
