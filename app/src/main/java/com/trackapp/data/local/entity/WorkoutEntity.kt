// A "workout" is a single gym session — it has a name (stored in "notes"),
// the date/time it was created, and a unique ID.
// This entity represents one row in the "workouts" table.

package com.trackapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
// UUID = Universally Unique Identifier — a randomly generated string that is
// practically guaranteed to be unique worldwide. Used as the row ID.
import java.util.UUID

@Entity(tableName = "workouts")
data class WorkoutEntity(
    // The default value uses UUID to generate a random ID when a new workout is
    // created, so we never have to specify the ID manually.
    @PrimaryKey val id: String = UUID.randomUUID().toString(),

    // The timestamp (milliseconds since 1 January 1970) when the workout was
    // created. System.currentTimeMillis() returns "right now" as a number.
    val date: Long = System.currentTimeMillis(),

    // The user-chosen name for the session (e.g. "Push Day").
    // Called "notes" in the database for historical reasons — it's treated as
    // the workout name throughout the app.
    val notes: String = ""
)
