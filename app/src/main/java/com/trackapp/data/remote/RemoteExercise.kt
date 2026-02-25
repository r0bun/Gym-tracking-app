// This file defines what an exercise looks like when it comes from the cloud
// (Supabase). It is a "DTO" — Data Transfer Object — a plain data class whose
// only job is to carry data received from the network.
//
// The exercise list lives in a PostgreSQL table on Supabase. When the app
// downloads it, the JSON looks like:
//   { "id": "abc123", "name": "Bench Press", "muscle_group": "Chest" }
// This class tells Kotlin how to turn that JSON into a usable object.

package com.trackapp.data.remote

// @Serializable lets the kotlinx.serialization library convert JSON ↔ Kotlin
// objects automatically (a process called serialization/deserialization).
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RemoteExercise(
    val id: String,

    val name: String,

    // The JSON field is named "muscle_group" (snake_case, the SQL convention),
    // but in Kotlin we prefer camelCase. @SerialName maps one to the other.
    @SerialName("muscle_group") val muscleGroup: String
)

// After downloading, each RemoteExercise is converted to an ExerciseEntity
// and saved to the local Room database (see ExerciseRepository.syncFromRemote).
