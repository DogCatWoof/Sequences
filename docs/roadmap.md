# Roadmap

## Status

- **No TODO/FIXME/HACK/XXX comments** found in the codebase
- No deprecated code markers
- No stub implementations beyond `GoogleAuthManager.getValidToken()` which throws `UnsupportedOperationException`

## Technical Debt

### Critical

| Issue | Location | Impact |
|---|---|---|
| **Broken instrumented test** | `SequenceDaoTest.kt:17` | Imports nonexistent `TaskDatabase` — test won't compile |
| **Destructive migration** | `SequencesDatabase.kt:46` | Any schema change destroys all user data |
| **`fullDataJson` never populated** | `StepEntity.kt` | Web step spec data (type, reps, weight, etc.) is lost on sync |
| **`FirestoreSource.uid` unused** | `FirestoreSource.kt:23` | All users share same Firestore collections — no data isolation |

### Moderate

| Issue | Location | Impact |
|---|---|---|
| **`FirestoreSyncPrefs` unused** | `FirestoreSyncPrefs.kt` | Incremental sync timestamp never passed to `pullAndMerge` |
| **No retry/backoff on sync** | `FirestoreSyncService.kt` | Failed syncs require manual retry |
| **`GoogleAuthManager.getValidToken()` throws** | `GoogleAuthManager.kt:96` | Dead code, will crash if called |
| **`fullDataJson` field never read** | `StepEntity.kt:17` | Stored but never deserialized for UI |

### Minor

| Issue | Location | Impact |
|---|---|---|
| **`mood_channel` unused** | `NotificationChannels.kt:8` | Registered but never used |
| **`reminders_channel` unused** | `NotificationChannels.kt:9` | Registered but never used |
| **`SequenceViewModel.addSequence` not called from UI** | `SequenceViewModel.kt:22` | Create functionality exists but no UI to trigger it |
| **`SequenceViewModel.addStep` not called from UI** | `SequenceViewModel.kt:30` | Same — create step has no UI |
| **`SequenceViewModel.deleteStep` not called from UI** | `SequenceViewModel.kt:42` | Delete step has no UI |
| **No `SequenceViewModel.deleteSequence` in UI** | `SequenceListScreen.kt` | No way to delete a sequence from the app |

## Missing Features

### Data Input

- [ ] UI to create new sequences (ViewModel method exists, no screen)
- [ ] UI to add steps to sequences
- [ ] UI to delete sequences from the list
- [ ] UI to edit sequence names/descriptions
- [ ] UI to reorder steps

### Step Types

- [ ] Timer display for `action` steps with `useDuration`
- [ ] Rep counter for `repetition` steps
- [ ] Weight/duration input for `repetition` steps
- [ ] `repeat_group` step rendering (recursive steps)
- [ ] Voice activation support for repetition steps

### Sync

- [ ] Implement incremental sync (use `FirestoreSyncPrefs`)
- [ ] Auto-sync on app launch
- [ ] Background sync with WorkManager
- [ ] Offline-first write queue
- [ ] Conflict resolution UI (or confirm last-write-wins policy)

### Polish

- [ ] Deep linking support
- [ ] Widget for quick sequence start
- [ ] Dark theme customization
- [ ] Onboarding flow
- [ ] Sequence sharing between users

### Testing

- [ ] Fix `SequenceDaoTest` (references `TaskDatabase`)
- [ ] Add UI tests for Compose screens
- [ ] Add `FirestoreSyncService` unit tests
- [ ] Add `SequenceViewModel` unit tests
- [ ] Add `GoogleAuthManager` unit tests

## Architecture Improvements

- [ ] Extract navigation to Navigation Compose
- [ ] Add proper Room migrations (replace `fallbackToDestructiveMigration`)
- [ ] Multi-module extraction (use convention plugins in `build-logic/`)
- [ ] Add Firebase Crashlytics for production error tracking
- [ ] Consider Kotlin Serialization instead of Gson
- [ ] Add ProGuard/R8 rules and enable minification for release builds
