package com.example.data.repository

import com.example.data.local.CustomerDao
import com.example.data.model.Customer
import com.example.data.model.Transaction
import kotlinx.coroutines.flow.Flow

class CustomerRepository(private val customerDao: CustomerDao) {

    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()
    val allTransactions: Flow<List<Transaction>> = customerDao.getAllTransactions()

    suspend fun addCustomer(customer: Customer): Long {
        return customerDao.insertCustomer(customer)
    }

    suspend fun deleteCustomer(customer: Customer) {
        customerDao.deleteTransactionsByCustomer(customer.id)
        customerDao.deleteCustomer(customer)
    }

    suspend fun addTransaction(transaction: Transaction): Long {
        return customerDao.insertTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        customerDao.deleteTransaction(transaction)
    }

    fun getTransactionsByCustomer(customerId: Long): Flow<List<Transaction>> {
        return customerDao.getTransactionsByCustomer(customerId)
    }
}
