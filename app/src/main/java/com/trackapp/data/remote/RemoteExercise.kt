package com.trackapp.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Remote DTO matching the `exercises` table in Supabase PostgreSQL.
 *
 * SQL to create the table in Supabase SQL editor:
 *   create table exercises (
 *     id uuid primary key default gen_random_uuid(),
 *     name text not null,
 *     muscle_group text not null
 *   );
 *
 * Seed data examples:
 *   insert into exercises (name, muscle_group) values
 *     ('Bench Press', 'Chest'),
 *     ('Squat', 'Legs'),
 *     ('Deadlift', 'Back'),
 *     ('Overhead Press', 'Shoulders'),
 *     ('Pull-Up', 'Back'),
 *     ('Barbell Row', 'Back'),
 *     ('Dumbbell Curl', 'Biceps'),
 *     ('Tricep Pushdown', 'Triceps'),
 *     ('Leg Press', 'Legs'),
 *     ('Plank', 'Core');
 */
@Serializable
data class RemoteExercise(
    val id: String,
    val name: String,
    @SerialName("muscle_group") val muscleGroup: String
)
