package com.example.data.local

import android.content.Context

abstract class AppDatabase {
    abstract fun customerDao(): CustomerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val helper = SQLiteDatabaseHelper(context.applicationContext)
                val instance = object : AppDatabase() {
                    override fun customerDao(): CustomerDao {
                        return SQLiteCustomerDao(helper)
                    }
                }
                // Trigger initial query to warm cache trigger flows
                helper.triggerUpdate()
                INSTANCE = instance
                instance
            }
        }
    }
}
