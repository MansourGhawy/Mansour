package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerId: Int,
    val amount: Double,               // Yemeni Rial (YR)
    val type: String,                 // "DEBT" (دين عليه - زيادة ما لي) or "PAYMENT" (تسديد منه - نقص ما لي)
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
