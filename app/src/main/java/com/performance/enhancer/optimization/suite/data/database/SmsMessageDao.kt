package com.performance.enhancer.optimization.suite.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.performance.enhancer.optimization.suite.data.model.SmsMessage

@Dao
interface SmsMessageDao {
    @Query("SELECT * FROM sms_messages ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<SmsMessage>>

    @Query("SELECT * FROM sms_messages WHERE isRead = 0 ORDER BY timestamp DESC")
    fun getUnreadMessages(): Flow<List<SmsMessage>>

    @Query("SELECT * FROM sms_messages WHERE packageName = :packageName ORDER BY timestamp DESC")
    fun getMessagesByPackage(packageName: String): Flow<List<SmsMessage>>

    @Query("SELECT * FROM sms_messages WHERE notificationKey = :notificationKey LIMIT 1")
    suspend fun getMessageByNotificationKey(notificationKey: String): SmsMessage?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: SmsMessage)

    @Update
    suspend fun updateMessage(message: SmsMessage)

    @Query("UPDATE sms_messages SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("DELETE FROM sms_messages WHERE id = :id")
    suspend fun deleteMessage(id: Long)

    @Query("DELETE FROM sms_messages")
    suspend fun deleteAllMessages()
}