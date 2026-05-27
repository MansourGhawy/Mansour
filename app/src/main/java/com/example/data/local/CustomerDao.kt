package com.example.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.example.data.model.Customer
import com.example.data.model.Transaction
import kotlinx.coroutines.flow.Flow

interface CustomerDao {
    suspend fun insertCustomer(customer: Customer): Long
    suspend fun deleteCustomer(customer: Customer)
    fun getAllCustomers(): Flow<List<Customer>>
    suspend fun insertTransaction(transaction: Transaction): Long
    suspend fun deleteTransaction(transaction: Transaction)
    fun getAllTransactions(): Flow<List<Transaction>>
    fun getTransactionsByCustomer(customerId: Long): Flow<List<Transaction>>
    suspend fun deleteTransactionsByCustomer(customerId: Long)
}

class SQLiteCustomerDao(private val helper: SQLiteDatabaseHelper) : CustomerDao {

    override suspend fun insertCustomer(customer: Customer): Long {
        val db = helper.writableDatabase
        val values = ContentValues().apply {
            if (customer.id > 0) {
                put("id", customer.id)
            }
            put("name", customer.name)
            put("phone", customer.phone)
            put("createdAt", customer.createdAt)
        }
        val id = db.insertWithOnConflict(
            "customers",
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
        helper.triggerUpdate()
        return id
    }

    override suspend fun deleteCustomer(customer: Customer) {
        val db = helper.writableDatabase
        db.delete("customers", "id = ?", arrayOf(customer.id.toString()))
        helper.triggerUpdate()
    }

    override fun getAllCustomers(): Flow<List<Customer>> {
        return helper.getCustomersFlow()
    }

    override suspend fun insertTransaction(transaction: Transaction): Long {
        val db = helper.writableDatabase
        val values = ContentValues().apply {
            if (transaction.id > 0) {
                put("id", transaction.id)
            }
            put("customerId", transaction.customerId)
            put("amount", transaction.amount)
            put("type", transaction.type)
            put("timestamp", transaction.timestamp)
            put("note", transaction.note)
        }
        val id = db.insertWithOnConflict(
            "transactions",
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
        helper.triggerUpdate()
        return id
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        val db = helper.writableDatabase
        db.delete("transactions", "id = ?", arrayOf(transaction.id.toString()))
        helper.triggerUpdate()
    }

    override fun getAllTransactions(): Flow<List<Transaction>> {
        return helper.getTransactionsFlow()
    }

    override fun getTransactionsByCustomer(customerId: Long): Flow<List<Transaction>> {
        return helper.getTransactionsByCustomerFlow(customerId)
    }

    override suspend fun deleteTransactionsByCustomer(customerId: Long) {
        val db = helper.writableDatabase
        db.delete("transactions", "customerId = ?", arrayOf(customerId.toString()))
        helper.triggerUpdate()
    }
}
