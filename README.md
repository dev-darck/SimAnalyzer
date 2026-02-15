<div align="center">
  <img src="app-icons/app-readme.png" alt="SimAnalyzer logo" width="120"/>

# SimAnalyzer

Telemetry-first desktop app for sim racing.  
Built with **Kotlin Multiplatform + Compose Desktop**.

<p>
  <a href="https://kotlinlang.org/docs/multiplatform.html"><img alt="Kotlin Multiplatform" src="https://img.shields.io/badge/Kotlin-Multiplatform-blue.svg?style=for-the-badge&logo=kotlin"/></a>
  <a href="https://www.jetbrains.com/compose-multiplatform/"><img alt="Compose Multiplatform" src="https://img.shields.io/badge/Compose-Multiplatform-brightgreen.svg?style=for-the-badge&logo=jetpackcompose"/></a>
  <a href="https://opensource.org/licenses/Apache-2.0"><img alt="License" src="https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=for-the-badge"/></a>
</p>
</div>

[![Compose Desktop (Windows)](https://github.com/dev-darck/SimAnalyzer/actions/workflows/compose-desktop-windows.yml/badge.svg?event=workflow_dispatch)](https://github.com/dev-darck/SimAnalyzer/actions/workflows/compose-desktop-windows.yml)

> [!IMPORTANT]
> Project status: **active development**.  
> Platform focus: **Desktop (Windows-first runtime)**.

## What SimAnalyzer is

SimAnalyzer reads telemetry from racing simulators, shows live HUD panels, records sessions, and provides post-session
analysis.

Planned product scope (TODO - fill later):

- TODO: Live telemetry HUD capabilities
- TODO: Session recording capabilities
- TODO: Post-session analytics capabilities
- TODO: Supported simulators and integrations

## Start here (priority order)

1. Run the app

```bash
./gradlew :composeApp:run
```

2. Understand module boundaries (`api` vs `impl`)

3. Understand build conventions (`moduleApi`, `moduleImpl`, `app`)

4. Only then deep-dive into feature modules

## Requirements

- JDK 21
- Gradle Wrapper (`./gradlew`)

## Quick setup

```bash
git clone https://github.com/dev-darck/SimAnalyzer.git
cd SimAnalyzer
./gradlew :composeApp:run
```

## Module map

Top-level module groups:

- `:composeApp` - desktop app entry point
- `:core:*` - shared app infrastructure (DI, navigation, telemetry, theme, utils)
- `:feature:*` - screens and HUD features
- `:games:*` - game-specific integrations (AC, LMU)
- `:ksp:*` - custom code generation tooling
- `build-logic` - Gradle conventions + project DSL

Most domains follow this split:

- `*/api` - contracts and stable surface
- `*/impl` - concrete implementation

Examples:

- `:core:di:api` + `:core:di:impl`
- `:games:game:api` + `:games:game:impl`
- `:feature:screens:session:api` + `:feature:screens:session:impl`

## moduleApi vs moduleImpl

The repo uses DSL helpers from `build-logic`:

- `moduleApi { ... }`
- `moduleImpl { ... }`
- `app { ... }`

### `moduleApi`

- KMP library conventions
- **Kotlin explicit API enabled**
- used for public contracts

### `moduleImpl`

- same KMP base conventions
- explicit API not enforced
- used for implementation details

Rule of thumb: put interfaces/models in `api`; put behavior/wiring in `impl`.

## Dependency DSL shortcuts

Inside module DSL, dependencies are source-set aware:

- `commonImpl`
- `jvmImpl`
- `jvmTest`
- `commonTestImpl`

Example:

```kotlin
dependencies {
    projects.core.di.api.jvmImpl
    projects.core.di.impl.jvmImpl
    lib.metro.runtime.jvmImpl
}
```

## DI style

- bindings live in `di` packages
- ViewModels are multibound via `@IntoMap` + `@ViewModelKey`
- prefer clean module boundaries over direct cross-feature coupling

## Build logic note

`build-logic/conventions/src/main/kotlin/convention-project-dsl.gradle.kts` can be intentionally empty.  
It acts as a marker precompiled script plugin that exposes the DSL across all modules.

## License

Apache License 2.0
