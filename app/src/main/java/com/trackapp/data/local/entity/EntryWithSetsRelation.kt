package com.trackapp.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Room @Relation helper — fetches a WorkoutEntry together with all its SetEntities in one query.
 */
data class EntryWithSetsRelation(
    @Embedded val entry: WorkoutEntryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "entryId"
    )
    val sets: List<SetEntity>
)
