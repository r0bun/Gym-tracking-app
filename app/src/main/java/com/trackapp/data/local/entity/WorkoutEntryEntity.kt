// A "workout entry" is one exercise logged inside a workout session.
// For example: "Bench Press — 3 sets" is one entry.
// Each entry links a Workout to an Exercise and contains multiple Sets.
// This entity represents one row in the "workout_entries" table.

package com.trackapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "workout_entries",

    // Two foreign keys:
    //  1. workoutId → must match an existing Workout row.
    //  2. exerciseId → must match an existing Exercise row.
    // CASCADE on Workout: delete the workout → all its entries (and their sets)
    //   are deleted automatically.
    // RESTRICT on Exercise: prevents deleting an exercise that is still
    //   referenced by a workout entry (protects historical data).
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
    // Indexes on both foreign key columns for fast lookups.
    indices = [
        Index(value = ["workoutId"]),
        Index(value = ["exerciseId"])
    ]
)
data class WorkoutEntryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),

    // The workout session this entry belongs to.
    val workoutId: String,

    // Which exercise was performed (links to ExerciseEntity).
    val exerciseId: String,

    // Summary fields — stored for quick display without loading every set.
    val sets: Int,       // total number of sets
    val reps: Int,       // average reps across sets
    val weightKg: Float, // max weight used (stored in kg for legacy reasons,
                         // but the app now primarily uses lbs via SetEntity)

    // Optional notes the user added for this exercise.
    val notes: String = "",

    // If two exercises are linked as a "superset", they share the same
    // supersetGroupId string. Null means this entry is not in a superset.
    val supersetGroupId: String? = null,

    // Whether the user chose to log weight in lbs (true) or kg (false)
    // for this specific exercise entry.
    val useLbs: Boolean = true
)
