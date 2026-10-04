package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val description: String,
    val date: Long,
    val startTime: String,
    val endTime: String,
    val location: String,
    val category: String,
    val calendarSource: String,
    val reminder: Boolean = true,
    val attendees: String = ""
)
