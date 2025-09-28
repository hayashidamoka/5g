# Project Configuration

## Language Settings
**For Japanese documentation and communication**: See [CLAUDE.ja.md](CLAUDE.ja.md)
**Default communication language**: English

## Project Overview
- **App Name**: 5G (com.teampansaru.fiveg)
- **Type**: Android Widget Application (Dancing Old Man Widget)
- **Main Features**: Network status monitoring, Widget display

## Development Environment
- **Android Studio Project**
- **Language**: Kotlin
- **Min SDK Version**: 30 (Android 11)
- **Target SDK**: 36 (Android 15)
- **Compile SDK**: 36

## Build Configuration
- **Gradle Version**: 8.14.3
- **Kotlin Version**: 2.2.0

## Main Components
- `DancingOldmanWidget`: Widget Provider
- `NetworkService`: Foreground Service
- `MainActivity`: Main Activity
- `WalkThroughFragment`: Tutorial Screen
- `CustomAdapter`: Custom Adapter

## Required Permissions
- ACCESS_NETWORK_STATE (Check network status)
- READ_PHONE_STATE (Read phone state)
- FOREGROUND_SERVICE (Foreground service)

## Coding Conventions
- Follow standard Kotlin naming conventions
- Package name: com.teampansaru.fiveg

## Important Notes
- Widget updates use AppWidgetManager
- Network monitoring runs in foreground service
- Android 11 or higher required (minSdkVersion 30)