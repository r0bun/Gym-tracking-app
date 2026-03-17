# TrackApp — Workout Tracker

A native Android workout tracking app built with **Kotlin + Jetpack Compose**, **Room (SQLite)**, and **Supabase**.

**[Download the latest APK here!](https://github.com/r0bun/Gym-tracking-app/releases/tag/v1.0.0)**

### Features
- **Account auth** — email/sign-up via Supabase with persistent session ("Keep me signed in")
- **Per-set logging** — each exercise entry has individual sets with reps, weight, and optional to-failure flag
- **kg / lbs toggle** — per-exercise unit preference, remembered across sessions
- **Superset linking** — link two exercises together with a shared superset label
- **Custom exercises** — create your own exercises locally; shown with a "Custom" chip in the picker
- **Rename custom exercises** — tap the pencil icon to rename any exercise you created
- **Workout templates** — start a new workout from a previous session to pre-load all exercises and sets
- **Exercise search** — search and filter cloud-synced exercise list by muscle group
- **Workout rename** — tap the title to rename any workout session
- **History** — browse and delete past workouts with confirmation dialog
- **Theme customization** — pick from 8 preset accent colors or enter any custom hex code; app recolors instantly
- **Compose previews** — every screen has @Preview composables for rapid UI iteration in Android Studio
- **Offline-first** — all workout data stored locally in Room; exercises cached from Supabase on login

---

## Tech Stack

| Layer | Technology |
|---|---|
| UI | Kotlin + Jetpack Compose + Material 3 |
| Local DB | Room (SQLite) |
| Backend / Auth | Supabase (PostgreSQL + Auth) |
| HTTP | Ktor Android client |
| Navigation | Navigation Compose |

---



## App Flow

```
Launch → Loading screen (resolves persisted Supabase session)
           ↓
       Already signed in?  ──yes──▶  Home Screen
           ↓ no
       Login / Sign Up  ("Keep me signed in" checkbox)
           ↓
       Sync exercises from Supabase → cached in Room SQLite
           ↓
       Home Screen → Start Workout (named session, or start from a previous workout)
                      ↓
                  Stored locally
           ↓
       Workout Screen → Pick exercises → log per-set reps/weight/to-failure
                        kg ↔ lbs toggle per exercise
                        Optional superset linking
                        Rename workout via tap on title
           ↓
       History Screen → Browse / delete past workouts
           ↓
       Settings Screen → Theme color picker (presets + custom hex)
```

---

## Database Schema (Local Room — v4)

```
exercises       : id | name | muscle_group | is_custom
                  ← synced from Supabase on login, cached locally
                  ← custom exercises stored locally only (is_custom = 1)

workouts        : id | date | notes
                  ← stored locally only

workout_entries : id | workout_id | exercise_id | notes | useLbs | superset_id
                  ← one row per exercise logged in a workout

sets            : id | entry_id | set_number | reps | weight_kg | to_failure
                  ← one row per individual set within an entry
```

---

## Folder Structure

```
app/src/main/java/com/trackapp/
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt            ← Room DB, version 4
│   │   ├── dao/                      ← ExerciseDao, WorkoutDao, WorkoutEntryDao, SetDao
│   │   └── entity/                   ← ExerciseEntity, WorkoutEntity, WorkoutEntryEntity,
│   │                                    SetEntity, EntryWithSetsRelation
│   ├── remote/
│   │   ├── SupabaseConfig.kt         ← Supabase client (initialized in Application.onCreate)
│   │   ├── SharedPreferencesSessionManager.kt  ← persists JWT session across restarts
│   │   └── RemoteExercise.kt
│   └── repository/
│       ├── AuthRepository.kt         ← auth + reactive isSignedInFlow
│       ├── ExerciseRepository.kt
│       ├── WorkoutRepository.kt
│       └── PreferencesRepository.kt  ← kg/lbs default, keep-signed-in, accent color
├── ui/
│   ├── navigation/                   ← Screen.kt, AppNavigation.kt (loading → login/home)
│   ├── screens/
│   │   ├── auth/                     ← LoginScreen, LoginViewModel
│   │   ├── home/                     ← HomeScreen, HomeViewModel
│   │   ├── workout/                  ← WorkoutScreen, WorkoutViewModel
│   │   ├── history/                  ← HistoryScreen, HistoryViewModel
│   │   └── settings/                 ← SettingsScreen, SettingsViewModel
│   └── theme/                        ← Color.kt, Theme.kt, Type.kt, AccentColor.kt
├── MainActivity.kt
└── TrackApplication.kt
```
