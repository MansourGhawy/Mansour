package com.example.data.model

data class Transaction(
    val id: Long = 0,
    val customerId: Long,
    val amount: Double,
    val type: String, // "DEBT" or "PAYMENT"
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = ""
)
