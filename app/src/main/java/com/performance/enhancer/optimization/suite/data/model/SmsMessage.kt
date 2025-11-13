package com.performance.enhancer.optimization.suite.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_messages")
data class SmsMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String,
    val content: String,
    val timestamp: Long,
    val sourceApp: String,
    val packageName: String,
    val isRead: Boolean = false,
    val notificationKey: String? = null
)