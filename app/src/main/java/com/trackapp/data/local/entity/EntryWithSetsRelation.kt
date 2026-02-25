// This file is a "helper" class that Room uses to load a workout entry
// together with ALL of its sets in a single database query.
//
// Normally you would need two separate queries:
//   1. Get the WorkoutEntryEntity
//   2. Get all SetEntities for that entry
//
// By using @Relation, Room does both steps automatically and gives us a
// nice combined object to work with.

package com.trackapp.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class EntryWithSetsRelation(
    // @Embedded means "treat all the fields of WorkoutEntryEntity as if they
    // were fields directly on this class". It's like flattening an object.
    @Embedded val entry: WorkoutEntryEntity,

    // @Relation tells Room to automatically fetch all SetEntity rows whose
    // "entryId" column matches the "id" of the embedded entry above.
    @Relation(
        parentColumn = "id",       // the "id" field from WorkoutEntryEntity
        entityColumn = "entryId"   // the "entryId" field from SetEntity
    )
    val sets: List<SetEntity>
)
