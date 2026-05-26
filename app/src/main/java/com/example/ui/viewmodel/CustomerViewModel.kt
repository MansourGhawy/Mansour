package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Customer
import com.example.data.model.Transaction
import com.example.data.repository.CustomerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale

data class CustomerWithBalance(
    val customer: Customer,
    val totalDebt: Double,
    val totalPayment: Double,
    val netBalance: Double, // positive means they owe me (عليه دين), negative means I owe them (له عندي)
    val lastTransactionTime: Long?,
    val lastTransactionNotes: String?
)

enum class SortOption {
    NAME,
    BALANCE_DESC,
    BALANCE_ASC,
    LAST_TRANSACTION
}

enum class FilterOption {
    ALL, RECEIVABLES, PAYABLES
}

class CustomerViewModel(application: Application) : AndroidViewModel(application) {
    private var repository: CustomerRepository = CustomerRepository(
        AppDatabase.getDatabase(application).customerDao()
    )
    
    // UI Theme state
    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    // Fingerprint lock logic simulation
    private val _isFingerprintEnabled = MutableStateFlow(false)
    val isFingerprintEnabled: StateFlow<Boolean> = _isFingerprintEnabled.asStateFlow()

    // Security PIN State
    private val _securityPin = MutableStateFlow("1234")
    val securityPin: StateFlow<String> = _securityPin.asStateFlow()

    // Temporary toast or status message
    private val _statusMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val statusMessage = _statusMessage.asSharedFlow()

    // Query and sort
    val searchQuery = MutableStateFlow("")
    val sortOption = MutableStateFlow(SortOption.NAME)
    val filterOption = MutableStateFlow(FilterOption.ALL)

    // Current selected customer for details
    private val _selectedCustomerId = MutableStateFlow<Int?>(null)
    val selectedCustomerId: StateFlow<Int?> = _selectedCustomerId.asStateFlow()

    // Google Drive integration states
    private val _isGoogleSignedIn = MutableStateFlow(false)
    val isGoogleSignedIn: StateFlow<Boolean> = _isGoogleSignedIn.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    private val _googleEmail = MutableStateFlow<String?>(null)
    val googleEmail: StateFlow<String?> = _googleEmail.asStateFlow()

    private val _googleDisplayName = MutableStateFlow<String?>(null)
    val googleDisplayName: StateFlow<String?> = _googleDisplayName.asStateFlow()

    private val _autoBackupEnabled = MutableStateFlow(false)
    val autoBackupEnabled: StateFlow<Boolean> = _autoBackupEnabled.asStateFlow()

    private val _lastBackupStatus = MutableStateFlow("")
    val lastBackupStatus: StateFlow<String> = _lastBackupStatus.asStateFlow()

    private val _driveSyncing = MutableStateFlow(false)
    val driveSyncing: StateFlow<Boolean> = _driveSyncing.asStateFlow()

    init {
        try {
            val prefs = application.getSharedPreferences("hesabat_habayeb_prefs", Context.MODE_PRIVATE)
            _isDarkTheme.value = prefs.getBoolean("dark_theme", false)
            _isFingerprintEnabled.value = prefs.getBoolean("fingerprint_enabled", false)
            _securityPin.value = prefs.getString("security_pin", "1234") ?: "1234"
            val historyStr = prefs.getString("recent_searches", "") ?: ""
            if (historyStr.isNotEmpty()) {
                _recentSearches.value = historyStr.split("|||")
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }

        try {
            val database = AppDatabase.getDatabase(application)
            repository = CustomerRepository(database.customerDao())
        } catch (t: Throwable) {
            t.printStackTrace()
        }

        try {
            refreshGoogleDriveState()
        } catch (t: Throwable) {
            t.printStackTrace()
        }

        try {
            triggerAutoBackupIfNeeded()
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    fun refreshGoogleDriveState() {
        try {
            val ctx = getApplication<Application>()
            val signedIn = com.example.data.repository.GoogleDriveBackupHelper.isUserSignedIn(ctx)
            _isGoogleSignedIn.value = signedIn
            _googleEmail.value = com.example.data.repository.GoogleDriveBackupHelper.getSignedInEmail(ctx)
            _googleDisplayName.value = com.example.data.repository.GoogleDriveBackupHelper.getSignedInName(ctx)
            _autoBackupEnabled.value = com.example.data.repository.GoogleDriveBackupHelper.isAutoBackupEnabled(ctx)
            _lastBackupStatus.value = com.example.data.repository.GoogleDriveBackupHelper.getBackupStatus(ctx)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        val ctx = getApplication<Application>()
        com.example.data.repository.GoogleDriveBackupHelper.setAutoBackupEnabled(ctx, enabled)
        _autoBackupEnabled.value = enabled
        triggerVibration()
        if (enabled && isGoogleSignedIn.value) {
            triggerAutoBackupIfNeeded()
        }
    }

    fun syncBackupToGoogleDrive() {
        if (!_isGoogleSignedIn.value) return
        viewModelScope.launch {
            _driveSyncing.value = true
            val ctx = getApplication<Application>()
            val binaryData = com.example.data.repository.BackupHelper.createSingleBinaryBackup(ctx)
            if (binaryData == null) {
                _statusMessage.emit("فشل تصدير البيانات محلياً")
                _driveSyncing.value = false
                return@launch
            }
            when (val result = com.example.data.repository.GoogleDriveBackupHelper.uploadBackup(ctx, binaryData)) {
                is com.example.data.repository.GDriveResult.Success -> {
                    _statusMessage.emit("تم مزامنة النسخة السحابية بنجاح")
                    _lastBackupStatus.value = com.example.data.repository.GoogleDriveBackupHelper.getBackupStatus(ctx)
                    triggerVibration()
                }
                is com.example.data.repository.GDriveResult.Error -> {
                    _statusMessage.emit(result.message)
                }
            }
            _driveSyncing.value = false
        }
    }

    fun restoreBackupFromGoogleDrive() {
        if (!_isGoogleSignedIn.value) return
        viewModelScope.launch {
            _driveSyncing.value = true
            val ctx = getApplication<Application>()
            when (val result = com.example.data.repository.GoogleDriveBackupHelper.downloadBackup(ctx)) {
                is com.example.data.repository.GDriveResult.Success -> {
                    val importSuccess = com.example.data.repository.BackupHelper.restoreSingleBinaryBackup(ctx, result.data)
                    if (importSuccess) {
                        try {
                            val database = AppDatabase.getDatabase(ctx)
                            repository = CustomerRepository(database.customerDao())
                            _statusMessage.emit("تم استرجاع النسخة الاحتياطية من جوجل درايف بنجاح")
                            triggerVibration()
                        } catch (e: Exception) {
                            _statusMessage.emit("فشل إعادة تشغيل قاعدة البيانات المسترجعة")
                        }
                    } else {
                        _statusMessage.emit("فشل معالجة ملف الاسترجاع السحابي")
                    }
                }
                is com.example.data.repository.GDriveResult.Error -> {
                    _statusMessage.emit(result.message)
                }
            }
            _driveSyncing.value = false
        }
    }

    fun triggerAutoBackupIfNeeded() {
        viewModelScope.launch {
            try {
                val ctx = getApplication<Application>()
                if (com.example.data.repository.GoogleDriveBackupHelper.isAutoBackupEnabled(ctx) && 
                    com.example.data.repository.GoogleDriveBackupHelper.isUserSignedIn(ctx)) {
                    val lastBackup = com.example.data.repository.GoogleDriveBackupHelper.getLastBackupTime(ctx)
                    val diff24h = 24 * 3600 * 1000L
                    if (System.currentTimeMillis() - lastBackup > diff24h) {
                        val binaryData = com.example.data.repository.BackupHelper.createSingleBinaryBackup(ctx)
                        if (binaryData != null) {
                            com.example.data.repository.GoogleDriveBackupHelper.uploadBackup(ctx, binaryData)
                            _lastBackupStatus.value = com.example.data.repository.GoogleDriveBackupHelper.getBackupStatus(ctx)
                        }
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    // Reactive lists
    val allCustomers: StateFlow<List<Customer>> = repository.allCustomers
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<Transaction>> = repository.allTransactions
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Combined StateFlow for all customers with their calculated balances (unfiltered)
    private val allCustomersWithBalances: Flow<List<CustomerWithBalance>> = combine(
        repository.allCustomers,
        repository.allTransactions
    ) { customers, transactions ->
        customers.map { customer ->
            val customerTrans = transactions.filter { it.customerId == customer.id }
            val totalDebt = customerTrans.filter { it.type == "DEBT" }.sumOf { it.amount }
            val totalPayment = customerTrans.filter { it.type == "PAYMENT" }.sumOf { it.amount }
            val balance = totalDebt - totalPayment
            val lastTrans = customerTrans.maxByOrNull { it.timestamp }
            
            CustomerWithBalance(
                customer = customer,
                totalDebt = totalDebt,
                totalPayment = totalPayment,
                netBalance = balance,
                lastTransactionTime = lastTrans?.timestamp,
                lastTransactionNotes = lastTrans?.notes
            )
        }
    }.flowOn(Dispatchers.Default)

    private val normalizedNamesCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    // Apply search query and sorting on the unfiltered list
    val customersWithBalances: StateFlow<List<CustomerWithBalance>> = combine(
        allCustomersWithBalances,
        searchQuery,
        sortOption,
        filterOption
    ) { mappedList, query, sort, filter ->
        val fuzzyScores = mutableMapOf<Int, Int>()

        // Apply filter option
        val matchedList = when (filter) {
            FilterOption.RECEIVABLES -> mappedList.filter { it.netBalance > 0 }
            FilterOption.PAYABLES -> mappedList.filter { it.netBalance < 0 }
            FilterOption.ALL -> mappedList
        }

        // Apply search query
        val filtered = if (query.isBlank()) {
            matchedList
        } else {
            val normQuery = com.example.utils.normalizeArabic(query)
            matchedList.filter { item ->
                val normName = normalizedNamesCache.getOrPut(item.customer.name) {
                    com.example.utils.normalizeArabic(item.customer.name)
                }
                val score = when {
                    normName == normQuery -> 0
                    normName.startsWith(normQuery) -> 1
                    normName.contains(normQuery) -> 2
                    com.example.utils.levenshteinDistance(normQuery, normName) <= 1 -> 3
                    normName.split(" ").any { com.example.utils.levenshteinDistance(normQuery, it) <= 1 } -> 4
                    else -> -1
                }
                if (score >= 0) {
                    fuzzyScores[item.customer.id] = score
                    true
                } else false
            }
        }

        // Apply sorting
        val sortedList = when (sort) {
            SortOption.NAME -> filtered.sortedBy { it.customer.name }
            SortOption.BALANCE_DESC -> filtered.sortedByDescending { it.netBalance }
            SortOption.BALANCE_ASC -> filtered.sortedBy { it.netBalance }
            SortOption.LAST_TRANSACTION -> filtered.sortedByDescending { it.lastTransactionTime ?: 0L }
        }
        
        if (query.isNotBlank()) {
            sortedList.sortedBy { fuzzyScores[it.customer.id] ?: 100 }
        } else {
            sortedList
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Financial Cards summary - calculate strictly from all unfiltered customers
    val financialSummary = allCustomersWithBalances.map { list ->
        val totalIMeOwe = list.filter { it.netBalance < 0 }.sumOf { kotlin.math.abs(it.netBalance) }
        val totalTheyOweMe = list.filter { it.netBalance > 0 }.sumOf { it.netBalance }
        val net = totalTheyOweMe - totalIMeOwe
        Triple(totalTheyOweMe, totalIMeOwe, net)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Triple(0.0, 0.0, 0.0))

    // Transactions for the currently selected customer
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedCustomerTransactions: StateFlow<List<Transaction>> = _selectedCustomerId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getTransactionsForCustomer(id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Details of the currently selected customer
    val selectedCustomerDetail: StateFlow<CustomerWithBalance?> = combine(
        customersWithBalances,
        _selectedCustomerId
    ) { list, id ->
        if (id != null) {
            list.firstOrNull { it.customer.id == id }
        } else {
            null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun selectCustomer(customerId: Int?) {
        _selectedCustomerId.value = customerId
    }

    // Settings modifiers
    fun toggleDarkTheme() {
        val newVal = !_isDarkTheme.value
        _isDarkTheme.value = newVal
        try {
            val prefs = getApplication<Application>().getSharedPreferences("hesabat_habayeb_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("dark_theme", newVal).apply()
        } catch (t: Throwable) {
            t.printStackTrace()
        }
        triggerVibration()
    }

    fun toggleFingerprint() {
        val newVal = !_isFingerprintEnabled.value
        _isFingerprintEnabled.value = newVal
        try {
            val prefs = getApplication<Application>().getSharedPreferences("hesabat_habayeb_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("fingerprint_enabled", newVal).apply()
        } catch (t: Throwable) {
            t.printStackTrace()
        }
        triggerVibration()
    }

    fun updateSecurityPin(newPin: String) {
        if (newPin.length == 4 && newPin.all { it.isDigit() }) {
            _securityPin.value = newPin
            try {
                val prefs = getApplication<Application>().getSharedPreferences("hesabat_habayeb_prefs", Context.MODE_PRIVATE)
                prefs.edit().putString("security_pin", newPin).apply()
                viewModelScope.launch {
                    _statusMessage.emit("تم تعديل الرمز الشخصي بنجاح")
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }

    // Core Mutators
    fun addRecentSearch(name: String) {
        val currentStr = getApplication<Application>().getSharedPreferences("hesabat_habayeb_prefs", Context.MODE_PRIVATE)
            .getString("recent_searches", "") ?: ""
        var currentList = if (currentStr.isNotEmpty()) currentStr.split("|||").toMutableList() else mutableListOf()
        
        currentList.remove(name)
        currentList.add(0, name) // move to top

        if (currentList.size > 10) {
            currentList = currentList.take(10).toMutableList()
        }

        _recentSearches.value = currentList
        saveRecentSearches(currentList)
    }

    fun removeRecentSearch(name: String) {
        val currentList = _recentSearches.value.toMutableList()
        currentList.remove(name)
        _recentSearches.value = currentList
        saveRecentSearches(currentList)
    }

    fun clearRecentSearches() {
        _recentSearches.value = emptyList()
        saveRecentSearches(emptyList())
    }

    private fun saveRecentSearches(list: List<String>) {
        try {
            val prefs = getApplication<Application>().getSharedPreferences("hesabat_habayeb_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("recent_searches", list.joinToString("|||")).apply()
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    fun addCustomer(name: String, phone: String, notes: String, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (name.isBlank()) {
                    withContext(Dispatchers.Main) {
                        _statusMessage.emit("الاسم مطلوب")
                    }
                    return@launch
                }
                
                // Defensive try-catch for Room database insert
                try {
                    repository.insertCustomer(Customer(name = name, phone = phone, notes = notes))
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        _statusMessage.emit("حدث خطأ أثناء الحفظ، يرجى المحاولة مرة أخرى")
                    }
                    return@launch
                }
                
                withContext(Dispatchers.Main) {
                    _statusMessage.emit("تم إضافة الزبون بنجاح")
                    triggerVibration()
                    onSuccess()
                }
            } catch (t: Throwable) {
                t.printStackTrace()
                withContext(Dispatchers.Main) {
                    _statusMessage.emit("حدث خطأ أثناء الحفظ، يرجى المحاولة مرة أخرى")
                }
            }
        }
    }

    fun updateCustomer(customer: Customer) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.updateCustomer(customer)
                }
                _statusMessage.emit("تم تعديل بيانات الزبون")
                triggerVibration()
            } catch (t: Throwable) {
                t.printStackTrace()
                _statusMessage.emit("فشل تعديل البيانات: ${t.localizedMessage}")
            }
        }
    }

    fun deleteCustomer(customer: Customer, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.deleteCustomer(customer)
                }
                removeRecentSearch(customer.name)
                _statusMessage.emit("تم حذف الزبون وسجله بالكامل")
                triggerVibration()
                onSuccess()
            } catch (t: Throwable) {
                t.printStackTrace()
                _statusMessage.emit("فشل حذف الزبون: ${t.localizedMessage}")
            }
        }
    }

    fun deleteMultipleCustomers(customers: List<Customer>, onSuccess: () -> Unit) {
        if (customers.isEmpty()) return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    customers.forEach { customer ->
                        repository.deleteCustomer(customer)
                    }
                }
                customers.forEach { customer ->
                    removeRecentSearch(customer.name)
                }
                _statusMessage.emit("تم حذف ${customers.size} من الزبائن بنجاح")
                triggerVibration()
                onSuccess()
            } catch (t: Throwable) {
                t.printStackTrace()
                _statusMessage.emit("فشل الحذف: ${t.localizedMessage}")
            }
        }
    }

    fun addTransaction(customerId: Int, amount: Double, type: String, notes: String, customTime: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                if (amount <= 0) {
                    _statusMessage.emit("المبلغ يجب أن يكون أكبر من صفر")
                    return@launch
                }
                val transaction = Transaction(
                    customerId = customerId,
                    amount = amount,
                    type = type,
                    notes = notes,
                    timestamp = customTime
                )
                withContext(Dispatchers.IO) {
                    repository.insertTransaction(transaction)
                }
                val typeText = if (type == "DEBT") "دين عليه" else "تسديد منه"
                _statusMessage.emit("تم إضافة معاملة $typeText بنجاح")
                triggerVibration()
                onSuccess()
            } catch (t: Throwable) {
                t.printStackTrace()
                _statusMessage.emit("فشل إضافة المعاملة: ${t.localizedMessage}")
            }
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.deleteTransaction(transaction)
                }
                _statusMessage.emit("تم حذف المعاملة")
                triggerVibration()
            } catch (t: Throwable) {
                t.printStackTrace()
                _statusMessage.emit("فشل حذف المعاملة")
            }
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.updateTransaction(transaction)
                }
                _statusMessage.emit("تم تعديل المعاملة")
                triggerVibration()
            } catch (t: Throwable) {
                t.printStackTrace()
                _statusMessage.emit("فشل تعديل المعاملة")
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.clearAllData()
                }
                _statusMessage.emit("تم مسح كافة البيانات من الجهاز")
                triggerVibration()
            } catch (t: Throwable) {
                t.printStackTrace()
                _statusMessage.emit("فشل مسح البيانات")
            }
        }
    }

    fun clearAllDataWithEmergencyBackup(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val success = withContext(Dispatchers.IO) {
                    val context = getApplication<Application>()
                    // 1. Create emergency backup
                    val dbFile = context.getDatabasePath("hesabat_habayeb_db")
                    if (!dbFile.exists()) {
                        // DB doesn't exist yet, nothing to back up, safe to continue
                        return@withContext true
                    }
                    val freeSpace = context.filesDir.freeSpace
                    val dbSize = dbFile.length()
                    // Safety check: ensure at least dbSize + 1.5MB is available
                    if (freeSpace < (dbSize + (1500 * 1024))) {
                        return@withContext false
                    }
                    
                    val timestamp = System.currentTimeMillis()
                    val backupFile = java.io.File(context.filesDir, "emergency_backup_$timestamp.db")
                    
                    // Copy primary database-file
                    dbFile.inputStream().use { input ->
                        backupFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    
                    // Copy active WAL / SHM databases sidecars if they exist
                    val walFile = java.io.File(dbFile.absolutePath + "-wal")
                    if (walFile.exists()) {
                        val backupWal = java.io.File(context.filesDir, "emergency_backup_$timestamp.db-wal")
                        walFile.inputStream().use { input ->
                            backupWal.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                    val shmFile = java.io.File(dbFile.absolutePath + "-shm")
                    if (shmFile.exists()) {
                        val backupShm = java.io.File(context.filesDir, "emergency_backup_$timestamp.db-shm")
                        shmFile.inputStream().use { input ->
                            backupShm.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                    
                    // 2. Perform deep database wipe/clear tables
                    repository.clearAllData()
                    
                    // 3. Reset SharedPreferences
                    val prefs = context.getSharedPreferences("hesabat_habayeb_prefs", Context.MODE_PRIVATE)
                    prefs.edit().clear().apply()
                    
                    true
                }
                
                if (success) {
                    // Reset internal memory representations to initial values
                    _isFingerprintEnabled.value = false
                    _securityPin.value = ""
                    _isDarkTheme.value = false
                    
                    _statusMessage.emit("تم النسخ الاحتياطي الطارئ ومسح جميع البيانات والتهيئة بنجاح")
                    triggerVibration()
                    onSuccess()
                } else {
                    onFailure("فشل المسح: مساحة تخزين الهاتف منخفضة للغاية للنسخ الاحتياطي الاحتيازي الطارئ!")
                }
            } catch (t: Throwable) {
                t.printStackTrace()
                onFailure("فشل مسح البيانات: ${t.localizedMessage}")
            }
        }
    }

    // Helper functions for GZIP compression & simple XOR key encryption
    private fun compressAndEncrypt(data: String): String {
        return try {
            val bos = java.io.ByteArrayOutputStream()
            val gzos = java.util.zip.GZIPOutputStream(bos)
            gzos.write(data.toByteArray(Charsets.UTF_8))
            gzos.close()
            val compressed = bos.toByteArray()
            
            val key = "HABAYEB_KEY_2026"
            val encrypted = ByteArray(compressed.size)
            for (i in compressed.indices) {
                encrypted[i] = (compressed[i].toInt() xor key[i % key.length].code).toByte()
            }
            
            android.util.Base64.encodeToString(encrypted, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    private fun decryptAndDecompress(base64Str: String): String {
        return try {
            val decoded = android.util.Base64.decode(base64Str, android.util.Base64.DEFAULT)
            val key = "HABAYEB_KEY_2026"
            val compressed = ByteArray(decoded.size)
            for (i in decoded.indices) {
                compressed[i] = (decoded[i].toInt() xor key[i % key.length].code).toByte()
            }
            
            val gzis = java.util.zip.GZIPInputStream(java.io.ByteArrayInputStream(compressed))
            val reader = gzis.bufferedReader(Charsets.UTF_8)
            val out = reader.readText()
            gzis.close()
            out
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    // Helper formatting function for YR
    fun formatCurrency(amount: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US)
        return formatter.format(amount) + " ر.ي"
    }

    fun triggerVibration() {
        try {
            val ctx = getApplication<Application>()
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(55, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(55)
                }
            }
        } catch (t: Throwable) {
            // Ignore if vibration fails
            t.printStackTrace()
        }
    }

    // Backup as compressed and encrypted base64 string of the entire database
    suspend fun exportBackup(): String = withContext(Dispatchers.IO) {
        try {
            val list = customersWithBalances.value
            val root = JSONObject()
            val custArray = JSONArray()
            list.forEach { item ->
                val cObj = JSONObject()
                cObj.put("name", item.customer.name)
                cObj.put("phone", item.customer.phone)
                cObj.put("notes", item.customer.notes)
                cObj.put("createdAt", item.customer.createdAt)
                
                // Fetch transactions for this customer directly synchronously or via all state
                val transArray = JSONArray()
                val transList = allTransactions.value.filter { it.customerId == item.customer.id }
                transList.forEach { t ->
                    val tObj = JSONObject()
                    tObj.put("amount", t.amount)
                    tObj.put("type", t.type)
                    tObj.put("notes", t.notes)
                    tObj.put("timestamp", t.timestamp)
                    transArray.put(tObj)
                }
                cObj.put("transactions", transArray)
                custArray.put(cObj)
            }
            root.put("version", 1)
            root.put("app", "hesabat_habayeb")
            root.put("timestamp", System.currentTimeMillis())
            root.put("data", custArray)
            
            triggerVibration()
            val rawJson = root.toString()
            val compressedCode = compressAndEncrypt(rawJson)
            if (compressedCode.isNotEmpty()) {
                compressedCode
            } else {
                rawJson
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            "فشل تصدير البيانات"
        }
    }

    fun importBackup(jsonString: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    var workingJson = jsonString.trim()
                    if (!workingJson.startsWith("{")) {
                        // Attempt fallback decryption and decompression for compressed Base64 code formats
                        val decrypted = decryptAndDecompress(workingJson)
                        if (decrypted.isNotEmpty()) {
                            workingJson = decrypted
                        }
                    }
                    val root = JSONObject(workingJson)
                    if (!root.has("app") || root.getString("app") != "hesabat_habayeb") {
                        _statusMessage.emit("ملف النسخة الاحتياطية غير صالح")
                        return@withContext
                    }
                    
                    val custArray = root.getJSONArray("data")
                    // Clear existing database
                    repository.clearAllData()
                    
                    for (i in 0 until custArray.length()) {
                        val cObj = custArray.getJSONObject(i)
                        val customer = Customer(
                            name = cObj.getString("name"),
                            phone = cObj.optString("phone", ""),
                            notes = cObj.optString("notes", ""),
                            createdAt = cObj.optLong("createdAt", System.currentTimeMillis())
                        )
                        val customerId = repository.insertCustomer(customer).toInt()
                        
                        val transArray = cObj.optJSONArray("transactions")
                        if (transArray != null) {
                            for (j in 0 until transArray.length()) {
                                val tObj = transArray.getJSONObject(j)
                                val transaction = Transaction(
                                    customerId = customerId,
                                    amount = tObj.getDouble("amount"),
                                    type = tObj.getString("type"),
                                    notes = tObj.optString("notes", ""),
                                    timestamp = tObj.optLong("timestamp", System.currentTimeMillis())
                                )
                                repository.insertTransaction(transaction)
                            }
                        }
                    }
                }
                _statusMessage.emit("تم استرجاع النسخة الاحتياطية بنجاح")
                triggerVibration()
            } catch (t: Throwable) {
                t.printStackTrace()
                _statusMessage.emit("فشل استيراد النسخة الاحتياطية: تنسيق خاطئ")
            }
        }
    }
}
