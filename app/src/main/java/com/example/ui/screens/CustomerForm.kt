package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.CustomerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerForm(
    viewModel: CustomerViewModel,
    isDark: Boolean,
    onBackClick: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    
    var nameError by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "زبون جديد",
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
            Text(
                text = "إضافة بيانات الزبون الجديد",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else PrimaryPurple
            )
            
            Text(
                text = "يرجى تعبئة الحقول أدناه. حقل الاسم إلزامي لإنشاء سجل حساب خاص بالزبون.",
                fontSize = 13.sp,
                color = if (isDark) Color(0xFFA09EB5) else Color(0xFF747D8C)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 1. Name input
            OutlinedTextField(
                value = name,
                onValueChange = { 
                    name = it
                    if (it.isNotBlank()) nameError = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("name_input"),
                label = { Text("اسم الزبون كاملاً *") },
                placeholder = { Text("مثال: محمد أحمد العباسي") },
                leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = "الاسم") },
                isError = nameError,
                supportingText = {
                    if (nameError) {
                        Text("الاسم حقل مطلوب، الرجاء إدخاله", color = NegativeRed)
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }
                ),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryPurple,
                    unfocusedBorderColor = if (isDark) Color(0xFF323048) else Color(0xFFDCDDE1),
                    errorBorderColor = NegativeRed
                )
            )

            // 2. Phone input
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("phone_input"),
                label = { Text("رقم الهاتف (اختياري)") },
                placeholder = { Text("مثال: 777XXXXXX") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "رقم الهاتف") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }
                ),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryPurple,
                    unfocusedBorderColor = if (isDark) Color(0xFF323048) else Color(0xFFDCDDE1)
                )
            )

            // 3. Notes input
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .testTag("notes_input"),
                label = { Text("ملاحظات إضافية (اختياري)") },
                placeholder = { Text("أضف أي تفاصيل أخرى، مثل العنوان أو تفاصيل التعامل والحد الأقصى للديون...") },
                leadingIcon = { Icon(Icons.Default.Notes, contentDescription = "ملاحظات") },
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
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryPurple,
                    unfocusedBorderColor = if (isDark) Color(0xFF323048) else Color(0xFFDCDDE1)
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            GradientButton(
                text = "حفظ وإضافة الزبون",
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                        viewModel.triggerVibration()
                    } else {
                        viewModel.addCustomer(name, phone, notes) {
                            onBackClick()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_customer_button")
            )
        }
    }
}

// Utility extension for remember state flow
@Composable
fun <T> rememberStateFlowOf(initialValue: T): MutableState<T> {
    return remember { mutableStateOf(initialValue) }
}
