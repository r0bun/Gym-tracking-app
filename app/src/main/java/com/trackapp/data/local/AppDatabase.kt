// This file sets up the local SQLite database on the user's device.
// "SQLite" is a tiny database engine that comes built into every Android phone
// — no internet needed, data lives entirely on the device.
//
// Room is a library that sits on top of SQLite and makes it much easier to use.
// Instead of writing raw SQL everywhere, we define Entities (tables) and DAOs
// (operations) in Kotlin, and Room handles the rest.

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

// @Database tells Room:
//   - which tables (entities) exist in the database
//   - the current version number (used to track schema changes)
//   - exportSchema = false means we don't write a JSON schema file to disk
@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutEntity::class,
        WorkoutEntryEntity::class,
        SetEntity::class
    ],
    version = 3,      // bump this number every time the schema changes
    exportSchema = false
)
// RoomDatabase is the base class Room requires us to extend.
abstract class AppDatabase : RoomDatabase() {

    // These abstract functions let the rest of the app get a DAO to talk to
    // each table. Room generates the actual implementations automatically.
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun workoutEntryDao(): WorkoutEntryDao
    abstract fun setDao(): SetDao

    // "companion object" in Kotlin is like a static section — things here belong
    // to the class itself, not to any specific instance.
    companion object {

        // @Volatile ensures that if multiple threads read INSTANCE at the same
        // time, they all see the most up-to-date value (thread safety).
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // ── Migrations ──────────────────────────────────────────────────────
        //
        // When the app is already installed on a user's phone and the database
        // schema changes (a new column is added, a table is created, etc.),
        // we need a "migration" — a script that upgrades the existing database
        // from the old version to the new one WITHOUT deleting the user's data.

        // Migration from version 2 → 3:
        // Adds a "useLbs" column to workout_entries so each exercise can
        // remember whether the user was logging in lbs or kg.
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // ALTER TABLE = modify an existing table (add a column here).
                // DEFAULT 1 means existing rows will default to "true" (lbs).
                db.execSQL(
                    "ALTER TABLE workout_entries ADD COLUMN useLbs INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

        // Migration from version 1 → 2:
        // Adds the supersetGroupId column to workout_entries (for linking two
        // exercises as a superset), and creates the workout_sets table so each
        // set can be stored individually (instead of just a summary count).
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE workout_entries ADD COLUMN supersetGroupId TEXT"
                )
                // CREATE TABLE IF NOT EXISTS = create the table only if it
                // doesn't already exist (safe to run multiple times).
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
                // Index speeds up queries that look up sets by entryId.
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_workout_sets_entryId ON workout_sets (entryId)"
                )
            }
        }

        // Returns the single shared database instance (Singleton pattern).
        // Using a single instance avoids race conditions and conserves resources.
        fun getInstance(context: Context): AppDatabase {
            // INSTANCE ?: ... means "if INSTANCE is null, run the block".
            return INSTANCE ?: synchronized(this) {
                // synchronized = only one thread can be inside this block at
                // a time, preventing the database from being created twice.
                val instance = Room.databaseBuilder(
                    context.applicationContext, // use app context, not Activity
                    AppDatabase::class.java,
                    "trackapp_database"          // the filename on disk
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3) // apply schema upgrades
                    // Fallback: if no migration covers the version gap, wipe and
                    // recreate the database. This loses data but prevents crashes.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
