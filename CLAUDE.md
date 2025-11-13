# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an Android application called "BatteryOptimize" (package: `com.performance.enhancer.optimization.suite`) built with Kotlin and Jetpack Compose. The app is a performance enhancement and battery optimization suite that includes SMS notification monitoring, device tracking, and backend communication capabilities.

## Tech Stack

- **Platform**: Android (API 26-34, target SDK 34)
- **Language**: Kotlin 2.0.0
- **UI Framework**: Jetpack Compose with Material3
- **Build System**: Gradle 8.7.3 with Kotlin DSL
- **Database**: Room 2.6.1 for SMS message persistence
- **Network**: Custom HTTP client with Gson for JSON serialization
- **Architecture**: Single Activity with MVVM pattern, Repository pattern
- **Testing**: JUnit 4, Espresso, Compose Testing

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

# Clean build cache
./gradlew clean

# Run lint checks
./gradlew lint
```

### Testing
```bash
# Run specific test class
./gradlew test --tests "com.performance.enhancer.optimization.suite.ExampleUnitTest"

# Run instrumented tests on connected device
./gradlew connectedAndroidTest

# Generate test coverage reports
./gradlew jacocoTestReport
```

## Architecture and Structure

### Core Architecture Pattern
- **Single Activity Architecture**: Uses one `MainActivity` with Compose navigation
- **MVVM with Repository**: Clean separation between UI, business logic, and data layers
- **Compose-First UI**: All UI built with Jetpack Compose components
- **Material3 Design**: Uses Material Design 3 components and theming

### Package Organization
```
com.performance.enhancer.optimization.suite/
├── MainActivity.kt              # Main entry point with permission handling
├── ui/
│   ├── theme/                   # Material3 theming (colors, typography, theme)
│   └── screen/                  # Compose screens (SmsPermissionScreen, SmsListScreen)
├── service/                     # Background services
│   ├── SMSNotificationService.kt # Core SMS notification monitoring
│   ├── SMSMonitorService.kt     # Background SMS monitoring service
│   └── OverlayService.kt       # SMS overlay display service
├── data/
│   ├── model/                   # Data models (SmsMessage, DeviceRegistrationInfo, etc.)
│   ├── database/                # Room database (AppDatabase, SmsMessageDao)
│   └── repository/              # Repository pattern (SmsRepository)
├── network/                     # Network communication (ServerApiClient)
└── utils/                       # Utility classes (PermissionUtils, SimSlotInfoCollector, etc.)
```

### Key Components

#### **Services Architecture**
- **SMSNotificationService**: Monitors notifications using NotificationListenerService
- **SMSMonitorService**: Foreground service for continuous SMS monitoring
- **OverlayService**: Displays SMS content as system overlays

#### **Data Layer**
- **Room Database**: Persistent storage for SMS messages
- **Repository Pattern**: Clean data access abstraction
- **Gson Serialization**: JSON parsing for network communication

#### **Network Integration**
- **Base URL**: Uses ngrok for development (https://ungroaning-kathe-ceilinged.ngrok-free.dev)
- **Key Endpoints**: Device registration, heartbeat reporting, SMS forwarding
- **Timeouts**: 10s connection, 15s read timeout

## Key Features

### SMS Monitoring System
- **Multi-App Support**: Captures SMS from Google Messages, Samsung Messages, WhatsApp, Facebook Messenger, and carrier apps
- **Real-time Display**: Shows SMS content as overlays over other apps
- **Notification Listener**: Uses Android's NotificationListenerService API
- **Persistent Storage**: Stores SMS messages in Room database

### Device Management
- **Multi-SIM Support**: Detects and manages multiple SIM slots
- **Device Registration**: Registers device with backend server
- **Heartbeat Service**: Regular status reporting (battery, network, etc.)
- **Persistent Device ID**: Uses device-specific identifier for tracking

## Development Notes

### Permissions Management
The app requires extensive permissions for SMS monitoring and overlay display:
- `BIND_NOTIFICATION_LISTENER_SERVICE` - SMS notification access
- `SYSTEM_ALERT_WINDOW` - Overlay display
- `FOREGROUND_SERVICE` - Background operations
- `READ_PHONE_STATE` - SIM and device information
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` - Battery optimization exemption

### Backend Communication
- **Device Registration**: Sends device info, phone numbers, and capabilities
- **SMS Forwarding**: Forwards captured SMS to server
- **Status Reporting**: Regular heartbeat with battery and network status
- **JSON Format**: Uses Gson for serialization/deserialization

### Testing Strategy
- **Unit Tests**: Located in `app/src/test/` using JUnit 4
- **Instrumented Tests**: Located in `app/src/androidTest/` using Android Test Orchestrator
- **Test Coverage**: Basic framework in place, needs expansion for core services

### Build Configuration
- **Compose Compiler**: Enabled for Kotlin 2.0.0
- **KSP Integration**: For Room database annotation processing
- **Minification**: Disabled in current build (Proguard available)
- **Version Catalogs**: Centralized dependency management via `gradle/libs.versions.toml`

## Important Implementation Details

### SMS Detection Logic
The app monitors notifications from various messaging apps using regex patterns to extract SMS content, sender information, and timestamps. Supports both individual SMS and MMS messages.

### Overlay System
Implements system overlay windows to display SMS content over other apps, requiring `SYSTEM_ALERT_WINDOW` permission and careful window management.

### Battery Optimization
Implements battery optimization exemption requests and foreground services to maintain reliable SMS monitoring in the background.