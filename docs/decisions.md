# Architecture Decisions

## 1. Room over SQLite Direct / Realm / DataStore

**Decision**: Use Room (version 2.8.4) as the local database.

**Rationale**:
- Room provides compile-time SQL verification via KSP annotation processing
- Reactive `Flow` return types enable seamless integration with Compose's `collectAsState()`
- Well-supported by Google with regular updates
- `@Upsert` simplifies the sync merge logic (insert-or-replace)
- TypeConverters handle `Instant` ↔ `String` conversion cleanly

**Alternatives considered**:
- **Raw SQLite**: More control but no compile-time safety, more boilerplate
- **Realm**: Heavier dependency, less Compose integration, licensing concerns
- **DataStore**: Only suitable for key-value or simple proto, not relational data

**Trade-offs**:
- Room adds KSP build time overhead
- Schema migrations require manual `Migration` objects (currently using destructive migration)
- No schema export (`exportSchema = false`) makes migration planning harder

---

## 2. StateFlow over LiveData

**Decision**: Use Kotlin `StateFlow` / `Flow` exclusively for all observable state.

**Rationale**:
- Kotlin-first API with `stateIn`, `flatMapLatest`, `map` operators
- Native coroutine integration — no lifecycle bridge needed
- `collectAsState()` is the idiomatic Compose collection pattern
- `WhileSubscribed(5_000)` provides automatic cleanup with configurable replay
- No `LiveData` dependencies needed in the UI layer

**Implementation pattern**:
```kotlin
val state: StateFlow<T> = repository.observe()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue)
```

**Trade-offs**:
- Requires `viewModelScope` for lifecycle-aware collection
- No automatic UI lifecycle pausing (but `WhileSubscribed` handles this)

---

## 3. Manual Navigation (No NavHost)

**Decision**: Use manual boolean state in `MainActivity` instead of Navigation Compose.

**Rationale**:
- Only 3 screens with trivial transitions (auth gate, settings toggle)
- No deep linking requirements (yet)
- No back-stack complexity
- Avoids NavHost boilerplate for simple visibility toggles

**Implementation**:
```kotlin
if (!isAuthenticated) AuthScreen(...)
else if (showSettings) SettingsScreen(...)
else SequenceListScreen(...)
```

**Trade-offs**:
- No saved state across process death (settings screen lost on rotation technically, though Compose handles this)
- Adding more screens will require refactoring
- No type-safe route definitions

---

## 4. Koin over Hilt / Dagger

**Decision**: Use Koin (version 4.2.1) for dependency injection.

**Rationale**:
- Runtime DI — no annotation processing, no code generation
- Simple `module { }` DSL is easy to read and maintain
- `koinViewModel()` and `koinInject()` integrate cleanly with Compose
- Less build-time overhead than Hilt/Dagger
- Good for single-module projects

**Implementation**: Six module files in `di/` package, loaded in `SequencesApp.onCreate()`.

**Trade-offs**:
- No compile-time DI safety (runtime errors for missing bindings)
- No `@ContributesAndroidInjector` equivalent — manual scoping
- Less IDE support than Hilt

---

## 5. Coroutine-Based Threading (Dispatchers.IO)

**Decision**: Use Kotlin coroutines with explicit dispatcher selection for all async work.

**Rationale**:
- `viewModelScope` for ViewModel operations (Main dispatcher)
- `Dispatchers.IO` for database and Firestore operations
- `withContext(Dispatchers.IO)` in `FirestoreSource` for thread safety
- `CoroutineScope(Dispatchers.IO)` in `StepReceiver` for background broadcast handling

**Rules**:
- No `GlobalScope` usage
- No `runBlocking` in production code
- `viewModelScope.launch { }` for all ViewModel-triggered mutations

---

## 6. Soft Delete Pattern

**Decision**: Use `isDeleted = true` flag instead of physical row deletion.

**Rationale**:
- Enables sync: deleted records must be uploaded to Firestore before removal
- Preserves referential integrity (runs reference sequences by ID)
- Consistent with the web app's behavior
- `WHERE isDeleted = 0` filter on all read queries

**Implementation**:
- `deleteSequence()` soft-deletes the sequence and all child steps
- `deleteStep()` soft-deletes individual steps
- Sync pushes deleted records to Firestore, then they remain as soft-deleted locally

**Trade-offs**:
- Requires index maintenance for soft-deleted records
- `deleteStepsForSequence()` does hard delete (potential inconsistency)

---

## 7. Timestamp-Based Sync Conflict Resolution

**Decision**: Use `lastModifiedAt` timestamp comparison for conflict resolution — remote wins if newer.

**Rationale**:
- Simple to implement and reason about
- Works for the current use case (single-device primary, web secondary)
- No need for operational transforms or CRDTs
- Consistent with typical offline-first patterns

**Implementation**:
```kotlin
if (local == null || remote.lastModifiedAt > local.lastModifiedAt) {
    dao.upsert(...)
}
```

**Trade-offs**:
- Clock skew between devices can cause data loss
- No user-facing conflict resolution
- Not suitable for concurrent multi-device editing

---

## 8. Broadcast Receiver for Notification Actions

**Decision**: Use a `BroadcastReceiver` (`StepReceiver`) to handle notification button actions.

**Rationale**:
- Android notifications require `PendingIntent` targets
- `BroadcastReceiver` is the standard pattern for notification actions
- Can run database operations via coroutine scope
- `goAsync()` prevents the system from killing the process mid-operation

**Implementation**:
- `ACTION_COMPLETE_STEP` and `ACTION_END_RUN` intents
- `KoinComponent` interface for DI in the receiver
- `CoroutineScope(Dispatchers.IO)` for async database work

---

## 9. Encrypted SharedPreferences for Token Storage

**Decision**: Use `EncryptedSharedPreferences` (via `androidx.security.crypto`) for auth token storage.

**Rationale**:
- AES-256 encryption at rest
- Backed by Android Keystore
- `MasterKey` with AES256_GCM scheme
- Graceful fallback: if encryption fails (key rotation, biometric change), wipes and retries, then falls back to plaintext

**Implementation**:
```kotlin
TokenStore.create(context)  // tries encrypted, falls back to plaintext
```

---

## 10. Convention Plugins for Build Logic

**Decision**: Use a `build-logic/` module with convention plugins instead of configuring each module directly.

**Rationale**:
- Shared Android config (compileSdk, minSdk, Java version) across potential future modules
- Three plugins: base library, Compose library, Room library
- Version catalog (`libs.versions.toml`) for all dependency versions
- Currently only `:app` module exists, but plugins are ready for multi-module extraction

---

## 11. Gson over Kotlin Serialization

**Decision**: Use Gson for JSON serialization (for `stepRecords` in `SequenceRunEntity`).

**Rationale**:
- Firestore SDK returns `Map<String, Any?>` which Gson handles naturally
- Simple use case: only one field needs JSON serialization
- No need for kotlinx.serialization's compile-time generation

**Trade-offs**:
- Runtime reflection-based (slower, no type safety)
- Mixed with manual `Map` construction for Firestore documents

---

## 12. No ProGuard / R8 Minification

**Decision**: `isMinifyEnabled = false` in release builds.

**Rationale**:
- Early development stage
- Firebase and gRPC libraries require complex ProGuard rules
- Not yet ready for production distribution

**Trade-offs**:
- Larger APK size
- No code obfuscation
