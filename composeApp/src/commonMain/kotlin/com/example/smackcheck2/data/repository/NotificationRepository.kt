package com.example.smackcheck2.data.repository

import com.example.smackcheck2.data.SupabaseClientProvider
import com.example.smackcheck2.data.dto.NotificationDto
import com.example.smackcheck2.model.Notification
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class NotificationRepository {
    private val client = SupabaseClientProvider.client
    private var notificationChannel: RealtimeChannel? = null

    suspend fun getNotifications(userId: String): List<Notification> {
        val result = client.postgrest["notifications"]
            .select {
                filter { eq("user_id", userId) }
                order("created_at", Order.DESCENDING)
                limit(50)
            }
            .decodeList<NotificationDto>()
        return result.map { it.toNotification() }
    }

    /**
     * Subscribe to real-time notification inserts for the current user.
     * Returns a Flow that emits each new Notification as it arrives via Supabase Realtime.
     * Call unsubscribeFromNotifications() when the subscriber is no longer active.
     */
    suspend fun subscribeToNotifications(userId: String): Flow<Notification> {
        // Clean up any existing channel before creating a new one
        notificationChannel?.let { client.realtime.removeChannel(it) }

        val channel = client.channel("notifications:$userId")
        notificationChannel = channel

        val flow: Flow<Notification> = channel
            .postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                table = "notifications"
                filter("user_id", FilterOperator.EQ, userId)
            }
            .map { action ->
                Json.decodeFromJsonElement(NotificationDto.serializer(), action.record)
                    .toNotification()
            }

        channel.subscribe()
        return flow
    }

    suspend fun unsubscribeFromNotifications() {
        notificationChannel?.let {
            client.realtime.removeChannel(it)
            notificationChannel = null
        }
    }

    suspend fun markAsRead(notificationId: String) {
        client.postgrest["notifications"]
            .update(mapOf("is_read" to true)) {
                filter { eq("id", notificationId) }
            }
    }

    suspend fun markAllAsRead(userId: String) {
        client.postgrest["notifications"]
            .update(mapOf("is_read" to true)) {
                filter {
                    eq("user_id", userId)
                    eq("is_read", false)
                }
            }
    }

    suspend fun getUnreadCount(userId: String): Int {
        val result = client.postgrest["notifications"]
            .select {
                filter {
                    eq("user_id", userId)
                    eq("is_read", false)
                }
            }
            .decodeList<NotificationDto>()
        return result.size
    }

    private fun NotificationDto.toNotification() = Notification(
        id = id ?: "",
        type = type,
        title = title,
        body = body,
        data = runCatching {
            data?.jsonObject?.mapValues { it.value.jsonPrimitive.content } ?: emptyMap()
        }.getOrDefault(emptyMap()),
        isRead = isRead,
        createdAt = runCatching {
            createdAt?.let { Instant.parse(it).toEpochMilliseconds() } ?: 0L
        }.getOrDefault(0L)
    )
}
