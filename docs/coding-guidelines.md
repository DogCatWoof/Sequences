# Coding Guidelines

## Language & Style

- **Kotlin** exclusively (no Java source files)
- `kotlin.code.style=official` in `gradle.properties`
- JVM toolchain: Java 21
- Kotlin 2.2.10

## Code Organization

### File Structure

```
app/src/main/java/org/meow/sequences/
├── SequencesApp.kt          # Application class
├── MainActivity.kt          # Single activity
├── GlobalErrorHandler.kt    # Error bus
├── core/
│   └── notifications/       # Notification channels
├── data/
│   ├── auth/                # Authentication
│   ├── database/            # Room database
│   ├── debug/               # Debug tools
│   ├── diagnostics/         # Query timing
│   ├── firestore/           # Cloud sync
│   └── sequence/            # Core domain entities, DAO, repository
└── ui/
    ├── screens/             # Compose screens + ViewModels
    └── theme/               # Material3 theme
```

### Naming Conventions

| Element | Convention | Examples |
|---|---|---|
| Classes | PascalCase | `SequenceRepository`, `GoogleAuthManager` |
| Functions | camelCase | `getAllSequences()`, `startRun()` |
| Constants | UPPER_SNAKE_CASE | `SEQUENCE_NOTIFICATION_ID`, `ACTION_COMPLETE_STEP` |
| Variables | camelCase | `expandedSequenceId`, `isAuthenticated` |
| Private variables | camelCase | `_entries`, `_errors` |
| Composable functions | PascalCase | `SequenceListScreen`, `ActiveRunCard` |
| Private composables | PascalCase | `SequenceItem` |
| Tables | snake_case | `sequences`, `steps` |
| Packages | lowercase | `org.meow.sequences.data.firestore` |

### File Naming

- Entities: `*Entity.kt` (e.g., `SequenceEntity.kt`)
- DAOs: `*Dao.kt`
- ViewModels: `*ViewModel.kt`
- Screens: `*Screen.kt`
- DI modules: `*Module.kt`
- Convention plugins: `autistic.android-*.gradle.kts`

## Architecture Patterns

### Layered Architecture

```
UI (Compose + ViewModel) → Repository → DAO → Room
                                    → FirestoreSource → Firestore
```

- ViewModels depend on Repository
- Repository depends on DAO + QueryLogger
- FirestoreSyncService depends on FirestoreSource + DAO
- No reverse dependencies

### State Management

- `StateFlow` for all observable state
- `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue)` pattern
- Sealed classes for UI state (`SequenceRunUiState`)
- Mutable state in Activity: `mutableStateOf` for auth/sync/settings toggles

### DI

- Koin modules defined as top-level `val` in separate files
- Singletons for database, DAO, repository, sync service
- ViewModels via `viewModel { }` DSL
- Compose injection: `koinViewModel()` and `koinInject()`

## Coroutine Rules

- All database and network operations use `Dispatchers.IO`
- ViewModels use `viewModelScope` (Main dispatcher by default)
- `FirestoreSource` wraps all calls in `withContext(Dispatchers.IO)`
- `StepReceiver` uses `CoroutineScope(Dispatchers.IO)` (not lifecycle-bound)
- No `GlobalScope` usage
- `SupervisorJob` used in `StepReceiver` coroutine scope

## Error Handling

- Exceptions propagate from repository/DAO layer
- UI layer catches and displays via Toast or `GlobalErrorHandler`
- `GlobalErrorHandler` uses `MutableSharedFlow` with `extraBufferCapacity = 8`
- `ExceptionReporter` logs to Logcat and reports to `GlobalErrorHandler`
- No crash reporting SDK (no Crashlytics)

## Testing

### Unit Tests

- **MockK** for mocking
- **kotlinx-coroutines-test** for coroutine testing
- `runTest` for coroutine test scope
- `UnconfinedTestDispatcher` for deterministic async
- `Dispatchers.setMain` / `resetMain` in ViewModel tests
- Mocking `SequenceRunNotificationManager` via `mockkObject`

### Instrumented Tests

- Room in-memory database for DAO tests
- JUnit4 + AndroidJUnit4 runner
- `runTest` for coroutine testing

### Test Naming

- Backtick method names: `` `getAllSequences exposes flow from dao` ``
- Descriptive, behavior-focused names

## Compose Conventions

- Material3 components only
- `@Composable` functions with `modifier` parameter as last optional param
- `collectAsState()` for Flow collection in Compose
- `remember { mutableStateOf() }` for local UI state
- Private composables for screen-internal components

## Import Ordering

Standard Kotlin ordering (alphabetical by package):
1. `android.*`
2. `androidx.*`
3. `com.google.*`
4. `io.mockk.*`
5. `kotlin.*`
6. `kotlinx.*`
7. `org.meow.*`

## Documentation

- KDoc on public classes and important functions
- No inline code comments (per project convention)
- `/** ... */` style for class-level documentation

## Build Conventions

- Convention plugins in `build-logic/` for shared config
- Version catalog for all dependency versions
- `local-only/` directory for secrets (gitignored)
- `google-services.json` copied from `local-only/` at build time
