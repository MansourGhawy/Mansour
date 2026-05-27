package com.example.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.data.model.Customer
import com.example.data.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class SQLiteDatabaseHelper(context: Context) : SQLiteOpenHelper(context, "hesabat_habayeb_db", null, 1) {

    private val customersUpdateTrigger = MutableStateFlow(0)
    private val transactionsUpdateTrigger = MutableStateFlow(0)

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS customers (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                phone TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
        """)
        
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                customerId INTEGER NOT NULL,
                amount REAL NOT NULL,
                type TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                note TEXT NOT NULL
            )
        """)

        // Seed initial mock data for Hesabat Habayeb
        val current = System.currentTimeMillis()
        db.execSQL("INSERT INTO customers (id, name, phone, createdAt) VALUES (1, 'أبو أحمد الكندري', '0771234567', ${current - 15 * 24 * 3600 * 1000L})")
        db.execSQL("INSERT INTO customers (id, name, phone, createdAt) VALUES (2, 'أم سارة اللبنانية', '0798765432', ${current - 12 * 24 * 3600 * 1000L})")
        db.execSQL("INSERT INTO customers (id, name, phone, createdAt) VALUES (3, 'خالد العتيبي', '0785554433', ${current - 10 * 24 * 3600 * 1000L})")
        db.execSQL("INSERT INTO customers (id, name, phone, createdAt) VALUES (4, 'فاطمة الزهراء', '0761112223', ${current - 20 * 24 * 3600 * 1000L})")
        db.execSQL("INSERT INTO customers (id, name, phone, createdAt) VALUES (5, 'جرير السوري', '0759998887', ${current - 45 * 24 * 3600 * 1000L})")

        // Seed initial transactions
        // Customer 1 - أبو أحمد الكندري
        db.execSQL("INSERT INTO transactions (customerId, amount, type, timestamp, note) VALUES (1, 450.0, 'DEBT', ${current - 5L * 24 * 3600 * 1000}, 'شراء بضاعة بالآجل')")
        db.execSQL("INSERT INTO transactions (customerId, amount, type, timestamp, note) VALUES (1, 150.0, 'PAYMENT', ${current - 2L * 24 * 3600 * 1000}, 'دفعة تحت الحساب')")

        // Customer 2 - أم سارة اللبنانية
        db.execSQL("INSERT INTO transactions (customerId, amount, type, timestamp, note) VALUES (2, 100.0, 'DEBT', ${current - 10L * 24 * 3600 * 1000}, 'توصيل طلبات دليفري')")
        db.execSQL("INSERT INTO transactions (customerId, amount, type, timestamp, note) VALUES (2, 200.0, 'DEBT', ${current - 8L * 24 * 3600 * 1000}, 'طلب إضافي')")
        db.execSQL("INSERT INTO transactions (customerId, amount, type, timestamp, note) VALUES (2, 350.0, 'PAYMENT', ${current - 3L * 24 * 3600 * 1000}, 'تسوية كامل الحساب وزيادة أمانة')")

        // Customer 3 - خالد العتيبي
        db.execSQL("INSERT INTO transactions (customerId, amount, type, timestamp, note) VALUES (3, 750.0, 'DEBT', ${current - 6L * 24 * 3600 * 1000}, 'فاتورة صيانة المحل')")

        // Customer 4 - فاطمة الزهراء
        db.execSQL("INSERT INTO transactions (customerId, amount, type, timestamp, note) VALUES (4, 200.0, 'PAYMENT', ${current - 12L * 24 * 3600 * 1000}, 'دفعة مقدمة لحجز بضاعة')")

        // Customer 5 - جرير السوري (Inactive over 30 days)
        db.execSQL("INSERT INTO transactions (customerId, amount, type, timestamp, note) VALUES (5, 1500.0, 'DEBT', ${current - 35L * 24 * 3600 * 1000}, 'بضاعة الموسم الماضي')")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS customers")
        db.execSQL("DROP TABLE IF EXISTS transactions")
        onCreate(db)
    }

    fun triggerUpdate() {
        customersUpdateTrigger.value = customersUpdateTrigger.value + 1
        transactionsUpdateTrigger.value = transactionsUpdateTrigger.value + 1
    }

    fun getAllCustomersList(): List<Customer> {
        val list = mutableListOf<Customer>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM customers ORDER BY name ASC", null)
        if (cursor.moveToFirst()) {
            val idIndex = cursor.getColumnIndexOrThrow("id")
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            val phoneIndex = cursor.getColumnIndexOrThrow("phone")
            val createdIndex = cursor.getColumnIndexOrThrow("createdAt")
            do {
                list.add(
                    Customer(
                        id = cursor.getLong(idIndex),
                        name = cursor.getString(nameIndex),
                        phone = cursor.getString(phoneIndex),
                        createdAt = cursor.getLong(createdIndex)
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun getAllTransactionsList(): List<Transaction> {
        val list = mutableListOf<Transaction>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM transactions ORDER BY timestamp DESC", null)
        if (cursor.moveToFirst()) {
            val idIndex = cursor.getColumnIndexOrThrow("id")
            val customerIdIndex = cursor.getColumnIndexOrThrow("customerId")
            val amountIndex = cursor.getColumnIndexOrThrow("amount")
            val typeIndex = cursor.getColumnIndexOrThrow("type")
            val timestampIndex = cursor.getColumnIndexOrThrow("timestamp")
            val noteIndex = cursor.getColumnIndexOrThrow("note")
            do {
                list.add(
                    Transaction(
                        id = cursor.getLong(idIndex),
                        customerId = cursor.getLong(customerIdIndex),
                        amount = cursor.getDouble(amountIndex),
                        type = cursor.getString(typeIndex),
                        timestamp = cursor.getLong(timestampIndex),
                        note = cursor.getString(noteIndex)
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun getTransactionsByCustomerId(customerId: Long): List<Transaction> {
        val list = mutableListOf<Transaction>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM transactions WHERE customerId = ? ORDER BY timestamp DESC",
            arrayOf(customerId.toString())
        )
        if (cursor.moveToFirst()) {
            val idIndex = cursor.getColumnIndexOrThrow("id")
            val customerIdIndex = cursor.getColumnIndexOrThrow("customerId")
            val amountIndex = cursor.getColumnIndexOrThrow("amount")
            val typeIndex = cursor.getColumnIndexOrThrow("type")
            val timestampIndex = cursor.getColumnIndexOrThrow("timestamp")
            val noteIndex = cursor.getColumnIndexOrThrow("note")
            do {
                list.add(
                    Transaction(
                        id = cursor.getLong(idIndex),
                        customerId = cursor.getLong(customerIdIndex),
                        amount = cursor.getDouble(amountIndex),
                        type = cursor.getString(typeIndex),
                        timestamp = cursor.getLong(timestampIndex),
                        note = cursor.getString(noteIndex)
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun getCustomersFlow(): Flow<List<Customer>> {
        return customersUpdateTrigger.map { getAllCustomersList() }
    }

    fun getTransactionsFlow(): Flow<List<Transaction>> {
        return transactionsUpdateTrigger.map { getAllTransactionsList() }
    }

    fun getTransactionsByCustomerFlow(customerId: Long): Flow<List<Transaction>> {
        return transactionsUpdateTrigger.map { getTransactionsByCustomerId(customerId) }
    }
}
