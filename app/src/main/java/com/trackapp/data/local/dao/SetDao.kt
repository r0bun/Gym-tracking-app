package com.trackapp.data.local.dao

import androidx.room.*
import com.trackapp.data.local.entity.SetEntity

@Dao
interface SetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(set: SetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sets: List<SetEntity>)

    @Update
    suspend fun update(set: SetEntity)

    @Delete
    suspend fun delete(set: SetEntity)

    @Query("DELETE FROM workout_sets WHERE entryId = :entryId")
    suspend fun deleteAllForEntry(entryId: String)

    @Query("SELECT * FROM workout_sets WHERE entryId = :entryId ORDER BY setNumber ASC")
    suspend fun getSetsForEntry(entryId: String): List<SetEntity>
}
