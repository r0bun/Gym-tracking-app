// This file is inside the "entity" folder, which holds blueprints for the
// database tables. Think of each entity as a row in a spreadsheet — each field
// is a column.

package com.trackapp.data.local.entity

// @Entity and @PrimaryKey come from the Room library, which lets us store data
// on the device like a mini database (SQLite).
import androidx.room.Entity
import androidx.room.PrimaryKey

// @Entity tells Room: "create a table in the database for this class."
// tableName = "exercises" is the name of that table.
@Entity(tableName = "exercises")
// "data class" is Kotlin shorthand for a class whose only job is to hold data.
// Room will automatically map each field below to a column in the table.
data class ExerciseEntity(
    // @PrimaryKey means this field is the unique ID for every row.
    // No two exercises can share the same id.
    @PrimaryKey val id: String,

    // The display name of the exercise (e.g. "Bench Press").
    val name: String,

    // Which muscle group this exercise targets (e.g. "Chest", "Legs").
    val muscleGroup: String
)
// Note: exercises are downloaded from Supabase (the cloud database) when the
// user logs in, then stored here locally so the app works offline.
