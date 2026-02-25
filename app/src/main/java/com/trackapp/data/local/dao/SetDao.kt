// This DAO handles all database operations for the "workout_sets" table.
// A "set" is one block of reps within an exercise entry
// (e.g. "Set 1: 10 reps at 100 lbs").

package com.trackapp.data.local.dao

import androidx.room.*
import com.trackapp.data.local.entity.SetEntity

@Dao
interface SetDao {

    // Inserts a single set. If a set with the same ID already exists, it is
    // replaced (used when saving edits to a set).
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(set: SetEntity)

    // Inserts a list of sets at once — more efficient than inserting one by one.
    // Called when the user saves a whole exercise with multiple sets.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sets: List<SetEntity>)

    // Updates an existing set row (e.g. the user edits the reps or weight).
    @Update
    suspend fun update(set: SetEntity)

    // Deletes a specific set from the database.
    @Delete
    suspend fun delete(set: SetEntity)

    // Deletes every set that belongs to a given workout entry.
    // Called before re-saving all sets when the user edits an exercise —
    // it's simpler to wipe and re-insert than to diff individual set changes.
    @Query("DELETE FROM workout_sets WHERE entryId = :entryId")
    suspend fun deleteAllForEntry(entryId: String)

    // Returns all sets for a given entry, ordered by set number (1, 2, 3, …).
    @Query("SELECT * FROM workout_sets WHERE entryId = :entryId ORDER BY setNumber ASC")
    suspend fun getSetsForEntry(entryId: String): List<SetEntity>
}
