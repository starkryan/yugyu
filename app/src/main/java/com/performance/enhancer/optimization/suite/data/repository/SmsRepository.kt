package com.performance.enhancer.optimization.suite.data.repository

import kotlinx.coroutines.flow.Flow
import com.performance.enhancer.optimization.suite.data.model.SmsMessage
import com.performance.enhancer.optimization.suite.data.database.SmsMessageDao

class SmsRepository(private val smsMessageDao: SmsMessageDao) {
    fun getAllMessages(): Flow<List<SmsMessage>> = smsMessageDao.getAllMessages()

    fun getUnreadMessages(): Flow<List<SmsMessage>> = smsMessageDao.getUnreadMessages()

    fun getMessagesByPackage(packageName: String): Flow<List<SmsMessage>> =
        smsMessageDao.getMessagesByPackage(packageName)

    suspend fun getMessageByNotificationKey(notificationKey: String): SmsMessage? =
        smsMessageDao.getMessageByNotificationKey(notificationKey)

    suspend fun insertMessage(message: SmsMessage) = smsMessageDao.insertMessage(message)

    suspend fun updateMessage(message: SmsMessage) = smsMessageDao.updateMessage(message)

    suspend fun markAsRead(id: Long) = smsMessageDao.markAsRead(id)

    suspend fun deleteMessage(id: Long) = smsMessageDao.deleteMessage(id)

    suspend fun deleteAllMessages() = smsMessageDao.deleteAllMessages()

    suspend fun insertOrUpdateMessage(message: SmsMessage) {
        val existingMessage = message.notificationKey?.let {
            getMessageByNotificationKey(it)
        }

        if (existingMessage != null) {
            updateMessage(message.copy(id = existingMessage.id))
        } else {
            insertMessage(message)
        }
    }
}