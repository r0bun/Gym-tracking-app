package com.trackapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Exercise entity — synced from Supabase and cached locally.
 */
@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val muscleGroup: String
)
