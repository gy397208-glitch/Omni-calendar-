package com.example.data

import kotlinx.coroutines.flow.Flow

class EventRepository(private val eventDao: EventDao) {
    val allEvents: Flow<List<EventEntity>> = eventDao.getAllEvents()

    fun getEventsBySource(source: String): Flow<List<EventEntity>> = eventDao.getEventsBySource(source)

    suspend fun insert(event: EventEntity) = eventDao.insertEvent(event)

    suspend fun update(event: EventEntity) = eventDao.updateEvent(event)

    suspend fun delete(event: EventEntity) = eventDao.deleteEvent(event)

    suspend fun deleteById(id: Long) = eventDao.deleteEventById(id)
}
