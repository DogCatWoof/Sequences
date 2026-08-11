# API Documentation

## Overview

Sequences uses **Firebase Firestore** as its backend API. There is no custom REST/GraphQL API. All network communication goes through the Firestore SDK.

## Authentication

- **Firebase Auth** with Google Sign-In only
- Credential Manager flow: Google ID token → Firebase `signInWithCredential`
- Firebase session maintained via `FirebaseAuth.getInstance().currentUser`
- No custom tokens, API keys in code, or session refresh logic

## Firestore Structure

### Collections

| Collection | Purpose | Owner |
|---|---|---|
| `sequences` | Sequence definitions | Web + Android |
| `steps` | Sequence step definitions (normalized) | Web + Android |
| `sequenceRuns` | Run history | Android writes, web reads |

### Document Shapes

#### `sequences/{docId}`

```json
{
  "name": "Morning Routine",
  "description": "Daily stretches",
  "stepIds": ["step1", "step2"],
  "isDeleted": false,
  "createdAt": "2024-01-01T00:00:00Z",
  "lastModifiedAt": Timestamp,
  "pendingFirestoreSync": false
}
```

#### `steps/{docId}`

```json
{
  "sequenceId": "parent-sequence-doc-id",
  "instruction": "Stretch hamstrings",
  "estimatedMinutes": 5,
  "position": 0,
  "specStepId": "uuid-from-web",
  "stepType": "action",
  "isDeleted": false,
  "lastModifiedAt": Timestamp
}
```

#### `sequenceRuns/{docId}`

```json
{
  "sequenceId": "parent-sequence-doc-id",
  "startedAt": Timestamp,
  "completedAt": Timestamp,
  "lastModifiedAt": Timestamp,
  "stepRecords": [
    { "stepId": "string", "stepType": "string", "label": "string", "sets": [] }
  ]
}
```

## Network Layer

### FirestoreSource

**File**: `data/firestore/FirestoreSource.kt`

Thin wrapper around `FirebaseFirestore`. All methods run on `Dispatchers.IO`.

| Method | Operation | Description |
|---|---|---|
| `upsert(uid, collection, docId, data)` | `document.set(data)` | Create or overwrite |
| `delete(uid, collection, docId)` | `document.delete()` | Delete document |
| `fetchAll(uid, collection)` | `collection.get()` | Get all docs |
| `fetchSince(uid, collection, since)` | `whereGreaterThan("lastModifiedAt", ...)` | Incremental fetch |

**Note**: The `uid` parameter is accepted but not used in the collection path. All users share the same Firestore collections.

### FirestoreSyncService

**File**: `data/firestore/FirestoreSyncService.kt`

Orchestrates bidirectional sync:

#### Pull (Remote → Local)

1. `pullSequences(uid, since)` — fetches all sequence docs, upserts if remote is newer
2. `pullSteps(uid, since)` — fetches step docs, resolves parent sequence by `firestoreId`
3. `pullSequenceRuns(uid, since)` — fetches run docs, resolves parent sequence by `firestoreId`

#### Push (Local → Remote)

1. `pushSequences(uid)` — uploads sequences with `pendingFirestoreSync = true`
2. `pushSequenceRuns(uid)` — uploads runs with `pendingFirestoreSync = true`

**Merge strategy**: Last-write-wins based on `lastModifiedAt` timestamp comparison.

### Sync Timestamp Persistence

**File**: `data/firestore/FirestoreSyncPrefs.kt`

- Uses DataStore Preferences (`firestore_sync`)
- Stores `last_firestore_pull_epoch_seconds`
- `FirestoreSyncPrefs.getLastPullAt()` / `setLastPullAt()` for incremental sync

**Note**: `FirestoreSyncPrefs` is defined but `FirestoreSyncService.pullAndMerge()` is always called with `since = null` from `MainActivity`, so incremental sync is not currently functional.

## Serialization

- **Gson** is used for serializing/deserializing `stepRecords` JSON in `SequenceRunEntity`
- Firestore documents are manually mapped via extension functions (`toDocument()`, `toEntity()`, `fromSnapshot()`)
- No kotlinx.serialization or Moshi

### Spec Models

**File**: `data/firestore/SpecModels.kt`

Intermediate models for Firestore ↔ entity conversion:
- `SpecSequence` — parsed from Firestore map
- `SpecStep` — parsed from Firestore map
- `SpecSequenceRun` — parsed from Firestore map, includes Gson for stepRecords

## Error Handling

- Network errors propagate as exceptions from Firestore SDK (`tasks.await()`)
- `MainActivity.syncFromFirestore()` catches `Exception` and shows a Toast
- No retry logic, no exponential backoff
- `FirestoreSource` methods use `withContext(Dispatchers.IO)` for thread safety

## Caching

- Room serves as the offline cache
- Firestore data is pulled into Room, then observed via Room's reactive `Flow`
- No additional HTTP or in-memory caching layer

## Rate Limiting

None implemented. Firestore has built-in quotas but no app-level throttling.

## Firebase Dependencies

| Library | Version | Purpose |
|---|---|---|
| Firebase Auth | BOM-managed | Authentication |
| Firebase Firestore | BOM-managed | Cloud database |
| gRPC OkHttp | 1.81.0 (forced to 1.68.0) | Firestore transport |
| Firebase BOM | 34.13.0 | Version management |
