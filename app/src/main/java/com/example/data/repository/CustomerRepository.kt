package com.example.data.repository

import com.example.data.local.CustomerDao
import com.example.data.model.Customer
import com.example.data.model.Transaction
import kotlinx.coroutines.flow.Flow

class CustomerRepository(private val customerDao: CustomerDao) {
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()
    val allTransactions: Flow<List<Transaction>> = customerDao.getAllTransactions()

    fun getTransactionsForCustomer(customerId: Int): Flow<List<Transaction>> =
        customerDao.getTransactionsForCustomer(customerId)

    suspend fun getCustomerById(id: Int): Customer? =
        customerDao.getCustomerById(id)

    suspend fun getTransactionById(id: Int): Transaction? =
        customerDao.getTransactionById(id)

    suspend fun insertCustomer(customer: Customer): Long =
        customerDao.insertCustomer(customer)

    suspend fun updateCustomer(customer: Customer) {
        customerDao.updateCustomer(customer)
    }

    suspend fun deleteCustomer(customer: Customer) {
        customerDao.deleteCustomerWithTransactions(customer)
    }

    suspend fun insertTransaction(transaction: Transaction): Long =
        customerDao.insertTransaction(transaction)

    suspend fun updateTransaction(transaction: Transaction) {
        customerDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        customerDao.deleteTransaction(transaction)
    }

    suspend fun clearAllData() {
        customerDao.clearAllData()
    }
}
