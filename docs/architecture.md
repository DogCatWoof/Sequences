# Architecture

## High-Level Overview

Sequences is a single-module Android application for managing and running step-by-step routines (sequences). It syncs data bidirectionally with Firebase Firestore, uses Room for local persistence, and authenticates via Google Sign-In.

```
┌──────────────────────────────────────────────────────────┐
│                      UI Layer                            │
│  MainActivity → Compose Screens → ViewModels (StateFlow) │
└────────────────────────┬─────────────────────────────────┘
                         │
┌────────────────────────▼─────────────────────────────────┐
│                   Repository Layer                        │
│              SequenceRepository (persistence)             │
└──────┬──────────────────────┬────────────────────────────┘
       │                      │
┌──────▼──────┐    ┌──────────▼──────────┐
│  Room DB    │    │  Firestore (cloud)  │
│  (SQLite)   │    │  FirestoreSource    │
│  SequenceDao│    │  FirestoreSyncSvc   │
└─────────────┘    └─────────────────────┘
```

## Module Structure

This is a **single-module** Android app. The `build-logic/` directory provides convention plugins for potential future multi-module extraction.

### Package Breakdown (`org.meow.sequences`)

| Package | Responsibility |
|---|---|
| `di/` | Koin DI module definitions (6 modules) |
| `data/database/` | Room database definition, type converters |
| `data/sequence/` | Entities, DAO, repository, notification manager, broadcast receiver |
| `data/firestore/` | Firestore sync service, document models, spec models |
| `data/auth/` | Google Sign-In via Credential Manager, encrypted token store |
| `data/debug/` | Debug settings, exception reporter |
| `data/diagnostics/` | In-memory query timing logger |
| `core/notifications/` | Notification channel registration |
| `ui/screens/` | Compose screens and ViewModels |
| `ui/theme/` | Material3 theme, colors, typography |

## Dependency Injection (Koin)

DI is wired in `SequencesApp.kt` via `startKoin`. Six modules are loaded at app startup:

```
databaseModule  → SequencesDatabase (singleton), SequenceDao (singleton)
diagnosticsModule → QueryLogger, DebugSettings, ExceptionReporter
authModule      → TokenStore, GoogleAuthManager
repositoryModule → SequenceRepository
firestoreModule → FirestoreSource, FirestoreSyncService
viewModelModule → SequenceViewModel, SequenceRunViewModel
```

All dependencies use **constructor injection**. ViewModels are provided via Koin's `viewModel` DSL and consumed in Compose via `koinViewModel()`.

### Key DI Files

- `di/DatabaseModule.kt` — Room database + DAO
- `di/RepositoryModule.kt` — Repository with DAO + QueryLogger
- `di/FirestoreModule.kt` — Firestore source + sync service
- `di/AuthModule.kt` — Token store + auth manager
- `di/DiagnosticsModule.kt` — Debug tools
- `di/ViewModelModule.kt` — Both ViewModels

## Data Flow

### Sequence Definition Flow

1. `SequenceViewModel` observes `SequenceRepository.getAllSequences()` (returns `Flow<List<SequenceEntity>>`)
2. Room emits updates reactively when data changes
3. ViewModel converts to `StateFlow` via `stateIn(viewModelScope, WhileSubscribed(5s))`
4. Compose collects via `collectAsState()`

### Active Run Flow

1. `SequenceRunViewModel` observes `SequenceRepository.getActiveRun()` via `flatMapLatest`
2. When a run exists, it also observes `getProgress(runId)` to compute completion state
3. Emits sealed `SequenceRunUiState` (Idle | Active)
4. Progress fraction, current step, and completed IDs are computed from the combined flows

### Sync Flow

1. User taps sync button or app auto-syncs on launch
2. `MainActivity.syncFromFirestore()` calls `FirestoreSyncService.pullAndMerge(uid, null)`
3. Pull: fetches all documents from Firestore collections (`sequences`, `steps`, `sequenceRuns`)
4. Compares `lastModifiedAt` timestamps — remote wins if newer
5. Upserts into Room via DAO
6. Push: reads pending local changes (`pendingFirestoreSync = true`), uploads to Firestore, clears flag

### Notification Run Flow

1. When a run starts, `SequenceRunNotificationManager.update()` posts an ongoing notification
2. "Done" action fires `ACTION_COMPLETE_STEP` broadcast → `StepReceiver`
3. Receiver marks step complete, checks if all steps done, auto-completes run if so
4. "End" action fires `ACTION_END_RUN` → completes run and cancels notification

## External Service Integrations

| Service | Purpose | Auth |
|---|---|---|
| Firebase Auth | Google Sign-In session | Google ID token → Firebase credential |
| Firebase Firestore | Cloud data sync | Firebase Auth (per-user paths) |
| Android Credential Manager | Google Sign-In UI | N/A |

## Build System

- **AGP 9.2.1** with Kotlin 2.2.10
- **Convention plugins** in `build-logic/`:
  - `autistic.android-library` — base Android library config
  - `autistic.android-library-compose` — adds Compose dependencies
  - `autistic.android-library-room` — adds Room + KSP
- **Version catalog** at `gradle/libs.versions.toml`
- **Configuration cache** enabled
- **KSP** for Room annotation processing
- **compileSdk / targetSdk**: 36, **minSdk**: 26
- **JVM toolchain**: 21
- gRPC dependencies forced to 1.68.0 for Firebase compatibility

### Build Notes

- `google-services.json` is copied from `local-only/` at build time (gitignored)
- `FIREBASE_WEB_CLIENT_ID` is read from `local-only/local.properties` and injected via `BuildConfig`
- Custom `run` task installs debug APK and launches via `adb`

---

## Missing Information (Human Input Required)

1. **Security rules for Firestore**: The spec mentions `request.auth.uid` checks but the actual Firestore security rules file is not in the repository. Are there additional field-level validation rules?

2. **Push notification tokens / FCM**: The app registers notification channels but doesn't appear to use FCM for push notifications. Is FCM planned, or is the app purely pull-based?

3. **Offline conflict resolution policy**: The current code uses "last write wins" based on `lastModifiedAt`. Is this the intended strategy, or should there be a conflict resolution UI?

4. **Analytics / crash reporting**: No Firebase Crashlytics or Analytics SDK is included. Is this intentional for privacy reasons, or planned?

5. **CI/CD pipeline**: No GitHub Actions or CI configuration exists. What is the deployment/release process?

6. **ProGuard / R8 rules**: `isMinifyEnabled = false` in release builds. Is obfuscation planned?

7. **The `fullDataJson` field on `StepEntity`**: Stores the complete step spec as JSON but is never populated by the current sync code. Was this intended as an escape hatch for future step types, or is it dead code?

8. **`FirestoreSource.colRef` ignores the `uid` parameter**: The method signature accepts `uid` but the Firestore collection path doesn't include it (`firestore.collection(collection)`). This means all users share the same Firestore collections — is this correct, or should paths be per-user?

9. **The `mood_channel` and `reminders_channel` notification channels**: Registered but never used anywhere in the codebase. Are these vestiges from another app or planned features?

10. **`GoogleAuthManager.getValidToken()`**: Throws `UnsupportedOperationException`. Is OAuth token-based API access (e.g., Google Tasks, Calendar) planned?
