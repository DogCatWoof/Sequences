# Database Documentation

## Database Overview

- **Engine**: Room (SQLite)
- **Name**: `sequences_database`
- **Version**: 3
- **Schema export**: Disabled (`exportSchema = false`)
- **File**: `data/database/SequencesDatabase.kt`

> **Firestore collections**: See [`Shared/docs/domain.md`](../../../Shared/docs/domain.md) for the canonical Firestore collection schemas. The Room schema below is the Android local cache; field names differ from Firestore (e.g. `instruction` vs `instructions`, `stepType` vs `type`).

## Entities

### SequenceEntity

**Table**: `sequences`

| Column | Type | Default | Notes |
|---|---|---|---|
| `id` | Long (PK, auto) | 0 | Local auto-generated ID |
| `name` | String | — | User-visible title |
| `description` | String | `""` | Optional description |
| `createdAt` | Instant | `Instant.now()` | Set once on creation |
| `firestoreId` | String? | null | Remote Firestore document ID |
| `lastModifiedAt` | Instant | `Instant.now()` | Updated on every save |
| `pendingFirestoreSync` | Boolean | true | True when local changes need upload |
| `isDeleted` | Boolean | false | Soft-delete flag |

### StepEntity

**Table**: `steps`

| Column | Type | Default | Notes |
|---|---|---|---|
| `id` | Long (PK, auto) | 0 | Local auto-generated ID |
| `sequenceId` | Long | — | FK to sequences.id |
| `instruction` | String | — | Step description |
| `estimatedMinutes` | Int? | null | Optional time estimate |
| `position` | Int | — | 0-based ordering |
| `specStepId` | String | `""` | Web app's step ID |
| `stepType` | String | `"action"` | Step type identifier |
| `fullDataJson` | String | `"{}"` | Full spec data as JSON |
| `firestoreId` | String? | null | Remote Firestore document ID |
| `lastModifiedAt` | Instant | `Instant.now()` | Updated on every save |
| `pendingFirestoreSync` | Boolean | true | True when local changes need upload |
| `isDeleted` | Boolean | false | Soft-delete flag |

### SequenceRunEntity

**Table**: `sequence_runs`

| Column | Type | Default | Notes |
|---|---|---|---|
| `id` | Long (PK, auto) | 0 | Local auto-generated ID |
| `sequenceId` | Long | — | FK to sequences.id |
| `startedAt` | Instant | — | When run started |
| `completedAt` | Instant? | null | null = in progress |
| `stepRecordsJson` | String | `"[]"` | Step records as JSON array |
| `firestoreId` | String? | null | Remote Firestore document ID |
| `lastModifiedAt` | Instant | `Instant.now()` | Updated on every save |
| `pendingFirestoreSync` | Boolean | true | True when local changes need upload |

### StepProgressEntity

**Table**: `step_progress`

| Column | Type | Notes |
|---|---|---|
| `runId` | Long (composite PK) | FK to sequence_runs.id |
| `stepId` | Long (composite PK) | FK to steps.id |
| `completedAt` | Instant | When step was marked done |

## Relationships

```
SequenceEntity (1) ──→ (N) StepEntity
     │                         │
     │  via sequenceId         │  via stepId
     ▼                         ▼
SequenceRunEntity (1) ──→ (N) StepProgressEntity
```

- A sequence has many steps (ordered by `position`)
- A sequence has many runs
- A run has many step-progress records (composite key: runId + stepId)

## Type Converters

```kotlin
// InstantConverter — stores Instant as ISO-8601 string
Instant → String (via Instant.toString())
String → Instant (via Instant.parse())
```

## Indexes

No explicit Room `@Index` annotations. Relies on:
- Primary key lookups (by `id`)
- Foreign key queries (`sequenceId` on steps, `sequenceId` on runs)
- `WHERE` clauses on `pendingFirestoreSync`, `isDeleted`

## DAO Operations

**File**: `data/sequence/SequenceDao.kt`

### Query Methods

| Method | Return Type | Description |
|---|---|---|
| `getAll()` | `Flow<List<SequenceEntity>>` | All non-deleted sequences, ordered by name |
| `getById(id)` | `SequenceEntity?` | Single sequence by local ID |
| `getSteps(sequenceId)` | `Flow<List<StepEntity>>` | Non-deleted steps for a sequence, ordered by position |
| `getStepsOnce(sequenceId)` | `List<StepEntity>` | Suspend variant (no Flow) |
| `getStepById(id)` | `StepEntity?` | Single step by local ID |
| `getActiveRun()` | `Flow<SequenceRunEntity?>` | First run where `completedAt IS NULL` |
| `getActiveRunOnce()` | `SequenceRunEntity?` | Suspend variant |
| `getRunById(id)` | `SequenceRunEntity?` | Single run by local ID |
| `getProgress(runId)` | `Flow<List<StepProgressEntity>>` | All progress for a run |
| `getProgressOnce(runId)` | `List<StepProgressEntity>` | Suspend variant |
| `getByFirestoreId(firestoreId)` | `SequenceEntity?` | Lookup by remote ID |
| `getStepByFirestoreId(firestoreId)` | `StepEntity?` | Lookup by remote ID |
| `getRunByFirestoreId(firestoreId)` | `SequenceRunEntity?` | Lookup by remote ID |

### Mutation Methods

| Method | Strategy | Description |
|---|---|---|
| `insertSequence` | INSERT | Returns new ID |
| `deleteSequence` | DELETE | Hard delete from DB |
| `insertStep` | INSERT | Returns new ID |
| `deleteStep` | DELETE | Hard delete from DB |
| `deleteStepsForSequence` | DELETE | Bulk delete by sequenceId |
| `insertRun` | INSERT | Returns new ID |
| `updateRun` | UPDATE | Updates existing run |
| `upsertProgress` | UPSERT (REPLACE) | Insert or replace step progress |
| `upsertSequence` | UPSERT | Insert or replace sequence |
| `upsertStep` | UPSERT | Insert or replace step |
| `upsertRun` | UPSERT | Insert or replace run |

### Sync-Specific Methods

| Method | Description |
|---|---|
| `getPendingFirestoreSync()` | Active sequences needing upload |
| `getPendingFirestoreDelete()` | Deleted sequences needing upload |
| `markSequenceFirestoreSynced(id, firestoreId)` | Clears sync flag, sets remote ID |
| `getPendingFirestoreStepSync()` | Active steps needing upload |
| `getPendingFirestoreStepDelete()` | Deleted steps needing upload |
| `markStepFirestoreSynced(id, firestoreId)` | Clears sync flag, sets remote ID |
| `getPendingFirestoreRunSync()` | Runs needing upload |
| `markRunFirestoreSynced(id, firestoreId)` | Clears sync flag, sets remote ID |

## Migration Strategy

**Current**: `fallbackToDestructiveMigration(true)` — Room drops and recreates the database on version mismatch.

There are no migration scripts. The database was bumped from v1 to v2 at some point, and destructive migration was used instead of writing migration code.

## Soft Delete Pattern

- `isDeleted = true` marks a record as deleted
- All read queries filter with `WHERE isDeleted = 0`
- Soft-deleted records are pushed to Firestore with `isDeleted = true`
- The `deleteSequence()` repository method soft-deletes the sequence and all its steps

## Repository Layer

**File**: `data/sequence/SequenceRepository.kt`

The repository wraps the DAO and adds:
1. **Timestamp stamping** — sets `lastModifiedAt` and `pendingFirestoreSync = true` on mutations
2. **Soft-delete cascade** — `deleteSequence()` soft-deletes all child steps before soft-deleting the sequence
3. **Query timing** — all operations are wrapped with `timed()` for performance logging

## Known Issues

1. **`SequenceDaoTest` references `TaskDatabase`** (`app/src/androidTest/.../SequenceDaoTest.kt:17`) — the test imports `org.meow.sequences.data.task.TaskDatabase` which does not exist. This test will fail to compile. Likely a leftover from a project rename.

2. **Destructive migration** — any schema change will destroy user data. Should be replaced with proper `Migration` objects before production release.
