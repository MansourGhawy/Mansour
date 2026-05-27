package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Customer
import com.example.data.model.Transaction
import com.example.data.repository.CustomerRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CustomerWithBalance(
    val customer: Customer,
    val netBalance: Double,
    val lastTransactionTime: Long?
)

class CustomerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CustomerRepository

    val allCustomers: StateFlow<List<Customer>>
    val allTransactions: StateFlow<List<Transaction>>

    val customersWithBalances: StateFlow<List<CustomerWithBalance>>
    val financialSummary: StateFlow<Triple<Double, Double, Double>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = CustomerRepository(database.customerDao())

        allCustomers = repository.allCustomers
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allTransactions = repository.allTransactions
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Combine customers and transactions into CustomerWithBalance objects
        customersWithBalances = combine(allCustomers, allTransactions) { customers, transactions ->
            customers.map { customer ->
                val customerTransactions = transactions.filter { it.customerId == customer.id }
                
                // Calculate balance: DEBT is money they owe me (+), PAYMENT is money they paid (-)
                val totalDebt = customerTransactions.filter { it.type == "DEBT" }.sumOf { it.amount }
                val totalPayment = customerTransactions.filter { it.type == "PAYMENT" }.sumOf { it.amount }
                val netBalance = totalDebt - totalPayment

                val lastTxTime = customerTransactions.maxOfOrNull { it.timestamp }

                CustomerWithBalance(
                    customer = customer,
                    netBalance = netBalance,
                    lastTransactionTime = lastTxTime
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Calculate general summary: (Total they owe me, Total I owe them, Net)
        financialSummary = customersWithBalances.map { list ->
            val positiveBalancesSum = list.filter { it.netBalance > 0 }.sumOf { it.netBalance }
            val negativeBalancesSum = list.filter { it.netBalance < 0 }.sumOf { -it.netBalance }
            val netOutstanding = positiveBalancesSum - negativeBalancesSum
            Triple(positiveBalancesSum, negativeBalancesSum, netOutstanding)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Triple(0.0, 0.0, 0.0))
    }

    fun addCustomer(name: String, phone: String) {
        viewModelScope.launch {
            repository.addCustomer(Customer(name = name, phone = phone))
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
        }
    }

    fun addTransaction(customerId: Long, amount: Double, type: String, note: String = "") {
        viewModelScope.launch {
            repository.addTransaction(
                Transaction(customerId = customerId, amount = amount, type = type, note = note)
            )
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun getTransactionsForCustomer(customerId: Long): Flow<List<Transaction>> {
        return repository.getTransactionsByCustomer(customerId)
    }
}
