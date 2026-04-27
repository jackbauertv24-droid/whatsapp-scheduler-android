package com.example.wascheduler.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM scheduled_messages WHERE status = :status ORDER BY scheduledFor ASC")
    fun getByStatus(status: String): Flow<List<ScheduledMessage>>
    
    @Query("SELECT * FROM scheduled_messages WHERE status = '${ScheduledMessage.STATUS_PENDING}' AND scheduledFor <= :timestamp ORDER BY scheduledFor ASC")
    suspend fun getDueMessages(timestamp: Long): List<ScheduledMessage>
    
    @Query("SELECT * FROM scheduled_messages ORDER BY scheduledFor DESC")
    fun getAll(): Flow<List<ScheduledMessage>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ScheduledMessage): Long
    
    @Update
    suspend fun update(message: ScheduledMessage)
    
    @Query("UPDATE scheduled_messages SET status = :status, sentAt = :sentAt, error = :error WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, sentAt: Long?, error: String?)
    
    @Query("SELECT * FROM scheduled_messages WHERE id = :id")
    suspend fun getById(id: Long): ScheduledMessage?
    
    @Delete
    suspend fun delete(message: ScheduledMessage)
    
    @Query("DELETE FROM scheduled_messages WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("DELETE FROM scheduled_messages WHERE status = '${ScheduledMessage.STATUS_SENT}' AND sentAt < :olderThan")
    suspend fun deleteOldSent(olderThan: Long)
    
    @Query("SELECT COUNT(*) FROM scheduled_messages WHERE status = '${ScheduledMessage.STATUS_PENDING}'")
    suspend fun getPendingCount(): Int
}