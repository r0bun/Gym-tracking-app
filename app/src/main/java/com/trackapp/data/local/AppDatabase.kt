package com.trackapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.trackapp.data.local.dao.ExerciseDao
import com.trackapp.data.local.dao.SetDao
import com.trackapp.data.local.dao.WorkoutDao
import com.trackapp.data.local.dao.WorkoutEntryDao
import com.trackapp.data.local.entity.ExerciseEntity
import com.trackapp.data.local.entity.SetEntity
import com.trackapp.data.local.entity.WorkoutEntity
import com.trackapp.data.local.entity.WorkoutEntryEntity

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutEntity::class,
        WorkoutEntryEntity::class,
        SetEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun workoutEntryDao(): WorkoutEntryDao
    abstract fun setDao(): SetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** Adds useLbs column to workout_entries (per-exercise unit preference). */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE workout_entries ADD COLUMN useLbs INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

        /** Adds supersetGroupId column to workout_entries and creates workout_sets table. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE workout_entries ADD COLUMN supersetGroupId TEXT"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS workout_sets (
                        id TEXT NOT NULL PRIMARY KEY,
                        entryId TEXT NOT NULL,
                        setNumber INTEGER NOT NULL,
                        reps INTEGER NOT NULL,
                        weightLbs REAL NOT NULL,
                        toFailure INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(entryId) REFERENCES workout_entries(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_workout_sets_entryId ON workout_sets (entryId)"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "trackapp_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
