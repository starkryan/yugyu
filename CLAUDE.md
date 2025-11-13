# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an Android application called "BatteryOptimize" (package: `com.performance.enhancer.optimization.suite`) built with Kotlin and Jetpack Compose. The app appears to be a performance enhancement and battery optimization suite for Android devices.

## Tech Stack

- **Platform**: Android (API 26-34, target SDK 34)
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material3
- **Build System**: Gradle with Kotlin DSL
- **Testing**: JUnit 4, Espresso, Compose Testing
- **Architecture**: Single Activity with Compose UI

## Common Commands

### Build and Run
```bash
# Build the app
./gradlew build

# Install debug build to connected device/emulator
./gradlew installDebug

# Run all unit tests
./gradlew test

# Run all instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Generate release build
./gradlew assembleRelease
```

### Development Tasks
```bash
# Clean build cache
./gradlew clean

# Run lint checks
./gradlew lint

# Generate test coverage reports
./gradlew jacocoTestReport
```

## Architecture and Structure

### Package Organization
- `com.performance.enhancer.optimization.suite` - Root package
- `MainActivity.kt` - Single entry point activity using Compose
- `ui/theme/` - Material3 theme definitions (Color.kt, Theme.kt, Type.kt)

### Key Architecture Patterns
- **Single Activity Architecture**: Uses one `MainActivity` with Compose navigation
- **Compose-First UI**: All UI built with Jetpack Compose components
- **Material3 Design**: Uses Material Design 3 components and theming
- **Component-based UI**: UI components organized as composables

### Project Structure
```
app/src/
├── main/java/com/performance/enhancer/optimization/suite/
│   ├── MainActivity.kt              # Main activity and entry point
│   └── ui/
│       └── theme/                   # Material3 theming (colors, typography, theme)
├── test/                           # Unit tests (JUnit)
└── androidTest/                    # Instrumented tests (Espresso, Compose testing)
```

## Testing Strategy

### Unit Tests
- Located in `app/src/test/java/com/performance/enhancer/optimization/suite/`
- Use JUnit 4 framework
- Example: `ExampleUnitTest.kt` demonstrates basic testing patterns

### Instrumented Tests
- Located in `app/src/androidTest/java/com/performance/enhancer/optimization/suite/`
- Use Android Test Orchestrator with Espresso and Compose Testing
- Example: `ExampleInstrumentedTest.kt` shows context validation

## Development Notes

### Dependencies Management
- Uses Gradle version catalogs (`gradle/libs.versions.toml`) for centralized dependency management
- Compose BOM (Bill of Materials) ensures compatible Compose library versions
- All dependencies are declared with version references for consistency

### Build Configuration
- Minimum SDK: 26 (Android 8.0)
- Target SDK: 34 (Android 14)
- Java 11 compatibility
- Compose compiler enabled for Kotlin 2.0.0

### Development Environment
- This is an Android Studio project
- Requires Android SDK and build tools
- Uses Gradle wrapper for consistent builds across environments
- KSP (Kotlin Symbol Processing) may be used for annotation processing