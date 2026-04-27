package com.example.wascheduler.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scheduled_messages")
data class ScheduledMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chatJid: String,
    val chatName: String,
    val isGroup: Boolean = false,
    val content: String,
    val scheduledFor: Long,
    val status: String = "pending",
    val createdAt: Long = System.currentTimeMillis(),
    val sentAt: Long? = null,
    val error: String? = null
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_SENT = "sent"
        const val STATUS_FAILED = "failed"
        const val STATUS_CANCELLED = "cancelled"
    }
}