# UI Documentation

## Screens

| Screen | File | Purpose |
|---|---|---|
| AuthScreen | `ui/screens/AuthScreen.kt` | Google Sign-In prompt (shown when unauthenticated) |
| SequenceListScreen | `ui/screens/SequenceListScreen.kt` | Main list of sequences with active run card |
| SettingsScreen | `ui/screens/SettingsScreen.kt` | Account management, debug toggle, sign-out |

## Navigation

There is **no navigation library** (no Navigation Compose, no NavHost). Navigation is handled via manual boolean state in `MainActivity`:

```
isAuthenticated == false  →  AuthScreen
showSettings == true      →  SettingsScreen
else                      →  SequenceListScreen (with TopAppBar)
```

Transitions:
- Sign-in success sets `isAuthenticated = true`
- Settings gear icon sets `showSettings = true`
- Back button in settings sets `showSettings = false`
- Sign-out sets `isAuthenticated = false`

This is a **single-activity, no-router** architecture. All screen content is rendered inline within `setContent {}`.

## State Management

### Pattern

- **ViewModels** hold `StateFlow` / `Flow` instances
- Compose collects via `.collectAsState()`
- `SharingStarted.WhileSubscribed(5_000)` used for all `stateIn` conversions (5-second replay)

### ViewModels

| ViewModel | Scope | State Type | Purpose |
|---|---|---|---|
| `SequenceViewModel` | `viewModelScope` | `StateFlow<List<SequenceEntity>>` | Sequence list + CRUD |
| `SequenceRunViewModel` | `viewModelScope` | `StateFlow<SequenceRunUiState>` | Active run state machine |

### UI State Model

`SequenceRunUiState` is a sealed class:

```kotlin
sealed class SequenceRunUiState {
    data object Idle : SequenceRunUiState()
    data class Active(
        val run: SequenceRunEntity,
        val sequence: SequenceEntity,
        val steps: List<StepEntity>,
        val completedStepIds: Set<Long>,
        val currentStep: StepEntity?,
        val progressFraction: Float,
    ) : SequenceRunUiState()
}
```

### Activity-Level State

`MainActivity` manages three pieces of mutable state:
- `isAuthenticated: Boolean` — controls auth gate
- `isSyncing: Boolean` — shows sync indicator in TopAppBar
- `showSettings: Boolean` — toggles settings screen

## Screen Details

### AuthScreen

- Centered layout with app title and subtitle
- Single "Sign in with Google" button
- Callback: `onSignInClick: () -> Unit`

### SequenceListScreen

- `LazyColumn` with all sequences
- If an active run exists, shows `ActiveRunCard` at top with:
  - Sequence name
  - `LinearProgressIndicator` for completion
  - Current step text ("Step X of Y: instruction")
  - "Done" button (completes current step)
  - "End Run" button (finishes run)
- Each sequence item shows:
  - Name and step count
  - "Start" button (hidden if active run exists)
  - Expand/collapse toggle
- Expanded sequence shows ordered steps with position and estimated time

### SettingsScreen

- Vertically scrollable column
- Account section: shows connected email, "Disconnect" button
- Diagnostics section: "Debug Mode" toggle switch
- "Back" button

## Reusable Components

| Component | File | Purpose |
|---|---|---|
| `SettingsSectionLabel` | `CommonComponents.kt` | Styled section header for settings |

This is currently the only shared component. The `ActiveRunCard` and `SequenceItem` are private composables within `SequenceListScreen.kt`.

## Theme

- **Material3** with dynamic color support (Android 12+)
- Dark/light mode follows system setting
- Fallback color scheme: purple-based (`Purple80`/`Purple40`)
- Default typography with only `bodyLarge` customized (16sp, 0.5sp letter spacing)
- File: `ui/theme/Theme.kt`, `Color.kt`, `Type.kt`

## Permissions

- `POST_NOTIFICATIONS` — for sequence run notifications
- `INTERNET` — Firestore sync
- `ACCESS_NETWORK_STATE` — network checks

## UI Testing

- Compose UI test dependencies included (`compose-ui-test-junit4`, `compose-ui-test-manifest`)
- No UI test files currently exist
- `SequenceDaoTest` exists as an instrumented test (see database.md)
