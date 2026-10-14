# Architecture

This document outlines the architecture of the application. The structure and conventions are
modelled directly on the [Bitwarden Android](https://github.com/bitwarden/android) app.

## Contents

- [Modules](#modules)
- [Data Layer](#data-layer)
- [UI Layer](#ui-layer)
- [Navigation](#navigation)

## Modules

The project is split up into a number of Gradle modules:

### Application Modules

- **`:app`**: The main application module. Contains the `MainActivity`, top-level application
  classes, and the feature-specific UI for the app.

### Core Library Modules

These libraries provide foundational capabilities utilized by application modules and other
feature-specific libraries:

- **`:annotation`**: Contains custom Java/Kotlin annotations used throughout the project. These
  annotations support compile-time behavior and provide metadata for other development tools.
- **`:core`**: A fundamental library containing shared business logic, essential data models, and
  common interfaces. It is designed with minimal or no direct Android framework dependencies to
  ensure high reusability and testability.
- **`:data`**: Manages the data layer for the applications. Responsibilities include defining
  repositories, data sources (for network APIs and local persistence), data transformation logic,
  and managing data synchronization. It abstracts data origin and storage details from other parts
  of the system.
- **`:network`**: Encapsulates all network communication logic. This includes API client setup
  (e.g., using Retrofit), defining request/response models for network operations, and handling
  network interceptors and authentication.
- **`:ui`**: A library module providing common UI elements, theming resources (colors, typography,
  shapes), custom Jetpack Compose components, base UI classes, and UI-related utility functions. It
  ensures a consistent user experience across different features and applications.

### Supporting Modules (Source Sets and Test Fixtures)

- **`src/main/`**: The primary source set of a module, containing its functional code.
- **`src/test/`**: Contains local unit tests (JVM-based) for the associated module.
- **`src/testFixtures/`**: Provides shared testing utilities, mock objects, sample data, or helper
  functions reusable across various test modules.

## Data Layer

The data layer is where all the UI-independent data is stored and retrieved. It consists of both
raw data sources as well as higher-level "repository" and "manager" classes.

Note that any functions exposed by a data layer class that must perform asynchronous work do so by
exposing **suspending functions** that may run inside coroutines while any streaming sources of
data are handled by exposing Flows.

### Data Sources

The lowest level of the data layer are the "data source" classes. These are the raw sources of data
that include data persisted on disk (SharedPreferences, Room, files), data retrieved from network
requests using Retrofit, and any other external source of data.

Note that these data sources are constructed in a manner that adheres to a very important principle
of the app: **that function calls should not throw exceptions**. In the case of data sources, this
tends to mean that suspending functions like those representing network requests should return a
`Result` type. This is an important responsibility of the data layer as a wrapper around other third
party libraries, as dependencies like Retrofit tend to throw exceptions to indicate errors instead.

A minimal data source consists of an interface, an `...Impl` implementation, and a Hilt module that
provides the interface, for example:

- `data/src/main/kotlin/com/example/myapplication/data/datasource/disk/UserPreferencesDiskSource.kt`
- `data/src/main/kotlin/com/example/myapplication/data/datasource/disk/UserPreferencesDiskSourceImpl.kt`
- `data/src/main/kotlin/com/example/myapplication/data/datasource/disk/di/UserPreferencesDiskSourceModule.kt`

### Managers

Manager classes represent something of a middle level of the data layer. While some manager classes
depend on the lower-level data sources, others are wrappers around OS-level classes while others
have no dependencies at all. The commonality amongst the manager classes is that they tend to have
a single discrete responsibility.

A minimal manager lives in `core`:

- `core/src/main/kotlin/com/example/myapplication/core/data/manager/dispatcher/DispatcherManager.kt`
- `core/src/main/kotlin/com/example/myapplication/core/data/manager/di/DispatcherManagerModule.kt`

### Repositories

Repository classes represent the outermost level of the data layer. They can take data sources,
managers, and in rare cases even other repositories as dependencies and are meant to be exposed
directly to the UI layer. They synthesize data from multiple sources and combine various
asynchronous requests as necessary in order to expose data to the UI layer in a more appropriate
form.

Repository classes also feature functions that do not throw exceptions, but unlike the lower levels
of the data layer the `Result` type should be avoided in favor of custom sealed classes that
represent the various success/error cases in a more processed form.

A minimal repository lives in `data`:

- `data/src/main/kotlin/com/example/myapplication/data/repository/userpreferences/UserPreferencesRepository.kt`
- `data/src/main/kotlin/com/example/myapplication/data/repository/di/UserPreferencesRepositoryModule.kt`

### Note on Dependency Injection

Nearly all classes in the data layer consist of interfaces representing exposed behavior and a
corresponding `...Impl` class implementing that interface. All `...Impl` classes are intended to be
manually constructed while their associated interfaces are provided for dependency injection via a
Hilt Module. This prevents the `...Impl` classes from being injected by accident and allows the
interfaces to be easily mocked/faked in tests.

## UI Layer

The UI layer adheres to the concept of unidirectional data flow and makes use of the MVVM design
pattern. Both concepts are in line what Google currently recommends as the best approach for
building the UI-layer of a modern Android application. The MVVM implementation is built around the
Android ViewModel class and the UI itself is constructed using Jetpack Compose, a declarative UI
framework specifically built around the unidirectional data flow approach.

Each screen in the app is associated with at least the following three classes/files:

- A `...ViewModel` class responsible for managing the data and state for the screen.
- A `...Screen` class that contains the Compose implementation of the UI.
- A `...Navigation` file containing the details for how to add the screen to the overall navigation
  graph and how navigate to it within the graph.

### ViewModels / MVVM

The app's approach to MVVM is based around the handling of "state", "actions", and "events" and is
encoded in the `BaseViewModel` class
(`ui/src/main/kotlin/com/example/myapplication/ui/platform/base/BaseViewModel.kt`).

- **State:** The "state" represents the complete internal and external **total state** of the
  ViewModel (VM). Any and all information needed to configure the UI that is associated with the VM
  should be included in this state and is exposed via the `BaseViewModel.stateFlow` property as
  `StateFlow<S>` (where `S` represents the unique type of state associated with the VM).

  There should be no additional `StateFlow` exposed from a VM that would represent some other kind
  of state; all state should be represented by `S`. Additionally, any internal state not directly
  needed by the UI but which influences the behavior of the VM should be included as well in order
  to keep all state managed by the VM in a single place.

- **Actions:** The "actions" represent interactions with the VM in some way that could potentially
  cause an update to that total state. These can be external actions coming from the user's
  interaction with the UI, like click events, or internal actions coming from some asynchronous
  process internal to the VM itself, like the result of some suspending functions. Actions are sent
  by interacting directly with `BaseViewModel.actionChannel` or by using the
  `BaseViewModel.sendAction` and `BaseViewModel.trySendAction` helpers. All actions are then
  processed synchronously in a queue in the `handleAction` function.

  It is worth emphasizing that state should never be updated inside a coroutine in a VM; all
  asynchronous work that results in a state update should do so by posting an internal action. This
  ensures that the only place that state changes can occur is synchronously inside the
  `handleAction` function. This makes the process of finding and reasoning about state changes
  easier and simplifies debugging.

- **Events:** The "events" represent discrete, one-shot side-effects and are typically associated
  with navigation events triggered by some user action. They are sent internally using
  `BaseViewModel.sendEvent` and may be consumed by the UI layer via the `BaseViewModel.eventFlow`
  property. An `EventsEffect`
  (`ui/src/main/kotlin/com/example/myapplication/ui/platform/base/util/EventsEffect.kt`) should
  typically be used to simplify the consumption of these events.

VMs are typically injected using Dagger Hilt by annotating them with `@HiltViewModel`. This allows
them to be constructed, retrieved, and cached in the Compose UI layer by calling `hiltViewModel()`
with the appropriate type. Dependencies passed into the VM constructor will typically be singletons
of the graph, such as `UserPreferencesRepository`.

### Example

The following files demonstrate these principles and best practices when using `BaseViewModel` to
implement a VM:

- `app/src/main/kotlin/com/example/myapplication/ui/home/feature/home/HomeViewModel.kt`
- `app/src/main/kotlin/com/example/myapplication/ui/home/feature/home/HomeScreen.kt`
- `app/src/test/kotlin/com/example/myapplication/ui/home/feature/home/HomeViewModelTest.kt`

### State Hoisting

State should be hoisted as high as is necessary to perform any relevant logic and no higher.

Any UI state that will not be used by logic inside the VM should remain in the UI layer. For
example, visibility toggle states for password inputs should typically remain out of the VM.

## Navigation

Navigation within the UI layer is split between two different strategies:

- **State-based navigation** is used at the root level of the app. The `RootNavViewModel`
  (`app/src/main/kotlin/com/example/myapplication/ui/platform/feature/rootnav/RootNavViewModel.kt`)
  exposes a sealed `RootNavState` that describes which top-level screen should currently be shown,
  and the `RootNavScreen` reacts to that state.

- **Event-based navigation** is used for everything below the root level. A screen emits a one-shot
  navigation event via its `...Event` type, and the associated `...Screen` consumes it through
  `EventsEffect` before delegating to the `NavController`.

Each destination is added to the navigation graph through its own `...Navigation.kt` file using
type-safe routes, and the graph itself is declared in
`app/src/main/kotlin/com/example/myapplication/ui/platform/navigation/MyApplicationNavHost.kt`.
