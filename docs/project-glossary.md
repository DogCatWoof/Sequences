# Project Glossary

## Domain Terms

| Term | Definition |
|---|---|
| **Sequence** | A named, ordered checklist of steps that a user runs repeatedly (e.g., "Morning Routine") |
| **Step** | A single instruction within a sequence (e.g., "Stretch hamstrings") |
| **Run** | A single execution of a sequence, from start to completion |
| **Step Progress** | A record that a specific step was completed within a run |
| **Active Run** | A run where `completedAt` is null — only one allowed at a time |
| **Soft Delete** | Setting `isDeleted = true` instead of removing the record from the database |
| **Pending Firestore Sync** | A flag indicating local changes need to be uploaded to Firestore |
| **Spec Step ID** | The UUID assigned by the web app (`crypto.randomUUID()`), stored as `specStepId` |

## Step Types

| Type | Description |
|---|---|
| `action` | Simple action, optionally with a duration timer |
| `repetition` | Exercise with sets, reps, weight/duration, rest periods |
| `repeat_group` | Group of sub-steps that repeat until done |

## Key Classes

| Class | File | Purpose |
|---|---|---|
| `SequencesApp` | `SequencesApp.kt` | Application class, initializes Koin |
| `MainActivity` | `MainActivity.kt` | Single activity, hosts Compose content and sync logic |
| `SequenceViewModel` | `SequenceViewModel.kt` | Manages sequence list and CRUD operations |
| `SequenceRunViewModel` | `SequenceRunViewModel.kt` | Manages active run state and step completion |
| `SequenceRepository` | `SequenceRepository.kt` | Persistence layer wrapping DAO with timestamp stamping |
| `SequenceDao` | `SequenceDao.kt` | Room data access for all entities |
| `SequencesDatabase` | `SequencesDatabase.kt` | Room database definition (v2) |
| `FirestoreSyncService` | `FirestoreSyncService.kt` | Bidirectional Firestore ↔ Room sync |
| `FirestoreSource` | `FirestoreSource.kt` | Thin Firestore SDK wrapper |
| `GoogleAuthManager` | `GoogleAuthManager.kt` | Google Sign-In via Credential Manager |
| `TokenStore` | `TokenStore.kt` | Encrypted SharedPreferences for auth tokens |
| `GlobalErrorHandler` | `GlobalErrorHandler.kt` | SharedFlow-based error bus for UI |
| `ExceptionReporter` | `ExceptionReporter.kt` | Logs and reports exceptions via GlobalErrorHandler |
| `QueryLogger` | `QueryLogger.kt` | In-memory log of slow database queries |
| `SequenceRunNotificationManager` | `SequenceRunNotificationManager.kt` | Builds/manages the persistent run notification |
| `StepReceiver` | `StepReceiver.kt` | BroadcastReceiver for notification actions |
| `DebugSettings` | `DebugSettings.kt` | SharedPreferences for debug mode toggle |

## Abbreviations

| Abbreviation | Meaning |
|---|---|
| `fs` | Firestore (e.g., `firestoreId`, `getByFirestoreId`) |
| `vm` | ViewModel (e.g., `vm: SequenceViewModel`) |
| `pk` | Primary Key |
| `fk` | Foreign Key |
| `id` | Identifier |
| `kv` | Key-Value |
| `json` | JavaScript Object Notation |
| `iso` | ISO 8601 date-time format |

## Configuration Keys

| Key | Location | Purpose |
|---|---|---|
| `firebase.web.client.id` | `local-only/local.properties` | Google Sign-In server client ID |
| `google-services.json` | `local-only/` | Firebase configuration |
| `FIREBASE_WEB_CLIENT_ID` | `BuildConfig` | Injected at build time from local.properties |
| `debug_enabled` | `debug_settings` SharedPreferences | Debug mode toggle |
| `last_firestore_pull_epoch_seconds` | `firestore_sync` DataStore | Last sync timestamp |
| `account_email` | `auth_token_store` EncryptedPrefs | Stored Google account email |
| `access_token` | `auth_token_store` EncryptedPrefs | OAuth access token |
| `token_expiry_ms` | `auth_token_store` EncryptedPrefs | Token expiration timestamp |

## Database Tables

| Table | Entity | Purpose |
|---|---|---|
| `sequences` | `SequenceEntity` | Sequence definitions |
| `steps` | `StepEntity` | Steps within sequences |
| `sequence_runs` | `SequenceRunEntity` | Execution records |
| `step_progress` | `StepProgressEntity` | Per-step completion within runs |

## Notification Constants

| Constant | Value | Purpose |
|---|---|---|
| `SEQUENCE_NOTIFICATION_ID` | `100` | Persistent notification for active run |
| `SEQUENCES_CHANNEL_ID` | `"sequences_channel"` | Channel for run progress notifications |
| `MOOD_CHANNEL_ID` | `"mood_channel"` | Registered but unused |
| `REMINDER_CHANNEL_ID` | `"reminders_channel"` | Registered but unused |

## Broadcast Actions

| Action | Constant | Purpose |
|---|---|---|
| `ACTION_COMPLETE_STEP` | `"org.meow.autistic.ACTION_COMPLETE_STEP"` | Notification "Done" button |
| `ACTION_END_RUN` | `"org.meow.autistic.ACTION_END_RUN"` | Notification "End" button |

## Package Naming

The app ID is `org.meow.autistic` (for Play Store / APK identity), but the package namespace is `org.meow.sequences`. This discrepancy exists because the app was likely renamed from "Autistic" to "Sequences" at some point.
