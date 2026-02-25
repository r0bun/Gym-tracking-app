// A "set" is one group of reps in an exercise — e.g. "10 reps of bench press
// at 100 lbs". Every set belongs to a workout entry (WorkoutEntryEntity).
// This entity represents one row in the "workout_sets" table.

package com.trackapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "workout_sets",

    // ForeignKey = a link between two tables.
    // Here we say: every set must belong to an existing WorkoutEntryEntity.
    // onDelete = CASCADE means: if the parent entry is deleted, all its sets
    // are automatically deleted too (so we never have orphan rows).
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntryEntity::class,
            parentColumns = ["id"],      // the "id" column in workout_entries
            childColumns = ["entryId"],  // the "entryId" column in THIS table
            onDelete = ForeignKey.CASCADE
        )
    ],
    // An "index" speeds up database lookups by entryId (like a book index).
    indices = [Index("entryId")]
)
data class SetEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),

    // Which workout entry this set belongs to.
    val entryId: String,

    // The order of this set within the entry (1st set, 2nd set, …).
    val setNumber: Int,

    // How many repetitions were performed.
    val reps: Int,

    // The weight lifted, always stored internally in lbs.
    // The UI converts to kg if the user prefers kg.
    val weightLbs: Float,

    // True if the user performed reps until they could not continue (failure).
    // When true, the reps field is not meaningful.
    val toFailure: Boolean = false
)
