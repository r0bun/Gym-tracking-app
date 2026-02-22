package com.trackapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * A logged exercise entry within a workout — links Workout → Exercise with sets/reps/weight.
 */
@Entity(
    tableName = "workout_entries",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["workoutId"]),
        Index(value = ["exerciseId"])
    ]
)
data class WorkoutEntryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val workoutId: String,
    val exerciseId: String,
    val sets: Int,
    val reps: Int,
    val weightKg: Float,
    val notes: String = "",
    val supersetGroupId: String? = null,
    val useLbs: Boolean = true
)
