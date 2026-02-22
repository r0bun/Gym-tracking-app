package com.trackapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Workout session — stored locally on device.
 */
@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val date: Long = System.currentTimeMillis(),
    val notes: String = ""
)
